-- =====================================================================
-- 00 · Permisos para PL/SQL
-- Se ejecuta UNA SOLA VEZ, conectado como SYSTEM a XEPDB1.
-- DESPACHO ya puede crear tablas, vistas y procedimientos (00_crear_usuario.sql);
-- lo único que le falta para esta parte es crear triggers.
-- =====================================================================
GRANT CREATE TRIGGER TO DESPACHO;

-- Comprobación: debe aparecer CREATE TRIGGER en la lista
SELECT PRIVILEGE FROM DBA_SYS_PRIVS WHERE GRANTEE = 'DESPACHO' ORDER BY PRIVILEGE;
