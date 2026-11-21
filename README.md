# Post-contenido — Unidad 6: Diagnóstico y Refactorización de Antipatrones de Diseño

**Curso:** Patrones de Diseño de Software (Código: 1156701-A)  
**Semestre:** Sexto Semestre  
**Estudiante:** Brayan Smith Albarracín Tarazona  
**Institución:** Universidad Francisco de Paula Santander (UFPS)  

---

## 1. Descripción General del Proyecto

Este repositorio documenta el diagnóstico sistemático y la refactorización guiada de antipatrones de diseño en el backend de comercio electrónico de gestión de pedidos (`pedidos-service`), implementado con **Java 17**, **Spring Boot**, **Spring JDBC (JdbcTemplate)** y base de datos en memoria **H2**.

El proyecto se desarrolla en dos partes estrechamente conectadas:
1. **Parte 1:** Diagnóstico riguroso y refactorización del componente inicial `GestorPedidos`, el cual combina los antipatrones *God Object* y *Spaghetti Code*.
2. **Parte 2:** Diagnóstico y corrección del antipatrón *Golden Hammer*, introducido durante la evolución del sistema al incorporar tres campañas de descuento promocional.

---

## 2. Diagnóstico de Antipatrones — Parte 1: GestorPedidos

### 2.1 Antipatrones Identificados con Evidencia Concreta de Código

Se identificó la presencia simultánea de dos antipatrones clásicos de la arquitectura orientada a objetos: **God Object (The Blob)** y **Spaghetti Code**.

#### Evidencia 1: Múltiples Responsabilidades Mezcladas (God Object)
La clase `GestorPedidos` y específicamente su método `procesarPedido(PedidoRequest request)` viola frontalmente el **Principio de Responsabilidad Única (SRP)** al asumir **seis (6) responsabilidades completamente dispares**:
1. **Validación de stock con acceso a base de datos (Líneas 28-44):** Itera sobre los ítems del pedido ejecutando consultas directas `SELECT stock FROM inventario WHERE producto_id = ?` y decidiendo el rechazo del pedido.
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
En una única secuencia lineal de código coexisten:
- Instrucciones SQL nativas embebidas (`CALL IDENTITY()`, `SELECT stock...`).
- Reglas de negocio institucional (descuentos por volumen/frecuencia y cortes horarios).
- Lógica de presentación y formato textual (`cuerpo.append("Estimado cliente...").append((int)(descuento * 100))`).
- Manejo de transporte y excepciones de red (`try { emailService.enviar(...) } catch (Exception e)`).

#### Evidencia 4: Rigidez ante Nuevos Requisitos
Si se requiriera agregar un nuevo tipo de cliente (por ejemplo, `CONVENIO_UNIVERSITARIO`), sería obligatorio modificar directamente el método central `procesarPedido`, agregando otro bloque `else if` en el cálculo de descuento (afectando líneas 82-102), arriesgando la estabilidad del cálculo de impuestos, persistencia y notificaciones, lo que viola abiertamente el **Principio Abierto/Cerrado (OCP)**.

---

## 3. Plan de Refactorización — Patrones Seleccionados

1. **Chain of Responsibility (Cadena de Responsabilidad) para Validaciones:**
   - Separar cada regla de rechazo en eslabones independientes (`ValidadorStock`, `ValidadorCliente`).
   - Justificación: Existe una dependencia de orden estricta y necesidad de corte anticipado (fail-fast).
2. **Strategy (Estrategia) para Cálculo de Descuentos:**
   - Separar cada política en clases dedicadas (`DescuentoVip`, `DescuentoFrecuente`, `DescuentoEstandar`) coordinadas por `SelectorEstrategiaDescuento`.
   - Justificación: No hay dependencia de orden; cada tipo de cliente aplica exactamente un algoritmo independiente.
3. **Repository Pattern:**
   - Encapsular todas las sentencias SQL en `PedidoRepository`.
4. **Servicio Cohesivo de Notificación:**
   - Aislar la construcción del mensaje y envío en `NotificacionPedidoService`.
5. **GestorPedidos como Orquestador Delgado:**
   - Coordinar el flujo en menos de 20 líneas de código limpio.
