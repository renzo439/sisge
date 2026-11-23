USE sisge;

ALTER TABLE usuarios ADD COLUMN username VARCHAR(100) NULL;

UPDATE usuarios
SET username = CONCAT(LOWER(SUBSTRING_INDEX(email, '@', 1)), '_', id);

ALTER TABLE usuarios MODIFY COLUMN username VARCHAR(100) NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_username UNIQUE (username);
