-- =====================================================================
-- 06 · Pruebas y demostración de todo lo de PL/SQL
-- Conectado como DESPACHO, después de instalar 01 a 05.
--
-- Este archivo NO deja cambios: la parte 5 trabaja dentro de un
-- SAVEPOINT y termina con ROLLBACK. Aun así, ejecútalo con la aplicación
-- APAGADA: mientras la parte 5 corre, las filas que toca quedan
-- bloqueadas y la aplicación tendría que esperar.
--
-- En VS Code: abre este archivo con la conexión DESPACHO y pulsa F5
-- (ejecutar script). La salida de DBMS_OUTPUT sale en "Script Output".
-- =====================================================================
SET SERVEROUTPUT ON
SET LINESIZE 200
SET PAGESIZE 60

-- ---------------------------------------------------------------------
-- 1. Qué se instaló (todo debe estar VALID / ENABLED)
-- ---------------------------------------------------------------------
SELECT OBJECT_TYPE, OBJECT_NAME, STATUS
  FROM USER_OBJECTS
 WHERE OBJECT_TYPE IN ('VIEW', 'FUNCTION', 'PROCEDURE', 'TRIGGER')
 ORDER BY OBJECT_TYPE, OBJECT_NAME;

SELECT TRIGGER_NAME, TRIGGER_TYPE, TRIGGERING_EVENT, TABLE_NAME, STATUS
  FROM USER_TRIGGERS
 ORDER BY TABLE_NAME, TRIGGER_NAME;

-- Si algo sale INVALID, esta consulta dice la línea y el error
SELECT NAME, TYPE, LINE, POSITION, TEXT FROM USER_ERRORS ORDER BY NAME, SEQUENCE;

-- ---------------------------------------------------------------------
-- 2. Vistas
-- ---------------------------------------------------------------------
SELECT * FROM VW_SERVICIOS_ACTIVOS ORDER BY CREADO;
SELECT * FROM VW_DESEMPENO_DOMICILIARIO ORDER BY ENTREGADOS DESC;
SELECT * FROM VW_NOVEDADES_ABIERTAS ORDER BY ABIERTO;
SELECT * FROM VW_RECAUDO_TURNOS ORDER BY INICIO DESC FETCH FIRST 10 ROWS ONLY;
SELECT * FROM VW_CLIENTES_FRECUENTES ORDER BY SERVICIOS DESC;

-- ---------------------------------------------------------------------
-- 3. Funciones (se usan dentro de un SELECT como cualquier función)
-- ---------------------------------------------------------------------
SELECT NOMBRE,
       FN_GANANCIAS_DOMICILIARIO(ID)                                    AS GANANCIAS_TOTAL,
       FN_GANANCIAS_DOMICILIARIO(ID, TRUNC(SYSDATE) - 6, TRUNC(SYSDATE)) AS GANANCIAS_7_DIAS
  FROM DOMICILIARIO
 ORDER BY NOMBRE;

SELECT ID, FN_NOMBRE_ESTADO(ESTADO) AS ESTADO, FN_MINUTOS_ENTREGA(ID) AS MINUTOS_ENTREGA
  FROM SERVICIO
 ORDER BY ID DESC
 FETCH FIRST 10 ROWS ONLY;

SELECT FN_PORCENTAJE_A_TIEMPO(TRUNC(SYSDATE) - 6, TRUNC(SYSDATE)) AS PCT_A_TIEMPO_7_DIAS FROM DUAL;

-- Error controlado: un servicio que no existe (debe mostrar ORA-20010)
SELECT FN_MINUTOS_ENTREGA(-1) FROM DUAL;

-- ---------------------------------------------------------------------
-- 4. Cursores
-- ---------------------------------------------------------------------
EXEC PR_RESUMEN_DOMICILIARIOS;
EXEC PR_SERVICIOS_ATRASADOS;
EXEC PR_REPORTE_DIA(TRUNC(SYSDATE) - 1);

-- Cursor de referencia: los servicios de un cliente de los datos demo
DECLARE
    v_cursor    SYS_REFCURSOR;
    v_id        NUMBER;
    v_estado    VARCHAR2(30);
    v_creado    TIMESTAMP;
    v_direccion VARCHAR2(150);
    v_valor     NUMBER;
    v_domi      VARCHAR2(80);
    v_minutos   NUMBER;
