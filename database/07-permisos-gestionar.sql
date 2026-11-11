USE sisge;
INSERT INTO permisos(codigo,descripcion) VALUES ('PERMISOS_GESTIONAR','Gestiona permisos');
INSERT INTO rol_permisos SELECT r.id,p.id FROM roles r JOIN permisos p WHERE r.nombre='ADMIN_GENERAL' AND p.codigo='PERMISOS_GESTIONAR';
