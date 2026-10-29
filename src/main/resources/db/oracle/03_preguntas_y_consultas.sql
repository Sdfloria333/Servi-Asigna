-- =====================================================================
-- ServiAsigna · Preguntas y consultas sobre la base de datos (Oracle XE)
-- Conéctate como DESPACHO y ejecuta la consulta de cada pregunta:
--   sqlplus DESPACHO/Despacho2026@localhost:1521/XEPDB1
-- Los resultados dependen de los datos cargados con 01_datos_demo.sql.
-- =====================================================================
SET LINESIZE 220
SET PAGESIZE 60
COLUMN NOMBRE FORMAT A22
COLUMN CLIENTE FORMAT A24
COLUMN DOMICILIARIO FORMAT A16
COLUMN DIRECCION FORMAT A36
COLUMN DIR_RECOGIDA FORMAT A34
COLUMN DIR_ENTREGA FORMAT A34
COLUMN REFERENCIA FORMAT A24
COLUMN NOTA FORMAT A32
COLUMN MOTIVO FORMAT A34
COLUMN MOTIVO_CIERRE FORMAT A34
COLUMN DESCRIPCION FORMAT A30
COLUMN TEXTO FORMAT A45

-- =====================================================================
-- PARTE 1 · Consultas sencillas (una sola tabla)
-- =====================================================================

-- 1. ¿Cuál es el nombre y teléfono de todos los clientes?
SELECT NOMBRE, TELEFONO FROM CLIENTE ORDER BY NOMBRE;

-- 2. ¿Cuál es el nombre, cédula y celular de los domiciliarios?
SELECT NOMBRE, DOCUMENTO AS CEDULA, TELEFONO AS CELULAR FROM DOMICILIARIO ORDER BY NOMBRE;

-- 3. ¿Qué usuarios tiene el sistema, con qué rol y en qué estado está su cuenta?
SELECT USUARIO, NOMBRE, ROL, ESTADO_CUENTA FROM USUARIO ORDER BY ROL, USUARIO;

-- 4. ¿Qué placa, tipo y capacidad tienen los vehículos registrados?
SELECT PLACA, TIPO, CAPACIDAD_UNIDADES FROM VEHICULO WHERE ACTIVO = 1;

-- 5. ¿Qué servicios están esperando domiciliario en este momento?
SELECT ID, DIR_RECOGIDA, DIR_ENTREGA, VALOR FROM SERVICIO WHERE ESTADO = 'EN_COLA';

-- 6. ¿Qué servicios se cancelaron y por qué motivo?
SELECT ID, TO_CHAR(CREADO, 'DD/MM HH24:MI') AS FECHA, MOTIVO_CIERRE FROM SERVICIO WHERE ESTADO = 'CANCELADO' ORDER BY CREADO;

-- 7. ¿Cuáles fueron los 5 servicios más caros?
SELECT ID, DIR_ENTREGA, VALOR FROM SERVICIO ORDER BY VALOR DESC FETCH FIRST 5 ROWS ONLY;

-- 8. ¿Qué servicios se pagaron en efectivo y cuánto recibió el domiciliario?
SELECT ID, VALOR, VALOR_RECIBIDO FROM SERVICIO WHERE FORMA_PAGO = 'EFECTIVO' AND ESTADO = 'ENTREGADO' ORDER BY ID;

-- 9. ¿Qué servicios tenían prioridad ALTA?
SELECT ID, ESTADO, NOTA FROM SERVICIO WHERE PRIORIDAD = 'ALTA' ORDER BY ID;

-- 10. ¿Cuál es la tarifa vigente?
SELECT NOMBRE, BASE, POR_KM, RECARGO_MEDIANO, RECARGO_GRANDE, MINIMO, PORCENTAJE_DOMICILIARIO FROM TARIFA WHERE ACTIVA = 1;

-- 11. ¿Qué parámetros de operación tiene configurados el sistema?
SELECT CLAVE, VALOR FROM PARAMETRO ORDER BY CLAVE;

-- =====================================================================
-- PARTE 2 · Consultas con JOIN (varias tablas)
-- =====================================================================

