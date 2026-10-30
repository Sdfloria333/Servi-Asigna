-- =====================================================================
-- V2 — Vistas de apoyo para reportes desde SQL Developer o Power BI.
-- La aplicación calcula los indicadores en IndicadoresService; estas
-- vistas permiten revisarlos directamente en la base de datos.
-- =====================================================================

-- (TIMESTAMP - TIMESTAMP da un intervalo; EXTRACT lo pasa a minutos.)
-- Tiempos de cada servicio en minutos (asignación, recogida, entrega) y si llegó a tiempo.
CREATE OR REPLACE VIEW VW_SERVICIO_TIEMPOS AS
SELECT s.ID,
       s.ESTADO,
       s.CREADO,
       s.DOMICILIARIO_ID,
       s.ORIGEN_ASIGNACION,
       s.ASIGNACION_MANUAL,
       s.VALOR,
       ROUND(EXTRACT(DAY FROM (s.ASIGNADO_EN - NVL(s.EN_COLA_DESDE, s.CREADO))) * 1440 + EXTRACT(HOUR FROM (s.ASIGNADO_EN - NVL(s.EN_COLA_DESDE, s.CREADO))) * 60 + EXTRACT(MINUTE FROM (s.ASIGNADO_EN - NVL(s.EN_COLA_DESDE, s.CREADO))) + EXTRACT(SECOND FROM (s.ASIGNADO_EN - NVL(s.EN_COLA_DESDE, s.CREADO))) / 60, 1) AS MIN_HASTA_ASIGNAR,
       ROUND(EXTRACT(DAY FROM (s.RECOGIDO - s.ASIGNADO_EN)) * 1440 + EXTRACT(HOUR FROM (s.RECOGIDO - s.ASIGNADO_EN)) * 60 + EXTRACT(MINUTE FROM (s.RECOGIDO - s.ASIGNADO_EN)) + EXTRACT(SECOND FROM (s.RECOGIDO - s.ASIGNADO_EN)) / 60, 1) AS MIN_HASTA_RECOGER,
       ROUND(EXTRACT(DAY FROM (s.ENTREGADO - s.CREADO)) * 1440 + EXTRACT(HOUR FROM (s.ENTREGADO - s.CREADO)) * 60 + EXTRACT(MINUTE FROM (s.ENTREGADO - s.CREADO)) + EXTRACT(SECOND FROM (s.ENTREGADO - s.CREADO)) / 60, 1) AS MIN_TOTAL,
       CASE WHEN s.ENTREGADO IS NULL THEN NULL WHEN s.ENTREGADO <= s.LIMITE THEN 1 ELSE 0 END        AS A_TIEMPO
  FROM SERVICIO s;

-- Resumen por día: creados, entregados, cancelados, fallidos, entregas a tiempo y tiempo promedio de asignación.
CREATE OR REPLACE VIEW VW_RESUMEN_DIARIO AS
SELECT TRUNC(CREADO)                                        AS DIA,
       COUNT(*)                                             AS CREADOS,
       SUM(CASE WHEN ESTADO = 'ENTREGADO' THEN 1 ELSE 0 END) AS ENTREGADOS,
       SUM(CASE WHEN ESTADO = 'CANCELADO' THEN 1 ELSE 0 END) AS CANCELADOS,
       SUM(CASE WHEN ESTADO = 'FALLIDO' THEN 1 ELSE 0 END)   AS FALLIDOS,
       SUM(NVL(A_TIEMPO, 0))                                AS A_TIEMPO,
       ROUND(AVG(MIN_HASTA_ASIGNAR), 1)                     AS PROM_MIN_ASIGNAR
  FROM VW_SERVICIO_TIEMPOS
 GROUP BY TRUNC(CREADO);
