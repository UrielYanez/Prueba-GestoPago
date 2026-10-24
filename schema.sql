-- 1. Tabla de Domicilios (1-1 con Cliente)
CREATE TABLE IF NOT EXISTS domicilios (
    id BIGSERIAL PRIMARY KEY,
    calle VARCHAR(255) NOT NULL,
    numero_exterior VARCHAR(50) NOT NULL,
    numero_interior VARCHAR(50),
    colonia VARCHAR(100),
    municipio VARCHAR(100),
    estado VARCHAR(100),
    codigo_postal VARCHAR(5) NOT NULL,
    pais VARCHAR(50) NOT NULL
);

-- 2. Tabla de Clientes
CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    segundo_nombre VARCHAR(50),
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    curp VARCHAR(18) NOT NULL UNIQUE,
    rfc VARCHAR(13) NOT NULL UNIQUE,
    sexo VARCHAR(20),
    nacionalidad VARCHAR(50),
    estado_civil VARCHAR(50),
    correo_electronico VARCHAR(100) NOT NULL UNIQUE,
    telefono_movil VARCHAR(10) NOT NULL,
    telefono_alternativo VARCHAR(10),
    ocupacion VARCHAR(255),
    empresa VARCHAR(255),
    ingreso_mensual DECIMAL(19, 2),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    domicilio_id BIGINT,
    CONSTRAINT fk_cliente_domicilio FOREIGN KEY (domicilio_id) REFERENCES domicilios(id)
);

-- 3. Tabla de Usuarios (1-1 con Cliente)
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    correo VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cliente_id BIGINT UNIQUE,
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

-- 4. Tabla de Cuentas (1-N con Cliente)
CREATE TABLE IF NOT EXISTS cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(10) NOT NULL UNIQUE,
    saldo DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    estatus VARCHAR(20) NOT NULL CHECK (estatus IN ('ACTIVA', 'INACTIVA', 'BLOQUEADA')),
    cliente_id BIGINT NOT NULL,
    CONSTRAINT fk_cuenta_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

-- 5. Tabla Tokens Gestopago (Auxiliar)
CREATE TABLE IF NOT EXISTS gestopago_tokens (
    id SERIAL PRIMARY KEY,
    id_distribuidor INT NOT NULL UNIQUE,
    token VARCHAR(1000) NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 6. Tabla de Pines de Confirmación (OTP transaccional con BCrypt)
CREATE TABLE IF NOT EXISTS transaccion_pines (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    pin_hash VARCHAR(255) NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    utilizado BOOLEAN NOT NULL DEFAULT FALSE,
    intentos INT NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pin_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE CASCADE
);

-- 7. Tabla de Transacciones y Comprobantes de Pago
CREATE TABLE IF NOT EXISTS transacciones_pago (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    id_servicio VARCHAR(50) NOT NULL,
    id_producto VARCHAR(50) NOT NULL,
    telefono VARCHAR(20),
    referencia VARCHAR(100),
    bandera BOOLEAN NOT NULL DEFAULT TRUE,
    monto DECIMAL(19, 2) NOT NULL,
    upc VARCHAR(50) NOT NULL UNIQUE,
    distribuidor VARCHAR(20) NOT NULL,
    id_tx_proveedor VARCHAR(100),
    num_autorizacion VARCHAR(100),
    codigo_respuesta VARCHAR(20),
    mensaje_respuesta VARCHAR(255),
    saldo_proveedor DECIMAL(19, 2),
    pin_gift_card VARCHAR(250),
    fecha_transaccion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaccion_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id)
);

-- Índices para busquedas
CREATE INDEX idx_clientes_curp ON clientes(curp);
CREATE INDEX idx_clientes_rfc ON clientes(rfc);
CREATE INDEX idx_clientes_correo ON clientes(correo_electronico);
CREATE INDEX idx_cuentas_numero ON cuentas(numero_cuenta);
CREATE INDEX idx_usuarios_correo ON usuarios(correo);
CREATE INDEX idx_pines_cuenta_expiracion ON transaccion_pines(cuenta_id, utilizado, fecha_expiracion);
CREATE INDEX idx_tx_cuenta ON transacciones_pago(cuenta_id);
CREATE INDEX idx_tx_upc ON transacciones_pago(upc);
CREATE INDEX idx_tx_proveedor ON transacciones_pago(id_tx_proveedor);
