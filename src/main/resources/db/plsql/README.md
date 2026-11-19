# PL/SQL de ServiAsigna: vistas, funciones, cursores y triggers

Esta carpeta tiene la parte de PL/SQL que pidió el profesor. Son **scripts manuales**: la aplicación no los ejecuta al arrancar (el migrador solo lee `db/migracion/`) y no depende de ellos. Se instalan a mano desde VS Code y se pueden quitar con `99_desinstalar.sql`.

## Qué hay

| Archivo | Se ejecuta como | Qué crea |
| --- | --- | --- |
| `00_permisos.sql` | `SYSTEM` | Da a `DESPACHO` el permiso `CREATE TRIGGER` (una sola vez) |
| `01_bitacora.sql` | `DESPACHO` | Tabla `BITACORA_BD` y procedimiento `PR_REGISTRAR_BITACORA` |
| `02_vistas.sql` | `DESPACHO` | 5 vistas nuevas |
| `03_funciones.sql` | `DESPACHO` | 4 funciones |
| `04_cursores.sql` | `DESPACHO` | 4 procedimientos y 1 función con cursores explícitos |
| `05_triggers.sql` | `DESPACHO` | 7 triggers |
| `instalar.sql` | `DESPACHO` | Ejecuta 01 a 05 en orden |
| `06_pruebas.sql` | `DESPACHO` | Prueba todo y deshace los cambios con `ROLLBACK` |
| `99_desinstalar.sql` | `DESPACHO` | Borra todo lo anterior |

### Vistas (más las 2 que ya existían de la migración V2)

| Vista | Muestra |
| --- | --- |
| `VW_SERVICIOS_ACTIVOS` | Servicios abiertos, su domiciliario, minutos abiertos y si están atrasados |
| `VW_DESEMPENO_DOMICILIARIO` | Por domiciliario: servicios, entregados, fallidos, cancelados, ganancias y % a tiempo |
| `VW_NOVEDADES_ABIERTAS` | Novedades sin cerrar con minutos abiertas y número de comentarios |
| `VW_RECAUDO_TURNOS` | Cuadre de dinero por turno (recaudado, entregado, pendiente) |
| `VW_CLIENTES_FRECUENTES` | Clientes activos con servicios, valor entregado y último servicio |

### Funciones

| Función | Devuelve |
| --- | --- |
| `FN_NOMBRE_ESTADO(estado)` | El estado en texto legible (`EN_COLA` → "Sin asignar") |
| `FN_GANANCIAS_DOMICILIARIO(id, desde, hasta)` | Lo que ganó un domiciliario en servicios entregados |
| `FN_MINUTOS_ENTREGA(id)` | Minutos de creación a entrega de un servicio (error -20010 si no existe) |
| `FN_PORCENTAJE_A_TIEMPO(desde, hasta)` | % de entregas a tiempo en un rango de fechas |

### Cursores

| Objeto | Tipo de cursor |
| --- | --- |
| `PR_RESUMEN_DOMICILIARIOS(desde, hasta)` | Cursor `FOR LOOP` y cursor con parámetro anidado |
| `PR_SERVICIOS_ATRASADOS` | `OPEN` / `FETCH` / `CLOSE` con `%ROWTYPE`, `%NOTFOUND`, `%ROWCOUNT`, `%ISOPEN` |
| `PR_REPORTE_DIA(fecha)` | Cursor con parámetro leído con `BULK COLLECT` |
| `FN_SERVICIOS_CLIENTE(telefono)` | Cursor de referencia `SYS_REFCURSOR` |
| `PR_DEPURAR_BITACORA(dias)` | Cursor `FOR UPDATE` con `WHERE CURRENT OF` (solo sobre `BITACORA_BD`) |

### Triggers

