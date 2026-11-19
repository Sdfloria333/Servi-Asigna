-- =====================================================================
-- 02 · Vistas
-- Conectado como DESPACHO.
--
-- La base ya tiene dos vistas creadas por la migración V2:
--   VW_SERVICIO_TIEMPOS  (tiempos de cada servicio)
--   VW_RESUMEN_DIARIO    (totales por día)
-- Estas cinco son nuevas. Solo leen: no cambian ningún dato y la
-- aplicación no depende de ellas.
-- =====================================================================

-- ---------------------------------------------------------------------
-- VW_SERVICIOS_ACTIVOS: servicios que todavía no terminan, con su
-- domiciliario y cuántos minutos llevan abiertos.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW VW_SERVICIOS_ACTIVOS AS
SELECT s.ID,
       s.ESTADO,
       s.PRIORIDAD,
       s.CLIENTE,
       s.TELEFONO,
       s.DIR_RECOGIDA,
       s.DIR_ENTREGA,
       s.VALOR,
       s.FORMA_PAGO,
       d.NOMBRE                                                AS DOMICILIARIO,
       s.CREADO,
       s.LIMITE,
       ROUND((SYSDATE - CAST(s.CREADO AS DATE)) * 1440)       AS MINUTOS_ABIERTO,
       CASE WHEN SYSDATE > CAST(s.LIMITE AS DATE) THEN 'SI' ELSE 'NO' END AS ATRASADO
  FROM SERVICIO s
  LEFT JOIN DOMICILIARIO d ON d.ID = s.DOMICILIARIO_ID
 WHERE s.ESTADO IN ('PROGRAMADO', 'EN_COLA', 'OFERTADO', 'ASIGNADO', 'EN_EJECUCION');

COMMENT ON TABLE VW_SERVICIOS_ACTIVOS IS 'Servicios abiertos con su domiciliario, minutos abiertos y si ya pasaron el límite.';

-- ---------------------------------------------------------------------
-- VW_DESEMPENO_DOMICILIARIO: totales por domiciliario (todas las fechas).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW VW_DESEMPENO_DOMICILIARIO AS
SELECT d.ID,
       d.NOMBRE,
       d.ESTADO_OPERATIVO,
       COUNT(s.ID)                                                        AS SERVICIOS,
       SUM(CASE WHEN s.ESTADO = 'ENTREGADO' THEN 1 ELSE 0 END)            AS ENTREGADOS,
       SUM(CASE WHEN s.ESTADO = 'FALLIDO'   THEN 1 ELSE 0 END)            AS FALLIDOS,
       SUM(CASE WHEN s.ESTADO = 'CANCELADO' THEN 1 ELSE 0 END)            AS CANCELADOS,
       NVL(SUM(CASE WHEN s.ESTADO = 'ENTREGADO' THEN s.PAGO_DOMICILIARIO END), 0) AS GANANCIAS,
       ROUND(100 * SUM(CASE WHEN s.ESTADO = 'ENTREGADO' AND s.ENTREGADO <= s.LIMITE THEN 1 ELSE 0 END)
                 / NULLIF(SUM(CASE WHEN s.ESTADO = 'ENTREGADO' THEN 1 ELSE 0 END), 0), 1) AS PCT_A_TIEMPO
  FROM DOMICILIARIO d
  LEFT JOIN SERVICIO s ON s.DOMICILIARIO_ID = d.ID
 GROUP BY d.ID, d.NOMBRE, d.ESTADO_OPERATIVO;

COMMENT ON TABLE VW_DESEMPENO_DOMICILIARIO IS 'Por domiciliario: servicios, entregados, fallidos, cancelados, ganancias y % a tiempo.';

-- ---------------------------------------------------------------------
-- VW_NOVEDADES_ABIERTAS: novedades sin cerrar, con el domiciliario y
-- cuántos comentarios de seguimiento tienen.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW VW_NOVEDADES_ABIERTAS AS
SELECT i.ID,
       i.TIPO,
       i.ESTADO,
       i.SERVICIO_ID,
       d.NOMBRE                                              AS DOMICILIARIO,
       i.REPORTADO_POR,
       i.DESCRIPCION,
       i.ABIERTO,
       ROUND((SYSDATE - CAST(i.ABIERTO AS DATE)) * 1440)    AS MINUTOS_ABIERTA,
       (SELECT COUNT(*) FROM SEGUIMIENTO_INCIDENTE f WHERE f.INCIDENTE_ID = i.ID) AS COMENTARIOS
  FROM INCIDENTE i
  LEFT JOIN DOMICILIARIO d ON d.ID = i.DOMICILIARIO_ID
 WHERE i.ESTADO <> 'CERRADO';

COMMENT ON TABLE VW_NOVEDADES_ABIERTAS IS 'Novedades abiertas o en atención, con minutos abiertas y número de comentarios.';

-- ---------------------------------------------------------------------
-- VW_RECAUDO_TURNOS: cuadre de dinero de cada turno.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW VW_RECAUDO_TURNOS AS
SELECT t.ID,
       d.NOMBRE                                  AS DOMICILIARIO,
       t.ESTADO,
       t.INICIO,
       t.FIN,
       t.COMPLETADOS,
       t.RECAUDO,
       t.RECAUDO_ENTREGADO,
       t.RECAUDO - t.RECAUDO_ENTREGADO           AS RECAUDO_PENDIENTE,
       t.GANANCIAS,
       ROUND(t.KM_RECORRIDOS, 1)                 AS KM
  FROM TURNO t
  JOIN DOMICILIARIO d ON d.ID = t.DOMICILIARIO_ID;

COMMENT ON TABLE VW_RECAUDO_TURNOS IS 'Turnos con el dinero recaudado, el entregado a la central y lo que falta por entregar.';

-- ---------------------------------------------------------------------
-- VW_CLIENTES_FRECUENTES: clientes activos con su número de servicios.
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW VW_CLIENTES_FRECUENTES AS
SELECT c.ID,
       c.NOMBRE,
       c.TELEFONO,
       COUNT(s.ID)                                                        AS SERVICIOS,
       SUM(CASE WHEN s.ESTADO = 'ENTREGADO' THEN 1 ELSE 0 END)            AS ENTREGADOS,
       NVL(SUM(CASE WHEN s.ESTADO = 'ENTREGADO' THEN s.VALOR END), 0)     AS VALOR_ENTREGADO,
       MAX(s.CREADO)                                                      AS ULTIMO_SERVICIO,
       (SELECT COUNT(*) FROM DIRECCION x WHERE x.CLIENTE_ID = c.ID AND x.ACTIVO = 1) AS DIRECCIONES
  FROM CLIENTE c
  LEFT JOIN SERVICIO s ON s.CLIENTE_ID = c.ID
 WHERE c.ACTIVO = 1
 GROUP BY c.ID, c.NOMBRE, c.TELEFONO;

COMMENT ON TABLE VW_CLIENTES_FRECUENTES IS 'Clientes activos con servicios, entregados, valor entregado, último servicio y direcciones.';
