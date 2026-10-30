-- V3: se quita el control de documentos de los domiciliarios (licencia, SOAT, tecnomecánica).
-- El domiciliario se identifica por su nombre, cédula, celular, la placa de su vehículo (tabla VEHICULO)
-- y su estado en la aplicación (tabla DOMICILIARIO). Este script no borra ningún otro dato.

-- 1. Primero la llave foránea hacia DOMICILIARIO y luego la tabla (el índice se borra con ella)
ALTER TABLE DOCUMENTO_DOMICILIARIO DROP CONSTRAINT FK_DOCU_DOMICILIARIO;
DROP TABLE DOCUMENTO_DOMICILIARIO;

-- 2. Los parámetros que solo servían para los documentos
DELETE FROM PARAMETRO WHERE CLAVE IN ('alerta.dias-documento', 'documentos.bloquear-vencidos');

-- 3. La tabla de vehículos queda descrita como el dato que identifica al domiciliario
COMMENT ON TABLE VEHICULO IS 'Vehículo del domiciliario: placa, tipo y capacidad en unidades.';
