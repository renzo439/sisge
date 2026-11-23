# SISGE

API REST para importar reemplazantes desde archivos Excel, revisarlos y consolidarlos en un histórico. Está desarrollada con Java 17, Spring Boot, Maven y MySQL.

## Requisitos

- JDK 17.
- Docker Desktop, para la opción con contenedores, o MySQL 8.0+ instalado localmente.
- Acceso a Maven Central para descargar dependencias en la primera ejecución.

El proyecto usa Maven Wrapper, por lo que no es necesario instalar Maven globalmente. El Wrapper usa Maven 3.9.14 para ser compatible con IntelliJ IDEA.

## Ejecución rápida con Docker

1. Cree el archivo de variables de entorno:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Edite `.env` y reemplace todos los valores de ejemplo. Genere un secreto JWT de al menos 32 caracteres y use una contraseña fuerte para el administrador inicial.

   ```dotenv
   MYSQL_USER=sisge
   MYSQL_PASSWORD=una-clave-local-segura
   MYSQL_ROOT_PASSWORD=otra-clave-local-segura
   JWT_SECRET=un-secreto-aleatorio-de-al-menos-32-caracteres
   BOOTSTRAP_ADMIN_EMAIL=admin@example.com
   BOOTSTRAP_ADMIN_PASSWORD=una-clave-de-administrador-segura
   BOOTSTRAP_ADMIN_USERNAME=admin
   ```

3. Inicie la base de datos y la aplicación:

   ```powershell
   docker compose up --build
   ```

La API quedará disponible en `http://localhost:8080`. Docker espera a que MySQL acepte conexiones antes de iniciar la aplicación. MySQL ejecuta automáticamente los scripts versionados de `database/` (incluido `04-personal-registry.sql`) solo al crear el volumen `mysql-data` por primera vez. La aplicación usa automáticamente `MYSQL_PASSWORD` para conectarse a MySQL, por lo que no debe definir `DB_PASSWORD` cuando usa Docker. Para una instalación limpia, cree un volumen Docker nuevo o ejecute todos los scripts en orden en una base vacía; no elimine un volumen que contenga datos.

El `Dockerfile` es multi-etapa: compila el proyecto desde el código fuente dentro de la imagen, por lo que no hace falta generar el jar manualmente. Después de actualizar el código, reconstruya la imagen para que los cambios se apliquen:

```powershell
docker compose build --no-cache app
docker compose up -d
```

> No elimine ni reinicialice el volumen de MySQL si contiene datos que desea conservar.
>
> Para actualizar un volumen existente, ejecute cada script nuevo manualmente. Por ejemplo, para aplicar la normalización del módulo de personal:
>
> ```powershell
> Get-Content -Raw database\05-personal-normalization.sql |
>   docker compose exec -T mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" sisge'
> ```

## Ejecución local

1. Cree el archivo de variables de entorno si todavía no existe y reemplace sus valores de ejemplo:

   ```powershell
   Copy-Item .env.example .env
   ```

   La aplicación carga `.env` automáticamente al ejecutarse desde la raíz del proyecto. Las variables definidas en PowerShell tienen prioridad sobre este archivo.

2. Cree una base MySQL vacía y ejecute los scripts en este orden:

   ```powershell
   mysql -u root -p < database\01-schema.sql
   mysql -u root -p sisge < database\02-reference-data.sql
   mysql -u root -p sisge < database\03-add-organizacion-udeo.sql
   mysql -u root -p sisge < database\04-personal-registry.sql
    mysql -u root -p sisge < database\05-personal-normalization.sql
    mysql -u root -p sisge < database\06-organizaciones-gestionar.sql
    mysql -u root -p sisge < database\07-permisos-gestionar.sql
    mysql -u root -p sisge < database\08-historical-hospital-raw.sql
    mysql -u root -p sisge < database\09-historical-dni.sql
    mysql -u root -p sisge < database\10-usuario-username.sql
   ```

