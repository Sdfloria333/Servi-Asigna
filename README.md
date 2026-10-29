# ServiAsigna — domicilios con asignación inteligente y GPS (Valledupar)

Proyecto de aula de Programación de Computadores III (SS462), Universidad Popular del Cesar.
Equipo: Sebastian Florian (líder técnico, backend), Oscar Araujo (interfaz) e Isaac Mattos (base de datos y documentación).

Spring Boot 3.3 · Java 21 · **Oracle XE** · JPA/Hibernate · Spring Security con **JWT** · **STOMP/WebSocket** · Thymeleaf + Bootstrap · **Leaflet + OpenStreetMap**

## 1. Preparar Oracle (una sola vez)
1. Instala **Oracle Database 21c XE** (o 18c XE) para Windows. Durante la instalación te pide una contraseña: es la de `SYSTEM`.
2. Abre **SQL Developer** (o `sqlplus`) y conéctate como `SYSTEM` al servicio **XEPDB1**
   (host `localhost`, puerto `1521`, nombre del servicio `XEPDB1`).
3. Ejecuta el archivo `src/main/resources/db/oracle/00_crear_usuario.sql`. Crea el usuario `DESPACHO` con clave `Despacho2026`.

**Las tablas no se crean a mano**: al arrancar, la aplicación ejecuta los scripts versionados
`src/main/resources/db/migracion/V1__esquema.sql` (25 tablas, llaves foráneas, índices y restricciones) y
`V2__vistas_reportes.sql` (vistas `VW_SERVICIO_TIEMPOS` y `VW_RESUMEN_DIARIO`), `V3__quitar_documentos.sql`
(borra la tabla de documentos de los domiciliarios, que ya no se usa) y `V4__quitar_modo_simulacion.sql` (borra el
parámetro del joystick de pruebas), y lo anota en la tabla `ESQUEMA_VERSION`.
Al final quedan **24 tablas** de la aplicación más `ESQUEMA_VERSION`.
Si tu base ya tenía scripts aplicados, al arrancar solo se aplican los que falten y los datos que ya cargaste se conservan.
Para cambiar el esquema después se agrega un `V5__....sql`; los ya aplicados no se editan (si se editan, la app avisa y no arranca).

Si tu Oracle usa otros datos, cámbialos con variables de entorno (o en `application.properties`):

| Variable | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:oracle:thin:@localhost:1521/XEPDB1` |
| `DB_USUARIO` | `DESPACHO` |
| `DB_CLAVE` | `Despacho2026` |
| `JWT_SECRETO` | uno de desarrollo (cámbialo en producción, mínimo 32 caracteres) |

Empezar de cero: `DROP USER DESPACHO CASCADE;` como SYSTEM y vuelve a ejecutar `00_crear_usuario.sql`.

## 2. Ejecutar
En VS Code abre la carpeta `despacho` y ejecuta `DespachoApplication`, o en la terminal:

    mvn spring-boot:run

Abre http://localhost:8080. La primera vez cada persona acepta la política de tratamiento de datos (Ley 1581).

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador (cuentas, parámetros, tarifa, auditoría) | `admin` | `admin123` |
| Despachador (operación) | `central` | `central123` |
| Domiciliarios | `juan`, `maria`, `pedro`, `ana` | `domi1234` |
| Solicitud pendiente (para probar la aprobación) | `luis` | `domi1234` |

Para ver varias personas a la vez usa una ventana normal y otras de incógnito (cada una con su sesión).
Los datos de prueba se crean solo si `despacho.datos-demo=true`.

**Celular**: el GPS y el micrófono exigen https: `mvn spring-boot:run -Dspring-boot.run.profiles=https` y abre `https://IP-DE-TU-PC:8443`.
La app del domiciliario usa siempre el GPS real. Para recoger o entregar debe estar a menos de `llegada-metros` (80 m) del punto;
para una demostración en un salón crea servicios cerca de donde estás o sube ese parámetro en Cuentas → Parámetros.

## 2.1 Cargar los datos de prueba (opcional, recomendado)
En `src/main/resources/db/oracle/` están:

| Archivo | Para qué |
|---|---|
| `00_crear_usuario.sql` | Crea el usuario DESPACHO (una vez, como SYSTEM) |
| `01_datos_demo.sql` | Una semana de operación: 8 clientes, 68 servicios con historial, ofertas, turnos, GPS y novedades |
| `02_consultas_demo.sql` | Consultas para mostrar la base: estructura, JOIN, agrupaciones, vistas y restricciones |
| `03_preguntas_y_consultas.sql` | 40 preguntas con su consulta |

