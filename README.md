# TacoCloud

TacoCloud es una aplicación distribuida y reactiva desarrollada con Spring Boot para gestionar pedidos de tacos. El sistema incluye autenticación y autorización, catálogo de ingredientes, procesamiento de órdenes, precios, inventario, flujo de cocina, mensajería mediante RabbitMQ, Transactional Outbox, procesamiento idempotente, observabilidad, métricas, API REST versionada y contrato OpenAPI.

## Descripción

El proyecto ofrece una API HTTP versionada y una interfaz web respaldadas por repositorios reactivos de MongoDB. Los precios y las validaciones se calculan en el servidor, el inventario se reserva de manera transaccional, los datos de pago se tokenizan y los eventos de las órdenes se guardan en un outbox antes de publicarse en RabbitMQ. Kitchen consume esos eventos con detección de duplicados, reintentos limitados y una cola de mensajes no procesables.

La aplicación también proporciona autorización basada en roles, creación idempotente de órdenes, identificadores de correlación, anuncios operativos, métricas de Micrometer, indicadores de salud y un contrato OpenAPI.

## Arquitectura

```text
Cliente
  |
  v
API/UI TacoCloud (puerto 8080) ---- Replica set de MongoDB
  |                                      | órdenes, inventario,
  | Transactional Outbox                 | usuarios e idempotencia
  v
RabbitMQ --------------------------> Kitchen (puerto 8081)
                                           |
                                           v
                                  Base de datos MongoDB de Kitchen
```

El repositorio contiene un reactor Maven en `tacocloud/`. Sus módulos principales son:

- `tacocloud`: aplicación principal ejecutable y configuración del runtime.
- `tacocloud-api`: controladores, DTO, servicios de negocio, precios, inventario y outbox.
- `tacocloud-domain-mongodb`: modelos de dominio de MongoDB.
- `tacocloud-data-mongodb`: repositorios reactivos y migraciones.
- `tacocloud-security`: registro, autenticación y autorización.
- `tacocloud-messaging-contract`: contrato compartido y versionado de eventos.
- `tacocloud-messaging-rabbitmq`: adaptador productor para RabbitMQ.
- `tacocloud-kitchen`: aplicación consumidora independiente de Kitchen.
- `tacocloud-ui`: frontend Angular empaquetado en la aplicación principal.

## Funcionalidades principales

- Catálogo de ingredientes y tacos con disponibilidad, existencias y precios.
- Registro seguro y acceso por roles para usuarios, administradores y Kitchen.
- Cálculo de precios, cupones y reserva de inventario del lado del servidor.
- Métodos de pago tokenizados sin devolver PAN ni CVV.
- Ciclo de vida, cancelación, historial, favoritos y calificaciones de órdenes.
- Transactional Outbox y selección del transporte de mensajería durante la ejecución.
- Consumidor idempotente de RabbitMQ con reintentos y manejo de DLQ.
- Propagación de `X-Correlation-Id` desde HTTP hasta Kitchen.
- Soporte de `Idempotency-Key` para reintentos seguros de órdenes.
- Anuncios administrativos persistentes con expiración.
- Health de Actuator, métricas de negocio y sondas de readiness/liveness.
- API versionada en `/api/v1` con compatibilidad para `/api`.
- Contrato OpenAPI 3.0.

## Tecnologías utilizadas

- Java 11.
- Spring Boot 2.5.3 y Spring Framework 5.3.x.
- Reactor 3.4.x y Spring Data Reactive MongoDB.
- Maven Wrapper 3.9.16.
- MongoDB 4.4.29 para ejecución transaccional local y pruebas de integración.
- RabbitMQ 3.11 con el complemento de administración.
- Testcontainers 1.17.6.
- Frontend Angular 5. El complemento frontend de Maven obtiene Node.js 12 durante la construcción.

## Requisitos

- JDK 11.
- Docker Desktop, Docker Engine u otro runtime compatible con Docker.
- Git.
- Acceso a Internet durante la primera construcción para descargar Maven, Node.js y las dependencias.

No es obligatorio instalar Maven por separado porque el repositorio incluye Maven Wrapper dentro de `tacocloud/`.

## Clonar el repositorio