BEGIN
    v_cursor := FN_SERVICIOS_CLIENTE('3001112233');
    LOOP
        FETCH v_cursor INTO v_id, v_estado, v_creado, v_direccion, v_valor, v_domi, v_minutos;
        EXIT WHEN v_cursor%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE('#' || v_id || ' | ' || v_estado || ' | ' || TO_CHAR(v_creado, 'DD/MM HH24:MI')
                             || ' | $' || v_valor || ' | ' || NVL(v_domi, 'sin domiciliario')
                             || ' | ' || NVL(TO_CHAR(v_minutos), '-') || ' min');
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('Servicios del cliente: ' || v_cursor%ROWCOUNT);
    CLOSE v_cursor;
END;
/
-- ---------------------------------------------------------------------
-- 5. Triggers (todo se deshace al final con ROLLBACK)
-- ---------------------------------------------------------------------
SAVEPOINT pruebas_plsql;

-- 5.1 Un cambio de valor queda en la bitácora (TRG_SERVICIO_BITACORA)
UPDATE SERVICIO SET VALOR = VALOR + 500 WHERE ID = (SELECT MIN(ID) FROM SERVICIO);

-- 5.2 Un cambio de parámetro queda en la bitácora (TRG_PARAMETRO_BITACORA)
UPDATE PARAMETRO SET VALOR = '45' WHERE CLAVE = 'oferta.segundos';

-- 5.3 Un cliente borrado a mano deja copia en la bitácora (TRG_CLIENTE_BORRADO)
INSERT INTO CLIENTE (NOMBRE, TELEFONO, ACTIVO, CREADO) VALUES ('Cliente de prueba PL/SQL', '3109998877', 1, SYSTIMESTAMP);
DELETE FROM CLIENTE WHERE TELEFONO = '3109998877';

-- Lo que registraron los triggers
SELECT ID, TABLA, REGISTRO_ID, OPERACION, CAMPO, VALOR_ANTERIOR, VALOR_NUEVO, USUARIO_BD, PROGRAMA
  FROM BITACORA_BD
 ORDER BY ID DESC
 FETCH FIRST 5 ROWS ONLY;

-- 5.4 Datos imposibles: cada bloque DEBE ser rechazado por un trigger.
--     Los errores se atrapan y se imprimen para que el script siga.
BEGIN
    UPDATE SERVICIO SET VALOR = -1000 WHERE ID = (SELECT MIN(ID) FROM SERVICIO);
    DBMS_OUTPUT.PUT_LINE('ERROR: se aceptó un valor negativo');
EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Rechazado (TRG_SERVICIO_VALIDAR): ' || SQLERRM);
END;
/

BEGIN
    UPDATE SERVICIO SET PIN = '12A' WHERE ID = (SELECT MIN(ID) FROM SERVICIO);
    DBMS_OUTPUT.PUT_LINE('ERROR: se aceptó un PIN inválido');
EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Rechazado (TRG_SERVICIO_VALIDAR): ' || SQLERRM);
END;
/

BEGIN
    DELETE FROM SERVICIO WHERE ID = (SELECT MIN(ID) FROM SERVICIO);
    DBMS_OUTPUT.PUT_LINE('ERROR: se borró un servicio');
EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Rechazado (TRG_SERVICIO_NO_BORRAR): ' || SQLERRM);
END;
/

BEGIN
    UPDATE TARIFA SET PORCENTAJE_DOMICILIARIO = 150 WHERE ACTIVA = 1;
    DBMS_OUTPUT.PUT_LINE('ERROR: se aceptó un porcentaje de 150');
EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Rechazado (TRG_TARIFA_VALIDAR): ' || SQLERRM);
END;
/

-- 5.5 Cursor FOR UPDATE: borra de la bitácora lo de más de 1 día (se deshace abajo)
EXEC PR_DEPURAR_BITACORA(1);

-- Deshacer TODO lo de la parte 5: la base queda como estaba
ROLLBACK TO pruebas_plsql;

SELECT COUNT(*) AS REGISTROS_DE_PRUEBA_QUE_QUEDAN
  FROM BITACORA_BD
 WHERE TABLA = 'CLIENTE' AND VALOR_ANTERIOR LIKE 'Cliente de prueba PL/SQL%';
