# Guía para subir ServiAsigna al repositorio

Repositorio: https://github.com/Sdfloria333/Servi-Asigna

**Estado actual:** la parte 1 (estructura base, 7 commits de Sebastian) ya está fusionada en `develop` con el
pull request #1. Después de eso **el código pasó a inglés** (clases, métodos, variables, rutas y JSON) con comentarios en español
e inglés; los textos de las pantallas siguen en español, los mensajes de los commits también, y **la base de datos no cambió**
(tablas, columnas y códigos en español). Por eso las partes que faltan vienen en el paquete `co.serviasigna` y antes de ellas
va una parte corta de Sebastian que pasa la estructura base al inglés.

Cada commit hace un solo cambio y el proyecto compila después de cada uno. Cada parte va en su propia rama; cuando su
*pull request* se fusiona en `develop`, **la rama se borra**. Las partes deben fusionarse **en este orden**:

| Orden (PR) | Integrante | Rama (se borra al fusionarla) | Pull request hacia `develop` | Commits | Archivos |
|---|---|---|---|---|---|
| 1 | Sebastian Florian | `feature/estructura-base` | Estructura base del proyecto Spring Boot (**ya fusionado**) | 7 | 7 |
| 2 | Sebastian Florian | `refactor/codigo-en-ingles` | Estructura base con el código en inglés | 4 | 4 |
| 3 | Isaac Mattos | `feature/base-de-datos` | Base de datos Oracle, entidades y repositorios | 33 | 87 |
| 4 | Sebastian Florian | `feature/backend` | Backend: motor, servicios, seguridad y API | 34 | 63 |
| 5 | Oscar Araujo | `feature/interfaz` | Interfaz: panel de la central y app del domiciliario | 23 | 44 |

Faltan 94 commits (Sebastian Florian 38, Isaac Mattos 33, Oscar Araujo 23).
Al final, `develop` tiene 202 archivos. La entrega final (`develop` → `main`) está en el punto 3 y es opcional.

**¿Ya habías copiado archivos del zip anterior (el que estaba en español)?** Antes de empezar tu parte, borra de tu carpeta
`Servi-Asigna` todo lo que no esté en `develop`: en la terminal, `git checkout develop`, `git pull` y luego `git status`.
Si salen archivos en rojo (por ejemplo `src/main/java/co/despacho/dominio/`), bórralos con `git clean -fd src docs`
(solo borra archivos que nunca se subieron). `git status` debe quedar limpio.

## 1. Preparación (una sola vez, ya hecha en parte)

**Sebastian:** ya creó el repositorio y la rama `develop`. Falta, si no lo hizo: agregar a Oscar e Isaac como colaboradores
(*Settings → Collaborators → Add people*), marcar **Automatically delete head branches** en *Settings → General → Pull Requests*
y dejar `develop` como *Default branch*.

**Los tres (cada uno en su computador):**

1. Instala Git desde https://git-scm.com (opciones por defecto) y reinicia VS Code.
2. Configura tu nombre y **el mismo correo de tu cuenta de GitHub**. Si el correo no coincide, GitHub no te cuenta los commits:

```powershell
git config --global user.name "Tu Nombre Apellido"
git config --global user.email "el-correo-de-tu-cuenta-de-github@ejemplo.com"
```

3. Clona el repositorio (si ya lo tienes clonado, solo entra a la carpeta y haz `git checkout develop` y `git pull`):

```powershell
git clone https://github.com/Sdfloria333/Servi-Asigna.git
cd Servi-Asigna
git checkout develop
```

La primera vez que hagas `git push`, Git abre una ventana para iniciar sesión en GitHub.

## 2. Las partes que faltan

### Parte 2 · Sebastian Florian · `refactor/codigo-en-ingles`

**Qué sube:** La clase principal pasa a co.serviasigna.ServiAsignaApplication, el artefacto Maven a serviasigna y las llaves de configuración y variables de entorno a inglés. La base de datos no cambia. Se fusiona antes de las demás partes, que ya vienen en el paquete co.serviasigna.

**Archivos:** 4 en 4 commits atómicos. **Zip:** `2_Sebastian_codigo_en_ingles.zip`. **Antes de empezar:** que el pull request #1 (estructura base) esté fusionado en develop: ya lo está.

| # | Mensaje del commit | Archivos |
|---|---|---|
| 1 | `refactor: mover la clase principal al paquete co.serviasigna` | `…/ServiAsignaApplication.java` · borra `co/despacho/DespachoApplication.java` |
| 2 | `build: renombrar el artefacto Maven a serviasigna` | `pom.xml` |
| 3 | `refactor(config): llaves de configuración y variables de entorno en inglés` | `resources/application.properties` |
| 4 | `docs: actualizar el README con los nombres en inglés` | `README.md` |

**Paso 1. Copiar los archivos.** Descomprime `2_Sebastian_codigo_en_ingles.zip`. Adentro hay una carpeta `archivos`: copia **todo su contenido**
dentro de la carpeta `Servi-Asigna` (la del repositorio clonado) y acepta reemplazar si pregunta.
No copies la carpeta `archivos` en sí, sino lo que tiene adentro.

**Paso 2. Hacer los commits.** Abre la terminal de VS Code en la carpeta `Servi-Asigna` (Terminal → Nueva terminal).
Tienes dos formas, con el mismo resultado:

- **Rápida:** ejecuta el script que viene en el zip (cambia la ruta por donde lo descomprimiste). Hace todos los commits en orden,
  se detiene si algo falla y al final sube la rama:

```powershell
powershell -ExecutionPolicy Bypass -File "$HOME\Downloads\2_Sebastian_codigo_en_ingles\subir.ps1"
```

- **Manual:** copia y ejecuta estos comandos en orden:

```powershell
git checkout develop
git pull
git checkout -b refactor/codigo-en-ingles

# Commit 1 de 4
git rm src/main/java/co/despacho/DespachoApplication.java
git add src/main/java/co/serviasigna/ServiAsignaApplication.java
git commit -m "refactor: mover la clase principal al paquete co.serviasigna" -m "DespachoApplication pasa a ServiAsignaApplication: el código del proyecto queda en inglés."

# Commit 2 de 4
git add pom.xml
git commit -m "build: renombrar el artefacto Maven a serviasigna" -m "groupId co.serviasigna y artifactId serviasigna, igual que el paquete Java."

# Commit 3 de 4
git add src/main/resources/application.properties
git commit -m "refactor(config): llaves de configuración y variables de entorno en inglés" -m "despacho.* pasa a serviasigna.*; las variables son DB_USER, DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD y DISPATCHER_PASSWORD. La conexión a Oracle y el usuario DESPACHO no cambian."

# Commit 4 de 4
git add README.md
git commit -m "docs: actualizar el README con los nombres en inglés" -m "Clase principal, rutas, roles y mapa del código; explica que la base de datos conserva los nombres en español."

git status
git push -u origin refactor/codigo-en-ingles
```

Antes del `git push`, `git status` debe decir *nothing to commit, working tree clean*. Si aparece algún archivo en rojo,
no es de esta parte: no lo subas y avisa.

**Paso 3. Pull request.** En GitHub sale el aviso amarillo **Compare & pull request**. Revisa que diga
**base: develop ← compare: refactor/codigo-en-ingles**, pon el título `Estructura base con el código en inglés` y esta descripción: *La clase principal pasa a co.serviasigna.ServiAsignaApplication, el artefacto Maven a serviasigna y las llaves de configuración y variables de entorno a inglés. La base de datos no cambia. Se fusiona antes de las demás partes, que ya vienen en el paquete co.serviasigna.*
Pulsa **Create pull request**.

**Paso 4. Fusionar y borrar la rama.** Sebastian revisa la pestaña *Files changed* y pulsa
**Merge pull request → Create a merge commit → Confirm merge** (no uses *Squash* ni *Rebase*, se perderían los commits atómicos).
Luego pulsa **Delete branch**. Quien subió la parte limpia su copia local:

