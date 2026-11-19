-- =====================================================================
-- Instala toda la parte de PL/SQL en orden (01 a 05).
-- Conectado como DESPACHO y DESPUÉS de haber ejecutado 00_permisos.sql
-- como SYSTEM. En VS Code: abre este archivo y pulsa F5.
-- Se puede ejecutar varias veces: las vistas, funciones, procedimientos y
-- triggers usan CREATE OR REPLACE. Solo 01 falla la segunda vez porque
-- BITACORA_BD ya existe (ese error se puede ignorar).
-- =====================================================================
SET SERVEROUTPUT ON

@@01_bitacora.sql
@@02_vistas.sql
@@03_funciones.sql
@@04_cursores.sql
@@05_triggers.sql

-- Resultado: todo debe salir VALID
SELECT OBJECT_TYPE, OBJECT_NAME, STATUS
  FROM USER_OBJECTS
 WHERE OBJECT_TYPE IN ('TABLE', 'VIEW', 'FUNCTION', 'PROCEDURE', 'TRIGGER')
   AND (OBJECT_NAME LIKE 'VW\_%' ESCAPE '\' OR OBJECT_NAME LIKE 'FN\_%' ESCAPE '\'
        OR OBJECT_NAME LIKE 'PR\_%' ESCAPE '\' OR OBJECT_NAME LIKE 'TRG\_%' ESCAPE '\'
        OR OBJECT_NAME = 'BITACORA_BD')
 ORDER BY OBJECT_TYPE, OBJECT_NAME;