-- 12. ¿Qué direcciones tiene guardadas cada cliente?
SELECT c.NOMBRE AS CLIENTE, c.TELEFONO, d.TEXTO AS DIRECCION, d.REFERENCIA
  FROM CLIENTE c JOIN DIRECCION d ON d.CLIENTE_ID = c.ID
 ORDER BY c.NOMBRE;

-- 13. ¿Qué vehículo tiene cada domiciliario?
SELECT d.NOMBRE AS DOMICILIARIO, v.PLACA, v.TIPO, v.CAPACIDAD_UNIDADES
  FROM DOMICILIARIO d LEFT JOIN VEHICULO v ON v.DOMICILIARIO_ID = d.ID AND v.ACTIVO = 1
 ORDER BY d.NOMBRE;

-- 14. ¿Cuál es el nombre, la placa y el estado en la aplicación de cada domiciliario?
SELECT d.NOMBRE AS DOMICILIARIO, NVL(v.PLACA, 'SIN VEHÍCULO') AS PLACA, d.ESTADO_OPERATIVO, u.ESTADO_CUENTA
  FROM DOMICILIARIO d
  JOIN USUARIO u ON u.ID = d.ID
  LEFT JOIN VEHICULO v ON v.DOMICILIARIO_ID = d.ID AND v.ACTIVO = 1
 ORDER BY d.NOMBRE;

-- 15. ¿Qué domiciliario entregó cada servicio y a qué cliente?
SELECT s.ID, c.NOMBRE AS CLIENTE, d.NOMBRE AS DOMICILIARIO, s.VALOR, TO_CHAR(s.ENTREGADO, 'DD/MM HH24:MI') AS ENTREGADO
  FROM SERVICIO s
  JOIN DOMICILIARIO d ON d.ID = s.DOMICILIARIO_ID
  LEFT JOIN CLIENTE c ON c.ID = s.CLIENTE_ID
 WHERE s.ESTADO = 'ENTREGADO' ORDER BY s.ID;

-- 16. ¿Cuál fue la línea de tiempo (cambios de estado) del servicio número 5?
SELECT TO_CHAR(FECHA, 'DD/MM HH24:MI:SS') AS FECHA, ESTADO_ANTERIOR, ESTADO_NUEVO, USUARIO, ORIGEN, MOTIVO
  FROM HISTORIAL_ESTADO_SERVICIO WHERE SERVICIO_ID = 5 ORDER BY FECHA, ID;

-- 17. ¿Qué ofertas fueron rechazadas, quién las rechazó y por qué?
SELECT o.SERVICIO_ID, d.NOMBRE AS DOMICILIARIO, o.MODO, o.MOTIVO_RECHAZO AS MOTIVO
  FROM OFERTA o JOIN DOMICILIARIO d ON d.ID = o.DOMICILIARIO_ID
 WHERE o.ESTADO = 'RECHAZADA' ORDER BY o.SERVICIO_ID;

-- 18. ¿Qué novedades se reportaron, en qué servicio y qué decidió la central?
SELECT i.SERVICIO_ID, d.NOMBRE AS DOMICILIARIO, i.TIPO, i.DESCRIPCION, i.DECISION
  FROM INCIDENTE i LEFT JOIN DOMICILIARIO d ON d.ID = i.DOMICILIARIO_ID ORDER BY i.ID;

-- 19. ¿Con qué PIN se confirmó cada entrega?
SELECT e.SERVICIO_ID, d.NOMBRE AS DOMICILIARIO, e.TIPO, e.DATO AS PIN, TO_CHAR(e.FECHA, 'DD/MM HH24:MI') AS FECHA
  FROM EVIDENCIA_ENTREGA e JOIN DOMICILIARIO d ON d.ID = e.DOMICILIARIO_ID ORDER BY e.SERVICIO_ID;

-- 20. ¿Qué mensajes se cruzaron la central y los domiciliarios?
SELECT d.NOMBRE AS DOMICILIARIO, CASE WHEN m.DE_CENTRAL = 1 THEN 'Central' ELSE 'Domiciliario' END AS ESCRIBE,
       m.TEXTO, TO_CHAR(m.FECHA, 'DD/MM HH24:MI') AS FECHA
  FROM MENSAJE m JOIN DOMICILIARIO d ON d.ID = m.DOMICILIARIO_ID ORDER BY m.FECHA;