```powershell
git checkout develop
git pull
git branch -d refactor/codigo-en-ingles
git fetch --prune
```

### Parte 3 · Isaac Mattos · `feature/base-de-datos`

**Qué sube:** Esquema Oracle versionado (V1 a V4), migrador con control SHA-256, entidades JPA del dominio, repositorios Spring Data, scripts de la presentación y la documentación del proyecto. El código está en inglés con comentarios en español e inglés; las tablas y columnas siguen en español.

**Archivos:** 87 en 33 commits atómicos. **Zip:** `3_Isaac_base_de_datos.zip`. **Antes de empezar:** que el pull request #2 esté fusionado en develop y su rama borrada.

| # | Mensaje del commit | Archivos |
|---|---|---|
| 1 | `feat(bd): crear el esquema inicial de Oracle (V1)` | `resources/db/migracion/V1__esquema.sql` |
| 2 | `feat(bd): agregar las vistas de tiempos y resumen diario (V2)` | `resources/db/migracion/V2__vistas_reportes.sql` |
| 3 | `feat(bd): migrador de scripts versionados con suma SHA-256` | `…/config/SchemaMigrator.java` |
| 4 | `feat(bd): ejecutar las migraciones antes de iniciar JPA` | `…/config/MigrationBeforeJpa.java` |
| 5 | `feat(dominio): punto geográfico y convertidor de minutos a fecha` | `…/domain/GeoPoint.java`, `…/domain/MinutesConverter.java` |
| 6 | `feat(dominio): estados de cuenta, servicio, parada, oferta, novedad y domiciliario` | `…/domain/DbCoded.java`, `…/domain/AccountStatus.java`, `…/domain/DeliveryStatus.java`, `…/domain/StopStatus.java`, `…/domain/OfferStatus.java`, `…/domain/IncidentStatus.java`, `…/domain/CourierStatus.java` |
| 7 | `feat(dominio): tipos de parada, vehículo, evidencia, novedad y forma de pago` | `…/domain/StopType.java`, `…/domain/VehicleType.java`, `…/domain/ProofType.java`, `…/domain/IncidentType.java`, `…/domain/IncidentDecision.java`, `…/domain/PaymentMethod.java`, `…/domain/PackageSize.java`, `…/domain/Priority.java`, `…/domain/AssignmentMode.java`, `…/domain/AssignmentOrigin.java` |
| 8 | `feat(dominio): máquina de estados del servicio` | `…/domain/DeliveryStateMachine.java` |
| 9 | `feat(usuarios): entidad de cuenta de usuario y roles` | `…/users/UserAccount.java`, `…/users/Role.java` |
| 10 | `feat(dominio): convertidores JPA que guardan los códigos en español` | `…/domain/DbCodeConverters.java` |
| 11 | `feat(dominio): política de datos, aceptación y auditoría` | `…/domain/DataPolicy.java`, `…/domain/PolicyAcceptance.java`, `…/domain/AuditEntry.java` |
| 12 | `feat(dominio): parámetros, tarifa y plantillas de mensaje` | `…/domain/Setting.java`, `…/domain/Rate.java`, `…/domain/MessageTemplate.java` |
| 13 | `feat(dominio): clientes y direcciones guardadas` | `…/domain/Customer.java`, `…/domain/Address.java` |
| 14 | `feat(dominio): servicio, paradas de recogida y entrega y ruta` | `…/domain/Delivery.java`, `…/domain/DeliveryStop.java`, `…/domain/Stop.java`, `…/domain/Route.java` |
| 15 | `feat(dominio): domiciliario, vehículo, turno y ubicación GPS` | `…/domain/Courier.java`, `…/domain/Vehicle.java`, `…/domain/Shift.java`, `…/domain/GpsLocation.java` |
| 16 | `feat(dominio): ofertas, historial de estados, evidencias y decisiones del motor` | `…/domain/Offer.java`, `…/domain/StatusChange.java`, `…/domain/DeliveryProof.java`, `…/domain/EngineDecision.java` |
| 17 | `feat(dominio): novedades, seguimiento, mensajes y eventos sin conexión` | `…/domain/Incident.java`, `…/domain/IncidentFollowUp.java`, `…/domain/ChatMessage.java`, `…/domain/ProcessedEvent.java` |
| 18 | `feat(persistencia): repositorios de cuentas, política y auditoría` | `…/users/UserAccountRepository.java`, `…/persistence/Repositories.java`, `…/persistence/DataPolicyRepository.java`, `…/persistence/PolicyAcceptanceRepository.java`, `…/persistence/AuditEntryRepository.java` |
| 19 | `feat(persistencia): repositorios de parámetros, tarifas y plantillas` | `…/persistence/SettingRepository.java`, `…/persistence/RateRepository.java`, `…/persistence/MessageTemplateRepository.java` |
| 20 | `feat(persistencia): repositorios de clientes y direcciones` | `…/persistence/CustomerRepository.java`, `…/persistence/AddressRepository.java` |
| 21 | `feat(persistencia): repositorios de servicios, paradas, rutas y ofertas` | `…/persistence/DeliveryRepository.java`, `…/persistence/DeliveryStopRepository.java`, `…/persistence/RouteRepository.java`, `…/persistence/OfferRepository.java` |
| 22 | `feat(persistencia): repositorios de domiciliarios, vehículos, turnos y GPS` | `…/persistence/CourierRepository.java`, `…/persistence/VehicleRepository.java`, `…/persistence/ShiftRepository.java`, `…/persistence/GpsLocationRepository.java` |
| 23 | `feat(persistencia): repositorios de historial, evidencias, decisiones, novedades, mensajes y eventos` | `…/persistence/StatusChangeRepository.java`, `…/persistence/DeliveryProofRepository.java`, `…/persistence/EngineDecisionRepository.java`, `…/persistence/IncidentRepository.java`, `…/persistence/IncidentFollowUpRepository.java`, `…/persistence/ChatMessageRepository.java`, `…/persistence/ProcessedEventRepository.java` |
| 24 | `feat(bd): quitar la tabla de documentos de domiciliarios (V3)` | `resources/db/migracion/V3__quitar_documentos.sql` |
| 25 | `feat(bd): quitar el parámetro del modo simulación (V4)` | `resources/db/migracion/V4__quitar_modo_simulacion.sql` |
| 26 | `feat(bd): script para crear el usuario DESPACHO en Oracle` | `resources/db/oracle/00_crear_usuario.sql` |
| 27 | `feat(bd): datos de prueba de una semana de operación` | `resources/db/oracle/generar_datos_demo.py`, `resources/db/oracle/01_datos_demo.sql` |
| 28 | `feat(bd): consultas de demostración del esquema` | `resources/db/oracle/02_consultas_demo.sql` |
| 29 | `feat(bd): 40 preguntas con su consulta SQL para la presentación` | `resources/db/oracle/03_preguntas_y_consultas.sql` |
| 30 | `docs: requerimientos que cumple el sistema` | `docs/REQUERIMIENTOS.md` |
| 31 | `docs: diccionario de tablas con los datos de prueba` | `docs/Diccionario_de_tablas_ServiAsigna.docx` |
| 32 | `docs: documento del proyecto de aula` | `docs/Proyecto_de_Aula_ServiAsigna.docx` |
| 33 | `docs: guía para subir el proyecto al repositorio` | `docs/GUIA_REPOSITORIO.md` |

**Paso 1. Copiar los archivos.** Descomprime `3_Isaac_base_de_datos.zip`. Adentro hay una carpeta `archivos`: copia **todo su contenido**
dentro de la carpeta `Servi-Asigna` (la del repositorio clonado) y acepta reemplazar si pregunta.
No copies la carpeta `archivos` en sí, sino lo que tiene adentro.

**Paso 2. Hacer los commits.** Abre la terminal de VS Code en la carpeta `Servi-Asigna` (Terminal → Nueva terminal).
Tienes dos formas, con el mismo resultado:

- **Rápida:** ejecuta el script que viene en el zip (cambia la ruta por donde lo descomprimiste). Hace todos los commits en orden,
  se detiene si algo falla y al final sube la rama:

```powershell
powershell -ExecutionPolicy Bypass -File "$HOME\Downloads\3_Isaac_base_de_datos\subir.ps1"
```

- **Manual:** copia y ejecuta estos comandos en orden:

```powershell
git checkout develop
git pull
git checkout -b feature/base-de-datos

# Commit 1 de 33
git add src/main/resources/db/migracion/V1__esquema.sql
git commit -m "feat(bd): crear el esquema inicial de Oracle (V1)" -m "25 tablas con llaves foráneas, índices, restricciones CHECK de los estados y comentarios de cada tabla."

# Commit 2 de 33
git add src/main/resources/db/migracion/V2__vistas_reportes.sql
git commit -m "feat(bd): agregar las vistas de tiempos y resumen diario (V2)" -m "VW_SERVICIO_TIEMPOS calcula los minutos de cada servicio y VW_RESUMEN_DIARIO resume cada día."

# Commit 3 de 33
git add src/main/java/co/serviasigna/config/SchemaMigrator.java
git commit -m "feat(bd): migrador de scripts versionados con suma SHA-256" -m "Ejecuta los V*.sql pendientes, los anota en ESQUEMA_VERSION y no arranca si uno aplicado cambió."

# Commit 4 de 33
git add src/main/java/co/serviasigna/config/MigrationBeforeJpa.java
git commit -m "feat(bd): ejecutar las migraciones antes de iniciar JPA" -m "Hibernate espera a que el esquema esté al día antes de mapear las tablas."

# Commit 5 de 33
git add src/main/java/co/serviasigna/domain/GeoPoint.java src/main/java/co/serviasigna/domain/MinutesConverter.java
git commit -m "feat(dominio): punto geográfico y convertidor de minutos a fecha" -m "GeoPoint calcula distancias en línea recta; MinutesConverter guarda las horas internas como TIMESTAMP."

# Commit 6 de 33
git add src/main/java/co/serviasigna/domain/DbCoded.java src/main/java/co/serviasigna/domain/AccountStatus.java src/main/java/co/serviasigna/domain/DeliveryStatus.java src/main/java/co/serviasigna/domain/StopStatus.java src/main/java/co/serviasigna/domain/OfferStatus.java src/main/java/co/serviasigna/domain/IncidentStatus.java src/main/java/co/serviasigna/domain/CourierStatus.java
git commit -m "feat(dominio): estados de cuenta, servicio, parada, oferta, novedad y domiciliario" -m "Enums con los valores que permiten las restricciones CHECK de la base de datos. Cada valor está en inglés y guarda su código en español para Oracle (interfaz DbCoded)."

# Commit 7 de 33
git add src/main/java/co/serviasigna/domain/StopType.java src/main/java/co/serviasigna/domain/VehicleType.java src/main/java/co/serviasigna/domain/ProofType.java src/main/java/co/serviasigna/domain/IncidentType.java src/main/java/co/serviasigna/domain/IncidentDecision.java src/main/java/co/serviasigna/domain/PaymentMethod.java src/main/java/co/serviasigna/domain/PackageSize.java src/main/java/co/serviasigna/domain/Priority.java src/main/java/co/serviasigna/domain/AssignmentMode.java src/main/java/co/serviasigna/domain/AssignmentOrigin.java
git commit -m "feat(dominio): tipos de parada, vehículo, evidencia, novedad y forma de pago" -m "Incluye tamaño del paquete con sus unidades, prioridad, modo y origen de la asignación."

# Commit 8 de 33
git add src/main/java/co/serviasigna/domain/DeliveryStateMachine.java
git commit -m "feat(dominio): máquina de estados del servicio" -m "Define las transiciones permitidas y rechaza cualquier otra."

# Commit 9 de 33
git add src/main/java/co/serviasigna/users/UserAccount.java src/main/java/co/serviasigna/users/Role.java
git commit -m "feat(usuarios): entidad de cuenta de usuario y roles" -m "UserAccount con contraseña cifrada, rol y estado de cuenta; roles ADMIN, DISPATCHER y COURIER (en Oracle ADMIN, DESPACHADOR y DOMICILIARIO)."

# Commit 10 de 33
git add src/main/java/co/serviasigna/domain/DbCodeConverters.java
git commit -m "feat(dominio): convertidores JPA que guardan los códigos en español" -m "DbCodeConverters lee y escribe cada enumeración con su código de la base de datos (por ejemplo QUEUED ↔ EN_COLA); se aplican solos a todas las entidades."

# Commit 11 de 33
git add src/main/java/co/serviasigna/domain/DataPolicy.java src/main/java/co/serviasigna/domain/PolicyAcceptance.java src/main/java/co/serviasigna/domain/AuditEntry.java
git commit -m "feat(dominio): política de datos, aceptación y auditoría" -m "Versiones de la política (Ley 1581), quién la aceptó y el registro de acciones de la central."

# Commit 12 de 33
git add src/main/java/co/serviasigna/domain/Setting.java src/main/java/co/serviasigna/domain/Rate.java src/main/java/co/serviasigna/domain/MessageTemplate.java
git commit -m "feat(dominio): parámetros, tarifa y plantillas de mensaje" -m "Rate calcula el valor por km y tamaño y el pago al domiciliario."

# Commit 13 de 33
git add src/main/java/co/serviasigna/domain/Customer.java src/main/java/co/serviasigna/domain/Address.java
git commit -m "feat(dominio): clientes y direcciones guardadas" -m "Cliente frecuente por teléfono con varias direcciones y sus coordenadas."

# Commit 14 de 33
git add src/main/java/co/serviasigna/domain/Delivery.java src/main/java/co/serviasigna/domain/DeliveryStop.java src/main/java/co/serviasigna/domain/Stop.java src/main/java/co/serviasigna/domain/Route.java
git commit -m "feat(dominio): servicio, paradas de recogida y entrega y ruta" -m "Delivery solo cambia de estado por la máquina de estados y registra la hora de cada cambio."

# Commit 15 de 33
git add src/main/java/co/serviasigna/domain/Courier.java src/main/java/co/serviasigna/domain/Vehicle.java src/main/java/co/serviasigna/domain/Shift.java src/main/java/co/serviasigna/domain/GpsLocation.java
git commit -m "feat(dominio): domiciliario, vehículo, turno y ubicación GPS" -m "El domiciliario se identifica por nombre, cédula, celular y placa, con su estado en la aplicación."

# Commit 16 de 33
git add src/main/java/co/serviasigna/domain/Offer.java src/main/java/co/serviasigna/domain/StatusChange.java src/main/java/co/serviasigna/domain/DeliveryProof.java src/main/java/co/serviasigna/domain/EngineDecision.java
git commit -m "feat(dominio): ofertas, historial de estados, evidencias y decisiones del motor" -m "Registro de cada oferta con temporizador, cada cambio de estado, cada prueba de entrega y cada decisión."

# Commit 17 de 33
git add src/main/java/co/serviasigna/domain/Incident.java src/main/java/co/serviasigna/domain/IncidentFollowUp.java src/main/java/co/serviasigna/domain/ChatMessage.java src/main/java/co/serviasigna/domain/ProcessedEvent.java
git commit -m "feat(dominio): novedades, seguimiento, mensajes y eventos sin conexión" -m "ProcessedEvent evita procesar dos veces un evento enviado sin señal."

# Commit 18 de 33
git add src/main/java/co/serviasigna/users/UserAccountRepository.java src/main/java/co/serviasigna/persistence/Repositories.java src/main/java/co/serviasigna/persistence/DataPolicyRepository.java src/main/java/co/serviasigna/persistence/PolicyAcceptanceRepository.java src/main/java/co/serviasigna/persistence/AuditEntryRepository.java
git commit -m "feat(persistencia): repositorios de cuentas, política y auditoría" -m "Spring Data JPA escribe las consultas a partir del nombre de cada método."

# Commit 19 de 33
git add src/main/java/co/serviasigna/persistence/SettingRepository.java src/main/java/co/serviasigna/persistence/RateRepository.java src/main/java/co/serviasigna/persistence/MessageTemplateRepository.java
git commit -m "feat(persistencia): repositorios de parámetros, tarifas y plantillas" -m "Lectura de la configuración de la operación."

# Commit 20 de 33
git add src/main/java/co/serviasigna/persistence/CustomerRepository.java src/main/java/co/serviasigna/persistence/AddressRepository.java
git commit -m "feat(persistencia): repositorios de clientes y direcciones" -m "Búsqueda de clientes por teléfono y por nombre."

# Commit 21 de 33
git add src/main/java/co/serviasigna/persistence/DeliveryRepository.java src/main/java/co/serviasigna/persistence/DeliveryStopRepository.java src/main/java/co/serviasigna/persistence/RouteRepository.java src/main/java/co/serviasigna/persistence/OfferRepository.java
git commit -m "feat(persistencia): repositorios de servicios, paradas, rutas y ofertas" -m "Consultas por estado, por rango de fechas y por cliente."

# Commit 22 de 33
git add src/main/java/co/serviasigna/persistence/CourierRepository.java src/main/java/co/serviasigna/persistence/VehicleRepository.java src/main/java/co/serviasigna/persistence/ShiftRepository.java src/main/java/co/serviasigna/persistence/GpsLocationRepository.java
git commit -m "feat(persistencia): repositorios de domiciliarios, vehículos, turnos y GPS" -m "Incluye el borrado del historial de GPS más antiguo que el tiempo de retención."

# Commit 23 de 33
git add src/main/java/co/serviasigna/persistence/StatusChangeRepository.java src/main/java/co/serviasigna/persistence/DeliveryProofRepository.java src/main/java/co/serviasigna/persistence/EngineDecisionRepository.java src/main/java/co/serviasigna/persistence/IncidentRepository.java src/main/java/co/serviasigna/persistence/IncidentFollowUpRepository.java src/main/java/co/serviasigna/persistence/ChatMessageRepository.java src/main/java/co/serviasigna/persistence/ProcessedEventRepository.java
git commit -m "feat(persistencia): repositorios de historial, evidencias, decisiones, novedades, mensajes y eventos" -m "Completa un repositorio por cada tabla del esquema."

# Commit 24 de 33
git add src/main/resources/db/migracion/V3__quitar_documentos.sql
git commit -m "feat(bd): quitar la tabla de documentos de domiciliarios (V3)" -m "Borra DOCUMENTO_DOMICILIARIO y sus 2 parámetros; los demás datos se conservan."

# Commit 25 de 33
git add src/main/resources/db/migracion/V4__quitar_modo_simulacion.sql
git commit -m "feat(bd): quitar el parámetro del modo simulación (V4)" -m "La app del domiciliario usa siempre el GPS real del celular."

# Commit 26 de 33
git add src/main/resources/db/oracle/00_crear_usuario.sql
git commit -m "feat(bd): script para crear el usuario DESPACHO en Oracle" -m "Se ejecuta una vez como SYSTEM en XEPDB1."

# Commit 27 de 33
git add src/main/resources/db/oracle/generar_datos_demo.py src/main/resources/db/oracle/01_datos_demo.sql
git commit -m "feat(bd): datos de prueba de una semana de operación" -m "68 servicios en 944 INSERT con fechas relativas a SYSDATE, y el generador en Python que los produce."

# Commit 28 de 33
git add src/main/resources/db/oracle/02_consultas_demo.sql
git commit -m "feat(bd): consultas de demostración del esquema" -m "Estructura, consultas básicas, JOIN, agrupaciones, vistas y restricciones dentro de una transacción."

# Commit 29 de 33
git add src/main/resources/db/oracle/03_preguntas_y_consultas.sql
git commit -m "feat(bd): 40 preguntas con su consulta SQL para la presentación" -m "De consultas simples a subconsultas, agrupaciones y vistas."

# Commit 30 de 33
git add docs/REQUERIMIENTOS.md
git commit -m "docs: requerimientos que cumple el sistema" -m "50 requerimientos funcionales, 17 reglas de negocio y 12 no funcionales."

# Commit 31 de 33
git add docs/Diccionario_de_tablas_ServiAsigna.docx
git commit -m "docs: diccionario de tablas con los datos de prueba" -m "Qué guarda cada tabla, sus columnas clave y el mapa del esquema."

# Commit 32 de 33
git add docs/Proyecto_de_Aula_ServiAsigna.docx
git commit -m "docs: documento del proyecto de aula" -m "Fases 1 a 4: problema, requerimientos, diseño, desarrollo y documentación técnica."

# Commit 33 de 33
git add docs/GUIA_REPOSITORIO.md
git commit -m "docs: guía para subir el proyecto al repositorio" -m "Orden de las partes, ramas, commits y comandos de cada integrante."

git status
git push -u origin feature/base-de-datos
```

