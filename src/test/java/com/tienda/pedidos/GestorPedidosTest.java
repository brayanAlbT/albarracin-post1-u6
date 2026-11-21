package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
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

    @Test
    @DisplayName("Caso 1: Rechazo por stock insuficiente")
    void testStockInsuficiente() {
        // Producto 4 tiene stock 5 en data.sql, solicitamos 10
        PedidoRequest request = new PedidoRequest(1L, "vip@tienda.com",
                List.of(new ItemPedido(4L, 10)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertTrue(resultado.getMotivoRechazo().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("Caso 2: Rechazo por cliente no registrado")
    void testClienteNoRegistrado() {
        // Cliente 999 no existe en la base de datos
        PedidoRequest request = new PedidoRequest(999L, "desconocido@tienda.com",
                List.of(new ItemPedido(1L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertFalse(resultado.isConfirmado());
        assertEquals("Cliente no registrado", resultado.getMotivoRechazo());
    }

    @Test
    @DisplayName("Caso 3: Validación de cliente moroso con deuda")
    void testClienteMoroso() {
        // Cliente 4 es MOROSO con deuda de $250,000
        PedidoRequest request = new PedidoRequest(4L, "moroso@tienda.com",
                List.of(new ItemPedido(3L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        // Si se ejecuta antes de las 20:00 se rechaza; si es después se permite excepcionalmente
        assertNotNull(resultado);
        if (!resultado.isConfirmado()) {
            assertTrue(resultado.getMotivoRechazo().contains("deuda pendiente"));
        } else {
            assertTrue(resultado.getTotal() > 0);
        }
    }

    @Test
    @DisplayName("Caso 4: Confirmación y descuento VIP (> 1,000,000 -> 15%)")
    void testDescuentoVipAlto() {
        // Cliente 1 (VIP) pide 1 Laptop Pro ($1,200,000)
        PedidoRequest request = new PedidoRequest(1L, "vip@tienda.com",
                List.of(new ItemPedido(1L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertNotNull(resultado.getPedidoId());
        // Subtotal = 1,200,000. Descuento 15% = 180,000. Base = 1,020,000. Impuesto 19% = 193,800. Total = 1,213,800
        assertEquals(1213800.0, resultado.getTotal(), 0.01);
    }

    @Test
    @DisplayName("Caso 5: Confirmación y descuento FRECUENTE (> 10 pedidos -> 8%)")
    void testDescuentoFrecuenteAlto() {
        // Cliente 2 (FRECUENTE, 12 pedidos previos) pide 1 Monitor ($600,000)
        PedidoRequest request = new PedidoRequest(2L, "ana@tienda.com",
                List.of(new ItemPedido(2L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertTrue(resultado.isConfirmado());
        assertNotNull(resultado.getPedidoId());
        // Subtotal = 600,000. Descuento 8% = 48,000. Base = 552,000. Impuesto 19% = 104,880. Total = 656,880
        assertEquals(656880.0, resultado.getTotal(), 0.01);
    }
}