-- 21. ¿Qué recorrido GPS hizo Juan (sus últimas 10 posiciones)?
SELECT u.LAT, u.LNG, u.PRECISION_M, TO_CHAR(u.FECHA_CAPTURA, 'DD/MM HH24:MI') AS HORA
  FROM UBICACION_GPS u JOIN DOMICILIARIO d ON d.ID = u.DOMICILIARIO_ID
 WHERE d.USUARIO = 'juan' ORDER BY u.FECHA_CAPTURA DESC FETCH FIRST 10 ROWS ONLY;

-- =====================================================================
-- PARTE 3 · Agrupaciones (COUNT, SUM, AVG, GROUP BY, HAVING)
-- =====================================================================

-- 22. ¿Cuántos servicios hay en cada estado?
SELECT ESTADO, COUNT(*) AS CANTIDAD FROM SERVICIO GROUP BY ESTADO ORDER BY CANTIDAD DESC;

-- 23. ¿Cuántos servicios entregó cada domiciliario y cuánto ganó?
SELECT d.NOMBRE AS DOMICILIARIO, COUNT(*) AS ENTREGAS, SUM(s.PAGO_DOMICILIARIO) AS GANANCIAS
  FROM SERVICIO s JOIN DOMICILIARIO d ON d.ID = s.DOMICILIARIO_ID
 WHERE s.ESTADO = 'ENTREGADO' GROUP BY d.NOMBRE ORDER BY ENTREGAS DESC;

-- 24. ¿Cuántos servicios se hicieron cada día y cuánto se vendió?
SELECT TO_CHAR(FECHA, 'DD/MM/YYYY') AS DIA, SERVICIOS, VENDIDO
  FROM (SELECT TRUNC(CREADO) AS FECHA, COUNT(*) AS SERVICIOS,
               SUM(CASE WHEN ESTADO = 'ENTREGADO' THEN VALOR ELSE 0 END) AS VENDIDO
          FROM SERVICIO GROUP BY TRUNC(CREADO))
 ORDER BY FECHA;

-- 25. ¿Cuántos pedidos ha hecho cada cliente?
SELECT c.NOMBRE AS CLIENTE, COUNT(s.ID) AS PEDIDOS
  FROM CLIENTE c LEFT JOIN SERVICIO s ON s.CLIENTE_ID = c.ID
 GROUP BY c.NOMBRE ORDER BY PEDIDOS DESC;

-- 26. ¿Qué clientes han hecho más de 8 pedidos? (HAVING)
SELECT c.NOMBRE AS CLIENTE, COUNT(*) AS PEDIDOS
  FROM CLIENTE c JOIN SERVICIO s ON s.CLIENTE_ID = c.ID
 GROUP BY c.NOMBRE HAVING COUNT(*) > 8 ORDER BY PEDIDOS DESC;

-- 27. ¿Cuál es el valor promedio, mínimo y máximo de un domicilio?
SELECT ROUND(AVG(VALOR)) AS PROMEDIO, MIN(VALOR) AS MINIMO, MAX(VALOR) AS MAXIMO FROM SERVICIO;

-- 28. ¿Cuánto se recaudó por cada forma de pago?
SELECT FORMA_PAGO, COUNT(*) AS SERVICIOS, SUM(VALOR) AS TOTAL
  FROM SERVICIO WHERE ESTADO = 'ENTREGADO' GROUP BY FORMA_PAGO ORDER BY TOTAL DESC;

-- 29. ¿Qué porcentaje de entregas llegó a tiempo por domiciliario?
SELECT d.NOMBRE AS DOMICILIARIO,
       ROUND(100 * SUM(CASE WHEN s.ENTREGADO <= s.LIMITE THEN 1 ELSE 0 END) / COUNT(*), 1) AS PCT_A_TIEMPO
  FROM SERVICIO s JOIN DOMICILIARIO d ON d.ID = s.DOMICILIARIO_ID
 WHERE s.ESTADO = 'ENTREGADO' GROUP BY d.NOMBRE ORDER BY PCT_A_TIEMPO DESC;

