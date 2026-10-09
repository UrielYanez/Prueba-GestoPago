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

-- Índices para busquedas
CREATE INDEX idx_clientes_curp ON clientes(curp);
CREATE INDEX idx_clientes_rfc ON clientes(rfc);
CREATE INDEX idx_clientes_correo ON clientes(correo_electronico);
CREATE INDEX idx_cuentas_numero ON cuentas(numero_cuenta);
CREATE INDEX idx_usuarios_correo ON usuarios(correo);
