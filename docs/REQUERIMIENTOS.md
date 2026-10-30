# Requerimientos de ServiAsigna

Versión 3 del proyecto · 28/10/2026

## 1. Introducción

Este documento recoge los requerimientos que el sistema ServiAsigna **cumple hoy**, verificados contra el código de la versión 3 del proyecto. No incluye funciones planeadas: los requerimientos nuevos se agregarán en versiones posteriores.

ServiAsigna es una aplicación web para la central de domicilios de Valledupar. La central crea los servicios de recogida y entrega, un motor los asigna al domiciliario más conveniente y cada domiciliario los atiende desde su celular con GPS.

**Cambios frente a la primera versión:**

- Se quitó el control de documentos de los domiciliarios (licencia, SOAT, vencimientos y sus alertas). La tabla `DOCUMENTO_DOMICILIARIO` se borró con el script `V3__quitar_documentos.sql`.
- Se quitó la simulación «motor contra asignación manual» de la pantalla Resumen y el joystick de pruebas de la app del domiciliario: la ubicación sale siempre del GPS real. El parámetro del joystick se borró con el script `V4__quitar_modo_simulacion.sql`.
- El domiciliario se identifica por nombre, cédula, celular, placa de su vehículo y estado en la aplicación.
- El código del programa (clases, métodos, variables, rutas de la API y JSON) pasó a inglés y tiene comentarios en español e inglés. Los textos que ve el usuario siguen en español y la base de datos no cambió: conserva sus tablas, columnas y códigos en español.

**Cómo leer los códigos:** RF = requerimiento funcional, RN = regla de negocio, RNF = requerimiento no funcional. La columna «Dónde se ve» indica la pantalla o el componente que lo cumple.

## 2. Actores y roles

El sistema tiene tres roles; cada pantalla y cada servicio de la API revisa el rol antes de responder.

| Rol | Quién es | Pantallas | Qué puede hacer |
| --- | --- | --- | --- |
| Administrador (ADMIN) | Dueño o encargado de la central | Servicios, Domiciliarios, Resumen, Cuentas | Todo lo del despachador, más aprobar domiciliarios, manejar cuentas, vehículos, parámetros, tarifa, política de datos y auditoría |
| Despachador (DISPATCHER; en Oracle DESPACHADOR) | Operador de la central | Servicios, Domiciliarios, Resumen | Crear, asignar, revocar y cancelar servicios; atender novedades; mensajes y radio; ver indicadores |
| Domiciliario (COURIER; en Oracle DOMICILIARIO) | Mensajero en moto, bicicleta, carro o a pie | App del celular (/courier) | Cambiar su estado, aceptar o rechazar ofertas, recoger y entregar, reportar novedades, mensajes y radio |

Cuentas de prueba: `admin` / `admin123`, `central` / `central123` y los domiciliarios `juan`, `maria`, `pedro` y `ana` con clave `domi1234` (`luis` queda pendiente de aprobación).

## 3. Requerimientos funcionales

El programa cumple 50 requerimientos funcionales, agrupados en 9 módulos.

### M1. Acceso y cuentas

| Código | Requerimiento | Dónde se ve |
| --- | --- | --- |
| RF-01 | Iniciar sesión con usuario y contraseña; el sistema lleva a cada rol a su pantalla | /login |
| RF-02 | El domiciliario se registra solo con nombre, cédula, celular, usuario y contraseña, y queda pendiente de aprobación | /registro |
| RF-03 | El administrador aprueba o rechaza las solicitudes de registro | Cuentas |
| RF-04 | El administrador cambia el estado de una cuenta: activa, inactiva o suspendida | Cuentas |
| RF-05 | El administrador crea cuentas de la central (administrador o despachador) con clave temporal | Cuentas |
| RF-06 | El administrador restablece la contraseña de cualquier cuenta con una clave temporal | Cuentas |
| RF-07 | Quien entra con clave temporal debe cambiarla antes de usar el sistema | /cambiar-clave |
| RF-08 | Cada usuario debe aceptar la política de tratamiento de datos vigente; si sale una versión nueva, la acepta otra vez | /politica |
| RF-09 | Cerrar sesión | Menú lateral |

### M2. Domiciliarios y vehículos

