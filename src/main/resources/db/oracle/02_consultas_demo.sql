-- =====================================================================
-- Consultas de demostración de la base de datos de ServiAsigna (Oracle XE)
-- Conéctate como DESPACHO:  sqlplus DESPACHO/Despacho2026@localhost:1521/XEPDB1
-- y ejecuta cada bloque (copia y pega) o todo el archivo:  @02_consultas_demo.sql
-- Nada de este archivo deja cambios: la parte 7 termina con ROLLBACK.
-- =====================================================================
SET LINESIZE 220
SET PAGESIZE 60
COLUMN NOMBRE FORMAT A22
COLUMN DOMICILIARIO FORMAT A16
COLUMN CLIENTE FORMAT A24
COLUMN ESTADO FORMAT A13
COLUMN DIR_RECOGIDA FORMAT A30
COLUMN DIR_ENTREGA FORMAT A30
COLUMN MOTIVO FORMAT A32
COLUMN TABLE_NAME FORMAT A28
COLUMN CONSTRAINT_TYPE FORMAT A16

-- ---------------------------------------------------------------------
-- 1. Estructura: tablas, restricciones y scripts aplicados
-- ---------------------------------------------------------------------
SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME;

-- Restricciones por tipo: P = llave primaria, R = foránea, U = única, C = CHECK / NOT NULL
SELECT CONSTRAINT_TYPE, COUNT(*) AS CANTIDAD FROM USER_CONSTRAINTS GROUP BY CONSTRAINT_TYPE ORDER BY CONSTRAINT_TYPE;

-- Llaves foráneas: qué tabla apunta a cuál
SELECT a.TABLE_NAME, a.CONSTRAINT_NAME, c.TABLE_NAME AS TABLA_REFERENCIADA
  FROM USER_CONSTRAINTS a JOIN USER_CONSTRAINTS c ON c.CONSTRAINT_NAME = a.R_CONSTRAINT_NAME
 WHERE a.CONSTRAINT_TYPE = 'R' ORDER BY a.TABLE_NAME;

-- Versiones del esquema que ejecutó el programa al arrancar
SELECT VERSION, ARCHIVO, APLICADO FROM ESQUEMA_VERSION ORDER BY VERSION;

-- Columnas de la tabla principal
DESC SERVICIO

-- ---------------------------------------------------------------------
-- 2. Consultas básicas (SELECT, WHERE, ORDER BY)
-- ---------------------------------------------------------------------
SELECT ID, USUARIO, NOMBRE, ROL, ESTADO_CUENTA FROM USUARIO ORDER BY ID;

SELECT ID, ESTADO, DIR_RECOGIDA, DIR_ENTREGA, VALOR FROM SERVICIO WHERE ESTADO = 'EN_COLA';

SELECT ID, ESTADO, VALOR, FORMA_PAGO FROM SERVICIO
 WHERE VALOR > 8000 AND FORMA_PAGO <> 'PREPAGADO' ORDER BY VALOR DESC;

-- ---------------------------------------------------------------------
-- 3. Consultas con JOIN
-- ---------------------------------------------------------------------
-- Servicios con su domiciliario y su cliente (LEFT JOIN: los cancelados sin domiciliario también salen)
SELECT s.ID, s.ESTADO, c.NOMBRE AS CLIENTE, d.NOMBRE AS DOMICILIARIO, s.VALOR,
       TO_CHAR(s.CREADO, 'DD/MM HH24:MI') AS CREADO, TO_CHAR(s.ENTREGADO, 'HH24:MI') AS ENTREGADO
  FROM SERVICIO s
  LEFT JOIN CLIENTE c ON c.ID = s.CLIENTE_ID
  LEFT JOIN DOMICILIARIO d ON d.ID = s.DOMICILIARIO_ID
 ORDER BY s.ID DESC FETCH FIRST 15 ROWS ONLY;

-- Línea de tiempo de un servicio (cambia el 5 por cualquier número de servicio)
SELECT TO_CHAR(FECHA, 'DD/MM HH24:MI:SS') AS FECHA, ESTADO_ANTERIOR, ESTADO_NUEVO, USUARIO, ORIGEN, MOTIVO
  FROM HISTORIAL_ESTADO_SERVICIO WHERE SERVICIO_ID = 5 ORDER BY FECHA, ID;

