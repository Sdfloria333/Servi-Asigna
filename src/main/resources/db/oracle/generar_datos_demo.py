# Genera db/oracle/01_datos_demo.sql: una semana de operación coherente (servicios, paradas, historial,
# ofertas, evidencias, turnos, GPS, novedades y auditoría) para mostrar la base de datos funcionando.
import random, math, sys

random.seed(2026)
DOMIS = ['juan', 'maria', 'pedro', 'ana']
CLIENTES = [
    ('Restaurante El Sabor', '3001112233', [('Calle 16 # 9-20, Centro', 10.4747, -73.2475, 'Frente al parque')]),
    ('Droguería La Popular', '3012223344', [('Carrera 9 # 13-45, Centro', 10.4766, -73.2463, None)]),
    ('Laura Méndez', '3023334455', [('Calle 31 # 4-30, Novalito', 10.4685, -73.2564, 'Casa esquinera'),
                                    ('Diagonal 21 # 18-15, Los Mayales', 10.4590, -73.2650, 'Apto 302')]),
    ('Pizzería Don Vito', '3034445566', [('Carrera 11 # 15-60, Gaitán', 10.4738, -73.2504, None)]),
    ('Carlos Ospina', '3045556677', [('Calle 7 # 19-40, Los Cortijos', 10.4842, -73.2652, 'Portón negro')]),
    ('Panadería Valle Pan', '3056667788', [('Carrera 19 # 9-12, Primero de Mayo', 10.4805, -73.2610, None)]),
    ('Andrea Torres', '3067778899', [('Calle 44 # 12-08, Villa Castro', 10.4560, -73.2470, 'Timbre dañado, llamar')]),
    ('Supermercado El Ahorro', '3078889900', [('Carrera 4 # 22-10, San Joaquín', 10.4690, -73.2440, None)]),
]
PUNTOS = [d for _, _, ds in CLIENTES for d in ds] + [
    ('Calle 12 # 6-55, Loperena', 10.4785, -73.2520, None), ('Carrera 7 # 30-20, Panamá', 10.4655, -73.2482, None),
    ('Transversal 18 # 40-15, Sicarare', 10.4590, -73.2550, None), ('Calle 21 # 23-30, Villa Miriam', 10.4710, -73.2690, None),
]
NOTAS = ['2 hamburguesas y gaseosa', 'Medicamentos (fórmula médica)', 'Pizza familiar hawaiana', 'Documentos en sobre sellado',
         'Mercado: 2 bolsas', 'Torta de cumpleaños, llevar con cuidado', 'Almuerzo ejecutivo x3', 'Paquete pequeño, frágil']
MOTIVOS_CANCELA = ['El cliente desistió del pedido', 'El restaurante no tenía el producto', 'Pedido duplicado']

def km(a, b):
    R = 6371
    dlat, dlng = math.radians(b[1] - a[1]), math.radians(b[2] - a[2])
    h = math.sin(dlat / 2) ** 2 + math.cos(math.radians(a[1])) * math.cos(math.radians(b[1])) * math.sin(dlng / 2) ** 2
    return 2 * R * math.asin(math.sqrt(h)) * 1.3

def s(v): return 'NULL' if v is None else "'" + str(v).replace("'", "''") + "'"
def t(dia, minuto): return f"TRUNC(SYSDATE) - {dia} + {minuto:.2f}/1440"   # minuto del día (0–1439)
DOM = lambda u: f"(SELECT ID FROM DOMICILIARIO WHERE USUARIO = '{u}')"
SID = "(SELECT MAX(ID) FROM SERVICIO)"

out = []
w = out.append
w("""-- =====================================================================
-- Datos de demostración: una semana de operación de ServiAsigna
-- Ejecútalo UNA vez, conectado como DESPACHO, con el programa APAGADO:
--   sqlplus DESPACHO/Despacho2026@localhost:1521/XEPDB1 @01_datos_demo.sql
-- Necesita que el programa haya arrancado antes al menos una vez
-- (crea las tablas y los domiciliarios juan, maria, pedro y ana).
-- Las fechas son relativas a hoy: siempre quedan en los últimos 7 días.
-- =====================================================================
SET DEFINE OFF
SET FEEDBACK OFF
""")

