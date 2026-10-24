-- Migración V6: Ajustar columnas de transacciones_pago para separar telefono y referencia con bandera
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'transacciones_pago' AND column_name = 'referencia_telefono'
    ) THEN
        ALTER TABLE transacciones_pago ALTER COLUMN referencia_telefono DROP NOT NULL;
    END IF;
END $$;

ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS telefono VARCHAR(20);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS referencia VARCHAR(100);
ALTER TABLE transacciones_pago ADD COLUMN IF NOT EXISTS bandera BOOLEAN DEFAULT TRUE;
