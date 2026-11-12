USE sisge;

ALTER TABLE reemplazantes_historico ADD COLUMN dni VARCHAR(20) NULL AFTER cuil;
CREATE INDEX idx_reemplazantes_historico_dni ON reemplazantes_historico(dni);

UPDATE reemplazantes_historico
SET dni = SUBSTRING(REGEXP_REPLACE(cuil, '[^0-9]', ''), 3, 8)
WHERE cuil IS NOT NULL
  AND REGEXP_REPLACE(cuil, '[^0-9]', '') REGEXP '^[0-9]{11}$';