3. Para reemplazar algún valor de `.env` solo durante la sesión actual de PowerShell, defina las variables de entorno:

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/sisge?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:DB_USERNAME = "sisge"
   $env:DB_PASSWORD = "una-clave-local-segura"
   $env:JWT_SECRET = "un-secreto-aleatorio-de-al-menos-32-caracteres"
   $env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
   $env:BOOTSTRAP_ADMIN_PASSWORD = "una-clave-de-administrador-segura"
   $env:BOOTSTRAP_ADMIN_USERNAME = "admin"
   ```

4. Inicie la aplicación:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   De forma predeterminada queda disponible en `http://localhost:8081`. Para usar otro puerto, asigne `SERVER_PORT` en `.env` con el formato `SERVER_PORT=8082`.

En el primer arranque, el administrador general se crea únicamente si `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` tienen valores y el correo todavía no existe. Su nombre de usuario se toma de `BOOTSTRAP_ADMIN_USERNAME` (por defecto `admin`), se normaliza a minúsculas y debe ser único. Su contraseña se almacena con BCrypt. Quite `BOOTSTRAP_ADMIN_PASSWORD` de su entorno después de provisionarlo.

## Compilación y pruebas

```powershell
.\mvnw.cmd clean install
```

Si usa IntelliJ IDEA, recargue el proyecto Maven después de abrirlo o cambiar el `pom.xml`: **Maven → Reload All Maven Projects**. El módulo compila explícitamente con `release 17`.

## Autenticación

Obtenga un token JWT. El campo `email` acepta tanto el correo como el nombre de usuario (no distingue mayúsculas):

```powershell
$body = @{
  email = "admin@example.com"
  password = "una-clave-de-administrador-segura"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body $body
```

Incluya el token recibido en el encabezado `Authorization` con el esquema `Bearer` para los demás endpoints.

Para obtener el usuario autenticado y sus permisos:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/api/v1/auth/me" `
  -Headers @{ Authorization = "Bearer $token" }
```

Devuelve `id`, `nombre`, `apellido`, `username`, `email`, `rolGlobal`, `global`, `permissions`, `organizacionId`, `organizacion`, `organizaciones`, `globalPermissions` y `organizationPermissions` (permisos por id de organización).

`permissions` es la unión efectiva de `globalPermissions` y de todos los `organizationPermissions`; es la fuente que debe usar el frontend para mostrar u ocultar módulos. `organizacionId` y `organizacion` (`{ id, nombre, udeo }`) son la organización de trabajo del usuario, elegida como la membresía activa más antigua; para usuarios globales ambos son `null`. El mismo `organizacionId` se incluye como claim en el JWT firmado, pero el frontend debe leerlo desde `/auth/me`, no decodificar el token. `organizaciones` es la lista completa para la cabecera: para un usuario común incluye solo las organizaciones donde tiene membresía activa; para un usuario global incluye **todas** las organizaciones del sistema. Está ordenada por nombre y **no debe usarse para inferir permisos**. Para permisos por organización usar `organizationPermissions[id]`; para permisos globales, `globalPermissions`. El frontend debe refrescar `/auth/me` en login, recarga y cada cambio de usuario/roles/membresías.

La autorización se aplica siempre en el backend y se recarga desde la base en cada request, no desde el claim del JWT: un usuario con solo `REEMPLAZANTES_VER` recibe `403` en endpoints de organización, roles, permisos, administración, personal, importaciones, etc. En `/api/v1/reemplazantes` solo puede consultar el histórico y el PDF; si no es global debe enviar `organizacionId` y este debe estar entre sus organizaciones con `REEMPLAZANTES_VER`. El `organizacionId` del token es solo el ámbito inicial para el front; el backend sigue validando membresía/permisos. Ocultar botones en el front es solo UX, no seguridad.

Los permisos de acceso por sección son `REEMPLAZANTES_VER`, `DASHBOARD_VER`, `LEGAJOS_VER` y `AUDITORIA_VER`. Un `*_VER` habilita la sección; las acciones dentro de cada módulo requieren permisos adicionales propios (por ejemplo `REEMPLAZANTES_IMPORTAR` o los `IMPORTACIONES_*`). Están cargados en `database/02-reference-data.sql` para instalaciones nuevas y en `database/11-section-view-permissions.sql` para bases existentes; `ADMIN_GENERAL` los recibe todos.

## Documentación de la API (Swagger)

La API se documenta automáticamente con springdoc-openapi. Con la aplicación en ejecución:

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- OpenAPI YAML: `http://localhost:8081/v3/api-docs.yaml`

