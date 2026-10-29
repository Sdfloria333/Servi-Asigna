-- V4: se quita el modo simulación (joystick) de la app del domiciliario.
-- Desde esta versión la posición del domiciliario sale siempre del GPS real del celular.
-- Solo se borra el parámetro que activaba el joystick; no se toca ningún otro dato.

DELETE FROM PARAMETRO WHERE CLAVE = 'simulacion';
