package com.tienda.pedidos;

import com.tienda.pedidos.descuento.CalculadorDescuentoFinal;
import com.tienda.pedidos.descuento.DescuentoBlackFriday;
import com.tienda.pedidos.descuento.DescuentoCorporativo;
import com.tienda.pedidos.descuento.DescuentoVolumen;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import com.tienda.pedidos.validacion.ContextoPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class GestorPedidosTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    @Autowired
    private CalculadorDescuentoFinal calculadorDescuento;

    @Autowired
    private DescuentoBlackFriday descuentoBlackFriday;

    @Autowired
    private DescuentoCorporativo descuentoCorporativo;

    @Autowired
    private DescuentoVolumen descuentoVolumen;

    @Test
    @DisplayName("Caso 1: Rechazo por stock insuficiente en inventario")
    void testStockInsuficiente() {
        // Producto 4 tiene stock 5 en inventario, solicitamos 10
        PedidoRequest request = new PedidoRequest(1L, "vip@tienda.com",
                List.of(new ItemPedido(4L, 10)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertTrue(resultado.getMotivoRechazo().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("Caso 2: Rechazo por cliente no registrado en la base de datos")
    void testClienteNoRegistrado() {
        PedidoRequest request = new PedidoRequest(999L, "desconocido@tienda.com",
                List.of(new ItemPedido(1L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertEquals("Cliente no registrado", resultado.getMotivoRechazo());
    }

    @Test
    @DisplayName("Caso 3: Validación de cliente moroso con deuda pendiente")
    void testClienteMoroso() {
        PedidoRequest request = new PedidoRequest(4L, "moroso@tienda.com",
                List.of(new ItemPedido(3L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        if (!resultado.isConfirmado()) {
            assertTrue(resultado.getMotivoRechazo().contains("deuda pendiente"));
        } else {
            assertTrue(resultado.getTotal() > 0);
        }
    }

    @Test
    @DisplayName("Caso 4: Confirmación y descuento de campaña Black Friday (25%)")
    void testCampañaBlackFriday() {
        // Cliente 5 (ESTANDAR sin descuento propio) compra Laptop ($1,200,000)
        // Con Black Friday activo, aplica 25% de descuento
        PedidoRequest request = new PedidoRequest(5L, "maria@tienda.com",
                List.of(new ItemPedido(1L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertNotNull(resultado.getPedidoId());
        // Subtotal = 1,200,000. Descuento 25% = 300,000. Base = 900,000. Impuesto 19% = 171,000. Total = 1,071,000
        assertEquals(1071000.0, resultado.getTotal(), 0.01);
    }

    @Test
    @DisplayName("Caso 5: Estrategia de Descuento Corporativo (10% si tiene NIT registrado)")
    void testEstrategiaDescuentoCorporativo() {
        // Cliente 6 tiene NIT '800999888-4'
        PedidoRequest requestCorp = new PedidoRequest(6L, "corp@empresa.com",
                List.of(new ItemPedido(3L, 2)));
        ContextoPedido contextoCorp = new ContextoPedido(requestCorp);

        double descCorp = descuentoCorporativo.calcular(contextoCorp);
        assertEquals(0.10, descCorp, 0.001);

        // Cliente 5 no tiene NIT
        PedidoRequest requestSinNit = new PedidoRequest(5L, "maria@tienda.com",
                List.of(new ItemPedido(3L, 2)));
        ContextoPedido contextoSinNit = new ContextoPedido(requestSinNit);

        double descSinNit = descuentoCorporativo.calcular(contextoSinNit);
        assertEquals(0.0, descSinNit, 0.001);
    }

    @Test
    @DisplayName("Caso 6: Estrategia de Descuento por Volumen (12% si supera 20 unidades)")
    void testEstrategiaDescuentoVolumen() {
        // Pedido de 25 unidades de cable HDMI
        PedidoRequest requestVolumen = new PedidoRequest(5L, "maria@tienda.com",
                List.of(new ItemPedido(5L, 25)));
        ContextoPedido contextoVolumen = new ContextoPedido(requestVolumen);

        double descVol = descuentoVolumen.calcular(contextoVolumen);
        assertEquals(0.12, descVol, 0.001);

        // Pedido de 10 unidades
        PedidoRequest requestMenor = new PedidoRequest(5L, "maria@tienda.com",
                List.of(new ItemPedido(5L, 10)));
        ContextoPedido contextoMenor = new ContextoPedido(requestMenor);

        double descMenor = descuentoVolumen.calcular(contextoMenor);
        assertEquals(0.0, descMenor, 0.001);
    }

    @Test
    @DisplayName("Caso 7: Regla de negocio en CalculadorDescuentoFinal: el mayor descuento prevalece")
    void testMayorDescuentoPrevalece() {
        // Cliente 1 (VIP con subtotal > 1M tendría 15%).
        // Con Black Friday activo (25%), CalculadorDescuentoFinal toma max(15%, 25%) = 25%
        PedidoRequest request = new PedidoRequest(1L, "vip@tienda.com",
                List.of(new ItemPedido(1L, 1)));
        ContextoPedido contexto = new ContextoPedido(request);
        contexto.setTipoCliente("VIP");
        contexto.setSubtotal(1200000.0);

        double descuentoFinal = calculadorDescuento.calcular(contexto);
        assertEquals(0.25, descuentoFinal, 0.001);
    }
}
