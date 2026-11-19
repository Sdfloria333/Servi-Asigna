-- =====================================================================
-- 04 · Cursores (procedimientos y una función con cursores explícitos)
-- Conectado como DESPACHO.
--
-- Cada objeto muestra una forma distinta de usar cursores:
--   PR_RESUMEN_DOMICILIARIOS  cursor FOR LOOP + cursor con parámetro anidado
--   PR_SERVICIOS_ATRASADOS    OPEN / FETCH / CLOSE, %ROWTYPE, %NOTFOUND,
--                             %ROWCOUNT, %ISOPEN
--   PR_REPORTE_DIA            cursor con parámetro y BULK COLLECT
--   FN_SERVICIOS_CLIENTE      cursor de referencia (SYS_REFCURSOR)
--   PR_DEPURAR_BITACORA       cursor FOR UPDATE + WHERE CURRENT OF
--
-- Los cuatro primeros solo LEEN. El último solo borra filas de
-- BITACORA_BD (la tabla nueva): nunca toca tablas de la aplicación.
-- Los resultados se ven con DBMS_OUTPUT: ejecuta antes SET SERVEROUTPUT ON.
-- =====================================================================

-- ---------------------------------------------------------------------
-- PR_RESUMEN_DOMICILIARIOS
-- Recorre los domiciliarios y, para cada uno, sus servicios entregados
-- entre dos fechas (por defecto, los últimos 7 días).
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE PR_RESUMEN_DOMICILIARIOS (
    p_desde IN DATE DEFAULT TRUNC(SYSDATE) - 6,
    p_hasta IN DATE DEFAULT TRUNC(SYSDATE)
) IS
    -- Cursor 1: todos los domiciliarios
    CURSOR c_domiciliarios IS
        SELECT ID, NOMBRE, ESTADO_OPERATIVO
          FROM DOMICILIARIO
         ORDER BY NOMBRE;

    -- Cursor 2: con parámetro, los servicios de UN domiciliario
    CURSOR c_servicios (p_domiciliario NUMBER) IS
        SELECT ID, ESTADO, VALOR, PAGO_DOMICILIARIO, ENTREGADO, LIMITE
          FROM SERVICIO
         WHERE DOMICILIARIO_ID = p_domiciliario
           AND CREADO >= p_desde
           AND CREADO <  p_hasta + 1;

    v_entregados  PLS_INTEGER;
    v_fallidos    PLS_INTEGER;
    v_a_tiempo    PLS_INTEGER;
    v_ganancias   NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Resumen de domiciliarios del ' || TO_CHAR(p_desde, 'DD/MM/YYYY')
                         || ' al ' || TO_CHAR(p_hasta, 'DD/MM/YYYY'));
    DBMS_OUTPUT.PUT_LINE(RPAD('-', 70, '-'));

    FOR d IN c_domiciliarios LOOP              -- abre, recorre y cierra solo
        v_entregados := 0;
        v_fallidos   := 0;
        v_a_tiempo   := 0;
        v_ganancias  := 0;

        FOR s IN c_servicios(d.ID) LOOP         -- cursor anidado con parámetro
            IF s.ESTADO = 'ENTREGADO' THEN
                v_entregados := v_entregados + 1;
                v_ganancias  := v_ganancias + NVL(s.PAGO_DOMICILIARIO, 0);
                IF s.ENTREGADO <= s.LIMITE THEN
                    v_a_tiempo := v_a_tiempo + 1;
                END IF;
            ELSIF s.ESTADO = 'FALLIDO' THEN
                v_fallidos := v_fallidos + 1;
            END IF;
        END LOOP;

        DBMS_OUTPUT.PUT_LINE(RPAD(d.NOMBRE, 20) || ' | ' || RPAD(d.ESTADO_OPERATIVO, 17)
                             || ' | entregados: ' || LPAD(v_entregados, 3)
                             || ' | a tiempo: '   || LPAD(v_a_tiempo, 3)
                             || ' | fallidos: '   || LPAD(v_fallidos, 2)
                             || ' | ganancias: $' || TO_CHAR(v_ganancias, 'FM999G999G990'));
    END LOOP;
END PR_RESUMEN_DOMICILIARIOS;
/

-- ---------------------------------------------------------------------
-- PR_SERVICIOS_ATRASADOS
-- Lista los servicios abiertos que ya pasaron su hora límite.
-- Usa el cursor "a mano": OPEN, FETCH en un LOOP, EXIT WHEN %NOTFOUND
-- y CLOSE. Lee de la vista VW_SERVICIOS_ACTIVOS (02_vistas.sql).
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE PR_SERVICIOS_ATRASADOS IS
    CURSOR c_atrasados IS
        SELECT *
          FROM VW_SERVICIOS_ACTIVOS
         WHERE ATRASADO = 'SI'
         ORDER BY LIMITE;

    r_servicio c_atrasados%ROWTYPE;            -- una fila con la forma del cursor
BEGIN
    OPEN c_atrasados;
    LOOP
        FETCH c_atrasados INTO r_servicio;
        EXIT WHEN c_atrasados%NOTFOUND;

        DBMS_OUTPUT.PUT_LINE('#' || r_servicio.ID
                             || ' | ' || FN_NOMBRE_ESTADO(r_servicio.ESTADO)
                             || ' | ' || r_servicio.PRIORIDAD
                             || ' | ' || NVL(r_servicio.DOMICILIARIO, 'sin domiciliario')
                             || ' | límite ' || TO_CHAR(r_servicio.LIMITE, 'DD/MM HH24:MI')
                             || ' | abierto hace ' || r_servicio.MINUTOS_ABIERTO || ' min');
    END LOOP;

    IF c_atrasados%ROWCOUNT = 0 THEN
        DBMS_OUTPUT.PUT_LINE('No hay servicios atrasados.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Total atrasados: ' || c_atrasados%ROWCOUNT);
    END IF;
    CLOSE c_atrasados;
