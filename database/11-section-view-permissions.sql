USE sisge;

INSERT INTO permisos(codigo,descripcion)
SELECT x.codigo,x.descripcion
FROM (
  SELECT 'DASHBOARD_VER' AS codigo,'Ver dashboard' AS descripcion
  UNION ALL SELECT 'LEGAJOS_VER','Ver legajos'
  UNION ALL SELECT 'AUDITORIA_VER','Ver auditoria'
) x
WHERE NOT EXISTS (SELECT 1 FROM permisos p WHERE p.codigo = x.codigo);

INSERT IGNORE INTO rol_permisos(rol_id,permiso_id)
SELECT r.id,p.id
FROM roles r
JOIN permisos p ON p.codigo IN ('DASHBOARD_VER','LEGAJOS_VER','AUDITORIA_VER')
WHERE r.nombre = 'ADMIN_GENERAL';