-- 30. ¿Cuántas ofertas aceptó y rechazó cada domiciliario?
SELECT d.NOMBRE AS DOMICILIARIO,
       SUM(CASE WHEN o.ESTADO = 'ACEPTADA' THEN 1 ELSE 0 END) AS ACEPTADAS,
       SUM(CASE WHEN o.ESTADO = 'RECHAZADA' THEN 1 ELSE 0 END) AS RECHAZADAS
  FROM OFERTA o JOIN DOMICILIARIO d ON d.ID = o.DOMICILIARIO_ID GROUP BY d.NOMBRE ORDER BY d.NOMBRE;

-- 31. ¿Cuánto trabajó cada domiciliario según sus turnos (entregas, km y ganancias)?
SELECT d.NOMBRE AS DOMICILIARIO, COUNT(*) AS TURNOS, SUM(t.COMPLETADOS) AS ENTREGAS,
       ROUND(SUM(t.KM_RECORRIDOS), 1) AS KM, SUM(t.GANANCIAS) AS GANANCIAS
  FROM TURNO t JOIN DOMICILIARIO d ON d.ID = t.DOMICILIARIO_ID GROUP BY d.NOMBRE ORDER BY GANANCIAS DESC;

-- 32. ¿Cuántos servicios asignó el motor y cuántos la central a mano?
SELECT CASE WHEN ASIGNACION_MANUAL = 1 THEN 'Manual (central)' ELSE 'Automática (motor)' END AS TIPO_ASIGNACION,
       COUNT(*) AS SERVICIOS
  FROM SERVICIO WHERE DOMICILIARIO_ID IS NOT NULL GROUP BY ASIGNACION_MANUAL;

-- =====================================================================
-- PARTE 4 · Subconsultas y vistas
-- =====================================================================

-- 33. ¿Qué domiciliario hizo más entregas?
SELECT NOMBRE FROM DOMICILIARIO
 WHERE ID = (SELECT DOMICILIARIO_ID FROM SERVICIO WHERE ESTADO = 'ENTREGADO'
              GROUP BY DOMICILIARIO_ID ORDER BY COUNT(*) DESC FETCH FIRST 1 ROW ONLY);

-- 34. ¿Qué servicios costaron más que el promedio?
SELECT ID, VALOR FROM SERVICIO WHERE VALOR > (SELECT AVG(VALOR) FROM SERVICIO) ORDER BY VALOR DESC;

-- 35. ¿Qué domiciliarios nunca han rechazado una oferta?
SELECT d.NOMBRE FROM DOMICILIARIO d
 WHERE NOT EXISTS (SELECT 1 FROM OFERTA o WHERE o.DOMICILIARIO_ID = d.ID AND o.ESTADO = 'RECHAZADA');

-- 36. ¿Qué domiciliarios no tienen ningún servicio asignado? (NOT IN)
SELECT NOMBRE FROM DOMICILIARIO
 WHERE ID NOT IN (SELECT DOMICILIARIO_ID FROM SERVICIO WHERE DOMICILIARIO_ID IS NOT NULL);

-- 37. ¿Qué scripts de la base de datos se han aplicado y cuándo? (control de versiones del esquema)
SELECT VERSION, DESCRIPCION, ARCHIVO, TO_CHAR(APLICADO, 'DD/MM/YYYY HH24:MI') AS APLICADO
  FROM ESQUEMA_VERSION ORDER BY VERSION;

-- 38. ¿Cuánto tardó cada servicio en asignarse, recogerse y entregarse? (vista)
SELECT ID, MIN_HASTA_ASIGNAR, MIN_HASTA_RECOGER, MIN_TOTAL, A_TIEMPO
  FROM VW_SERVICIO_TIEMPOS WHERE ESTADO = 'ENTREGADO' ORDER BY ID;

-- 39. ¿Cuál es el resumen de cada día: creados, entregados, cancelados y a tiempo? (vista)
SELECT * FROM VW_RESUMEN_DIARIO ORDER BY DIA;

-- 40. ¿Cuál es el tiempo promedio de asignación y de entrega en minutos? (indicadores I1 e I2)
SELECT ROUND(AVG(MIN_HASTA_ASIGNAR), 1) AS PROM_MIN_ASIGNAR, ROUND(AVG(MIN_TOTAL), 1) AS PROM_MIN_ENTREGA
  FROM VW_SERVICIO_TIEMPOS WHERE ESTADO = 'ENTREGADO';