Pasos: arranca el programa una vez (crea las tablas y los domiciliarios), **apágalo**, copia los archivos a `C:\sql` y ejecuta:

    sqlplus DESPACHO/Despacho2026@localhost:1521/XEPDB1
    @C:\sql\01_datos_demo.sql

Ejecútalo una sola vez y vuelve a arrancar el programa.

## 3. Qué hace
- **Servicios** (central): mapa OpenStreetMap en vivo, crear servicios buscando la dirección (Nominatim) o con clics en el mapa,
  cliente frecuente por teléfono, tamaño, prioridad, forma de pago, valor sugerido por tarifa, programar a futuro.
  Modo del motor **Manual / Difusión / Automático**. Cola con semáforo de tiempo, asignar, quitar y cancelar con motivo,
  y detalle con línea de tiempo, ofertas, prueba de entrega y decisiones del motor.
- **Domiciliarios** (central): estado, vehículo y carga, ruta, turno y recaudo, mensajes; novedades con su decisión.
- **Resumen**: alertas, indicadores I1–I7 frente a la línea base, flujo, desempeño y CSV.
- **Cuentas** (admin): aprobar domiciliarios, estados de cuenta, claves temporales, vehículos (placa), parámetros, tarifa y auditoría.
- **App del domiciliario** (`/domi`, instalable como PWA): disponible / pausa / fuera de servicio, oferta con temporizador,
  recoger y entregar con PIN, foto, firma y valor cobrado, novedades, mensajes, turno, GPS, radio de voz y
  cola sin conexión (lo que confirma sin señal se envía al volver).
- **Tiempo real**: STOMP en `/ws/stomp` (`/topic/central`, `/user/queue/avisos`) y radio de voz en `/ws/radio`.
- **Seguridad**: token JWT con expiración en cookie HttpOnly (o `Authorization: Bearer` pidiéndolo en `POST /api/auth/token`),
  roles ADMIN / DESPACHADOR / DOMICILIARIO, sesión única por domiciliario, cambio obligatorio de clave temporal, CSRF.

## 4. Mapa del código
    src/main/java/co/despacho/
    ├── dominio/       Entidades JPA (tablas) y máquina de estados del servicio
    ├── persistencia/  Repositorios Spring Data
    ├── motor/ reglas/ Motor de asignación y sus reglas (agrega reglas con @Component)
    ├── servicio/      DespachoService (operación en memoria + guardado en Oracle), parámetros, consultas
    ├── usuarios/ catalogo/ reportes/   Cuentas, clientes/vehículos/tarifas e indicadores
    ├── seguridad/     JWT, filtros, permisos (SeguridadConfig)
    ├── tiemporeal/ radio/   STOMP y radio de voz
    ├── api/           REST (JSON): CentralApi, AdminApi, DomiApi, AuthApi
    ├── web/           Páginas
    └── config/        MigradorEsquema (scripts SQL versionados) y el motor
    src/main/resources/db/          Scripts de Oracle (migracion/ se aplica sola; oracle/ es para SQL Developer o sqlplus)
    src/main/resources/templates/   Páginas Thymeleaf
    src/main/resources/static/      JavaScript, estilos, Leaflet y la PWA del domiciliario
    docs/                           Documento del proyecto, requerimientos y diccionario de tablas

## 5. Qué se verificó
El entorno donde se construyó no tiene Oracle, así que se probó con la **misma aplicación Spring real** sobre H2 en modo Oracle:
los scripts V1 a V4 corren completos, Hibernate mapea las 24 tablas sin errores y una prueba de punta a punta de la API
(cuentas, política, sesión única, ofertas, recoger/entregar con PIN, cancelaciones, novedades, mensajes, clientes,
indicadores, CSV, administración) pasa completa, igual que una prueba en navegador de la central y la app del domiciliario (moviendo el GPS del navegador hasta cada parada).
Lo primero en tu PC: arrancar contra Oracle y revisar la consola; si algo falla allí, el mensaje dice qué script y qué sentencia.

## 6. Documentación
| Archivo | Contenido |
|---|---|
| `docs/REQUERIMIENTOS.md` | Requerimientos funcionales, reglas de negocio y no funcionales que el programa cumple hoy |
| `docs/Proyecto_de_Aula_ServiAsigna.docx` | Documento del proyecto de aula (Fases 1 a 4) |
| `docs/Diccionario_de_tablas_ServiAsigna.docx` | Qué guarda cada tabla, con los datos de prueba |
