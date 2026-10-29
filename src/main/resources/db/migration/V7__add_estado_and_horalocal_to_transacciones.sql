-- V7: Agregar estado de transacción y hora_local para soporte de reserva en 2 fases y conciliación (confirmTx.do)
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'EXITOSA';
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS hora_local VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_transacciones_pago_estado ON transacciones_pago(estado);