EXCEPTION
    WHEN OTHERS THEN
        IF c_atrasados%ISOPEN THEN             -- nunca dejar un cursor abierto
            CLOSE c_atrasados;
        END IF;
        RAISE;
END PR_SERVICIOS_ATRASADOS;
/

-- ---------------------------------------------------------------------
-- PR_REPORTE_DIA
-- Reporte de un día: cuántos servicios hubo por estado y su valor.
-- El cursor recibe la fecha como parámetro y se lee de una sola vez
-- con BULK COLLECT (trae todas las filas a una colección en memoria).
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE PR_REPORTE_DIA (p_fecha IN DATE DEFAULT TRUNC(SYSDATE)) IS
    CURSOR c_por_estado (p_dia DATE) IS
        SELECT ESTADO, COUNT(*) AS CANTIDAD, NVL(SUM(VALOR), 0) AS VALOR
          FROM SERVICIO
         WHERE CREADO >= TRUNC(p_dia)
           AND CREADO <  TRUNC(p_dia) + 1
         GROUP BY ESTADO
         ORDER BY CANTIDAD DESC;

    TYPE t_filas IS TABLE OF c_por_estado%ROWTYPE;
    v_filas  t_filas;
    v_total  PLS_INTEGER := 0;
    v_valor  NUMBER      := 0;
BEGIN
    OPEN c_por_estado(p_fecha);
    FETCH c_por_estado BULK COLLECT INTO v_filas;
    CLOSE c_por_estado;

    DBMS_OUTPUT.PUT_LINE('Servicios creados el ' || TO_CHAR(p_fecha, 'DD/MM/YYYY'));
    IF v_filas.COUNT = 0 THEN
        DBMS_OUTPUT.PUT_LINE('Ese día no hubo servicios.');
        RETURN;
    END IF;

    FOR i IN 1 .. v_filas.COUNT LOOP
        DBMS_OUTPUT.PUT_LINE(RPAD(FN_NOMBRE_ESTADO(v_filas(i).ESTADO), 12)
                             || LPAD(v_filas(i).CANTIDAD, 5)
                             || '   $' || TO_CHAR(v_filas(i).VALOR, 'FM999G999G990'));
        v_total := v_total + v_filas(i).CANTIDAD;
        v_valor := v_valor + v_filas(i).VALOR;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE(RPAD('Total', 12) || LPAD(v_total, 5) || '   $' || TO_CHAR(v_valor, 'FM999G999G990'));
    DBMS_OUTPUT.PUT_LINE('Puntualidad del día: '
                         || NVL(TO_CHAR(FN_PORCENTAJE_A_TIEMPO(TRUNC(p_fecha), TRUNC(p_fecha))), 'sin entregas') || ' %');
END PR_REPORTE_DIA;
/

-- ---------------------------------------------------------------------
-- FN_SERVICIOS_CLIENTE
-- Devuelve un cursor abierto (SYS_REFCURSOR) con los servicios de un
-- cliente buscado por teléfono. Quien lo llama recorre las filas.
-- Ejemplo en VS Code / SQL Developer:
--   VARIABLE rc REFCURSOR
--   EXEC :rc := FN_SERVICIOS_CLIENTE('3001112233');
--   PRINT rc
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION FN_SERVICIOS_CLIENTE (p_telefono IN VARCHAR2)
RETURN SYS_REFCURSOR IS
    v_cursor SYS_REFCURSOR;
BEGIN
    OPEN v_cursor FOR
        SELECT s.ID,
               FN_NOMBRE_ESTADO(s.ESTADO) AS ESTADO,
               s.CREADO,
               s.DIR_ENTREGA,
               s.VALOR,
               d.NOMBRE                   AS DOMICILIARIO,
               FN_MINUTOS_ENTREGA(s.ID)   AS MINUTOS_ENTREGA
          FROM SERVICIO s
          JOIN CLIENTE c ON c.ID = s.CLIENTE_ID
          LEFT JOIN DOMICILIARIO d ON d.ID = s.DOMICILIARIO_ID
         WHERE c.TELEFONO = p_telefono
         ORDER BY s.CREADO DESC;
    RETURN v_cursor;
END FN_SERVICIOS_CLIENTE;
/

-- ---------------------------------------------------------------------
-- PR_DEPURAR_BITACORA
-- Borra los registros de BITACORA_BD más viejos que p_dias.
-- Cursor FOR UPDATE: bloquea las filas que va a borrar y las borra con
-- WHERE CURRENT OF (la fila donde está parado el cursor).
-- No hace COMMIT: quien lo ejecuta decide si confirma o deshace.
-- ---------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE PR_DEPURAR_BITACORA (p_dias IN NUMBER DEFAULT 90) IS
    CURSOR c_viejos IS
        SELECT ID
          FROM BITACORA_BD
         WHERE FECHA < SYSTIMESTAMP - NUMTODSINTERVAL(p_dias, 'DAY')
           FOR UPDATE;

    v_borrados PLS_INTEGER := 0;
BEGIN
    IF p_dias IS NULL OR p_dias < 1 THEN
        RAISE_APPLICATION_ERROR(-20011, 'Los días deben ser 1 o más');
    END IF;

    FOR r IN c_viejos LOOP
        DELETE FROM BITACORA_BD WHERE CURRENT OF c_viejos;
        v_borrados := v_borrados + 1;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('Registros de bitácora borrados: ' || v_borrados
                         || ' (haz COMMIT para confirmar o ROLLBACK para deshacer)');
END PR_DEPURAR_BITACORA;
/
