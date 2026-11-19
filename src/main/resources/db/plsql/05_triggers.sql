-- =====================================================================
-- 05 · Triggers (disparadores)
-- Conectado como DESPACHO, después de 00_permisos.sql (CREATE TRIGGER)
-- y de 01_bitacora.sql (BITACORA_BD y PR_REGISTRAR_BITACORA).
--
-- Reglas que siguen todos estos triggers para no dañar la aplicación:
--   1. NUNCA cambian columnas de las tablas de la aplicación (no hay
--      ":NEW.X := ..."): la aplicación tiene los datos en memoria y no
--      se enteraría del cambio.
--   2. Solo ESCRIBEN en BITACORA_BD (tabla nueva) o RECHAZAN datos
--      imposibles que la aplicación ya valida por su lado (un valor
--      negativo, un PIN que no es de 4 dígitos...). Con el uso normal
--      del programa nunca se disparan esos errores.
--   3. No hay triggers en tablas que cambian cada pocos segundos
--      (UBICACION_GPS, DOMICILIARIO) para no hacer lenta la operación.
--
-- Códigos de error propios: -20001 a -20099 (rango que Oracle reserva
-- para errores de usuario con RAISE_APPLICATION_ERROR).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. TRG_SERVICIO_VALIDAR · BEFORE INSERT OR UPDATE · por fila
-- Última barrera antes de guardar un servicio. Rechaza datos que no
-- pueden existir. (Lo mismo valida Java en Requests y DispatchService.)
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_SERVICIO_VALIDAR
BEFORE INSERT OR UPDATE ON SERVICIO
FOR EACH ROW
BEGIN
    IF :NEW.VALOR < 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'El valor del servicio no puede ser negativo');
    END IF;

    IF :NEW.VALOR_RECIBIDO < 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'El dinero recibido no puede ser negativo');
    END IF;

    IF NOT REGEXP_LIKE(:NEW.PIN, '^[0-9]{4}$') THEN
        RAISE_APPLICATION_ERROR(-20003, 'El PIN debe tener exactamente 4 dígitos');
    END IF;

    IF :NEW.ENTREGADO IS NOT NULL AND :NEW.ENTREGADO < :NEW.CREADO THEN
        RAISE_APPLICATION_ERROR(-20004, 'La hora de entrega no puede ser anterior a la de creación');
    END IF;
END TRG_SERVICIO_VALIDAR;
/

-- ---------------------------------------------------------------------
-- 2. TRG_SERVICIO_BITACORA · AFTER INSERT OR UPDATE · por fila
-- Deja en la bitácora la creación de cada servicio y cada cambio de
-- estado, de valor o de domiciliario.
-- Hibernate manda todas las columnas en cada UPDATE, por eso se compara
-- el valor anterior (:OLD) con el nuevo (:NEW) y solo se registra lo que
-- de verdad cambió.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_SERVICIO_BITACORA
AFTER INSERT OR UPDATE OF ESTADO, VALOR, DOMICILIARIO_ID ON SERVICIO
FOR EACH ROW
BEGIN
    IF INSERTING THEN
        PR_REGISTRAR_BITACORA('SERVICIO', :NEW.ID, 'INSERT', 'ESTADO', NULL, :NEW.ESTADO);
    ELSE  -- UPDATING
        IF :OLD.ESTADO <> :NEW.ESTADO THEN
            PR_REGISTRAR_BITACORA('SERVICIO', :NEW.ID, 'UPDATE', 'ESTADO', :OLD.ESTADO, :NEW.ESTADO);
        END IF;

        IF :OLD.VALOR <> :NEW.VALOR THEN
            PR_REGISTRAR_BITACORA('SERVICIO', :NEW.ID, 'UPDATE', 'VALOR', :OLD.VALOR, :NEW.VALOR);
        END IF;

        -- DOMICILIARIO_ID puede ser NULL: NVL evita que "NULL <> 5" quede sin respuesta
        IF NVL(:OLD.DOMICILIARIO_ID, -1) <> NVL(:NEW.DOMICILIARIO_ID, -1) THEN
            PR_REGISTRAR_BITACORA('SERVICIO', :NEW.ID, 'UPDATE', 'DOMICILIARIO_ID',
                                  :OLD.DOMICILIARIO_ID, :NEW.DOMICILIARIO_ID);
        END IF;
    END IF;
END TRG_SERVICIO_BITACORA;
/