-- Ofertas: a quién se le ofreció cada servicio y qué respondió
SELECT o.SERVICIO_ID, d.NOMBRE AS DOMICILIARIO, o.MODO, o.ESTADO, o.MOTIVO_RECHAZO
  FROM OFERTA o JOIN DOMICILIARIO d ON d.ID = o.DOMICILIARIO_ID
 ORDER BY o.SERVICIO_ID FETCH FIRST 15 ROWS ONLY;

-- Clientes con sus direcciones guardadas
SELECT c.NOMBRE, c.TELEFONO, dr.TEXTO AS DIRECCION, dr.REFERENCIA
  FROM CLIENTE c JOIN DIRECCION dr ON dr.CLIENTE_ID = c.ID ORDER BY c.NOMBRE;

-- ---------------------------------------------------------------------
-- 4. Agregaciones (COUNT, SUM, AVG, GROUP BY, HAVING, CASE)
-- ---------------------------------------------------------------------
-- Servicios por estado
SELECT ESTADO, COUNT(*) AS CANTIDAD FROM SERVICIO GROUP BY ESTADO ORDER BY CANTIDAD DESC;

-- Desempeño por domiciliario: entregas, plata cobrada, ganancias y % a tiempo
SELECT d.NOMBRE AS DOMICILIARIO,
       COUNT(*) AS ENTREGAS,
       SUM(NVL(s.VALOR_RECIBIDO, 0)) AS COBRADO,
       SUM(s.PAGO_DOMICILIARIO) AS GANANCIAS,
       ROUND(100 * SUM(CASE WHEN s.ENTREGADO <= s.LIMITE THEN 1 ELSE 0 END) / COUNT(*), 1) AS PCT_A_TIEMPO
  FROM SERVICIO s JOIN DOMICILIARIO d ON d.ID = s.DOMICILIARIO_ID
 WHERE s.ESTADO = 'ENTREGADO'
 GROUP BY d.NOMBRE ORDER BY ENTREGAS DESC;

-- Servicios y ventas por día
SELECT TO_CHAR(FECHA, 'DD/MM/YYYY') AS DIA, SERVICIOS, VENDIDO
  FROM (SELECT TRUNC(CREADO) AS FECHA, COUNT(*) AS SERVICIOS,
               SUM(CASE WHEN ESTADO = 'ENTREGADO' THEN VALOR ELSE 0 END) AS VENDIDO
          FROM SERVICIO GROUP BY TRUNC(CREADO))
 ORDER BY FECHA;

-- Clientes frecuentes: más de 5 pedidos (HAVING)
SELECT CLIENTE, COUNT(*) AS PEDIDOS FROM SERVICIO
 WHERE CLIENTE IS NOT NULL GROUP BY CLIENTE HAVING COUNT(*) > 5 ORDER BY PEDIDOS DESC;

-- Asignaciones del motor contra asignaciones manuales (indicador I3)
SELECT ORIGEN_ASIGNACION, COUNT(*) AS CANTIDAD,
       ROUND(100 * RATIO_TO_REPORT(COUNT(*)) OVER (), 1) AS PORCENTAJE
  FROM SERVICIO WHERE ORIGEN_ASIGNACION IS NOT NULL GROUP BY ORIGEN_ASIGNACION;

-- Tasa de aceptación de ofertas (indicador I5)
SELECT ESTADO, COUNT(*) AS OFERTAS FROM OFERTA GROUP BY ESTADO;

-- ---------------------------------------------------------------------
-- 5. Subconsultas
-- ---------------------------------------------------------------------
-- El domiciliario con más entregas
SELECT NOMBRE FROM DOMICILIARIO
 WHERE ID = (SELECT DOMICILIARIO_ID FROM SERVICIO WHERE ESTADO = 'ENTREGADO'
              GROUP BY DOMICILIARIO_ID ORDER BY COUNT(*) DESC FETCH FIRST 1 ROW ONLY);

-- Servicios más caros que el promedio
SELECT ID, VALOR FROM SERVICIO WHERE VALOR > (SELECT AVG(VALOR) FROM SERVICIO) ORDER BY VALOR DESC FETCH FIRST 10 ROWS ONLY;