| Código | Requerimiento | Dónde se ve |
| --- | --- | --- |
| RF-10 | Guardar los datos del domiciliario: nombre, cédula y celular | Registro y Cuentas |
| RF-11 | Asignar o cambiar el vehículo del domiciliario: placa y tipo (moto, bicicleta, carro o a pie); la capacidad sale del tipo | Cuentas |
| RF-12 | Ver a cada domiciliario con su estado en la aplicación: fuera de servicio, disponible, ocupado o en ruta | Domiciliarios |
| RF-13 | Ver en el mapa la posición en vivo de los domiciliarios conectados | Servicios y Domiciliarios |
| RF-14 | Ver el recorrido GPS de un domiciliario en las últimas horas (máximo 24) | Domiciliarios |
| RF-15 | Ver el turno de cada domiciliario: entregas, kilómetros, ganancias y dinero por entregar | Domiciliarios |
| RF-16 | Registrar que la central recibió el dinero que cobró el domiciliario | Domiciliarios |

### M3. Servicios

| Código | Requerimiento | Dónde se ve |
| --- | --- | --- |
| RF-17 | Crear un servicio con dirección de recogida y de entrega, detalle, tamaño, prioridad, forma de pago, valor y tiempo máximo de entrega | Servicios |
| RF-18 | Buscar la dirección escribiéndola (Nominatim) o marcarla con un clic en el mapa | Servicios |
| RF-19 | Sugerir el valor del servicio con la tarifa vigente según distancia y tamaño | Servicios |
| RF-20 | Programar un servicio para una hora futura, hasta 7 días adelante | Servicios |
| RF-21 | Buscar un cliente frecuente por teléfono y usar sus direcciones guardadas | Servicios |
| RF-22 | Editar un servicio mientras no se haya recogido | Servicios |
| RF-23 | Cancelar un servicio con motivo obligatorio | Servicios |
| RF-24 | Ver el detalle de un servicio: línea de tiempo, ofertas, pruebas de entrega, decisiones del motor y novedades | Servicios |
| RF-25 | Buscar servicios anteriores por fechas, estado o texto (cliente, teléfono, dirección o número) | Servicios |
| RF-26 | Generar un PIN por servicio que solo conoce el cliente y que se pide al entregar | Servicios y app |

### M4. Asignación (motor)

| Código | Requerimiento | Dónde se ve |
| --- | --- | --- |
| RF-27 | Cambiar el modo de asignación: manual, difusión o automático | Barra superior |
| RF-28 | En modo automático, ofrecer el servicio al domiciliario que mejor lo puede hacer según distancia, ruta actual, carga y tiempo límite | Motor |
| RF-29 | En modo difusión, ofrecer el servicio a varios domiciliarios a la vez; lo toma el primero que acepte | Motor |
| RF-30 | En modo manual, la central escoge el domiciliario | Servicios |
| RF-31 | Revocar un servicio asignado y devolverlo a la cola | Servicios |
| RF-32 | Juntar varios servicios en la ruta de un domiciliario y reordenar sus paradas periódicamente | Motor |
| RF-33 | Calcular distancias y tiempos por calles reales con OSRM, con respaldo en línea recta si OSRM no responde | Motor |
| RF-34 | Guardar cada decisión del motor con los candidatos que evaluó | Detalle del servicio |

### M5. App del domiciliario

| Código | Requerimiento | Dónde se ve |
| --- | --- | --- |
| RF-35 | Cambiar su estado: disponible, ocupado o fuera de servicio | App |
| RF-36 | Enviar su posición GPS real cada pocos segundos mientras trabaja; la última lectura siempre llega al servidor | App |
| RF-37 | Recibir ofertas con temporizador y aceptarlas o rechazarlas con motivo | App |
| RF-38 | Ver su ruta con las paradas en orden y el mapa | App |
| RF-39 | Confirmar la recogida y la entrega solo cuando está cerca del punto | App |
| RF-40 | Dejar prueba de entrega: PIN del cliente y, si se exige, foto o firma | App |
| RF-41 | Seguir trabajando sin señal: los eventos se guardan y se envían al volver la conexión, sin duplicarse | App |
| RF-42 | Instalar la app en el celular como aplicación (PWA) | App |

### M6. Novedades

| Código | Requerimiento | Dónde se ve |
| --- | --- | --- |
| RF-43 | El domiciliario reporta una novedad (cliente no responde, dirección errada, accidente…) con foto opcional | App |
| RF-44 | La central registra novedades, las comenta y decide qué hacer (reintentar, liberar a la cola, cancelar, marcar fallido o sin acción) | Domiciliarios |

