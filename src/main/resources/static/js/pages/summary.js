// Resumen: alertas en vivo, indicadores I1–I7 frente a la línea base, flujo, desempeño, y CSV.
import { api } from '../api.js';

// Nombre en pantalla de cada cifra del resumen (las llaves vienen del servidor en inglés)
const FIGURE_LABELS = {
  created: 'creados', delivered: 'entregados', cancelled: 'cancelados', failed: 'fallidos', open: 'abiertos',
  pickupWaitMin: 'espera recogida (min)', totalValue: 'valor total', collected: 'recaudo', courierPayments: 'pago domiciliarios',
  kmTravelled: 'km recorridos', interventions: 'intervenciones', operatingHours: 'horas de operación', incidents: 'novedades',
};
import { $, start, action, esc } from '../common.js';
import { todayIso, pesos } from '../util.js';

start(e => {
  const a = e.alerts || [];
  $('alerts').innerHTML = a.length ? a.map(x => `<li class="alert-item alert-item-${x.level}"><i class="bi ${x.level >= 3 ? 'bi-exclamation-octagon-fill' : x.level === 2 ? 'bi-exclamation-triangle-fill' : 'bi-info-circle'}"></i>
      ${x.link ? `<a href="${esc(x.link)}">${esc(x.text)}</a>` : esc(x.text)}</li>`).join('') : '<li class="text-soft">Todo en orden.</li>';
});

$('from').value = $('to').value = todayIso();
const fmt = (v, u) => v == null ? '—' : (Math.round(v * 10) / 10).toLocaleString('es-CO') + (u ? ' ' + u : '');

async function load() {
  const range = { from: $('from').value, to: $('to').value };
  $('csvDeliveries').href = api.csvUrl('deliveries', range);
  $('csvIndicators').href = api.csvUrl('indicators', range);
  const r = await action(api.indicators(range));
  if (!r) return;
  $('figures').innerHTML = Object.entries(r.figures).map(([k, v]) => `<div><b>${typeof v === 'number' && k === 'collected' ? pesos(v) : fmt(v)}</b><span>${esc(FIGURE_LABELS[k] || k)}</span></div>`).join('');
  $('indicatorsTable').innerHTML = `<thead><tr><th>Código</th><th>Indicador</th><th>Valor</th><th>Línea base</th><th>Variación</th><th>Muestras</th></tr></thead><tbody>` +
    r.indicators.map(i => `<tr title="${esc(i.formula)}"><td><b>${i.code}</b></td><td>${esc(i.name)}</td><td>${fmt(i.value, i.unit)}</td><td>${fmt(i.baseline, i.unit)}</td>
      <td class="${i.improves == null ? '' : i.improves ? 'text-green-c' : 'text-red-c'}">${i.variationPct == null ? '—' : (i.variationPct > 0 ? '+' : '') + fmt(i.variationPct, '%')}</td><td>${i.samples}</td></tr>`).join('') + '</tbody>';
  const max = Math.max(1, ...r.flow.map(p => Math.max(p.created, p.delivered)));
  $('flow').innerHTML = r.flow.map(p => `<div class="flow-col" title="${esc(p.label)}: ${p.created} creados, ${p.delivered} entregados">
      <div class="bars"><i class="b-created" style="height:${p.created / max * 100}%"></i><i class="b-delivered" style="height:${p.delivered / max * 100}%"></i></div><span>${esc(p.label)}</span></div>`).join('')
    + '<div class="flow-legend"><i class="b-created"></i> creados <i class="b-delivered"></i> entregados</div>';
  $('performanceTable').innerHTML = `<thead><tr><th>Domiciliario</th><th>Entregados</th><th>A tiempo</th><th>Min. promedio</th><th>Aceptación</th><th>Km</th><th>Ganancias</th><th>Recaudo pendiente</th></tr></thead><tbody>` +
    (r.couriers.map(d => `<tr><td>${esc(d.name)}</td><td>${d.delivered}</td><td>${fmt(d.onTimePct, '%')}</td><td>${fmt(d.averageMinutes)}</td>
      <td>${fmt(d.acceptancePct, '%')}</td><td>${fmt(d.km)}</td><td>${pesos(d.earnings)}</td><td>${pesos(d.collectedPending)}</td></tr>`).join('') || '<tr><td colspan="8" class="text-soft">Sin datos en el rango.</td></tr>') + '</tbody>';
}
$('rangeForm').onsubmit = e => { e.preventDefault(); load(); };
load();