-- ---------------------------------------------------------------------
-- 3. TRG_SERVICIO_NO_BORRAR · BEFORE DELETE · por sentencia
-- Los servicios no se borran: se cancelan. Este trigger es "por
-- sentencia" (sin FOR EACH ROW): se dispara una sola vez por cada DELETE,
-- borre una fila o mil, y lo rechaza antes de tocar nada.
-- La aplicación nunca borra servicios, así que no le afecta.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_SERVICIO_NO_BORRAR
BEFORE DELETE ON SERVICIO
BEGIN
    RAISE_APPLICATION_ERROR(-20005, 'Los servicios no se borran: se cancelan (estado CANCELADO)');
END TRG_SERVICIO_NO_BORRAR;
/

-- ---------------------------------------------------------------------
-- 4. TRG_USUARIO_BITACORA · AFTER UPDATE OF ESTADO_CUENTA, ROL · por fila
-- Registra cuando una cuenta se aprueba, suspende, rechaza o cambia de
-- rol. La cláusula WHEN filtra antes de entrar al cuerpo: si el estado
-- y el rol siguen iguales (por ejemplo, al iniciar sesión) no hace nada.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_USUARIO_BITACORA
AFTER UPDATE OF ESTADO_CUENTA, ROL ON USUARIO
FOR EACH ROW
WHEN (OLD.ESTADO_CUENTA <> NEW.ESTADO_CUENTA OR OLD.ROL <> NEW.ROL)
BEGIN
    IF :OLD.ESTADO_CUENTA <> :NEW.ESTADO_CUENTA THEN
        PR_REGISTRAR_BITACORA('USUARIO', :NEW.ID, 'UPDATE', 'ESTADO_CUENTA',
                              :NEW.USUARIO || ': ' || :OLD.ESTADO_CUENTA, :NEW.ESTADO_CUENTA);
    END IF;
    IF :OLD.ROL <> :NEW.ROL THEN
        PR_REGISTRAR_BITACORA('USUARIO', :NEW.ID, 'UPDATE', 'ROL',
                              :NEW.USUARIO || ': ' || :OLD.ROL, :NEW.ROL);
    END IF;
END TRG_USUARIO_BITACORA;
/

-- ---------------------------------------------------------------------
-- 5. TRG_PARAMETRO_BITACORA · AFTER UPDATE · por fila
-- Registra cada cambio de un parámetro de operación (modo del motor,
-- segundos de oferta, etc.) con su valor anterior y el nuevo.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PARAMETRO_BITACORA
AFTER UPDATE OF VALOR ON PARAMETRO
FOR EACH ROW
WHEN (OLD.VALOR <> NEW.VALOR)
BEGIN
    PR_REGISTRAR_BITACORA('PARAMETRO', :NEW.CLAVE, 'UPDATE', 'VALOR', :OLD.VALOR, :NEW.VALOR);
END TRG_PARAMETRO_BITACORA;
/

-- ---------------------------------------------------------------------
-- 6. TRG_TARIFA_VALIDAR · BEFORE INSERT OR UPDATE · por fila
-- Una tarifa no puede tener montos negativos y el porcentaje del
-- domiciliario debe estar entre 0 y 100.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_TARIFA_VALIDAR
BEFORE INSERT OR UPDATE ON TARIFA
FOR EACH ROW
BEGIN
    IF :NEW.BASE < 0 OR :NEW.POR_KM < 0 OR :NEW.RECARGO_MEDIANO < 0
       OR :NEW.RECARGO_GRANDE < 0 OR :NEW.MINIMO < 0 THEN
        RAISE_APPLICATION_ERROR(-20006, 'Los valores de la tarifa no pueden ser negativos');
    END IF;

    IF :NEW.PORCENTAJE_DOMICILIARIO NOT BETWEEN 0 AND 100 THEN
        RAISE_APPLICATION_ERROR(-20007, 'El porcentaje del domiciliario debe estar entre 0 y 100');
    END IF;
END TRG_TARIFA_VALIDAR;
/

-- ---------------------------------------------------------------------
-- 7. TRG_CLIENTE_BORRADO · AFTER DELETE · por fila
-- La aplicación desactiva clientes (ACTIVO = 0) en vez de borrarlos.
-- Si alguien borra uno a mano, queda copia de sus datos en la bitácora
-- (:OLD tiene la fila tal como estaba antes de borrarla).
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_CLIENTE_BORRADO
AFTER DELETE ON CLIENTE
FOR EACH ROW
BEGIN
    PR_REGISTRAR_BITACORA('CLIENTE', :OLD.ID, 'DELETE', NULL,
                          :OLD.NOMBRE || ' · ' || :OLD.TELEFONO, NULL);
END TRG_CLIENTE_BORRADO;
/