-- Domiciliarios que nunca han rechazado una oferta (NOT EXISTS)
SELECT d.NOMBRE FROM DOMICILIARIO d
 WHERE NOT EXISTS (SELECT 1 FROM OFERTA o WHERE o.DOMICILIARIO_ID = d.ID AND o.ESTADO = 'RECHAZADA');

-- ---------------------------------------------------------------------
-- 6. Vistas creadas por el script V2
-- ---------------------------------------------------------------------
SELECT * FROM VW_RESUMEN_DIARIO ORDER BY DIA;

SELECT ID, ESTADO, MIN_HASTA_ASIGNAR, MIN_HASTA_RECOGER, MIN_TOTAL, A_TIEMPO
  FROM VW_SERVICIO_TIEMPOS WHERE ESTADO = 'ENTREGADO' ORDER BY ID FETCH FIRST 10 ROWS ONLY;

-- Tiempo promedio de asignación y de entrega en minutos (indicadores I1 e I2)
SELECT ROUND(AVG(MIN_HASTA_ASIGNAR), 1) AS PROM_MIN_ASIGNAR, ROUND(AVG(MIN_TOTAL), 1) AS PROM_MIN_ENTREGA
  FROM VW_SERVICIO_TIEMPOS WHERE ESTADO = 'ENTREGADO';

-- ---------------------------------------------------------------------
-- 7. INSERT, UPDATE, DELETE y restricciones (todo se deshace al final)
-- ---------------------------------------------------------------------
SAVEPOINT demo;

INSERT INTO CLIENTE (NOMBRE, TELEFONO, ACTIVO, CREADO) VALUES ('Cliente de prueba', '3109990000', 1, SYSTIMESTAMP);
SELECT ID, NOMBRE, TELEFONO FROM CLIENTE WHERE TELEFONO = '3109990000';

UPDATE CLIENTE SET NOMBRE = 'Cliente de prueba (editado)' WHERE TELEFONO = '3109990000';
SELECT ID, NOMBRE FROM CLIENTE WHERE TELEFONO = '3109990000';

DELETE FROM CLIENTE WHERE TELEFONO = '3109990000';
SELECT COUNT(*) AS QUEDAN FROM CLIENTE WHERE TELEFONO = '3109990000';

-- Las siguientes sentencias DEBEN fallar: así se ve que la base de datos protege los datos.
-- CHECK: un estado que no existe en la máquina de estados (ORA-02290)
UPDATE SERVICIO SET ESTADO = 'VOLANDO' WHERE ID = 1;
-- UNIQUE: teléfono de cliente repetido (ORA-00001)
INSERT INTO CLIENTE (NOMBRE, TELEFONO, ACTIVO, CREADO) VALUES ('Repetido', '3001112233', 1, SYSTIMESTAMP);
-- FOREIGN KEY: oferta a un servicio que no existe (ORA-02291)
INSERT INTO OFERTA (SERVICIO_ID, DOMICILIARIO_ID, MODO, ESTADO, ENVIADA, LIMITE)
VALUES (999999, (SELECT MIN(ID) FROM DOMICILIARIO), 'AUTOMATICO', 'PENDIENTE', SYSTIMESTAMP, SYSTIMESTAMP);
-- FOREIGN KEY: borrar un servicio que tiene paradas, historial y ofertas (ORA-02292)
DELETE FROM SERVICIO WHERE ID = 1;
-- NOT NULL: servicio sin PIN (ORA-01400)
INSERT INTO SERVICIO (ESTADO, RECOGIDA_LAT, RECOGIDA_LNG, ENTREGA_LAT, ENTREGA_LNG, TAMANO, PRIORIDAD, FORMA_PAGO, VALOR,
                      CREADO, LISTO_A, LIMITE_RECOGIDA, LIMITE, ASIGNACION_MANUAL, DEVOLUCION)
VALUES ('EN_COLA', 10.47, -73.25, 10.46, -73.26, 'PEQUENO', 'NORMAL', 'EFECTIVO', 6000,
        SYSTIMESTAMP, SYSTIMESTAMP, SYSTIMESTAMP, SYSTIMESTAMP, 0, 0);

ROLLBACK TO demo;
