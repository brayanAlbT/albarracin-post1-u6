# Post-contenido — Unidad 6: Diagnóstico y Refactorización de Antipatrones de Diseño

**Curso:** Patrones de Diseño de Software (Código: 1156701-A)  
**Semestre:** Sexto Semestre  
**Estudiante:** Brayan Smith Albarracín Tarazona  
**Institución:** Universidad Francisco de Paula Santander (UFPS)  

---

## 1. Descripción del Proyecto

Este repositorio contiene la solución integral para el post-contenido de la **Unidad 6: Diagnóstico y Refactorización de Antipatrones de Diseño**, desarrollado sobre un único proyecto Spring Boot (`pedidos-service`) en la raíz del repositorio.

El sistema simula el backend de procesamiento de pedidos de un comercio electrónico universitario, implementando acceso a datos mediante **Spring JDBC (JdbcTemplate)** y base de datos relacional en memoria **H2**. La actividad se estructura en dos fases complementarias:
1. **Parte 1 — Diagnóstico y Refactorización de GestorPedidos:** Análisis con evidencia concreta del código fuente de un *God Object* con *Spaghetti Code*, refactorizándolo mediante capas cohesivas aplicando **Chain of Responsibility**, **Strategy** y el patrón **Repository**.
2. **Parte 2 — Crecimiento del Sistema y Diagnóstico de un Segundo Antipatrón:** Detección y corrección del antipatrón **Golden Hammer** introducido tras agregar tres nuevas campañas de descuento promocional a la cadena de validación, reestructurándolas bajo el patrón **Strategy** con un **CalculadorDescuentoFinal** y purgando el código obsoleto para prevenir un **Lava Flow**.

---

## 2. Decisiones de Diseño

### 2.1 Parte 1 — GestorPedidos

#### Antipatrón Identificado: God Object y Spaghetti Code Combinados
El componente original `GestorPedidos.procesarPedido(PedidoRequest request)` concentraba en un único método procedimental de más de 120 líneas (y dentro de una clase con 6 métodos auxiliares que sumaban 340 líneas) **seis (6) responsabilidades totalmente heterogéneas**, violando el Principio de Responsabilidad Única (SRP):
- **Validación de stock (Líneas 28-44):** Consultas SQL directas sobre la tabla `inventario` iterando ítem por ítem.
- **Validación de cliente y mora con restricción horaria (Líneas 46-70):** Consultas SQL sobre `clientes` y `facturas`, evaluando condicionalmente `LocalTime.now().isBefore(LocalTime.of(20, 0))`.
- **Cálculo de subtotal (Líneas 72-80):** Consultas SQL iterativas sobre la tabla `productos`.
- **Cálculo de descuentos anidados (Líneas 82-102):** Hasta 3 niveles de bifurcación condicional (`if (tipoCliente.equals("VIP"))` -> `if (subtotal > 1_000_000)`...) y consultas embebidas a `pedidos`.
- **Persistencia transaccional directa vía JDBC (Líneas 106-126):** Inserciones directas en `pedidos`, llamada a la función propietaria `CALL IDENTITY()`, inserciones en `detalle_pedido` y actualizaciones directas de decremento de inventario sin capa de repositorio ni demarcación `@Transactional`.
- **Construcción y envío de notificaciones (Líneas 128-147):** Construcción artesanal de cuerpo de correo con `StringBuilder` y llamada con manejo de excepción a `EmailService`.

#### Patrones Aplicados y Justificación
1. **Chain of Responsibility para Validaciones (`ValidadorPedido`, `ValidadorStock`, `ValidadorCliente`):**
   - *Justificación:* Las validaciones de negocio presentan una **dependencia estricta de orden** y un requerimiento crítico de **corte anticipado (fail-fast)**. Si un producto carece de stock suficiente, es ineficiente y semánticamente incorrecto consultar en la base de datos la mora del cliente.
   - *Alternativa descartada:* Una colección de métodos booleanos o una lista `List<Predicate<ContextoPedido>>`. Se descartó porque evalúa todos los predicados sin permitir que el primer eslabón aborte la secuencia de inmediato, obligando a introducir comprobaciones repetitivas de estado.