| Trigger | Momento | Nivel | Qué hace |
| --- | --- | --- | --- |
| `TRG_SERVICIO_VALIDAR` | `BEFORE INSERT OR UPDATE` en `SERVICIO` | Fila | Rechaza valor o dinero recibido negativos, PIN que no sea de 4 dígitos y entrega antes de la creación |
| `TRG_SERVICIO_BITACORA` | `AFTER INSERT OR UPDATE` en `SERVICIO` | Fila | Registra creación y cambios de estado, valor y domiciliario |
| `TRG_SERVICIO_NO_BORRAR` | `BEFORE DELETE` en `SERVICIO` | Sentencia | Impide borrar servicios (se cancelan) |
| `TRG_USUARIO_BITACORA` | `AFTER UPDATE` en `USUARIO` | Fila con `WHEN` | Registra cambios de estado de cuenta y de rol |
| `TRG_PARAMETRO_BITACORA` | `AFTER UPDATE` en `PARAMETRO` | Fila con `WHEN` | Registra cambios de parámetros con valor anterior y nuevo |
| `TRG_TARIFA_VALIDAR` | `BEFORE INSERT OR UPDATE` en `TARIFA` | Fila | Rechaza montos negativos y porcentajes fuera de 0–100 |
| `TRG_CLIENTE_BORRADO` | `AFTER DELETE` en `CLIENTE` | Fila | Guarda copia del cliente borrado |

## Por qué no daña la aplicación

- Ningún trigger cambia columnas de las tablas de la aplicación. `DispatchService` tiene los servicios en memoria y no vería un cambio hecho por la base.
- Los triggers solo escriben en `BITACORA_BD`, una tabla nueva que Java no usa, o rechazan datos que la aplicación ya valida por su lado. Con el uso normal del programa esos errores nunca salen.
- No hay triggers en tablas que cambian cada pocos segundos (`UBICACION_GPS`, `DOMICILIARIO`).
- Las vistas, funciones y cursores solo leen. El único que borra es `PR_DEPURAR_BITACORA`, y solo filas de `BITACORA_BD`, sin `COMMIT`.
- Las migraciones V1–V4 no se tocaron.

Con los triggers instalados, el ejemplo `DELETE FROM SERVICIO` de `02_consultas_demo.sql` falla con `ORA-20005` (el trigger) en vez de `ORA-02292` (la llave foránea). Sigue siendo un error esperado.

## Cómo instalarlo en VS Code

1. Instala la extensión **SQL Developer for VS Code** de Oracle (en Extensiones busca "SQL Developer"). Es la que se conecta a la base y ejecuta scripts. Si además usas una extensión de resaltado de PL/SQL, los archivos `.sql` se ven igual con ella.
2. Crea dos conexiones en el panel de SQL Developer: una con `SYSTEM` y otra con `DESPACHO` / `Despacho2026`, ambas a `localhost`, puerto `1521`, servicio `XEPDB1`.
3. Con la conexión `SYSTEM`, abre `00_permisos.sql` y pulsa **F5** (ejecutar script). Solo la primera vez.
4. Con la conexión `DESPACHO`, abre `instalar.sql` y pulsa **F5**. Al final debe listar todo como `VALID`. Si VS Code no encuentra los archivos de `@@`, abre `01` a `05` uno por uno, en orden, y pulsa **F5** en cada uno.
5. Con la aplicación **apagada**, abre `06_pruebas.sql` y pulsa **F5**. La salida de `DBMS_OUTPUT` aparece en la pestaña **Script Output**.

**F5** ejecuta el archivo completo; **Ctrl+Enter** ejecuta solo la sentencia donde está el cursor. Los bloques PL/SQL terminan con una línea que solo tiene `/`: esa barra es la que los ejecuta.

Para ver la bitácora después de usar la aplicación:

```sql
SELECT FECHA, TABLA, REGISTRO_ID, OPERACION, CAMPO, VALOR_ANTERIOR, VALOR_NUEVO, PROGRAMA
  FROM BITACORA_BD ORDER BY ID DESC;
```

`PROGRAMA = 'JDBC Thin Client'` significa que el cambio lo hizo la aplicación. Cualquier otro valor es alguien conectado a mano (SQL Developer, VS Code…).