```bash
git clone <URL-del-repositorio>
cd <directorio-del-repositorio>/tacocloud
```

Todos los comandos Maven siguientes se ejecutan desde el directorio del reactor `tacocloud/`.

## Construcción del proyecto

Linux o macOS:

```bash
./mvnw clean package -DskipTests
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Los artefactos ejecutables se generan en:

- `tacocloud/target/tacocloud-0.0.17-SNAPSHOT.jar`
- `tacocloud-kitchen/target/tacocloud-kitchen-0.0.17-SNAPSHOT.jar`

El reactor resuelve directamente todos los módulos internos; no requiere una instalación previa en un repositorio Maven específico del usuario.

## Ejecución de pruebas

Inicia Docker y ejecuta:

```bash
./mvnw clean verify
```

En Windows:

```powershell
.\mvnw.cmd clean verify
```

La suite de integración crea contenedores aislados de MongoDB y RabbitMQ mediante Testcontainers. No depende de los servicios iniciados manualmente que se describen más adelante.

### Testcontainers y Docker Desktop

Algunas instalaciones recientes de Docker Desktop rechazan la versión antigua de la API de Docker seleccionada por la versión de Testcontainers/docker-java del proyecto. Sólo si aparece ese error de compatibilidad, ejecuta:

```powershell
.\mvnw.cmd "-Dapi.version=1.44" clean verify
```

En Linux o macOS:

```bash
./mvnw "-Dapi.version=1.44" clean verify
```

`api.version=1.44` es un workaround local de compatibilidad y no una propiedad de la aplicación ni un requisito universal.

## Infraestructura local

Desde `tacocloud/`, inicia MongoDB y RabbitMQ mediante el archivo Compose de la raíz:

```bash
docker compose -f ../compose.yaml up -d
```

La configuración inicia MongoDB 4.4.29 en el puerto `27017`, inicializa el replica set `rs0`, inicia RabbitMQ en el puerto `5672` y publica RabbitMQ Management en el puerto `15672`.

```bash
docker compose -f ../compose.yaml ps
docker compose -f ../compose.yaml down
```

El segundo comando detiene los servicios sin eliminar los datos de MongoDB.

### MongoDB

TacoCloud y Kitchen utilizan bases de datos separadas dentro del mismo replica set. Las URI deben incluir `?replicaSet=rs0` para permitir las transacciones de inventario y outbox.

### RabbitMQ

TacoCloud publica los eventos de órdenes y Kitchen los consume desde `tacocloud.order.queue`. La configuración local utiliza el puerto AMQP `5672`.

## Variables de entorno

| Variable | Descripción | Ejemplo de desarrollo |
|---|---|---|
| `SPRING_DATA_MONGODB_URI` | Conexión MongoDB de la aplicación principal | `mongodb://localhost:27017/tacocloud?replicaSet=rs0` |
| `TACOCLOUD_KITCHEN_MONGODB_URI` | Conexión MongoDB de Kitchen | `mongodb://localhost:27017/tacocloud-kitchen?replicaSet=rs0` |
| `TACOCLOUD_MESSAGING_TRANSPORT` | Adaptador productor | `rabbit` |
| `TACOCLOUD_MESSAGING_DESTINATION` | Cola de órdenes | `tacocloud.order.queue` |
| `TACOCLOUD_RABBIT_HOST` | Host de RabbitMQ | `localhost` |
| `TACOCLOUD_RABBIT_PORT` | Puerto AMQP | `5672` |
| `TACOCLOUD_RABBIT_USERNAME` | Usuario de RabbitMQ | `guest` |
| `TACOCLOUD_RABBIT_PASSWORD` | Contraseña de RabbitMQ | `guest` |
| `TACOCLOUD_TC11_DEMO_PASSWORD` | Contraseña de los usuarios demo locales | Elegir al menos 8 caracteres |
| `TACOCLOUD_ALLOWED_ORIGIN` | Origen permitido por CORS | `http://localhost:8080` |
| `TACOCLOUD_JMS_HEALTH_ENABLED` | Habilita el health check de Artemis/JMS | `false` |

