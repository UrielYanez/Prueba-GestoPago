ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS session_id_activo VARCHAR(100);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS plataforma_activa VARCHAR(20);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_inicio_sesion TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_expiracion_sesion TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS totp_habilitado BOOLEAN DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultimo_codigo_2fa_usado VARCHAR(10);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS codigo_recuperacion_hash VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS expiracion_codigo_recuperacion TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS intentos_fallidos_auth INTEGER DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS bloqueado_hasta TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ultimo_envio_2fa TIMESTAMP;

ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS id_operacion_cliente VARCHAR(80);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS nombre_producto VARCHAR(150);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS nombre_servicio VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_transacciones_id_operacion_cliente ON transacciones_pago(id_operacion_cliente);
CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta_estado_fecha ON transacciones_pago(cuenta_id, estado, fecha_transaccion DESC);

