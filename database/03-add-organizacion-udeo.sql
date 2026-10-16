USE sisge;
ALTER TABLE organizaciones ADD COLUMN udeo VARCHAR(255) NULL;
ALTER TABLE organizaciones ADD UNIQUE KEY uk_organizaciones_udeo (udeo);
