USE sisge;

ALTER TABLE importaciones MODIFY COLUMN organizacion_id BIGINT NULL;
ALTER TABLE importaciones ADD COLUMN hospital_nombre VARCHAR(255) NULL AFTER organizacion_id;
ALTER TABLE importaciones ADD COLUMN udeo VARCHAR(255) NULL AFTER hospital_nombre;
CREATE INDEX idx_importaciones_hospital ON importaciones(hospital_nombre, udeo);

ALTER TABLE reemplazantes_historico MODIFY COLUMN organizacion_id BIGINT NULL;
ALTER TABLE reemplazantes_historico ADD COLUMN hospital_nombre VARCHAR(255) NULL AFTER organizacion_id;
ALTER TABLE reemplazantes_historico ADD COLUMN udeo VARCHAR(255) NULL AFTER hospital_nombre;
CREATE INDEX idx_reemplazantes_historico_hospital ON reemplazantes_historico(hospital_nombre, udeo);

UPDATE importaciones i
JOIN organizaciones o ON o.id = i.organizacion_id
SET i.hospital_nombre = o.nombre, i.udeo = o.udeo
WHERE i.hospital_nombre IS NULL;

UPDATE reemplazantes_historico h
JOIN organizaciones o ON o.id = h.organizacion_id
SET h.hospital_nombre = o.nombre, h.udeo = o.udeo
WHERE h.hospital_nombre IS NULL;