w("-- ---------------- Clientes y sus direcciones ----------------")
for nombre, tel, dirs in CLIENTES:
    w(f"INSERT INTO CLIENTE (NOMBRE, TELEFONO, ACTIVO, CREADO) VALUES ({s(nombre)}, {s(tel)}, 1, {t(10, 480)});")
    for texto, lat, lng, ref in dirs:
        w(f"INSERT INTO DIRECCION (CLIENTE_ID, TEXTO, REFERENCIA, LAT, LNG, ACTIVO) VALUES "
          f"((SELECT ID FROM CLIENTE WHERE TELEFONO = {s(tel)}), {s(texto)}, {s(ref)}, {lat}, {lng}, 1);")

resumen = {}   # (dia, domi) -> [completados, recaudo, ganancias, km]
w("\n-- ---------------- Servicios de los últimos 7 días ----------------")
n = 0
for dia in range(7, 0, -1):
    cantidad = random.randint(7, 11)
    minutos = sorted(random.sample(range(9 * 60, 20 * 60), cantidad))
    for m0 in minutos:
        n += 1
        cli = random.choice(CLIENTES)
        recogida = random.choice(PUNTOS)
        entrega = random.choice([p for p in PUNTOS if p != recogida])
        distancia = km(recogida, entrega)
        tamano = random.choices(['PEQUENO', 'MEDIANO', 'GRANDE'], [70, 22, 8])[0]
        valor = max(5000, round((4000 + 900 * distancia + {'PEQUENO': 0, 'MEDIANO': 1500, 'GRANDE': 4000}[tamano]) / 100) * 100)
        pago = round(valor * 0.7)
        forma = random.choices(['PREPAGADO', 'EFECTIVO', 'TRANSFERENCIA'], [45, 40, 15])[0]
        pin = f"{random.randint(0, 9999):04d}"
        destino = random.random()
        estado = 'ENTREGADO' if destino < 0.86 else ('CANCELADO' if destino < 0.95 else 'FALLIDO')
        domi = random.choice(DOMIS)
        manual = random.random() < 0.15
        origen = 'MANUAL' if manual else random.choices(['AUTOMATICO', 'DIFUSION'], [80, 20])[0]
        prioridad = random.choices(['NORMAL', 'ALTA'], [85, 15])[0]
        nota = random.choice(NOTAS)
        asig = m0 + random.uniform(0.5, 6)
        reco = asig + random.uniform(6, 18)
        entr = reco + random.uniform(8, 26)
        limite, lim_reco = m0 + 45, m0 + 25
        con_domi = estado != 'CANCELADO' or random.random() < 0.4
        cerrado = entr if estado == 'ENTREGADO' else (reco + 12 if estado == 'FALLIDO' else asig + 3)
        cols = ("ESTADO, RECOGIDA_LAT, RECOGIDA_LNG, DIR_RECOGIDA, ENTREGA_LAT, ENTREGA_LNG, DIR_ENTREGA, CLIENTE_ID, CLIENTE, TELEFONO, NOTA, "
                "TAMANO, PRIORIDAD, FORMA_PAGO, VALOR, VALOR_RECIBIDO, PAGO_DOMICILIARIO, PIN, CREADO, LISTO_A, LIMITE_RECOGIDA, LIMITE, "
                "EN_COLA_DESDE, ASIGNADO_EN, RECOGIDO, ENTREGADO, CERRADO, DOMICILIARIO_ID, ORIGEN_ASIGNACION, ASIGNACION_MANUAL, DEVOLUCION, MOTIVO_CIERRE, CREADO_POR")
        recibido = valor if (estado == 'ENTREGADO' and forma != 'PREPAGADO') else None
        motivo = random.choice(MOTIVOS_CANCELA) if estado == 'CANCELADO' else ('El cliente no responde' if estado == 'FALLIDO' else None)
        vals = [s(estado), recogida[1], recogida[2], s(recogida[0]), entrega[1], entrega[2], s(entrega[0]),
                f"(SELECT ID FROM CLIENTE WHERE TELEFONO = {s(cli[1])})", s(cli[0]), s(cli[1]), s(nota),
                s(tamano), s(prioridad), s(forma), valor, recibido if recibido is not None else 'NULL',
                pago if estado == 'ENTREGADO' else 'NULL', s(pin), t(dia, m0), t(dia, m0), t(dia, lim_reco), t(dia, limite),
                t(dia, m0), t(dia, asig) if con_domi else 'NULL', t(dia, reco) if estado != 'CANCELADO' else 'NULL',
                t(dia, entr) if estado == 'ENTREGADO' else 'NULL', t(dia, cerrado),
                DOM(domi) if con_domi else 'NULL', s(origen) if con_domi else 'NULL', 1 if (manual and con_domi) else 0, 0, s(motivo), "'central'"]
        w(f"\n-- Servicio {n}: {estado.lower()}")
        w(f"INSERT INTO SERVICIO ({cols}) VALUES ({', '.join(map(str, vals))});")

        # Paradas
        if estado == 'ENTREGADO':
            pr, pe = ('COMPLETADA', reco), ('COMPLETADA', entr)
        elif estado == 'FALLIDO':
            pr, pe = ('COMPLETADA', reco), ('FALLIDA', None)
        else:
            pr, pe = ('PENDIENTE', None), ('PENDIENTE', None)
        for tipo, (est, fin), sec in (('RECOGER', pr, 1), ('ENTREGAR', pe, 2)):
            llegada = (fin - 2) if fin else (reco + 10 if est == 'FALLIDA' else None)
            w(f"INSERT INTO PARADA (SERVICIO_ID, TIPO, ESTADO, DOMICILIARIO_ID, SECUENCIA, HORA_ESTIMADA, HORA_LLEGADA, HORA_FIN) VALUES "
              f"({SID}, '{tipo}', '{est}', {DOM(domi) if con_domi else 'NULL'}, {sec if con_domi else 'NULL'}, "
              f"{t(dia, (reco if tipo == 'RECOGER' else entr) - 1)}, {t(dia, llegada) if llegada else 'NULL'}, {t(dia, fin) if fin else 'NULL'});")

        # Historial de estados
        hist = [(None, 'CREADO', m0, 'CENTRAL', None), ('CREADO', 'EN_COLA', m0, 'SISTEMA', None)]
        if con_domi:
            if manual:
                hist.append(('EN_COLA', 'ASIGNADO', asig, 'CENTRAL', 'Asignación manual: está cerca'))
            else:
                hist += [('EN_COLA', 'OFERTADO', asig - 0.4, 'MOTOR', None), ('OFERTADO', 'ASIGNADO', asig, 'DOMICILIARIO', 'Aceptó la oferta')]
        if estado == 'ENTREGADO':
            hist += [('ASIGNADO', 'EN_EJECUCION', reco, 'DOMICILIARIO', None), ('EN_EJECUCION', 'ENTREGADO', entr, 'DOMICILIARIO', 'PIN correcto')]
        elif estado == 'FALLIDO':
            hist += [('ASIGNADO', 'EN_EJECUCION', reco, 'DOMICILIARIO', None), ('EN_EJECUCION', 'FALLIDO', cerrado, 'CENTRAL', motivo)]
        else:
            hist.append(('ASIGNADO' if con_domi else 'EN_COLA', 'CANCELADO', cerrado, 'CENTRAL', motivo))
        for ant, nue, mm, ori, mot in hist:
            usuario = domi if ori == 'DOMICILIARIO' else ('central' if ori == 'CENTRAL' else 'sistema')
            w(f"INSERT INTO HISTORIAL_ESTADO_SERVICIO (SERVICIO_ID, ESTADO_ANTERIOR, ESTADO_NUEVO, FECHA, USUARIO, ORIGEN, MOTIVO) VALUES "
              f"({SID}, {s(ant)}, '{nue}', {t(dia, mm)}, '{usuario}', '{ori}', {s(mot)});")

        # Ofertas y decisión del motor
        if con_domi and not manual:
            if random.random() < 0.25:
                otro = random.choice([d for d in DOMIS if d != domi])
                w(f"INSERT INTO OFERTA (SERVICIO_ID, DOMICILIARIO_ID, MODO, ESTADO, ENVIADA, LIMITE, RESPONDIDA, COSTO, MOTIVO_RECHAZO) VALUES "
                  f"({SID}, {DOM(otro)}, '{origen}', 'RECHAZADA', {t(dia, asig - 1.2)}, {t(dia, asig - 0.7)}, {t(dia, asig - 0.9)}, "
                  f"{random.uniform(4, 12):.1f}, 'Estoy lejos');")
            w(f"INSERT INTO OFERTA (SERVICIO_ID, DOMICILIARIO_ID, MODO, ESTADO, ENVIADA, LIMITE, RESPONDIDA, COSTO) VALUES "
              f"({SID}, {DOM(domi)}, '{origen}', 'ACEPTADA', {t(dia, asig - 0.4)}, {t(dia, asig + 0.1)}, {t(dia, asig)}, {random.uniform(2, 9):.1f});")
            w(f"INSERT INTO DECISION_MOTOR (SERVICIO_ID, FECHA, MODO, ELEGIDO_ID, COSTO, CANDIDATOS) VALUES "
              f"({SID}, {t(dia, asig - 0.4)}, '{origen}', {DOM(domi)}, {random.uniform(2, 9):.1f}, "
              f"'{domi}: costo bajo; otros candidatos más lejos');")
        if manual and con_domi:
            w(f"INSERT INTO AUDITORIA (FECHA, USUARIO, ACCION, ENTIDAD, ENTIDAD_ID, DETALLE, INTERVENCION_MANUAL) VALUES "
              f"({t(dia, asig)}, 'central', 'ASIGNAR_MANUAL', 'SERVICIO', {SID}, 'A {domi}: está cerca', 1);")
        if estado == 'CANCELADO':
            w(f"INSERT INTO AUDITORIA (FECHA, USUARIO, ACCION, ENTIDAD, ENTIDAD_ID, DETALLE, INTERVENCION_MANUAL) VALUES "
              f"({t(dia, cerrado)}, 'central', 'CANCELAR', 'SERVICIO', {SID}, {s(motivo)}, 1);")

        # Prueba de entrega y GPS
        if estado == 'ENTREGADO':
            w(f"INSERT INTO EVIDENCIA_ENTREGA (SERVICIO_ID, DOMICILIARIO_ID, TIPO, DATO, FECHA) VALUES ({SID}, {DOM(domi)}, 'PIN', '{pin}', {t(dia, entr)});")
        if estado != 'CANCELADO':
            for p, mm in ((recogida, reco - 1), (entrega, entr - 1)):
                w(f"INSERT INTO UBICACION_GPS (DOMICILIARIO_ID, LAT, LNG, PRECISION_M, ESTADO_OPERATIVO, FECHA_CAPTURA, FECHA_RECEPCION) VALUES "
                  f"({DOM(domi)}, {p[1]}, {p[2]}, {random.randint(5, 25)}, 'EN_RUTA', {t(dia, mm)}, {t(dia, mm + 0.05)});")

        # Novedad del servicio fallido
        if estado == 'FALLIDO':
            w(f"INSERT INTO INCIDENTE (TIPO, SERVICIO_ID, DOMICILIARIO_ID, REPORTADO_POR, DESCRIPCION, ESTADO, DECISION, RESOLUCION, ABIERTO, CERRADO, CERRADO_POR) VALUES "
              f"('CLIENTE_NO_RESPONDE', {SID}, {DOM(domi)}, '{domi}', 'Llamé 3 veces y nadie abre', 'CERRADO', 'MARCAR_FALLIDO', "
              f"'Se intentó contactar al cliente sin éxito', {t(dia, reco + 10)}, {t(dia, cerrado)}, 'central');")
            w(f"INSERT INTO SEGUIMIENTO_INCIDENTE (INCIDENTE_ID, USUARIO, COMENTARIO, FECHA) VALUES "
              f"((SELECT MAX(ID) FROM INCIDENTE), 'central', 'Llamé al cliente desde la central, tampoco contesta', {t(dia, reco + 11)});")

        if estado == 'ENTREGADO':
            r = resumen.setdefault((dia, domi), [0, 0, 0, 0.0])
            r[0] += 1; r[1] += recibido or 0; r[2] += pago; r[3] += distancia