Para probar endpoints protegidos, obtenga el token JWT y use el botón **Authorize** de Swagger UI (esquema `bearerAuth`).

## Gestión de roles y organizaciones

La administración central gestiona los roles con el permiso global `ROLES_GESTIONAR`:

```text
GET    /api/v1/roles
GET    /api/v1/roles/{id}
POST   /api/v1/roles
PUT    /api/v1/roles/{id}
DELETE /api/v1/roles/{id}
```

Las operaciones de alta y modificación reciben `nombre`, `descripcion`, `globalRole` y la lista de `permisos` por código. Un rol asignado a usuarios, ya sea como rol de organización o rol global, no puede eliminarse.

Las organizaciones se gestionan con el permiso global `ORGANIZACIONES_GESTIONAR`:

```text
GET    /api/v1/admin/organizaciones
GET    /api/v1/admin/organizaciones/{id}
POST   /api/v1/admin/organizaciones
PUT    /api/v1/admin/organizaciones/{id}
DELETE /api/v1/admin/organizaciones/{id}
```

Las operaciones de alta y modificación reciben `nombre`, `udeo` (único) y `descripcion`. Una organización con usuarios asignados, importaciones, designaciones u otros datos asociados no puede eliminarse.

## Gestión de permisos

Los permisos se administran con el permiso global `PERMISOS_GESTIONAR` (la consulta también admite `ROLES_GESTIONAR`, para cargar el catálogo desde la edición de roles):

```text
GET    /api/v1/permisos
GET    /api/v1/permisos/{id}
POST   /api/v1/permisos
PUT    /api/v1/permisos/{id}
DELETE /api/v1/permisos/{id}
```

Las operaciones de alta y modificación reciben `codigo` y `descripcion`; el código se normaliza a mayúsculas, debe ser único y solo admite letras, dígitos y guiones bajos. Un permiso asignado a roles no puede eliminarse. Cambiar el código de un permiso que el sistema evalúa (por ejemplo `ROLES_GESTIONAR`) altera la autorización, porque los servicios lo referencian por código.

## Gestión de usuarios

Los usuarios de una organización se consultan y su rol se edita con el permiso `USUARIOS_GESTIONAR` sobre esa organización:

```text
GET  /api/v1/organizaciones/{organizacionId}/usuarios
GET  /api/v1/organizaciones/{organizacionId}/usuarios/{usuarioId}
POST /api/v1/organizaciones/{organizacionId}/usuarios
PUT  /api/v1/organizaciones/{organizacionId}/usuarios/{usuarioId}/rol
```

El listado y el detalle devuelven `usuarioId`, `nombre`, `apellido`, `username`, `email`, `estado`, `rol` y `createdAt` de la membresía. El cambio de rol recibe `{ "rol": "OPERADOR" }`.

El alta recibe:

```json
{
  "nombre": "Ana",
  "apellido": "Pérez",
  "username": "ana.perez",
  "email": "ana@example.com",
  "password": "ClaveSegura123!",
  "rol": "OPERADOR"
}
```

`username` es obligatorio, único, se normaliza a minúsculas y admite letras, dígitos, puntos, guiones y guiones bajos. `password` es opcional: si se omite o está vacío se genera una contraseña temporal de 16 caracteres; si se envía debe tener entre 8 y 100 caracteres. En ambos casos se almacena con BCrypt y la contraseña efectiva se devuelve en el campo `password` de la respuesta (201). Ejemplo de la respuesta:

```json
{
  "id": 12,
  "nombre": "Ana",
  "apellido": "Pérez",
  "username": "ana.perez",
  "email": "ana@example.com",
  "organizacionId": 2,
  "organizacionNombre": "Hospital Norte",
  "rol": "OPERADOR",
  "password": "ClaveSegura123!"
}
```

Los usuarios se consultan globalmente y su rol global se edita desde la administración central, con el permiso global `USUARIOS_GESTIONAR`:

```text
GET /api/v1/admin/usuarios?q={texto}
GET /api/v1/admin/usuarios/membresias?q={texto}
GET /api/v1/admin/usuarios/{usuarioId}
PUT /api/v1/admin/usuarios/{usuarioId}/rol-global
```

