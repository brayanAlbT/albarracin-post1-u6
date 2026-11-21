-- Clientes
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (1, 'Carlos VIP', 'VIP', '900123456-1');
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (2, 'Ana Frecuente Plus', 'FRECUENTE', '900234567-2');
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (3, 'Luis Frecuente', 'FRECUENTE', NULL);
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (4, 'Pedro Moroso', 'MOROSO', NULL);
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (5, 'Maria Estandar', 'ESTANDAR', NULL);
INSERT INTO clientes (id, nombre, tipo_cliente, nit) VALUES (6, 'Empresa Corp SA', 'ESTANDAR', '800999888-4');

-- Productos
INSERT INTO productos (id, nombre, precio) VALUES (1, 'Laptop Pro', 1200000.0);
INSERT INTO productos (id, nombre, precio) VALUES (2, 'Monitor 4K', 600000.0);
INSERT INTO productos (id, nombre, precio) VALUES (3, 'Teclado Mecanico', 100000.0);
INSERT INTO productos (id, nombre, precio) VALUES (4, 'Mouse Gamer', 50000.0);
INSERT INTO productos (id, nombre, precio) VALUES (5, 'Cable HDMI', 20000.0);

-- Inventario
INSERT INTO inventario (producto_id, stock) VALUES (1, 50);
INSERT INTO inventario (producto_id, stock) VALUES (2, 40);
INSERT INTO inventario (producto_id, stock) VALUES (3, 100);
INSERT INTO inventario (producto_id, stock) VALUES (4, 5);
INSERT INTO inventario (producto_id, stock) VALUES (5, 500);

-- Facturas pendientes (Cliente 4 con deuda de $250,000)
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (4, 250000.0, FALSE);

-- Pedidos previos para Cliente 2 (12 pedidos registrados)
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (2, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');

-- Pedidos previos para Cliente 3 (5 pedidos registrados)
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (3, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (3, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (3, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (3, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (3, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