Antes del `git push`, `git status` debe decir *nothing to commit, working tree clean*. Si aparece algún archivo en rojo,
no es de esta parte: no lo subas y avisa.

**Paso 3. Pull request.** En GitHub sale el aviso amarillo **Compare & pull request**. Revisa que diga
**base: develop ← compare: feature/base-de-datos**, pon el título `Base de datos Oracle, entidades y repositorios` y esta descripción: *Esquema Oracle versionado (V1 a V4), migrador con control SHA-256, entidades JPA del dominio, repositorios Spring Data, scripts de la presentación y la documentación del proyecto. El código está en inglés con comentarios en español e inglés; las tablas y columnas siguen en español.*
Pulsa **Create pull request**.

**Paso 4. Fusionar y borrar la rama.** Sebastian revisa la pestaña *Files changed* y pulsa
**Merge pull request → Create a merge commit → Confirm merge** (no uses *Squash* ni *Rebase*, se perderían los commits atómicos).
Luego pulsa **Delete branch**. Quien subió la parte limpia su copia local:

```powershell
git checkout develop
git pull
git branch -d feature/base-de-datos
git fetch --prune
```

### Parte 4 · Sebastian Florian · `feature/backend`

**Qué sube:** Motor de asignación con reglas, casos de uso del despacho, cuentas y catálogos, seguridad con JWT, indicadores, API REST y tiempo real (STOMP y radio).

**Archivos:** 63 en 34 commits atómicos. **Zip:** `4_Sebastian_backend.zip`. **Antes de empezar:** que el pull request #3 esté fusionado en develop y su rama borrada.