w("\n-- ---------------- Turnos cerrados de cada domiciliario ----------------")
for (dia, domi), (comp, rec, gan, k) in sorted(resumen.items()):
    w(f"INSERT INTO TURNO (DOMICILIARIO_ID, INICIO, FIN, COMPLETADOS, RECAUDO, RECAUDO_ENTREGADO, GANANCIAS, KM_RECORRIDOS, ESTADO) VALUES "
      f"({DOM(domi)}, {t(dia, 480)}, {t(dia, 1230)}, {comp}, {rec}, {rec}, {gan}, {k + comp * 1.5:.1f}, 'CERRADO');")

w("\n-- ---------------- Mensajes entre la central y los domiciliarios ----------------")
for dia, domi, de_central, texto in [(1, 'juan', 1, 'Juan, el cliente de Novalito pide timbrar dos veces'), (1, 'juan', 0, 'Recibido'),
                                     (2, 'maria', 0, 'Hay mucho tráfico en la 44, voy un poco tarde'), (2, 'maria', 1, 'Tranquila, ya le aviso al cliente')]:
    w(f"INSERT INTO MENSAJE (DOMICILIARIO_ID, DE_CENTRAL, AUTOR, TEXTO, FECHA, LEIDO) VALUES "
      f"({DOM(domi)}, {de_central}, '{'Central' if de_central else domi}', {s(texto)}, {t(dia, 720)}, 1);")

