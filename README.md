# Post-contenido — Unidad 6: Diagnóstico y Refactorización de Antipatrones de Diseño

**Curso:** Patrones de Diseño de Software (Código: 1156701-A)  
**Semestre:** Sexto Semestre  
**Estudiante:** Brayan Smith Albarracín Tarazona  
**Institución:** Universidad Francisco de Paula Santander (UFPS)  

---

## 1. Descripción General del Proyecto

Este repositorio documenta el diagnóstico sistemático y la refactorización guiada de antipatrones de diseño en el backend de comercio electrónico de gestión de pedidos (`pedidos-service`), implementado con **Java 17**, **Spring Boot**, **Spring JDBC (JdbcTemplate)** y base de datos en memoria **H2**.

El proyecto se desarrolla en dos partes interconectadas:
1. **Parte 1:** Diagnóstico riguroso y refactorización del componente inicial `GestorPedidos`, el cual combina los antipatrones *God Object* y *Spaghetti Code*.
2. **Parte 2:** Diagnóstico y corrección del antipatrón *Golden Hammer*, introducido durante la evolución del sistema al incorporar tres campañas de descuento promocional.

---

## 2. Decisiones de Diseño — Parte 1: GestorPedidos

### 2.1 Antipatrones Identificados con Evidencia Concreta de Código
Se diagnosticó la presencia simultánea de dos antipatrones: **God Object (The Blob)** y **Spaghetti Code**.

#### Evidencia 1: Múltiples Responsabilidades Mezcladas (God Object)
La clase `GestorPedidos` y específicamente su método `procesarPedido(PedidoRequest request)` viola frontalmente el **Principio de Responsabilidad Única (SRP)** al asumir **seis (6) responsabilidades completamente dispares**:
1. **Validación de stock con acceso a BD (Líneas 28-44):** Itera sobre los ítems del pedido ejecutando consultas directas `SELECT stock FROM inventario WHERE producto_id = ?` y decidiendo el rechazo del pedido.
2. **Validación de cliente y reglas de mora con excepción horaria (Líneas 46-70):** Ejecuta `SELECT tipo_cliente FROM clientes WHERE id = ?` y `SELECT SUM(monto) FROM facturas WHERE cliente_id = ? AND pagada = false`, evaluando reglas temporales complejas (`LocalTime.now().isBefore(LocalTime.of(20, 0))`).
3. **Cálculo de subtotal monetario (Líneas 72-80):** Itera sobre los productos ejecutando consultas `SELECT precio FROM productos WHERE id = ?` y acumulando totales.
4. **Cálculo de políticas de descuento (Líneas 82-102):** Implementa lógica condicional ramificada según el tipo de cliente (`VIP`, `FRECUENTE`) y montos o historial de compras (`SELECT COUNT(*) FROM pedidos`).
5. **Persistencia relacional directa sin abstracción (Líneas 106-126):** Gestiona inserciones SQL en `pedidos`, llamadas nativas a `CALL IDENTITY()`, inserciones en `detalle_pedido` y actualizaciones directas de stock en `inventario` (`UPDATE inventario SET stock = stock - ?`). Todo sin demarcación transaccional (`@Transactional`) ni repositorios dedicados.
6. **Construcción y despacho de notificaciones (Líneas 128-147):** Formatea cadenas de texto con StringBuilder para armar el cuerpo del correo de confirmación y gestiona la llamada e inconsistencias del servicio `emailService`.

Adicionalmente, la clase acumula 6 métodos auxiliares privados históricos (`obtenerHistorialCliente`, `formatearFactura`, `calcularImpuestoRegional`, `reintentarNotificacion`, `purgarPedidosVencidos`, `construirCuerpoCorreo`), totalizando cientos de líneas de código bajo un único bean.

#### Evidencia 2: Anidamiento Profundo y Flujo Enredado (Spaghetti Code)
El método presenta un flujo de control procedimental con múltiples niveles de anidamiento condicional:
- En la validación de mora: 3 niveles de indentación (`if (tipoCliente.equals("MOROSO"))` -> `if (deudaPendiente != null && deudaPendiente > 0)` -> `if (ahora.isBefore(LocalTime.of(20, 0)))`).
- En el cálculo de descuento VIP: 2 a 3 niveles condicionales (`if (tipoCliente.equals("VIP"))` -> `if (subtotal > 1_000_000)` / `else if (subtotal > 500_000)` / `else`).
- En el cálculo de descuento FRECUENTE: 2 niveles condicionales combinados con consulta SQL embebida en medio de la lógica aritmética.

#### Evidencia 3: Mezcla de Niveles de Abstracción
Coexisten en un único método SQL embebido, reglas de negocio de corte de facturación, formateo de texto de correo y captura de fallos de red.