| # | Mensaje del commit | Archivos |
|---|---|---|
| 1 | `feat(servicio): reloj de la operación y área de Valledupar` | `…/service/Clock.java`, `…/service/OperatingArea.java` |
| 2 | `feat(servicio): parámetros de operación editables` | `…/service/Settings.java` |
| 3 | `feat(servicio): agrupar los repositorios en DataStore` | `…/service/DataStore.java` |
| 4 | `feat(motor): distancias en línea recta y matriz de tiempos` | `…/engine/Distances.java`, `…/engine/DistanceMatrix.java`, `…/engine/Environment.java` |
| 5 | `feat(motor): contexto, horario y evaluación de una ruta` | `…/engine/RouteContext.java`, `…/engine/Schedule.java`, `…/engine/RouteEvaluation.java` |
| 6 | `feat(reglas): interfaces de reglas duras y blandas` | `…/rules/HardRule.java`, `…/rules/SoftRule.java` |
| 7 | `feat(reglas): prohibir exceder la capacidad y el máximo de servicios` | `…/rules/VehicleCapacity.java`, `…/rules/MaxDeliveries.java` |
| 8 | `feat(reglas): costos por atraso, distancia, reserva y balance de carga` | `…/rules/LateMinutes.java`, `…/rules/TravelledDistance.java`, `…/rules/FreeCourierReserve.java`, `…/rules/LoadBalance.java` |
| 9 | `feat(motor): evaluador de rutas con las reglas inyectadas` | `…/engine/RouteEvaluator.java` |
| 10 | `feat(motor): asignación por inserción de costo mínimo` | `…/engine/AssignmentEngine.java` |
| 11 | `feat(motor): distancias por calles reales con OSRM` | `…/engine/Routing.java` |
| 12 | `feat(config): registrar el motor y sus reglas como beans` | `…/config/EngineConfig.java` |
| 13 | `feat(servicio): datos de entrada para crear, editar y confirmar servicios` | `…/service/DeliveryData.java`, `…/service/EditData.java`, `…/service/Confirmation.java` |
| 14 | `feat(servicio): vistas del panel de la central y de la app` | `…/service/view/` |
| 15 | `feat(servicio): interfaz de notificaciones en tiempo real` | `…/service/Notifier.java` |
| 16 | `feat(servicio): casos de uso del despacho` | `…/service/DispatchService.java` |
| 17 | `feat(servicio): planificador de servicios programados` | `…/service/ScheduledDeliveryPlanner.java` |
| 18 | `feat(servicio): consultas de historial, detalle y novedades` | `…/service/QueryService.java` |
| 19 | `feat(catalogo): clientes, direcciones, vehículos, tarifas y plantillas` | `…/catalog/CatalogService.java` |
| 20 | `feat(usuarios): cuentas, registro, aprobación y política de datos` | `…/users/UserAccountService.java` |
| 21 | `feat(usuarios): crear los datos de demostración al iniciar` | `…/users/DemoDataLoader.java` |
| 22 | `feat(seguridad): emitir y validar el token JWT` | `…/security/TokenService.java` |
| 23 | `feat(seguridad): cargar usuarios y validar la sesión única` | `…/security/AccountDetailsService.java`, `…/security/SessionValidator.java` |
| 24 | `feat(seguridad): permisos por rol, CSRF y acceso obligatorio a clave y política` | `…/security/SecurityConfig.java`, `…/security/TokenResolver.java`, `…/security/AccessFilter.java` |
| 25 | `feat(reportes): indicadores I1 a I7, flujo, desempeño y CSV` | `…/reports/IndicatorService.java` |
| 26 | `feat(api): errores en JSON y rango de fechas` | `…/api/ErrorHandler.java`, `…/api/DateRange.java` |
| 27 | `feat(api): datos de las peticiones con validación` | `…/api/Requests.java`, `…/api/CurrentUser.java` |
| 28 | `feat(api): API de autenticación` | `…/api/AuthApi.java` |
| 29 | `feat(api): API de la central` | `…/api/CentralApi.java` |
| 30 | `feat(api): API del domiciliario` | `…/api/CourierApi.java` |
| 31 | `feat(api): API de administración` | `…/api/AdminApi.java` |
| 32 | `feat(tiempo-real): configurar STOMP y controlar las suscripciones` | `…/realtime/StompConfig.java`, `…/realtime/SubscriptionInterceptor.java` |
| 33 | `feat(tiempo-real): enviar el estado al panel y los avisos al domiciliario` | `…/realtime/StompNotifier.java` |
| 34 | `feat(radio): radio de voz por WebSocket` | `…/radio/RadioConfig.java`, `…/radio/RadioChannel.java` |

**Paso 1. Copiar los archivos.** Descomprime `4_Sebastian_backend.zip`. Adentro hay una carpeta `archivos`: copia **todo su contenido**
dentro de la carpeta `Servi-Asigna` (la del repositorio clonado) y acepta reemplazar si pregunta.
No copies la carpeta `archivos` en sí, sino lo que tiene adentro.

**Paso 2. Hacer los commits.** Abre la terminal de VS Code en la carpeta `Servi-Asigna` (Terminal → Nueva terminal).
Tienes dos formas, con el mismo resultado:

- **Rápida:** ejecuta el script que viene en el zip (cambia la ruta por donde lo descomprimiste). Hace todos los commits en orden,
  se detiene si algo falla y al final sube la rama:

```powershell
powershell -ExecutionPolicy Bypass -File "$HOME\Downloads\4_Sebastian_backend\subir.ps1"
```

- **Manual:** copia y ejecuta estos comandos en orden:

```powershell
git checkout develop
git pull
git checkout -b feature/backend

# Commit 1 de 34
git add src/main/java/co/serviasigna/service/Clock.java src/main/java/co/serviasigna/service/OperatingArea.java
git commit -m "feat(servicio): reloj de la operación y área de Valledupar" -m "Clock da la hora en minutos (zona de Bogotá); OperatingArea rechaza puntos fuera de la ciudad."

# Commit 2 de 34
git add src/main/java/co/serviasigna/service/Settings.java
git commit -m "feat(servicio): parámetros de operación editables" -m "31 parámetros con valor por defecto que se guardan en la tabla PARAMETRO y se aplican al instante."

# Commit 3 de 34
git add src/main/java/co/serviasigna/service/DataStore.java
git commit -m "feat(servicio): agrupar los repositorios en DataStore" -m "Un solo objeto inyectable con todos los repositorios."

# Commit 4 de 34
git add src/main/java/co/serviasigna/engine/Distances.java src/main/java/co/serviasigna/engine/DistanceMatrix.java src/main/java/co/serviasigna/engine/Environment.java
git commit -m "feat(motor): distancias en línea recta y matriz de tiempos" -m "Distances es la interfaz; DistanceMatrix guarda los km y minutos entre todos los puntos."

# Commit 5 de 34
git add src/main/java/co/serviasigna/engine/RouteContext.java src/main/java/co/serviasigna/engine/Schedule.java src/main/java/co/serviasigna/engine/RouteEvaluation.java
git commit -m "feat(motor): contexto, horario y evaluación de una ruta" -m "Datos que necesitan las reglas para juzgar una ruta y el resultado de evaluarla."

# Commit 6 de 34
git add src/main/java/co/serviasigna/rules/HardRule.java src/main/java/co/serviasigna/rules/SoftRule.java
git commit -m "feat(reglas): interfaces de reglas duras y blandas" -m "Una regla dura prohíbe una ruta; una blanda le suma costo."

# Commit 7 de 34
git add src/main/java/co/serviasigna/rules/VehicleCapacity.java src/main/java/co/serviasigna/rules/MaxDeliveries.java
git commit -m "feat(reglas): prohibir exceder la capacidad y el máximo de servicios" -m "VehicleCapacity y MaxDeliveries."

# Commit 8 de 34
git add src/main/java/co/serviasigna/rules/LateMinutes.java src/main/java/co/serviasigna/rules/TravelledDistance.java src/main/java/co/serviasigna/rules/FreeCourierReserve.java src/main/java/co/serviasigna/rules/LoadBalance.java
git commit -m "feat(reglas): costos por atraso, distancia, reserva y balance de carga" -m "LateMinutes, TravelledDistance, FreeCourierReserve y LoadBalance."

# Commit 9 de 34
git add src/main/java/co/serviasigna/engine/RouteEvaluator.java
git commit -m "feat(motor): evaluador de rutas con las reglas inyectadas" -m "Simula la ruta desde la posición actual y aplica todas las reglas registradas."

# Commit 10 de 34
git add src/main/java/co/serviasigna/engine/AssignmentEngine.java
git commit -m "feat(motor): asignación por inserción de costo mínimo" -m "Inserta el servicio en la mejor posición de la ruta de cada candidato y reordena las paradas."

# Commit 11 de 34
git add src/main/java/co/serviasigna/engine/Routing.java
git commit -m "feat(motor): distancias por calles reales con OSRM" -m "Si OSRM no responde, usa la línea recta corregida por el factor de rodeo."

# Commit 12 de 34
git add src/main/java/co/serviasigna/config/EngineConfig.java
git commit -m "feat(config): registrar el motor y sus reglas como beans" -m "Spring entrega al evaluador todas las reglas que encuentre."

# Commit 13 de 34
git add src/main/java/co/serviasigna/service/DeliveryData.java src/main/java/co/serviasigna/service/EditData.java src/main/java/co/serviasigna/service/Confirmation.java
git commit -m "feat(servicio): datos de entrada para crear, editar y confirmar servicios" -m "DeliveryData, EditData y Confirmation."

# Commit 14 de 34
git add src/main/java/co/serviasigna/service/view/
git commit -m "feat(servicio): vistas del panel de la central y de la app" -m "Objetos que se envían a las pantallas sin exponer las entidades."

# Commit 15 de 34
git add src/main/java/co/serviasigna/service/Notifier.java
git commit -m "feat(servicio): interfaz de notificaciones en tiempo real" -m "DispatchService avisa los cambios sin depender de STOMP."

# Commit 16 de 34
git add src/main/java/co/serviasigna/service/DispatchService.java
git commit -m "feat(servicio): casos de uso del despacho" -m "Crear, asignar, ofertar, aceptar, recoger, entregar, cancelar y novedades, con la operación en memoria y guardado en Oracle."

# Commit 17 de 34
git add src/main/java/co/serviasigna/service/ScheduledDeliveryPlanner.java
git commit -m "feat(servicio): planificador de servicios programados" -m "Libera los servicios programados cuando llega su hora."

# Commit 18 de 34
git add src/main/java/co/serviasigna/service/QueryService.java
git commit -m "feat(servicio): consultas de historial, detalle y novedades" -m "Búsqueda por fechas, estado o texto y la línea de tiempo de cada servicio."

# Commit 19 de 34
git add src/main/java/co/serviasigna/catalog/CatalogService.java
git commit -m "feat(catalogo): clientes, direcciones, vehículos, tarifas y plantillas" -m "CatalogService con auditoría de cada cambio."

# Commit 20 de 34
git add src/main/java/co/serviasigna/users/UserAccountService.java
git commit -m "feat(usuarios): cuentas, registro, aprobación y política de datos" -m "Claves temporales, cambio obligatorio, aceptación de la política y sesión única."

# Commit 21 de 34
git add src/main/java/co/serviasigna/users/DemoDataLoader.java
git commit -m "feat(usuarios): crear los datos de demostración al iniciar" -m "Cuentas de prueba, vehículos, clientes, tarifa, parámetros y plantillas si serviasigna.demo-data=true."

# Commit 22 de 34
git add src/main/java/co/serviasigna/security/TokenService.java
git commit -m "feat(seguridad): emitir y validar el token JWT" -m "Token HS256 firmado con el secreto de la configuración y con el identificador de la sesión."

# Commit 23 de 34
git add src/main/java/co/serviasigna/security/AccountDetailsService.java src/main/java/co/serviasigna/security/SessionValidator.java
git commit -m "feat(seguridad): cargar usuarios y validar la sesión única" -m "Si el usuario entra en otro equipo, el token anterior deja de servir."

# Commit 24 de 34
git add src/main/java/co/serviasigna/security/SecurityConfig.java src/main/java/co/serviasigna/security/TokenResolver.java src/main/java/co/serviasigna/security/AccessFilter.java
git commit -m "feat(seguridad): permisos por rol, CSRF y acceso obligatorio a clave y política" -m "SecurityConfig define quién entra a cada ruta; TokenResolver lee el token de la cookie o del encabezado; AccessFilter exige cambiar la clave temporal y aceptar la política antes de seguir."

# Commit 25 de 34
git add src/main/java/co/serviasigna/reports/IndicatorService.java
git commit -m "feat(reportes): indicadores I1 a I7, flujo, desempeño y CSV" -m "Cada indicador se compara con su línea base."

# Commit 26 de 34
git add src/main/java/co/serviasigna/api/ErrorHandler.java src/main/java/co/serviasigna/api/DateRange.java
git commit -m "feat(api): errores en JSON y rango de fechas" -m "ErrorHandler convierte las excepciones en respuestas con el código HTTP correcto."

# Commit 27 de 34
git add src/main/java/co/serviasigna/api/Requests.java src/main/java/co/serviasigna/api/CurrentUser.java
git commit -m "feat(api): datos de las peticiones con validación" -m "Records con Bean Validation y CurrentUser para saber qué usuario hace la petición."

# Commit 28 de 34
git add src/main/java/co/serviasigna/api/AuthApi.java
git commit -m "feat(api): API de autenticación" -m "Token, usuario actual, política y cambio de clave en /api/auth."

# Commit 29 de 34
git add src/main/java/co/serviasigna/api/CentralApi.java
git commit -m "feat(api): API de la central" -m "Servicios, asignación, novedades, mensajes, clientes, indicadores y CSV en /api/central."

# Commit 30 de 34
git add src/main/java/co/serviasigna/api/CourierApi.java
git commit -m "feat(api): API del domiciliario" -m "Estado, GPS, ofertas, recoger y entregar con pruebas, novedades y mensajes en /api/courier."

# Commit 31 de 34
git add src/main/java/co/serviasigna/api/AdminApi.java
git commit -m "feat(api): API de administración" -m "Cuentas, vehículos, parámetros, tarifa, política, plantillas y auditoría en /api/central/admin."

# Commit 32 de 34
git add src/main/java/co/serviasigna/realtime/StompConfig.java src/main/java/co/serviasigna/realtime/SubscriptionInterceptor.java
git commit -m "feat(tiempo-real): configurar STOMP y controlar las suscripciones" -m "Cada domiciliario solo puede escuchar su propia cola de avisos."

# Commit 33 de 34
git add src/main/java/co/serviasigna/realtime/StompNotifier.java
git commit -m "feat(tiempo-real): enviar el estado al panel y los avisos al domiciliario" -m "StompNotifier publica el estado cada 0,4 s cuando hay cambios."

# Commit 34 de 34
git add src/main/java/co/serviasigna/radio/RadioConfig.java src/main/java/co/serviasigna/radio/RadioChannel.java
git commit -m "feat(radio): radio de voz por WebSocket" -m "Canal «mantén para hablar» entre la central y los domiciliarios conectados."

git status
git push -u origin feature/backend
```

Antes del `git push`, `git status` debe decir *nothing to commit, working tree clean*. Si aparece algún archivo en rojo,
no es de esta parte: no lo subas y avisa.

**Paso 3. Pull request.** En GitHub sale el aviso amarillo **Compare & pull request**. Revisa que diga
**base: develop ← compare: feature/backend**, pon el título `Backend: motor, servicios, seguridad y API` y esta descripción: *Motor de asignación con reglas, casos de uso del despacho, cuentas y catálogos, seguridad con JWT, indicadores, API REST y tiempo real (STOMP y radio).*
Pulsa **Create pull request**.

**Paso 4. Fusionar y borrar la rama.** Sebastian revisa la pestaña *Files changed* y pulsa
**Merge pull request → Create a merge commit → Confirm merge** (no uses *Squash* ni *Rebase*, se perderían los commits atómicos).
Luego pulsa **Delete branch**. Quien subió la parte limpia su copia local:

```powershell
git checkout develop
git pull
git branch -d feature/backend
git fetch --prune
```

### Parte 5 · Oscar Araujo · `feature/interfaz`

**Qué sube:** Estilos, Leaflet, PWA, módulos JavaScript, páginas de acceso, panel de la central, app del domiciliario con GPS y los controladores de las páginas. Con esta parte el sistema queda completo.

**Archivos:** 44 en 23 commits atómicos. **Zip:** `5_Oscar_interfaz.zip`. **Antes de empezar:** que el pull request #4 esté fusionado en develop y su rama borrada.