Spring Boot Admin está deshabilitado por defecto. Cuando el servidor Admin esté activo, configura `TACOCLOUD_ADMIN_ENABLED=true`, `TACOCLOUD_ADMIN_URL`, `TACOCLOUD_ADMIN_USERNAME` y `TACOCLOUD_ADMIN_PASSWORD`. Las credenciales del datasource de producción, Artemis y correo también se proporcionan mediante variables de entorno; consulta `.env.example`.

El archivo `.env.example` es una plantilla para la ejecución local desde el IDE. Cópialo como `.env`, elige una contraseña demo y nunca agregues `.env` al repositorio.

## Ejecutar TacoCloud

Construye primero el reactor. Desde `tacocloud/`, configura una contraseña demo local e inicia el JAR ejecutable.

Linux o macOS:

```bash
export TACOCLOUD_TC11_DEMO_PASSWORD='elige-una-contrasena-demo'
export TACOCLOUD_MESSAGING_TRANSPORT='rabbit'
export SPRING_DATA_MONGODB_URI='mongodb://localhost:27017/tacocloud?replicaSet=rs0'
java -jar tacocloud/target/tacocloud-0.0.17-SNAPSHOT.jar --spring.profiles.active=tc11-demo
```

Windows PowerShell:

```powershell
$env:TACOCLOUD_TC11_DEMO_PASSWORD = 'elige-una-contrasena-demo'
$env:TACOCLOUD_MESSAGING_TRANSPORT = 'rabbit'
$env:SPRING_DATA_MONGODB_URI = 'mongodb://localhost:27017/tacocloud?replicaSet=rs0'
java -jar tacocloud\target\tacocloud-0.0.17-SNAPSHOT.jar --spring.profiles.active=tc11-demo
```

La aplicación inicia en `http://localhost:8080` y crea estos usuarios exclusivos para desarrollo con la contraseña configurada:

- `user-a`, `user-b`: `ROLE_USER`.
- `admin-test`: `ROLE_ADMIN`.
- `kitchen-test`: `ROLE_KITCHEN`.

No habilites el perfil `tc11-demo` ni utilices estas cuentas en producción.

## Ejecutar Kitchen

En una segunda terminal, desde `tacocloud/`:

Linux o macOS:

```bash
export TACOCLOUD_KITCHEN_MONGODB_URI='mongodb://localhost:27017/tacocloud-kitchen?replicaSet=rs0'
java -jar tacocloud-kitchen/target/tacocloud-kitchen-0.0.17-SNAPSHOT.jar --spring.profiles.active=rabbitmq-listener
```

Windows PowerShell:

```powershell
$env:TACOCLOUD_KITCHEN_MONGODB_URI = 'mongodb://localhost:27017/tacocloud-kitchen?replicaSet=rs0'
java -jar tacocloud-kitchen\target\tacocloud-kitchen-0.0.17-SNAPSHOT.jar --spring.profiles.active=rabbitmq-listener
```

Kitchen inicia en `http://localhost:8081`.

## API REST

El prefijo admitido de la API es `http://localhost:8080/api/v1`. Las rutas heredadas `/api/**` continúan disponibles y devuelven los encabezados HTTP `Deprecation` y `Link` hacia su sucesor.

## OpenAPI

El contrato OpenAPI está disponible en `http://localhost:8080/openapi.yaml`.

## Observabilidad

- Resumen público de salud: `http://localhost:8080/actuator/health`
- Liveness: `http://localhost:8080/actuator/health/liveness`
- Readiness: `http://localhost:8080/actuator/health/readiness`
- Métricas: `http://localhost:8080/actuator/metrics`

Las métricas y la información detallada de Actuator requieren un administrador autenticado. El health puede devolver legítimamente `DOWN` o HTTP 503 si MongoDB, RabbitMQ o el Transactional Outbox no están disponibles.

## RabbitMQ Management

Abre `http://localhost:15672`. El servicio local de Compose utiliza los valores de desarrollo predeterminados `guest` / `guest`. Sustitúyelos en cualquier entorno compartido o de producción.

## Ejemplos de uso

Consultar el catálogo público conservando un identificador de correlación:

```bash
curl -i -H "X-Correlation-Id: ejemplo.solicitud-001" \
  http://localhost:8080/api/v1/ingredients
```