2. **Strategy para el Cálculo de Descuentos (`EstrategiaDescuento`, `DescuentoVip`, `DescuentoFrecuente`, `DescuentoEstandar`, `SelectorEstrategiaDescuento`):**
   - *Justificación:* A diferencia de las validaciones, los descuentos no tienen una dependencia secuencial ni requieren cortar la ejecución: se aplica exactamente una regla algorítmica determinada por el tipo de cliente. Encapsular cada regla en una clase Strategy desacopla la adición de futuros tipos de cliente (cumpliendo OCP).
   - *Alternativa descartada:* Incorporar el cálculo de descuentos como un eslabón adicional en la cadena de validación. Se descartó porque la cadena modela control de flujo binario (aceptar o rechazar), no computación de valores numéricos de negocio.
3. **Repository Pattern (`PedidoRepository`):**
   - *Justificación:* Aísla todas las operaciones SQL nativas (`INSERT`, `UPDATE`, `CALL IDENTITY()`) de la lógica del dominio.
4. **Notificación Cohesiva (`NotificacionPedidoService`):**
   - *Justificación:* Encapsula el formato y la integración con el servicio de correo electrónico.
5. **Orquestador Delgado (`GestorPedidos`):**
   - Se reduce a coordinar las cuatro capas en menos de 25 líneas limpias y legibles.

---

### 2.2 Parte 2 — Crecimiento del Proyecto

#### Antipatrón Identificado: Golden Hammer (El Martillo Dorado)
Al solicitarse tres nuevas campañas promocionales (**Black Friday**, **Corporativo**, **Volumen**), se cometió el error clásico de diseño de reutilizar la solución exitosa de la Parte 1 (**Chain of Responsibility**) donde la forma del problema no correspondía:
- **Evidencia 1: Ausencia de orden y corte:** Las clases `PromocionBlackFriday`, `PromocionCorporativo` y `PromocionVolumen` no rechazan solicitudes ni tienen una secuencia requerida (evaluar volumen antes o después de Black Friday produce idéntico resultado).
- **Evidencia 2: Violación semántica del contrato:** La clase base `ValidadorPedido` define el contrato para validar y rechazar. Los tres eslabones de campaña jamás rechazaban, utilizándose de forma inapropiada únicamente porque *"los eslabones ya sabían cómo conectarse entre sí"*.
- **Evidencia 3: Acoplamiento por estado mutable compartido:** Se contaminó `ContextoPedido` con un campo mutable `descuentoCampana` sobre el cual los eslabones competían por efectos colaterales (`if (valor > this.descuentoCampana) ...`).

#### Patrón Aplicado y Justificación
- **Patrón Strategy Unificado + `CalculadorDescuentoFinal`:**
  - Las tres campañas se modelaron como estrategias independientes (`DescuentoBlackFriday`, `DescuentoCorporativo`, `DescuentoVolumen`) implementando `EstrategiaDescuento`.
  - El componente `CalculadorDescuentoFinal` combina de forma declarativa e inmutable la estrategia por tipo de cliente con la colección de campañas activas, seleccionando el mayor descuento aplicable mediante programación funcional:
    ```java
    double porCampana = campanas.stream()
            .mapToDouble(estrategia -> estrategia.calcular(contexto))
            .max().orElse(0.0);
    return Math.max(porTipoCliente, porCampana);
    ```
- **Eliminación Total de Código Obsoleto (Prevención de Lava Flow):**
  - Se eliminaron físicamente con `git rm` las clases `PromocionBlackFriday`, `PromocionCorporativo`, `PromocionVolumen` y el campo mutable `descuentoCampana` de `ContextoPedido`.
  - *Justificación:* Dejar clases obsoletas comentadas "por si acaso" en el código activo es el origen directo del antipatrón **Lava Flow** (flujo de lava), donde código muerto permanece por miedo a romper dependencias. El historial de Git es el repositorio legítimo para rastrear la evolución histórica.

---

## 3. Comparación de Comportamiento: Antes vs. Después

