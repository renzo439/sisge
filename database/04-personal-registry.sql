USE sisge;

CREATE TABLE sectores(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  organizacion_id BIGINT NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  UNIQUE KEY uk_sector_organizacion_nombre(organizacion_id,nombre),
  FOREIGN KEY(organizacion_id) REFERENCES organizaciones(id)
) ENGINE=InnoDB;
CREATE TABLE personas(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cuil VARCHAR(20) NULL UNIQUE,
  dni VARCHAR(20) NOT NULL UNIQUE,
  nombres VARCHAR(100) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  sexo VARCHAR(50),
  fecha_nacimiento DATE
) ENGINE=InnoDB;
CREATE TABLE designaciones(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  persona_id BIGINT NOT NULL,
  organizacion_id BIGINT NOT NULL,
  sector_id BIGINT NULL,
  escalafon VARCHAR(255),
  categoria VARCHAR(255),
  situacion_revista VARCHAR(255),
  agrupamiento VARCHAR(255),
  funcion VARCHAR(255),
  fecha_inicio DATE NOT NULL,
  antiguedad_anios INT,
  afectado BOOLEAN NOT NULL DEFAULT FALSE,
  vigente BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY(persona_id) REFERENCES personas(id),
  FOREIGN KEY(organizacion_id) REFERENCES organizaciones(id),
  FOREIGN KEY(sector_id) REFERENCES sectores(id),
  INDEX idx_designaciones_persona(persona_id),
  INDEX idx_designaciones_organizacion_vigente(organizacion_id,vigente)
) ENGINE=InnoDB;
CREATE TABLE importaciones_personal(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  organizacion_id BIGINT NOT NULL,
  creado_por_usuario_id BIGINT NOT NULL,
  archivo_origen VARCHAR(255) NOT NULL,
  estado VARCHAR(30) NOT NULL,
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_aprobacion TIMESTAMP NULL,
  FOREIGN KEY(organizacion_id) REFERENCES organizaciones(id),
  FOREIGN KEY(creado_por_usuario_id) REFERENCES usuarios(id),
  INDEX idx_importaciones_personal_organizacion(organizacion_id,estado)
) ENGINE=InnoDB;
CREATE TABLE importacion_personal_filas(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  importacion_id BIGINT NOT NULL,
  fila_origen INT NOT NULL,
  valores_crudos TEXT NOT NULL,
  dni VARCHAR(20), cuil VARCHAR(20), nombres VARCHAR(100), apellido VARCHAR(100), sexo VARCHAR(50), fecha_nacimiento DATE,
  organizacion_id BIGINT NULL, sector_id BIGINT NULL,
  escalafon VARCHAR(255), categoria VARCHAR(255), situacion_revista VARCHAR(255), agrupamiento VARCHAR(255), funcion VARCHAR(255),
  fecha_inicio DATE, antiguedad_anios INT, afectado BOOLEAN NULL, revisada BOOLEAN NOT NULL DEFAULT FALSE, errores TEXT,
  UNIQUE KEY uk_importacion_personal_fila(importacion_id,fila_origen),
  FOREIGN KEY(importacion_id) REFERENCES importaciones_personal(id),
  FOREIGN KEY(organizacion_id) REFERENCES organizaciones(id),
  FOREIGN KEY(sector_id) REFERENCES sectores(id)
) ENGINE=InnoDB;

INSERT IGNORE INTO permisos(codigo,descripcion) VALUES
('PERSONAL_IMPORTAR','Importa personal a revisión'),
('PERSONAL_REVISAR','Revisa filas de personal'),
('PERSONAL_APROBAR','Aprueba registros oficiales de personal'),
('PERSONAL_VER','Consulta padrón oficial de personal');
INSERT IGNORE INTO rol_permisos(rol_id,permiso_id)
SELECT r.id,p.id FROM roles r JOIN permisos p WHERE r.nombre='ADMIN_GENERAL'
  AND p.codigo IN ('PERSONAL_IMPORTAR','PERSONAL_REVISAR','PERSONAL_APROBAR','PERSONAL_VER');
INSERT IGNORE INTO rol_permisos(rol_id,permiso_id)
SELECT r.id,p.id FROM roles r JOIN permisos p WHERE r.nombre='OPERADOR'
  AND p.codigo IN ('PERSONAL_IMPORTAR','PERSONAL_REVISAR');
INSERT IGNORE INTO rol_permisos(rol_id,permiso_id)
SELECT r.id,p.id FROM roles r JOIN permisos p WHERE r.nombre='CONSULTA'
  AND p.codigo = 'PERSONAL_VER';