Comprobar la salud:

```bash
curl http://localhost:8080/actuator/health
```

Crear una orden idempotente después de obtener un token desde `/api/v1/payment-methods/tokenize`:

```bash
curl -u user-a:CONTRASENA_DEMO \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ejemplo-orden-0001" \
  -H "X-Correlation-Id: ejemplo.orden-001" \
  -d '{
    "deliveryName":"Usuario de ejemplo",
    "deliveryStreet":"Calle Ejemplo 100",
    "deliveryCity":"Ciudad Ejemplo",
    "deliveryState":"EX",
    "deliveryZip":"00000",
    "paymentMethodId":"ID_DEL_TOKEN",
    "items":[{"taco":{"name":"Taco de ejemplo","ingredientIds":["FLTO","CARN"]},"quantity":1}]
  }' \
  http://localhost:8080/api/v1/orders
```

Repetir la misma solicitud con el mismo `Idempotency-Key` devuelve la misma orden. Reutilizar la clave con una solicitud diferente devuelve HTTP 409 mediante el formato común `ApiProblem`.

## Estructura del proyecto

```text
.
|-- admin-server/                 Servidor opcional de Spring Boot Admin
|-- docs/                         Documentación funcional del proyecto
|-- tacocloud/                    Reactor Maven
|   |-- tacocloud/                Aplicación principal
|   |-- tacocloud-api/            API y servicios de negocio
|   |-- tacocloud-data-mongodb/   Persistencia reactiva
|   |-- tacocloud-security/       Seguridad y registro
|   |-- tacocloud-kitchen/        Consumidor Kitchen
|   `-- tacocloud-ui/             Frontend Angular
|-- compose.yaml                  MongoDB y RabbitMQ para desarrollo
`-- .env.example                  Plantilla de configuración local
```

## Documentación

La documentación detallada del proyecto, incluyendo las pruebas funcionales, evidencias de ejecución, validaciones y resultados obtenidos durante el desarrollo de TacoCloud, se encuentra disponible en los siguientes formatos:

- [Documentación de pruebas funcionales - PDF](docs/TacoCloud-Pruebas-Funcionales.pdf)
- [Documentación de pruebas funcionales - Word](docs/TacoCloud-Pruebas-Funcionales.docx)

## Seguridad

- Las contraseñas y credenciales de infraestructura se proporcionan mediante variables de entorno.
- Las cuentas demo sólo existen cuando está habilitado el perfil `tc11-demo`.
- Los DTO públicos no devuelven hashes de contraseñas, autoridades internas, PAN, CVV ni tokens de pago completos.
- Fuera del desarrollo local deben utilizarse credenciales dedicadas, TLS y un administrador de secretos.

## Solución de problemas

### Fallan las transacciones de MongoDB

La aplicación principal requiere un replica set para las escrituras transaccionales de inventario y outbox. Utiliza `compose.yaml` e incluye `?replicaSet=rs0` en ambas URI de MongoDB.

### Las pruebas de Testcontainers se omiten o no pueden negociar la API de Docker

Comprueba que Docker esté iniciado. Si el error indica específicamente una versión no compatible de la API de Docker, repite la ejecución con `"-Dapi.version=1.44"`.

### Un puerto ya está en uso

El entorno local utiliza los puertos `8080`, `8081`, `27017`, `5672` y `15672`. Detén el servicio que causa el conflicto o ajusta la propiedad de la aplicación o el mapeo de Compose correspondiente.

### RabbitMQ no está disponible

Comprueba `docker compose ps`, el health check de RabbitMQ y las variables `TACOCLOUD_RABBIT_*`. TacoCloud debe utilizar `TACOCLOUD_MESSAGING_TRANSPORT=rabbit` cuando se requiera la integración con Kitchen.

## Estado de las pruebas

La revisión de publicación validó el reactor Maven completo mediante una construcción limpia y comprobó por separado las pruebas de transacción/outbox de MongoDB, concurrencia de anuncios, idempotencia de órdenes, contrato de API, autorización y RabbitMQ E2E. Las pruebas deshabilitadas intencionalmente en el código se informan como omitidas y no se ocultan.