El listado global acepta `q` para buscar por nombre, apellido, username o email e incluye el `rolGlobal` de cada usuario (o `null`). El rol global se asigna con `{ "rol": "ADMIN_GENERAL" }` o se quita con `rol` nulo o vacío.

`GET /api/v1/admin/usuarios/membresias` devuelve la grilla global para administración: una fila por usuario y organización con `usuarioId`, `nombre`, `apellido`, `username`, `email`, `estado`, `rolGlobal`, `organizacionId`, `organizacionNombre`, `rol`, `membresiaEstado` y `createdAt` (alta en esa organización). Un usuario sin membresía (por ejemplo el administrador inicial) aparece con `organizacionId`, `organizacionNombre`, `rol` y `createdAt` en `null`. Acepta el mismo `q` y busca además por organización y rol. Requiere el permiso global `USUARIOS_GESTIONAR`.

Un rol global no puede asignarse a una membresía de organización y solo un rol global puede usarse como rol global. Los cambios de permisos son efectivos de inmediato: los permisos se evalúan contra la base en cada petición, sin depender del token emitido.

## Flujo de importación

1. Un operador sube un `.xlsx` mediante `multipart/form-data`, con la parte `file`, a `POST /api/v1/organizaciones/{organizacionId}/importaciones/upload`.
2. La importación queda en `EN_PROCESO`; sus filas solo existen en la tabla temporal.
3. El operador edita los cinco campos adicionales y envía la carga a revisión.
4. La administración central puede observar, rechazar o aprobar.
5. Aprobar cierra la importación atómicamente y copia las filas a `reemplazantes_historico`.

Para migrar un Excel histórico que ya fue revisado, un administrador central con los permisos `IMPORTACIONES_APROBAR` e `IMPORTACIONES_CERRAR` puede enviar el archivo mediante `multipart/form-data` a `POST /api/v1/admin/importaciones/organizaciones/{organizacionId}/migrar-historico`. La importación se registra directamente como `CERRADA` y sus filas se copian al histórico en la misma transacción. El archivo debe cumplir el mismo formato y no contener filas con errores.

Para un Excel histórico consolidado con varios hospitales, use `POST /api/v1/admin/importaciones/migrar-historico-consolidado` con una parte `file` de tipo `multipart/form-data`. El archivo debe incluir las columnas `Hospital` y `UDEO` antes de las columnas habituales. Los valores de `Hospital` y `UDEO` se guardan literalmente tal como vienen en el Excel: no se reemplazan por un id ni se crean organizaciones automáticamente. Se genera una importación cerrada por cada combinación de `Hospital` y `UDEO`; la organización queda nula y puede asociarse más adelante. Para archivos grandes, las filas se insertan en lotes de 500 y la respuesta contiene solo el resumen de cada importación creada. Si alguna fila tiene errores bloqueantes, la respuesta `409` incluye en `details` la fila de Excel (`fila`) y las reglas incumplidas (`errores`), y no se migra ninguna fila.

El histórico se puede consultar por `organizacionId`, `importacionId`, `reemplazante`, `dni`, `cuil`, `titular`, `lugarTrabajo`, `fechaCarga`, `hospital` o `udeo`. El `dni` se deriva del CUIL al migrar. Las filas migradas sin organización solo son visibles para usuarios globales con `REEMPLAZANTES_VER` hasta que se les asocie una organización.

Para imprimir los resultados de una búsqueda, `GET /api/v1/reemplazantes/reporte` acepta **los mismos filtros** que la consulta (salvo `page` y `size`) y devuelve un PDF con las columnas `Reemplazante`, `Desde`, `Hasta` y `Hospital` (con su UDEO entre paréntesis). El PDF incluye todas las filas que coinciden con los filtros, ordenadas por id, no solo la página actual; está limitado a 10.000 registros y lo indica en el documento si se alcanza el límite. Requiere `REEMPLAZANTES_VER` y aplica las mismas reglas de organización que la consulta. La respuesta usa `Content-Type: application/pdf` y `Content-Disposition: inline; filename="reporte-reemplazantes-YYYY-MM-DD.pdf"`.

Estados permitidos:

```text
EN_PROCESO -> PENDIENTE_REVISION -> CERRADA
PENDIENTE_REVISION -> OBSERVADA -> PENDIENTE_REVISION
PENDIENTE_REVISION -> RECHAZADA
```

## Padrón de personal

