CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100),
    tipo_cliente VARCHAR(50),
    nit VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100),
    precio DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS inventario (
    producto_id BIGINT PRIMARY KEY,
    stock INT,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE IF NOT EXISTS facturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    monto DOUBLE PRECISION,
    pagada BOOLEAN,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    subtotal DOUBLE PRECISION,
    descuento DOUBLE PRECISION,
    impuesto DOUBLE PRECISION,
    total DOUBLE PRECISION,
    fecha TIMESTAMP,
    estado VARCHAR(50),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE IF NOT EXISTS detalle_pedido (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT,
    producto_id BIGINT,
    cantidad INT,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);
