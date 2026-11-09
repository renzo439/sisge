-- Incremental, idempotent migration for installations created before 2026.
-- Run after 04-personal-registry.sql; it never deletes staging or production data.
USE sisge;

CREATE TABLE IF NOT EXISTS dependencias (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, organizacion_id BIGINT NOT NULL, nombre VARCHAR(255) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE KEY uk_dependencia_organizacion_nombre(organizacion_id,nombre),
  CONSTRAINT fk_dependencia_organizacion FOREIGN KEY(organizacion_id) REFERENCES organizaciones(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS escalafones (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100), nombre VARCHAR(255) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE KEY uk_escalafon_nombre(nombre)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS categorias (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100), nombre VARCHAR(255) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE KEY uk_categoria_nombre(nombre)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS situaciones_revista (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100), nombre VARCHAR(255) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE KEY uk_situacion_revista_nombre(nombre)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS agrupamientos (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100), nombre VARCHAR(255) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE KEY uk_agrupamiento_nombre(nombre)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS funciones (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100), nombre VARCHAR(255) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE KEY uk_funcion_nombre(nombre)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS tipos_afectacion (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100), nombre VARCHAR(255) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE KEY uk_tipo_afectacion_nombre(nombre)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS titulos (id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100), nombre VARCHAR(255) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE, UNIQUE KEY uk_titulo_nombre(nombre)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS adicionales (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, codigo VARCHAR(100) NOT NULL UNIQUE, nombre VARCHAR(255) NOT NULL,
  descripcion VARCHAR(500), activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;
INSERT IGNORE INTO adicionales(codigo,nombre) VALUES ('ADICIONAL_MH','Adicional M H'),('ADICIONAL_JEFATURA','Adicional por jefatura');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
  IN table_name_value VARCHAR(64),
  IN column_name_value VARCHAR(64),
  IN column_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = table_name_value
      AND column_name = column_name_value
  ) THEN
    SET @statement = CONCAT(
      'ALTER TABLE `', table_name_value, '` ADD COLUMN `',
      column_name_value, '` ', column_definition
    );
    PREPARE alter_statement FROM @statement;
    EXECUTE alter_statement;
    DEALLOCATE PREPARE alter_statement;
  END IF;
END//
DELIMITER ;

CALL add_column_if_missing('sectores', 'dependencia_id', 'BIGINT NULL');
CALL add_column_if_missing('sectores', 'activo', 'BOOLEAN NOT NULL DEFAULT TRUE');
CALL add_column_if_missing('designaciones', 'escalafon_id', 'BIGINT NULL');
CALL add_column_if_missing('designaciones', 'categoria_id', 'BIGINT NULL');
CALL add_column_if_missing('designaciones', 'situacion_revista_id', 'BIGINT NULL');
CALL add_column_if_missing('designaciones', 'agrupamiento_id', 'BIGINT NULL');
CALL add_column_if_missing('designaciones', 'funcion_id', 'BIGINT NULL');
CALL add_column_if_missing('designaciones', 'tipo_afectacion_id', 'BIGINT NULL');
CALL add_column_if_missing('importaciones_personal', 'fecha_revision', 'TIMESTAMP NULL');
CALL add_column_if_missing('importaciones_personal', 'revisado_por_usuario_id', 'BIGINT NULL');
CALL add_column_if_missing('importaciones_personal', 'cantidad_filas', 'INT NOT NULL DEFAULT 0');
CALL add_column_if_missing('importaciones_personal', 'cantidad_filas_validas', 'INT NOT NULL DEFAULT 0');
CALL add_column_if_missing('importaciones_personal', 'cantidad_filas_con_error', 'INT NOT NULL DEFAULT 0');
CALL add_column_if_missing('importacion_personal_filas', 'dependencia_id', 'BIGINT NULL');
CALL add_column_if_missing('importacion_personal_filas', 'escalafon_id', 'BIGINT NULL');
CALL add_column_if_missing('importacion_personal_filas', 'categoria_id', 'BIGINT NULL');
CALL add_column_if_missing('importacion_personal_filas', 'situacion_revista_id', 'BIGINT NULL');
CALL add_column_if_missing('importacion_personal_filas', 'agrupamiento_id', 'BIGINT NULL');
CALL add_column_if_missing('importacion_personal_filas', 'funcion_id', 'BIGINT NULL');
CALL add_column_if_missing('importacion_personal_filas', 'tipo_afectacion_id', 'BIGINT NULL');
CALL add_column_if_missing('importacion_personal_filas', 'titulo_id', 'BIGINT NULL');
CALL add_column_if_missing('importacion_personal_filas', 'adicional_mh', 'VARCHAR(255) NULL');
CALL add_column_if_missing('importacion_personal_filas', 'adicional_jefatura', 'VARCHAR(255) NULL');
CALL add_column_if_missing('importacion_personal_filas', 'estado_relevamiento', 'VARCHAR(30) NOT NULL DEFAULT ''PENDIENTE''');
DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS designacion_adicionales (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, designacion_id BIGINT NOT NULL, adicional_id BIGINT NOT NULL,
 valor DECIMAL(15,2) NULL, porcentaje DECIMAL(10,2) NULL, fecha_desde DATE NULL, fecha_hasta DATE NULL,
 vigente BOOLEAN NOT NULL DEFAULT TRUE, observaciones TEXT, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_designacion_adicional_vigente(designacion_id,adicional_id,vigente),
 FOREIGN KEY(designacion_id) REFERENCES designaciones(id), FOREIGN KEY(adicional_id) REFERENCES adicionales(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS auditoria_eventos (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, usuario_id BIGINT NULL, organizacion_id BIGINT NULL, accion VARCHAR(100) NOT NULL,
 entidad VARCHAR(100) NOT NULL, entidad_id BIGINT NULL, fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(usuario_id) REFERENCES usuarios(id), FOREIGN KEY(organizacion_id) REFERENCES organizaciones(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS auditoria_cambios (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, evento_id BIGINT NOT NULL, valor_anterior TEXT, valor_nuevo TEXT,
 FOREIGN KEY(evento_id) REFERENCES auditoria_eventos(id)
) ENGINE=InnoDB;

-- Populate masters and resolve legacy text without auto-creating values during future imports.
INSERT IGNORE INTO escalafones(nombre) SELECT DISTINCT TRIM(escalafon) FROM designaciones WHERE escalafon IS NOT NULL AND TRIM(escalafon) <> '';
INSERT IGNORE INTO categorias(nombre) SELECT DISTINCT TRIM(categoria) FROM designaciones WHERE categoria IS NOT NULL AND TRIM(categoria) <> '';
INSERT IGNORE INTO situaciones_revista(nombre) SELECT DISTINCT TRIM(situacion_revista) FROM designaciones WHERE situacion_revista IS NOT NULL AND TRIM(situacion_revista) <> '';
INSERT IGNORE INTO agrupamientos(nombre) SELECT DISTINCT TRIM(agrupamiento) FROM designaciones WHERE agrupamiento IS NOT NULL AND TRIM(agrupamiento) <> '';
INSERT IGNORE INTO funciones(nombre) SELECT DISTINCT TRIM(funcion) FROM designaciones WHERE funcion IS NOT NULL AND TRIM(funcion) <> '';
UPDATE designaciones d LEFT JOIN escalafones e ON LOWER(TRIM(d.escalafon))=LOWER(e.nombre) SET d.escalafon_id=e.id;
UPDATE designaciones d LEFT JOIN categorias e ON LOWER(TRIM(d.categoria))=LOWER(e.nombre) SET d.categoria_id=e.id;
UPDATE designaciones d LEFT JOIN situaciones_revista e ON LOWER(TRIM(d.situacion_revista))=LOWER(e.nombre) SET d.situacion_revista_id=e.id;
UPDATE designaciones d LEFT JOIN agrupamientos e ON LOWER(TRIM(d.agrupamiento))=LOWER(e.nombre) SET d.agrupamiento_id=e.id;
UPDATE designaciones d LEFT JOIN funciones e ON LOWER(TRIM(d.funcion))=LOWER(e.nombre) SET d.funcion_id=e.id;