| # | Mensaje del commit | Archivos |
|---|---|---|
| 1 | `style(ui): tema visual del sistema` | `resources/static/css/` |
| 2 | `feat(ui): ícono de la aplicación` | `resources/static/icons/` |
| 3 | `build(ui): agregar Leaflet 1.9.4 local` | `resources/static/vendor/` |
| 4 | `feat(pwa): manifiesto y service worker` | `resources/static/manifest.webmanifest`, `resources/static/sw.js` |
| 5 | `feat(ui): utilidades, etiquetas y funciones comunes` | `resources/static/js/util.js`, `resources/static/js/labels.js`, `resources/static/js/common.js` |
| 6 | `feat(ui): cliente de la API REST` | `resources/static/js/api.js` |
| 7 | `feat(ui): mapa con Leaflet, Nominatim y OSRM` | `resources/static/js/map.js` |
| 8 | `feat(ui): conexión STOMP en tiempo real` | `resources/static/js/stomp.js` |
| 9 | `feat(ui): radio «mantén para hablar»` | `resources/static/js/radio.js` |
| 10 | `feat(ui): panel lateral con el detalle del servicio` | `resources/static/js/detail.js` |
| 11 | `feat(ui): fragmentos comunes de las páginas` | `resources/templates/fragments.html` |
| 12 | `feat(ui): página de inicio de sesión` | `resources/templates/login.html` |
| 13 | `feat(ui): registro de domiciliarios` | `resources/templates/register.html` |
| 14 | `feat(ui): aceptar la política y cambiar la clave temporal` | `resources/templates/policy.html`, `resources/templates/change-password.html` |
| 15 | `feat(ui): página de acceso denegado` | `resources/templates/error/` |
| 16 | `feat(ui): pantalla Servicios de la central` | `resources/templates/central/deliveries.html`, `resources/static/js/pages/deliveries.js` |
| 17 | `feat(ui): pantalla Domiciliarios` | `resources/templates/central/couriers.html`, `resources/static/js/pages/couriers.js` |
| 18 | `feat(ui): pantalla Resumen` | `resources/templates/central/summary.html`, `resources/static/js/pages/summary.js` |
| 19 | `feat(ui): pantalla Cuentas y configuración` | `resources/templates/central/accounts.html`, `resources/static/js/pages/accounts.js` |
| 20 | `feat(ui): app del domiciliario` | `resources/templates/courier/`, `resources/static/js/pages/courier.js` |
| 21 | `feat(web): controlador de acceso, registro, clave y política` | `…/web/AccessController.java` |
| 22 | `feat(web): rutas de las páginas de la central y del domiciliario` | `…/web/CentralPages.java`, `…/web/CourierPages.java` |
| 23 | `feat(web): mostrar el usuario conectado en todas las páginas` | `…/web/PageModelAttributes.java` |

**Paso 1. Copiar los archivos.** Descomprime `5_Oscar_interfaz.zip`. Adentro hay una carpeta `archivos`: copia **todo su contenido**
dentro de la carpeta `Servi-Asigna` (la del repositorio clonado) y acepta reemplazar si pregunta.
No copies la carpeta `archivos` en sí, sino lo que tiene adentro.

**Paso 2. Hacer los commits.** Abre la terminal de VS Code en la carpeta `Servi-Asigna` (Terminal → Nueva terminal).
Tienes dos formas, con el mismo resultado:

- **Rápida:** ejecuta el script que viene en el zip (cambia la ruta por donde lo descomprimiste). Hace todos los commits en orden,
  se detiene si algo falla y al final sube la rama:

```powershell
powershell -ExecutionPolicy Bypass -File "$HOME\Downloads\5_Oscar_interfaz\subir.ps1"
```

- **Manual:** copia y ejecuta estos comandos en orden:

```powershell
git checkout develop
git pull
git checkout -b feature/interfaz

# Commit 1 de 23
git add src/main/resources/static/css/
git commit -m "style(ui): tema visual del sistema" -m "Colores, tarjetas, tablas, chips de estado, mapa y diseño para celular sobre Bootstrap."

# Commit 2 de 23
git add src/main/resources/static/icons/
git commit -m "feat(ui): ícono de la aplicación" -m "Ícono SVG usado en el navegador y al instalar la app."

# Commit 3 de 23
git add src/main/resources/static/vendor/
git commit -m "build(ui): agregar Leaflet 1.9.4 local" -m "El mapa funciona aunque no haya acceso a un CDN."

# Commit 4 de 23
git add src/main/resources/static/manifest.webmanifest src/main/resources/static/sw.js
git commit -m "feat(pwa): manifiesto y service worker" -m "Permite instalar la app del domiciliario y guarda los archivos estáticos en caché."

# Commit 5 de 23
git add src/main/resources/static/js/util.js src/main/resources/static/js/labels.js src/main/resources/static/js/common.js
git commit -m "feat(ui): utilidades, etiquetas y funciones comunes" -m "Formato de horas y pesos, nombres de estados y avisos en pantalla."

# Commit 6 de 23
git add src/main/resources/static/js/api.js
git commit -m "feat(ui): cliente de la API REST" -m "Peticiones con el token CSRF y manejo de errores."

# Commit 7 de 23
git add src/main/resources/static/js/map.js
git commit -m "feat(ui): mapa con Leaflet, Nominatim y OSRM" -m "Buscar direcciones, marcar puntos, dibujar domiciliarios y rutas por calles."

# Commit 8 de 23
git add src/main/resources/static/js/stomp.js
git commit -m "feat(ui): conexión STOMP en tiempo real" -m "Reconexión automática si se pierde la señal."

# Commit 9 de 23
git add src/main/resources/static/js/radio.js
git commit -m "feat(ui): radio «mantén para hablar»" -m "Graba con el micrófono mientras se mantiene el botón o la barra espaciadora."

# Commit 10 de 23
git add src/main/resources/static/js/detail.js
git commit -m "feat(ui): panel lateral con el detalle del servicio" -m "Línea de tiempo, ofertas, pruebas de entrega, decisiones del motor y novedades."

# Commit 11 de 23
git add src/main/resources/templates/fragments.html
git commit -m "feat(ui): fragmentos comunes de las páginas" -m "Cabecera, menú de la central y barra superior con el reloj y el modo del motor."

# Commit 12 de 23
git add src/main/resources/templates/login.html
git commit -m "feat(ui): página de inicio de sesión" -m "Formulario de usuario y contraseña."

# Commit 13 de 23
git add src/main/resources/templates/register.html
git commit -m "feat(ui): registro de domiciliarios" -m "Nombre, cédula, celular, usuario, contraseña y autorización de datos."

# Commit 14 de 23
git add src/main/resources/templates/policy.html src/main/resources/templates/change-password.html
git commit -m "feat(ui): aceptar la política y cambiar la clave temporal" -m "Páginas obligatorias antes de usar el sistema."

# Commit 15 de 23
git add src/main/resources/templates/error/
git commit -m "feat(ui): página de acceso denegado" -m "Se muestra cuando un rol entra a una pantalla que no le corresponde."

# Commit 16 de 23
git add src/main/resources/templates/central/deliveries.html src/main/resources/static/js/pages/deliveries.js
git commit -m "feat(ui): pantalla Servicios de la central" -m "Crear servicios en el mapa, modo de asignación, cola del día, asignar, revocar y cancelar."

# Commit 17 de 23
git add src/main/resources/templates/central/couriers.html src/main/resources/static/js/pages/couriers.js
git commit -m "feat(ui): pantalla Domiciliarios" -m "Estado en vivo, turno, recorrido GPS, mensajes, recaudo y novedades."

# Commit 18 de 23
git add src/main/resources/templates/central/summary.html src/main/resources/static/js/pages/summary.js
git commit -m "feat(ui): pantalla Resumen" -m "Alertas en vivo, indicadores frente a la línea base, flujo, desempeño y descarga en CSV."

# Commit 19 de 23
git add src/main/resources/templates/central/accounts.html src/main/resources/static/js/pages/accounts.js
git commit -m "feat(ui): pantalla Cuentas y configuración" -m "Aprobar domiciliarios, estados de cuenta, placa del vehículo, claves, parámetros, tarifa y auditoría."

# Commit 20 de 23
git add src/main/resources/templates/courier/ src/main/resources/static/js/pages/courier.js
git commit -m "feat(ui): app del domiciliario" -m "Estado, ofertas con temporizador, ruta, GPS real, recoger y entregar con PIN, foto o firma, y cola sin conexión."

# Commit 21 de 23
git add src/main/java/co/serviasigna/web/AccessController.java
git commit -m "feat(web): controlador de acceso, registro, clave y política" -m "Redirige a cada rol a su pantalla después de entrar."

# Commit 22 de 23
git add src/main/java/co/serviasigna/web/CentralPages.java src/main/java/co/serviasigna/web/CourierPages.java
git commit -m "feat(web): rutas de las páginas de la central y del domiciliario" -m "CentralPages y CourierPages."

# Commit 23 de 23
git add src/main/java/co/serviasigna/web/PageModelAttributes.java
git commit -m "feat(web): mostrar el usuario conectado en todas las páginas" -m "Nombre y rol para el menú y la barra superior."

git status
git push -u origin feature/interfaz
```