El padrón utiliza una carga temporal, seguida de relevamiento, validación central y consolidación. Subir un archivo nunca modifica directamente `personas`, `designaciones` ni `designacion_adicionales`.

### Preparación y carga del Excel

El archivo debe ser `.xlsx`, no estar vacío y contener datos en la primera hoja. Las cabeceras no distinguen mayúsculas, minúsculas ni acentos. Son obligatorias `DNI`, `Nombres`, `Apellido` y `Fecha Inicio`; las fechas admiten `yyyy-MM-dd` o `d/M/yyyy`. SisGe conserva todas las columnas y valores originales en `valores_crudos`, incluso las columnas que no reconoce.

El operador autorizado para la organización carga el archivo:

```text
POST /api/v1/organizaciones/{organizacionId}/personal/importaciones
Content-Type: multipart/form-data
file=<archivo.xlsx>

```

La respuesta contiene el `importacionId`, la cantidad de filas procesadas, válidas y con errores. Consulte el estado y cada fila con:

```text
GET /api/v1/personal/importaciones/{importacionId}
```

### Relevamiento por organización

El responsable revisa las filas con error y las que requieren confirmación funcional. Para cada corrección debe enviar:

```text
PUT /api/v1/personal/importaciones/{importacionId}/filas/{filaId}
```

La revisión debe completar los datos personales, la organización y los IDs de las relaciones aplicables: `dependenciaId`, `sectorId`, `escalafonId`, `categoriaId`, `situacionRevistaId`, `agrupamientoId`, `funcionId`, `tipoAfectacionId` y `tituloId`. Los valores de maestros inexistentes no se crean automáticamente: deben darse de alta y luego seleccionarse con su ID correspondiente.

Una persona de una organización solo puede relevar la importación de esa organización; no puede cambiar la fila a otra organización. Cada modificación queda auditada junto con los valores anteriores y nuevos.

### Confirmación, aprobación y consolidación

Antes de confirmar el Excel, el responsable debe verificar que:

1. Todas las filas estén marcadas como revisadas.
2. No existan errores de formato, DNI, fechas o campos obligatorios.
3. Las relaciones laborales estén asociadas a maestros válidos mediante IDs, especialmente escalafón, categoría, situación de revista, agrupamiento y función.
4. Los adicionales `ADICIONAL M H` y `ADICIONAL POR JEFATURA` sean numéricos cuando tengan valor.
5. Las diferencias de DNI y CUIL, posibles duplicados y asignaciones de organización estén resueltas.

La administración central ejecuta primero la validación previa, que no modifica las tablas definitivas:

```text
POST /api/v1/admin/personal/importaciones/{importacionId}/validar
```

El resultado informa personas y designaciones nuevas o existentes, maestros faltantes, filas inválidas, adicionales inválidos y posibles duplicados. Si hay errores bloqueantes, se vuelve al relevamiento.

Cuando la validación es satisfactoria, la administración central aprueba la importación:

```text
POST /api/v1/personal/importaciones/{importacionId}/aprobar
```

La aprobación confirma la carga, pero todavía no inserta datos definitivos. Finalmente, y solo para una importación aprobada sin errores bloqueantes, la administración inicia la consolidación:

```text
POST /api/v1/admin/personal/importaciones/{importacionId}/consolidar
```

La consolidación crea o actualiza personas por DNI/CUIL, designaciones y adicionales sin duplicar registros vigentes; además, registra la trazabilidad y la auditoría. El padrón consolidado se consulta con:

```text
GET /api/v1/personas?organizacionId={id}&q={texto}
```

Los permisos requeridos son `PERSONAL_IMPORTAR`, `PERSONAL_REVISAR`, `PERSONAL_APROBAR` y `PERSONAL_VER`. La validación, aprobación y consolidación requieren el permiso global `PERSONAL_APROBAR`.

## Estructura del código

El paquete raíz `com.sisge.ms` usa las capas `controller`, `service`, `dto`, `entity`, `repository`, `model`, `mapper`, `security`, `exception` y `config`. Las entidades usan nombres del dominio en español; los repositorios conservan los nombres en inglés.

La autorización se evalúa contra el rol de la membresía del usuario en la organización objetivo. Los permisos obtenidos en una organización no dan acceso a datos de otra.