### 2.2 Patrones Aplicados y Alternativas Descartadas en Parte 1
- **Validación — Patrón Chain of Responsibility (`ValidadorPedido`, `ValidadorStock`, `ValidadorCliente`):**
  - *Justificación:* Las validaciones poseen una dependencia de orden real y obligan a un corte anticipado (*fail-fast*). Si el stock falla, no tiene sentido verificar cliente ni deuda.
  - *Alternativa descartada:* Una lista de métodos booleanos o `List<Predicate<ContextoPedido>>`. Se descartó porque evalúa todos los elementos sin garantizar corte anticipado nativo sin acoplamiento adicional.
- **Descuentos — Patrón Strategy (`EstrategiaDescuento`, `DescuentoVip`, `DescuentoFrecuente`, `DescuentoEstandar`, `SelectorEstrategiaDescuento`):**
  - *Justificación:* No hay dependencia de orden. Cada tipo de cliente aplica exactamente un algoritmo independiente.
  - *Alternativa descartada:* Incorporar el descuento como un eslabón más de la cadena de validación. Se descartó porque no requiere cortar el flujo ni posee dependencia de orden; obligaría a introducir banderas artificiales.
- **Persistencia — Patrón Repository (`PedidoRepository`):**
  - *Justificación:* Centraliza las operaciones SQL y desacopla el modelo de datos de la lógica de negocio.
- **Notificación — `NotificacionPedidoService`:**
  - *Justificación:* Aísla la composición de la plantilla y el transporte de mensajería.
- **Orquestador delgado (`GestorPedidos`):**
  - Coordina las 4 capas limpiamente en pocas líneas.

---

## 3. Diagnóstico de Antipatrones — Parte 2: El Sistema Crece

### 3.1 Antipatrón Identificado con Evidencia Concreta de Código: Golden Hammer (Martillo Dorado)

Durante el ciclo de evolución, ante el requerimiento de incorporar tres nuevas campañas promocionales (**Black Friday**, **Corporativo**, **Volumen**), se introdujo el antipatrón **Golden Hammer**.

#### Evidencia 1: Ausencia Total de Dependencia de Orden y Corte Anticipado
En las tres clases añadidas a la cadena (`PromocionBlackFriday`, `PromocionCorporativo`, `PromocionVolumen`):
- Ninguna de ellas valida ni rechaza pedidos: el método `ejecutar(ContextoPedido contexto)` jamás invoca `contexto.rechazar(...)`.
- El orden relativo entre ellas es completamente irrelevante: ejecutar `PromocionVolumen` antes o después de `PromocionCorporativo` produce idéntico resultado numérico.
- No hay corte de flujo. La justificación técnica por la cual se adoptó Chain of Responsibility en la Parte 1 (orden estricto entre stock y cliente con *short-circuit*) **no existe** en las promociones.

#### Evidencia 2: Perversión Semántica del Contrato de la Cadena
La clase base abstracta `ValidadorPedido` tiene por contrato:
> *"Eslabón de la cadena: cada validador decide si el pedido continúa o se rechaza"*.

Al forzar las promociones a heredar de `ValidadorPedido`, se pervierte el propósito y semántica del contrato (violando LSP conceptualmente), convirtiendo una cadena de validación defensiva en un mecanismo espurio de mutación de estado.

#### Evidencia 3: Efectos Secundarios mediante Estado Mutable Compartido
Para soportar este enfoque, se agregó al `ContextoPedido` el campo `descuentoCampana` y el método:
```java
public void aplicarDescuentoCampana(double valor) {
    if (valor > this.descuentoCampana) this.descuentoCampana = valor;
}
```
Esto genera acoplamiento oculto por efectos colaterales sobre un objeto compartido que viaja por la cadena. Si el día de mañana dos campañas necesitaran acumularse (sumarse en lugar de tomar el máximo), la cadena se vuelve ambigua e incontrolable.

#### Evidencia 4: Síndrome del "Martillo Dorado"
Se utilizó `ValidadorPedido` única y exclusivamente porque *"los eslabones ya sabían cómo conectarse entre sí"* y funcionó exitosamente en la Parte 1, aplicando la misma herramienta a un problema con una naturaleza completamente diferente.

---

## 4. Plan de Corrección para Parte 2

1. **Eliminar por completo los eslabones espurios:** Remover `PromocionBlackFriday`, `PromocionCorporativo`, `PromocionVolumen` y el campo `descuentoCampana` de `ContextoPedido` (evitando el antipatrón *Lava Flow* al no dejar código comentado).
2. **Modelar las campañas como `EstrategiaDescuento`:** Crear `DescuentoBlackFriday`, `DescuentoCorporativo` y `DescuentoVolumen` implementando la interfaz común.
3. **Introducir `CalculadorDescuentoFinal`:** Combinar de manera declarativa y funcional la estrategia por tipo de cliente con la lista de campañas activas, aplicando la regla de negocio de mayor descuento sin efectos colaterales mutables.