Antes del `git push`, `git status` debe decir *nothing to commit, working tree clean*. Si aparece algún archivo en rojo,
no es de esta parte: no lo subas y avisa.

**Paso 3. Pull request.** En GitHub sale el aviso amarillo **Compare & pull request**. Revisa que diga
**base: develop ← compare: feature/interfaz**, pon el título `Interfaz: panel de la central y app del domiciliario` y esta descripción: *Estilos, Leaflet, PWA, módulos JavaScript, páginas de acceso, panel de la central, app del domiciliario con GPS y los controladores de las páginas. Con esta parte el sistema queda completo.*
Pulsa **Create pull request**.

**Paso 4. Fusionar y borrar la rama.** Sebastian revisa la pestaña *Files changed* y pulsa
**Merge pull request → Create a merge commit → Confirm merge** (no uses *Squash* ni *Rebase*, se perderían los commits atómicos).
Luego pulsa **Delete branch**. Quien subió la parte limpia su copia local:

```powershell
git checkout develop
git pull
git branch -d feature/interfaz
git fetch --prune
```

## 3. Entrega final a `main` (opcional)

Con todas las partes en `develop`, Sebastian puede publicar la versión estable sin borrar `develop`:
*Pull requests → New pull request*, **base: main ← compare: develop**, título `Versión 1.0 de ServiAsigna`,
**Create pull request → Merge pull request → Create a merge commit**. Luego *Releases → Draft a new release* con la etiqueta
`v1.0` sobre `main`. Aquí **no** se borra la rama: `develop` sigue siendo la rama de trabajo.

## 4. Cómo debe quedar el repositorio

**Ramas que quedan: 2.** `develop` con todo el proyecto y `main` (con el *Initial commit*, o igual a `develop` si se hizo
la entrega final). Las ramas de cada parte se crearon, se fusionaron y se borraron; siguen visibles en cada pull request cerrado.

**Commits:** 1 inicial + 101 atómicos + 5 de fusión = 107 en `develop`
(108 si se fusiona en `main`). En *Insights → Contributors*, sin contar fusiones:
Sebastian 46 (incluye el inicial), Isaac 33 y Oscar 23.

**Pull requests:** 5 fusionados hacia `develop` (#1 a #5), más el #6 hacia `main` si se hace la entrega final.

**Historial de `develop`** (`git log --first-parent --oneline develop`, de lo más nuevo a lo más viejo):

```
Merge pull request #5 from Sdfloria333/feature/interfaz             ← 23 commits de Oscar
Merge pull request #4 from Sdfloria333/feature/backend              ← 34 commits de Sebastian
Merge pull request #3 from Sdfloria333/feature/base-de-datos        ← 33 commits de Isaac
Merge pull request #2 from Sdfloria333/refactor/codigo-en-ingles    ← 4 commits de Sebastian
Merge pull request #1 from Sdfloria333/feature/estructura-base      ← 7 commits de Sebastian (ya fusionado)
Initial commit
```

**Carpetas en `develop`:**

```
Servi-Asigna/
├── README.md, pom.xml, .gitignore, .gitattributes      (Partes 1 y 2)
├── docs/                     requerimientos, diccionario, documento del proyecto y esta guía (Parte 3)
└── src/main/
    ├── java/co/serviasigna/
    │   ├── ServiAsignaApplication.java   (Parte 2)
    │   ├── domain/ persistence/          entidades, enumeraciones con su código de Oracle y repositorios (Parte 3)
    │   ├── config/                       migrador de scripts (Parte 3) y registro del motor (Parte 4)
    │   ├── users/                        UserAccount, Role, UserAccountRepository (Parte 3); UserAccountService, DemoDataLoader (Parte 4)
    │   ├── service/ engine/ rules/       casos de uso y motor de asignación (Parte 4)
    │   ├── catalog/ reports/             clientes, vehículos, tarifas e indicadores (Parte 4)
    │   ├── security/ api/                JWT, permisos y API REST (Parte 4)
    │   ├── realtime/ radio/              STOMP y radio de voz (Parte 4)
    │   └── web/                          controladores de las páginas (Parte 5)
    └── resources/
        ├── application*.properties, despacho-https.p12   (Partes 1 y 2)
        ├── db/migracion/                 V1 a V4, se aplican solos al arrancar (Parte 3)
        ├── db/oracle/                    scripts para la presentación (Parte 3)
        ├── templates/                    páginas (Parte 5)
        └── static/                       CSS, JavaScript, Leaflet y la PWA (Parte 5)
```

**Lo que nunca se sube:** `target/` (se genera al compilar), `.vscode/`, `.idea/`, archivos `.log` y contraseñas reales.
El `.gitignore` ya los excluye.

**Para el documento del proyecto (Gestión con Git):** 107 commits en `develop`, 6 ramas creadas
(`develop` y 5 de partes), 5 fusionadas en `develop` y borradas después de fusionarse; última fusión
`feature/interfaz → develop` (pull request #5).

## 5. Comprobación final

En una carpeta nueva:

```powershell
git clone https://github.com/Sdfloria333/Servi-Asigna.git prueba-final
cd prueba-final
git checkout develop
git log --oneline --graph
mvn spring-boot:run
```

La consola debe mostrar `Scripts aplicados` (o `Base de datos al día`) y `Started ServiAsignaApplication`.
Si tu Oracle ya tenía los datos de antes, se conservan: la base de datos no cambió con el paso del código a inglés.

## 6. Problemas comunes

| Mensaje | Qué pasa | Solución |
|---|---|---|
| `Permission to ... denied` / `403` | No aceptaste la invitación de colaborador | Acepta la invitación del correo y repite el `git push` |
| `running scripts is disabled` | PowerShell bloquea scripts | Usa el comando con `-ExecutionPolicy Bypass` tal cual, o los comandos manuales |
| `Hay archivos del zip anterior (en español)` | Copiaste archivos del zip viejo | Borra `src/main/java/co/despacho/dominio` (o usa `git clean -fd src docs`) y repite |
| `A branch named '...' already exists` | Ya habías empezado esa parte | `git checkout develop` y `git branch -D nombre-de-la-rama`, y vuelve a empezar |
| `rejected ... (fetch first)` | Tu copia está atrasada | `git pull` y luego `git push` |
| `pathspec ... did not match any files` | Copiaste la carpeta `archivos` en vez de su contenido, o estás en otra carpeta | Revisa que `src/` esté dentro de `Servi-Asigna` |
| `git status` muestra archivos en rojo al final | Copiaste archivos que no son de tu parte | No los agregues; avisa en el grupo |
| Tus commits salen con otro nombre en GitHub | El correo de `git config` no es el de tu cuenta | Corrige `git config --global user.email` antes de seguir |
| `The branch ... is not fully merged` al borrarla | El pull request todavía no se fusionó | Espera la fusión en GitHub, haz `git pull` en develop y repite |
