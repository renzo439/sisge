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
   ```

3. Inicie la base de datos y la aplicación:

   ```powershell
   docker compose up --build
   ```

La API quedará disponible en `http://localhost:8080`. Docker espera a que MySQL acepte conexiones antes de iniciar la aplicación. MySQL ejecuta automáticamente `database/01-schema.sql` y `database/02-reference-data.sql` solo al crear el volumen `mysql-data` por primera vez. La aplicación usa automáticamente `MYSQL_PASSWORD` para conectarse a MySQL, por lo que no debe definir `DB_PASSWORD` cuando usa Docker.

> No elimine ni reinicialice el volumen de MySQL si contiene datos que desea conservar.

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
   ```

3. Para reemplazar algún valor de `.env` solo durante la sesión actual de PowerShell, defina las variables de entorno:

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/sisge?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:DB_USERNAME = "sisge"
   $env:DB_PASSWORD = "una-clave-local-segura"
   $env:JWT_SECRET = "un-secreto-aleatorio-de-al-menos-32-caracteres"
   $env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
   $env:BOOTSTRAP_ADMIN_PASSWORD = "una-clave-de-administrador-segura"
   ```

4. Inicie la aplicación:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   De forma predeterminada queda disponible en `http://localhost:8081`. Para usar otro puerto, asigne `SERVER_PORT` en `.env` con el formato `SERVER_PORT=8082`.

En el primer arranque, el administrador general se crea únicamente si `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` tienen valores y el correo todavía no existe. Su contraseña se almacena con BCrypt. Quite `BOOTSTRAP_ADMIN_PASSWORD` de su entorno después de provisionarlo.

## Compilación y pruebas

```powershell
.\mvnw.cmd clean install
```

Si usa IntelliJ IDEA, recargue el proyecto Maven después de abrirlo o cambiar el `pom.xml`: **Maven → Reload All Maven Projects**. El módulo compila explícitamente con `release 17`.

## Autenticación

Obtenga un token JWT:

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

## Flujo de importación

1. Un operador sube un `.xlsx` mediante `multipart/form-data`, con la parte `file`, a `POST /api/v1/organizaciones/{organizacionId}/importaciones/upload`.
2. La importación queda en `EN_PROCESO`; sus filas solo existen en la tabla temporal.
3. El operador edita los cinco campos adicionales y envía la carga a revisión.
4. La administración central puede observar, rechazar o aprobar.
5. Aprobar cierra la importación atómicamente y copia las filas a `reemplazantes_historico`.

Para migrar un Excel histórico que ya fue revisado, un administrador central con los permisos `IMPORTACIONES_APROBAR` e `IMPORTACIONES_CERRAR` puede enviar el archivo mediante `multipart/form-data` a `POST /api/v1/admin/importaciones/organizaciones/{organizacionId}/migrar-historico`. La importación se registra directamente como `CERRADA` y sus filas se copian al histórico en la misma transacción. El archivo debe cumplir el mismo formato y no contener filas con errores.

Para un Excel histórico consolidado con varios hospitales, use `POST /api/v1/admin/importaciones/migrar-historico-consolidado` con una parte `file` de tipo `multipart/form-data`. El archivo debe incluir las columnas `Hospital` y `UDEO` antes de las columnas habituales. `UDEO` identifica de forma única la organización: las organizaciones inexistentes se crean automáticamente con el nombre de `Hospital`, y se genera una importación cerrada por cada UDEO. Para archivos grandes, las filas se insertan en lotes de 500 y la respuesta contiene solo el resumen de cada importación creada. Si alguna fila tiene errores bloqueantes, la respuesta `409` incluye en `details` la fila de Excel (`fila`) y las reglas incumplidas (`errores`).

Estados permitidos:

```text
EN_PROCESO -> PENDIENTE_REVISION -> CERRADA
PENDIENTE_REVISION -> OBSERVADA -> PENDIENTE_REVISION
PENDIENTE_REVISION -> RECHAZADA
```

## Estructura del código

El paquete raíz `com.sisge.ms` usa las capas `controller`, `service`, `dto`, `entity`, `repository`, `model`, `mapper`, `security`, `exception` y `config`. Las entidades usan nombres del dominio en español; los repositorios conservan los nombres en inglés.

La autorización se evalúa contra el rol de la membresía del usuario en la organización objetivo. Los permisos obtenidos en una organización no dan acceso a datos de otra.