### M7. Comunicación

| Código | Requerimiento | Dónde se ve |
| --- | --- | --- |
| RF-45 | Mensajes de texto entre la central y cada domiciliario, con mensajes predefinidos | Domiciliarios y app |
| RF-46 | Radio de voz «mantén para hablar» entre la central y los domiciliarios conectados | Menú lateral y app |

### M8. Resumen y reportes

| Código | Requerimiento | Dónde se ve |
| --- | --- | --- |
| RF-47 | Mostrar alertas en vivo: servicios sin asignar, domiciliarios sin señal, rechazos seguidos, sin vehículo, mensajes, novedades y cuentas por aprobar | Resumen |
| RF-48 | Calcular los indicadores I1 a I7 en un rango de fechas y compararlos con la línea base | Resumen |
| RF-49 | Mostrar el flujo de servicios por hora y el desempeño por domiciliario | Resumen |
| RF-50 | Descargar los servicios y los indicadores en CSV | Resumen |

### M9. Administración

Además de lo anterior, el administrador edita los 31 parámetros de operación (se aplican al instante), publica una tarifa nueva, publica versiones de la política de datos y consulta la auditoría de las acciones del día. Todo está en la pantalla Cuentas.

## 4. Reglas de negocio

Estas reglas las hace cumplir el servidor; aunque alguien intente saltarlas desde la API, el sistema responde con error.

| Código | Regla |
| --- | --- |
| RN-01 | Un servicio solo pasa de un estado a otro permitido: creado → programado o en cola → ofertado → asignado → en ejecución → entregado o fallido; cancelado se permite antes de entregar |
| RN-02 | Si dos domiciliarios aceptan la misma oferta, gana el primero y el otro recibe «otro domiciliario ya lo tomó» |
| RN-03 | Solo recibe ofertas quien tiene la cuenta activa, un vehículo registrado, está disponible o en ruta, tiene señal GPS y no tiene otra oferta pendiente |
| RN-04 | Un pedido no puede superar la capacidad del vehículo: pequeño = 1, mediano = 2, grande = 4 unidades (moto 4, bicicleta 2) |
| RN-05 | La oferta vence a los 30 segundos; tras 3 rechazos o vencimientos seguidos se alerta a la central |
| RN-06 | A quien ya va en ruta solo se le ofrece un servicio si el desvío es de máximo 2 km |
| RN-07 | Si se cancela un servicio ya recogido, se crea una devolución al punto de recogida |
| RN-08 | Recoger o entregar solo se acepta a menos de 80 m de la parada |
| RN-09 | Para entregar se exige el PIN del cliente (y foto o firma si el administrador lo activa) |
| RN-10 | En servicios contra entrega se registra el dinero recibido y queda como recaudo pendiente del domiciliario |
| RN-11 | Cada cambio de estado queda en el historial con quién, cuándo, desde dónde y por qué |
| RN-12 | Si un pedido listo no se recoge en 20 minutos, pasa a prioridad máxima |
| RN-13 | La ubicación GPS solo se recibe mientras el domiciliario está trabajando |
| RN-14 | Un domiciliario no puede ponerse fuera de servicio mientras lleve pedidos |
| RN-15 | La asignación manual de la central prevalece sobre la del motor |
| RN-16 | Cancelar o revocar exige un motivo, y las acciones de la central quedan en la auditoría |
| RN-17 | Todas las horas se manejan en la zona horaria de Bogotá (UTC-5) |

Los valores de las reglas (30 s, 3 rechazos, 2 km, 80 m, 20 min) son parámetros que el administrador puede cambiar.

## 5. Requerimientos no funcionales

