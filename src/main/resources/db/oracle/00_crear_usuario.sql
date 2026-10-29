-- =====================================================================
-- Se ejecuta UNA SOLA VEZ, conectado como SYSTEM a la base XEPDB1
-- (SQL Developer o:  sqlplus system/TU_CLAVE@localhost:1521/XEPDB1 ).
-- Crea el usuario dueño de las tablas. Las tablas las crea la aplicación.
-- Cambia 'Despacho2026' por una clave tuya y ponla en DB_PASSWORD.
-- =====================================================================
CREATE USER DESPACHO IDENTIFIED BY "Despacho2026"
  DEFAULT TABLESPACE USERS
  TEMPORARY TABLESPACE TEMP
  QUOTA UNLIMITED ON USERS;

GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE, CREATE PROCEDURE TO DESPACHO;

-- Para empezar de cero (borra TODO lo del sistema):
--   DROP USER DESPACHO CASCADE;
-- y vuelve a ejecutar este archivo.
