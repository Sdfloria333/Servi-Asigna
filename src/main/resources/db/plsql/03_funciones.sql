-- =====================================================================
-- 03 · Funciones
-- Conectado como DESPACHO.
-- Todas solo LEEN datos (SELECT). Se pueden usar en consultas:
--   SELECT NOMBRE, FN_GANANCIAS_DOMICILIARIO(ID) FROM DOMICILIARIO;
-- =====================================================================

-- ---------------------------------------------------------------------
-- FN_NOMBRE_ESTADO: traduce el código de estado de un servicio a texto
-- legible. Ejemplo: FN_NOMBRE_ESTADO('EN_COLA') = 'Sin asignar'.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_NOMBRE_ESTADO (p_estado IN VARCHAR2)
RETURN VARCHAR2 DETERMINISTIC IS
BEGIN
    RETURN CASE p_estado
               WHEN 'CREADO'       THEN 'Creado'
               WHEN 'PROGRAMADO'   THEN 'Programado'
               WHEN 'EN_COLA'      THEN 'Sin asignar'
               WHEN 'OFERTADO'     THEN 'Ofertado'
               WHEN 'ASIGNADO'     THEN 'Asignado'
               WHEN 'EN_EJECUCION' THEN 'En camino'
               WHEN 'ENTREGADO'    THEN 'Entregado'
               WHEN 'FALLIDO'      THEN 'Fallido'
               WHEN 'CANCELADO'    THEN 'Cancelado'
               ELSE p_estado
           END;
END FN_NOMBRE_ESTADO;
/

-- ---------------------------------------------------------------------
-- FN_GANANCIAS_DOMICILIARIO: lo que ganó un domiciliario en servicios
-- entregados entre dos fechas. Sin fechas = todo el histórico.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_GANANCIAS_DOMICILIARIO (
    p_domiciliario IN NUMBER,
    p_desde        IN DATE DEFAULT NULL,
    p_hasta        IN DATE DEFAULT NULL
) RETURN NUMBER IS
    v_total NUMBER;
BEGIN
    SELECT NVL(SUM(PAGO_DOMICILIARIO), 0)
      INTO v_total
      FROM SERVICIO
     WHERE DOMICILIARIO_ID = p_domiciliario
       AND ESTADO = 'ENTREGADO'
       AND (p_desde IS NULL OR ENTREGADO >= p_desde)
       AND (p_hasta IS NULL OR ENTREGADO <  p_hasta + 1);
    RETURN v_total;
END FN_GANANCIAS_DOMICILIARIO;
/

-- ---------------------------------------------------------------------
-- FN_MINUTOS_ENTREGA: minutos desde que se creó un servicio hasta que se
-- entregó. Devuelve NULL si todavía no se ha entregado.
-- Si el servicio no existe lanza el error -20010 (excepción NO_DATA_FOUND).
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_MINUTOS_ENTREGA (p_servicio IN NUMBER)
RETURN NUMBER IS
    v_creado     SERVICIO.CREADO%TYPE;
    v_entregado  SERVICIO.ENTREGADO%TYPE;
BEGIN
    SELECT CREADO, ENTREGADO
      INTO v_creado, v_entregado
      FROM SERVICIO
     WHERE ID = p_servicio;

    IF v_entregado IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN ROUND((CAST(v_entregado AS DATE) - CAST(v_creado AS DATE)) * 1440, 1);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20010, 'No existe el servicio #' || p_servicio);
END FN_MINUTOS_ENTREGA;
/

-- ---------------------------------------------------------------------
-- FN_PORCENTAJE_A_TIEMPO: % de servicios entregados antes de su límite
-- entre dos fechas (indicador de puntualidad). NULL si no hubo entregas.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_PORCENTAJE_A_TIEMPO (
    p_desde IN DATE,
    p_hasta IN DATE
) RETURN NUMBER IS
    v_entregados NUMBER;
    v_a_tiempo   NUMBER;
BEGIN
    SELECT COUNT(*),
           SUM(CASE WHEN ENTREGADO <= LIMITE THEN 1 ELSE 0 END)
      INTO v_entregados, v_a_tiempo
      FROM SERVICIO
     WHERE ESTADO = 'ENTREGADO'
       AND ENTREGADO >= p_desde
       AND ENTREGADO <  p_hasta + 1;

    IF v_entregados = 0 THEN
        RETURN NULL;
    END IF;
    RETURN ROUND(100 * v_a_tiempo / v_entregados, 1);
END FN_PORCENTAJE_A_TIEMPO;
/