| Código | Tipo | Requerimiento | Cómo se cumple |
| --- | --- | --- | --- |
| RNF-01 | Seguridad | Contraseñas guardadas cifradas | BCrypt |
| RNF-02 | Seguridad | Sesión con token firmado y una sola sesión activa por usuario | JWT en cookie HttpOnly; al entrar en otro equipo se cierra la sesión anterior |
| RNF-03 | Seguridad | Cada rol solo ve lo suyo; el domiciliario nunca ve el PIN del cliente | Permisos por ruta y por rol; protección CSRF en formularios |
| RNF-04 | Privacidad | Cumplir la Ley 1581 de 2012 de protección de datos | Aceptación registrada de la política (fecha, versión e IP); el GPS se borra a los 90 días |
| RNF-05 | Tiempo real | La central ve los cambios sin recargar la página | STOMP sobre WebSocket; el estado se envía cada 0,4 s cuando hay cambios |
| RNF-06 | Rendimiento | Responder bien con la operación de una central mediana | Prueba de carga: 50 domiciliarios y 307 servicios en 2,5 min, 0 errores; panel con p95 de 0,56 s |
| RNF-07 | Disponibilidad | La app del domiciliario funciona con señal intermitente | Cola de eventos sin conexión con identificador único que evita duplicados |
| RNF-08 | Usabilidad | Interfaz en español, adaptada al celular en la app del domiciliario | Bootstrap, PWA instalable, mapas Leaflet con OpenStreetMap |
| RNF-09 | Mantenibilidad | El esquema de la base de datos vive en el código y se versiona | Scripts V1 a V4 aplicados al arrancar y anotados con su suma SHA-256 en `ESQUEMA_VERSION` |
| RNF-10 | Trazabilidad | Toda acción importante queda registrada | Historial de estados, decisiones del motor y auditoría |
| RNF-11 | Configurabilidad | Cambiar la operación sin tocar el código | 31 parámetros editables que se aplican al instante |
| RNF-12 | Plataforma | Tecnologías del proyecto | Java 21, Spring Boot 3.3.5, Oracle Database XE 21c, servicios gratuitos de OpenStreetMap (mapa, Nominatim y OSRM) |

## 6. Base de datos que respalda los requerimientos

La base Oracle tiene 24 tablas del programa más `ESQUEMA_VERSION`, con 19 llaves foráneas, 22 índices y 2 vistas de reportes (`VW_SERVICIO_TIEMPOS` y `VW_RESUMEN_DIARIO`).

| Módulo | Tablas |
| --- | --- |
| M1. Acceso y cuentas | USUARIO, POLITICA_DATOS, ACEPTACION_POLITICA |
| M2. Domiciliarios y vehículos | DOMICILIARIO, VEHICULO, TURNO, UBICACION_GPS |
| M3. Servicios | SERVICIO, PARADA, CLIENTE, DIRECCION, TARIFA, HISTORIAL_ESTADO_SERVICIO, EVIDENCIA_ENTREGA |
| M4. Asignación | OFERTA, RUTA, DECISION_MOTOR |
| M5. App del domiciliario | EVENTO_PROCESADO (eventos enviados sin conexión) |
| M6. Novedades | INCIDENTE, SEGUIMIENTO_INCIDENTE |
| M7. Comunicación | MENSAJE, PLANTILLA_MENSAJE |
| M8 y M9. Reportes y administración | PARAMETRO, AUDITORIA y las 2 vistas |

Los scripts se aplican solos al arrancar la aplicación:

1. `V1__esquema.sql` crea las tablas, llaves, índices y restricciones.
2. `V2__vistas_reportes.sql` crea las vistas de reportes.
3. `V3__quitar_documentos.sql` borra la tabla `DOCUMENTO_DOMICILIARIO` y sus 2 parámetros.
4. `V4__quitar_modo_simulacion.sql` borra el parámetro del joystick de pruebas.

Si la base ya tenía datos, solo se aplican los scripts que falten y los datos se conservan.

Los datos de prueba (`01_datos_demo.sql`, 68 servicios) y las 40 preguntas con sus consultas (`03_preguntas_y_consultas.sql`) se verificaron sobre esta versión: todas corren sin error.

## 7. Fuera del alcance actual

Esto **no** está en el programa y no debe presentarse como cumplido:

- Control de documentos de los domiciliarios (licencia, SOAT, tecnomecánica) y sus alertas de vencimiento. Se quitó en la versión 2.
- Simulación que compara el motor contra la asignación manual. Se quitó en la versión 2. Los indicadores siguen comparándose con la línea base.
- Joystick de pruebas (modo simulación) para mover al domiciliario sin GPS. Se quitó en la versión 3: la app usa siempre el GPS real del celular.
- Pagos en línea, facturación electrónica y seguimiento del pedido por parte del cliente final.
- Notificaciones por SMS, correo o WhatsApp.

Los requerimientos nuevos se agregarán aquí con los siguientes códigos libres: RF-51, RN-18 y RNF-13.
