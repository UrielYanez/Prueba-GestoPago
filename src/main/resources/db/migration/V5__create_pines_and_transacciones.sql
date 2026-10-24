-- 1. Tabla de Pines de Transacción (OTP temporal con hash)
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

CREATE INDEX IF NOT EXISTS idx_pines_cuenta_expiracion ON transaccion_pines(cuenta_id, utilizado, fecha_expiracion);

-- 2. Tabla de Transacciones y Comprobantes de Pago
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

CREATE INDEX IF NOT EXISTS idx_tx_cuenta ON transacciones_pago(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_tx_upc ON transacciones_pago(upc);
CREATE INDEX IF NOT EXISTS idx_tx_proveedor ON transacciones_pago(id_tx_proveedor);
