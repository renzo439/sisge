USE sisge;
INSERT INTO permisos(codigo,descripcion) VALUES
('REEMPLAZANTES_IMPORTAR','Carga Excel'),('REEMPLAZANTES_VER','Consulta histórico'),('IMPORTACIONES_VER_PROPIAS','Consulta importaciones propias'),('IMPORTACIONES_EDITAR_PROPIAS','Edita borradores'),('IMPORTACIONES_ENVIAR_REVISION','Envía a revisión'),('IMPORTACIONES_VER_TODAS','Consulta global'),('IMPORTACIONES_REVISAR','Revisa'),('IMPORTACIONES_APROBAR','Aprueba y cierra'),('IMPORTACIONES_OBSERVAR','Observa'),('IMPORTACIONES_RECHAZAR','Rechaza'),('IMPORTACIONES_CERRAR','Cierra'),('USUARIOS_GESTIONAR','Gestiona usuarios'),('ROLES_GESTIONAR','Gestiona roles');
INSERT INTO roles(nombre,descripcion,global_role) VALUES ('ADMIN_GENERAL','Administración central',TRUE),('OPERADOR','Operador de organización',FALSE),('CONSULTA','Consulta de organización',FALSE);
INSERT INTO rol_permisos SELECT r.id,p.id FROM roles r JOIN permisos p WHERE r.nombre='ADMIN_GENERAL';
INSERT INTO rol_permisos SELECT r.id,p.id FROM roles r JOIN permisos p WHERE r.nombre='OPERADOR' AND p.codigo IN ('REEMPLAZANTES_IMPORTAR','IMPORTACIONES_VER_PROPIAS','IMPORTACIONES_EDITAR_PROPIAS','IMPORTACIONES_ENVIAR_REVISION');
INSERT INTO rol_permisos SELECT r.id,p.id FROM roles r JOIN permisos p WHERE r.nombre='CONSULTA' AND p.codigo='REEMPLAZANTES_VER';
