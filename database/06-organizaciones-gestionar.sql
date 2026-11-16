USE sisge;
INSERT INTO permisos(codigo,descripcion) VALUES ('ORGANIZACIONES_GESTIONAR','Gestiona organizaciones');
INSERT INTO rol_permisos SELECT r.id,p.id FROM roles r JOIN permisos p WHERE r.nombre='ADMIN_GENERAL' AND p.codigo='ORGANIZACIONES_GESTIONAR';
