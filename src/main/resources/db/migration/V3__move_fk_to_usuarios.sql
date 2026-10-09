ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS cliente_id BIGINT;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id);

-- Opcional: Migrar la data existente
UPDATE usuarios u
SET cliente_id = c.id
FROM clientes c
WHERE c.usuario_id = u.id;

-- Limpiar la columna vieja en clientes (opcional, pero deja el esquema idéntico a lo solicitado)
ALTER TABLE clientes DROP COLUMN IF EXISTS usuario_id;