w("\n-- ---------------- 3 servicios esperando domiciliario (EN_COLA, creados ahora) ----------------")
w("-- Al arrancar el programa y poner disponible a un domiciliario, el motor los asigna en vivo.")
for i, (rec, ent, nota) in enumerate([(PUNTOS[0], PUNTOS[3], 'Almuerzo ejecutivo x2'), (PUNTOS[1], PUNTOS[6], 'Medicamentos'),
                                      (PUNTOS[5], PUNTOS[9], 'Pan y gaseosa')]):
    w(f"INSERT INTO SERVICIO (ESTADO, RECOGIDA_LAT, RECOGIDA_LNG, DIR_RECOGIDA, ENTREGA_LAT, ENTREGA_LNG, DIR_ENTREGA, NOTA, TAMANO, PRIORIDAD, "
      f"FORMA_PAGO, VALOR, PIN, CREADO, LISTO_A, LIMITE_RECOGIDA, LIMITE, EN_COLA_DESDE, ASIGNACION_MANUAL, DEVOLUCION, CREADO_POR) VALUES "
      f"('EN_COLA', {rec[1]}, {rec[2]}, {s(rec[0])}, {ent[1]}, {ent[2]}, {s(ent[0])}, {s(nota)}, 'PEQUENO', 'NORMAL', 'EFECTIVO', {6000 + i * 500}, "
      f"'{1234 + i * 1111}', SYSDATE, SYSDATE, SYSDATE + 25/1440, SYSDATE + 45/1440, SYSDATE, 0, 0, 'central');")
    w(f"INSERT INTO HISTORIAL_ESTADO_SERVICIO (SERVICIO_ID, ESTADO_ANTERIOR, ESTADO_NUEVO, FECHA, USUARIO, ORIGEN) VALUES ({SID}, NULL, 'CREADO', SYSDATE, 'central', 'CENTRAL');")
    w(f"INSERT INTO HISTORIAL_ESTADO_SERVICIO (SERVICIO_ID, ESTADO_ANTERIOR, ESTADO_NUEVO, FECHA, USUARIO, ORIGEN) VALUES ({SID}, 'CREADO', 'EN_COLA', SYSDATE, 'sistema', 'SISTEMA');")

w("\nCOMMIT;")
w("SET FEEDBACK ON")
w(f"PROMPT Datos cargados: {n + 3} servicios, {len(CLIENTES)} clientes y {len(resumen)} turnos.")
open(sys.argv[1], 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print(n + 3, 'servicios,', sum(1 for l in out if l.startswith('INSERT')), 'INSERT')