| Escenario de Prueba | Comportamiento en GestorPedidos Original | Comportamiento en Versión Refactorizada (Final) | Resultado Observable |
| :--- | :--- | :--- | :--- |
| **Stock insuficiente (Prod 4: stock 5, pide 10)** | Rechaza en el bucle JDBC (`Stock insuficiente: producto 4`) | `ValidadorStock` aborta la cadena y rechaza anticipadamente | **Idéntico (Rechazado)** |
| **Cliente inexistente (ID 999)** | Rechaza tras consultar cliente (`Cliente no registrado`) | `ValidadorCliente` detecta nulo y rechaza | **Idéntico (Rechazado)** |
| **Cliente moroso (ID 4, deuda $250k)** | Evalúa horario institucional (< 20:00 rechaza) | `ValidadorCliente` evalúa regla temporal y rechaza | **Idéntico (Rechazado)** |
| **Cliente VIP con subtotal > $1,000,000** | Aplica 15% descuento | Con Black Friday activo toma 25% (mayor descuento) | **Equivalente y consistente** |
| **Cliente Corporativo (con NIT)** | No existía en el diseño original | Aplica 10% mediante `DescuentoCorporativo` | **Satisface nuevo requerimiento** |
| **Compra por Volumen (> 20 unidades)** | No existía en el diseño original | Aplica 12% mediante `DescuentoVolumen` | **Satisface nuevo requerimiento** |

---

## 4. Estructura del Repositorio

```text
albarracin-post1-u6/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/tienda/pedidos/
    │   │   ├── PedidosServiceApplication.java
    │   │   ├── dto/
    │   │   │   ├── ItemPedido.java
    │   │   │   ├── PedidoRequest.java
    │   │   │   └── ResultadoPedido.java
    │   │   ├── validacion/
    │   │   │   ├── ContextoPedido.java
    │   │   │   ├── ValidadorPedido.java
    │   │   │   ├── ValidadorStock.java
    │   │   │   └── ValidadorCliente.java
    │   │   ├── descuento/
    │   │   │   ├── EstrategiaDescuento.java
    │   │   │   ├── DescuentoVip.java
    │   │   │   ├── DescuentoFrecuente.java
    │   │   │   ├── DescuentoEstandar.java
    │   │   │   ├── DescuentoBlackFriday.java
    │   │   │   ├── DescuentoCorporativo.java
    │   │   │   ├── DescuentoVolumen.java
    │   │   │   ├── SelectorEstrategiaDescuento.java
    │   │   │   └── CalculadorDescuentoFinal.java
    │   │   └── service/
    │   │       ├── GestorPedidos.java
    │   │       ├── PedidoRepository.java
    │   │       ├── EmailService.java
    │   │       ├── ConsoleEmailService.java
    │   │       └── NotificacionPedidoService.java
    │   └── resources/
    │       ├── application.properties
    │       ├── schema.sql
    │       └── data.sql
    └── test/
        └── java/com/tienda/pedidos/
            └── GestorPedidosTest.java
```

---

## 5. Cómo Ejecutar

### Prerrequisitos
- Java JDK 17 o superior instalado.
- Apache Maven 3.8 o superior.

### Compilación y Ejecución
```bash
# Compilar el proyecto
mvn clean compile

# Ejecutar la suite de pruebas automatizadas
mvn test

# Iniciar la aplicación Spring Boot
mvn spring-boot:run
```

---

## 6. Herramientas Utilizadas

- **Lenguaje:** Java 17 LTS
- **Framework:** Spring Boot 3.2.5 (Spring Framework 6, Spring JDBC, Spring Web)
- **Base de Datos:** H2 Database (embebida en memoria con scripts de inicialización DDL/DML)
- **Construcción:** Apache Maven
- **Control de Versiones:** Git, GitHub
- **Pruebas:** JUnit 5, Spring Boot Test

---

## 7. Conclusiones

1. La refactorización guiada por diagnóstico riguroso demostró que separar un *God Object* procedimental en capas cohesivas mediante patrones GoF (**Chain of Responsibility**, **Strategy** y **Repository**) incrementa sustancialmente la legibilidad, mantenibilidad y cobertura de pruebas unitarias sin alterar el comportamiento observable del sistema.
2. Identificar el antipatrón *Golden Hammer* en la Parte 2 evidenció que ningún patrón es una solución universal: aunque *Chain of Responsibility* resolvió eficazmente las validaciones dependientes de orden y corte, forzar su uso para campañas de descuento no secuenciales violó la coherencia semántica del contrato y degradó el diseño con estado mutable compartido.
3. Modelar las promociones como estrategias polimórficas desacopladas y eliminar de raíz los eslabones mal concebidos (en lugar de comentarlos) evitó la acumulación de deuda técnica por *Lava Flow*, garantizando una arquitectura extensible conforme a los principios SOLID y las mejores prácticas de la ingeniería de software.
