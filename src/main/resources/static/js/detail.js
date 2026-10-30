// Panel con todo lo que pasó con un servicio: datos, línea de tiempo, ofertas, pruebas de entrega y decisiones del motor.
import { api } from './api.js';
import { esc, sidePanel, notice } from './common.js';
import { dateTime, formatTime, pesos } from './util.js';
import { DELIVERY_STATUSES, PRIORITIES, PACKAGE_SIZES, PAYMENT_METHODS, chip } from './labels.js';

export async function openDetail(id) {
  const panel = sidePanel('Servicio #' + id);
  panel.body.innerHTML = '<p class="text-soft">Cargando…</p>';
  let d;
  try { d = await api.detail(id); } catch (e) { panel.body.innerHTML = `<p class="text-red-c">${esc(e.message)}</p>`; return; }
  const s = d.delivery;
  const row = (k, v) => v == null || v === '' ? '' : `<dt>${k}</dt><dd>${v}</dd>`;
  panel.body.innerHTML = `
    <div class="mb-2">${chip(DELIVERY_STATUSES[s.status])} ${chip(PRIORITIES[s.priority])}</div>
    <dl class="data">
      ${row('Recoger', esc(s.pickupAddress || `${s.pickup.lat.toFixed(5)}, ${s.pickup.lng.toFixed(5)}`))}
      ${row('Entregar', esc(s.dropoffAddress || `${s.dropoff.lat.toFixed(5)}, ${s.dropoff.lng.toFixed(5)}`))}
      ${row('Cliente', esc([s.customer, s.phone].filter(Boolean).join(' · ')))}
      ${row('Detalle', esc(s.note))}
      ${row('Tamaño', PACKAGE_SIZES[s.size])}
      ${row('Pago', PAYMENT_METHODS[s.paymentMethod] + ' · ' + pesos(s.value) + (s.amountReceived != null ? ' (recibió ' + pesos(s.amountReceived) + ')' : ''))}
      ${row('PIN de entrega', `<b class="pin">${esc(s.pin)}</b>`)}
      ${row('Domiciliario', esc(s.courier) + (s.origin ? ` <span class="text-soft">(${s.origin.toLowerCase()})</span>` : ''))}
      ${row('Creado', dateTime(s.created) + (s.createdBy ? ' por ' + esc(s.createdBy) : ''))}
      ${row('Límite de entrega', formatTime(s.deadline))}
      ${row('Entregado', s.delivered ? formatTime(s.delivered) : null)}
      ${row('Motivo de cierre', esc(s.closeReason))}
    </dl>
    <h3>Línea de tiempo</h3>
    <ol class="timeline">${d.timeline.map(e => `<li><time>${formatTime(e.date)}</time><b>${esc(e.title)}</b>
      ${e.detail ? `<span>${esc(e.detail)}</span>` : ''}${e.username ? `<span class="text-soft"> · ${esc(e.username)}</span>` : ''}</li>`).join('')}</ol>
    ${d.proofs.length ? `<h3>Prueba de entrega</h3><div class="proofs">${d.proofs.map(e =>
      e.type === 'PIN' ? `<div><i class="bi bi-key"></i> PIN correcto · ${formatTime(e.date)}</div>` :
      `<figure><img src="${esc(e.item)}" alt="${e.type}"><figcaption>${e.type === 'PHOTO' ? 'Foto' : 'Firma'} · ${formatTime(e.date)}</figcaption></figure>`).join('')}</div>` : ''}
    ${d.offers.length ? `<h3>Ofertas</h3><table class="table table-sm"><tbody>${d.offers.map(o =>
      `<tr><td>${formatTime(o.sent)}</td><td>${esc(o.courier || '#' + o.courierId)}</td><td>${o.status.toLowerCase()}</td><td class="text-soft">${esc(o.reason || '')}</td></tr>`).join('')}</tbody></table>` : ''}
    ${d.decisions.length ? `<h3>Decisiones del motor</h3>${d.decisions.map(x =>
      `<details class="small"><summary>${formatTime(x.date)} · ${x.mode.toLowerCase()} → ${esc(x.chosen || 'nobody')}${x.cost != null ? ' (costo ' + x.cost.toFixed(1) + ')' : ''}</summary><pre>${esc(x.candidates || '')}</pre></details>`).join('')}` : ''}
    ${d.incidents.length ? `<h3>Novedades</h3>${d.incidents.map(n => `<p class="small"><b>${esc(n.type)}</b>: ${esc(n.description)} <span class="text-soft">(${n.status.toLowerCase()})</span></p>`).join('')}` : ''}`;
}

export { notice };
