// Tarjeta por domiciliario (estado, vehículo, carga, turno y recaudo, mensajes) y novedades abiertas con su decisión.
import { api } from '../api.js';
import { $, start, action, notice, esc, askText, sidePanel } from '../common.js';
import { formatTime, pesos, duration, nowMin } from '../util.js';
import { COURIER_STATUSES, COURIER_DETAIL, DECISIONS, chip, courierColor, initials } from '../labels.js';
import { openDetail } from '../detail.js';

let status = null, incidentsVersion = -1;

const refresh = start(e => {
  status = e;
  renderCouriers();
  if (e.openIncidents !== incidentsVersion) { incidentsVersion = e.openIncidents; loadIncidents(); }
});

function renderCouriers() {
  const now = nowMin();
  const order = [...status.couriers].sort((a, b) => (b.connected - a.connected) || a.name.localeCompare(b.name));
  $('couriers').innerHTML = order.map(d => `
    <div class="panel courier-card ${d.connected ? '' : 'off'}">
      <div class="d-flex gap-2 align-items-center mb-2">
        <span class="avatar" style="background:${courierColor(d.id)}">${initials(d.name)}</span>
        <div class="flex-grow-1"><b>${esc(d.name)}</b><div class="small text-soft">@${esc(d.username)} ${d.phone ? '· ' + esc(d.phone) : ''}</div></div>
        ${chip(COURIER_STATUSES[d.courierStatus])}
      </div>
      <div class="small">${COURIER_DETAIL[d.status] || d.status}${d.noSignal ? ' · <b class="text-red-c"><i class="bi bi-wifi-off"></i> sin señal</b>' : ''}
        ${d.connectedSince ? ` · trabajando hace ${duration(now - d.connectedSince)}` : ''}</div>
      <div class="small text-soft">${esc(d.vehicle || 'Sin vehículo registrado')} · carga ${d.currentLoad}/${d.capacity}</div>
      ${!d.eligible && d.ineligibleReason ? `<div class="small text-red-c">No recibe ofertas: ${esc(d.ineligibleReason)}</div>` : ''}
      ${d.consecutiveRejections ? `<div class="small text-amber-c">${d.consecutiveRejections} ofertas rechazadas seguidas</div>` : ''}
      ${d.route?.length ? `<ol class="route-mini">${d.route.map(p => `<li><a href="#" data-detail="${p.deliveryId}">#${p.deliveryId}</a> ${p.type === 'PICKUP' ? 'pickUp' : 'entregar'} ~${formatTime(p.arrives)}</li>`).join('')}</ol>` : ''}
      ${d.shift ? `<div class="shift-mini"><span>${d.shift.completed} entregas</span><span>cobrado ${pesos(d.shift.collected)}</span>
        <span class="${d.shift.pending ? 'text-red-c' : ''}">por entregar ${pesos(d.shift.pending)}</span></div>` : ''}
      <div class="d-flex gap-2 mt-2">
        <button class="btn btn-sm btn-outline-secondary" data-chat="${d.id}"><i class="bi bi-chat-dots"></i> Mensajes${d.unreadMessages ? ` <span class="badge-item">${d.unreadMessages}</span>` : ''}</button>
        ${d.shift?.pending ? `<button class="btn btn-sm btn-outline-secondary" data-collected="${d.id}" data-value="${d.shift.pending}"><i class="bi bi-cash"></i> Recibir dinero</button>` : ''}
      </div>
    </div>`).join('') || '<p class="text-soft">Aún no hay domiciliarios aprobados.</p>';
}

$('couriers').onclick = async ev => {
  const t = ev.target.closest('[data-chat],[data-collected],[data-detail]');
  if (!t) return;
  ev.preventDefault();
  if (t.dataset.detail) return openDetail(+t.dataset.detail);
  if (t.dataset.collected) {
    const v = await askText({ title: 'Dinero recibido del domiciliario', label: 'Valor en pesos', value: t.dataset.value, button: 'Registrar' });
    if (v && await action(api.collected(+t.dataset.collected, Number(v.replace(/\D/g, ''))), 'Recaudo registrado')) refresh();
  }
  if (t.dataset.chat) openChat(+t.dataset.chat);
};

async function openChat(courierId) {
  const d = status.couriers.find(x => x.id === courierId);
  const panel = sidePanel('Mensajes con ' + d.name);
  const templates = (await api.templates().catch(() => [])).filter(p => p.forRole === 'CENTRAL');
  const render = async () => {
    const ms = await api.messages(courierId);
    panel.body.querySelector('.chat').innerHTML = ms.slice().reverse().map(m =>
      `<div class="bubble ${m.fromCentral ? 'mine' : ''}"><span>${esc(m.text)}</span><time>${formatTime(m.date)}</time></div>`).join('') || '<p class="text-soft">Sin mensajes.</p>';
  };
  panel.body.innerHTML = `<div class="chat"></div>
    <div class="templates">${templates.map(p => `<button type="button" class="btn btn-sm btn-outline-secondary">${esc(p.text)}</button>`).join('')}</div>
    <form class="d-flex gap-2 mt-2"><input class="form-control" maxlength="300" placeholder="Escribe un mensaje"><button class="btn btn-primary">Enviar</button></form>`;
  const form = panel.body.querySelector('form'), input = form.querySelector('input');
  panel.body.querySelectorAll('.templates button').forEach(b => b.onclick = () => { input.value = b.textContent; input.focus(); });
  form.onsubmit = async ev => { ev.preventDefault(); if (input.value.trim() && await action(api.write(courierId, input.value.trim()))) { input.value = ''; render(); } };
  render();
}

async function loadIncidents() {
  const list = await api.incidents({ open: true }).catch(e => { notice(e.message); return []; });
  $('incidents').innerHTML = list.length ? list.map(n => `
    <div class="incident">
      <div><b>${esc(n.typeText)}</b> ${n.deliveryId ? `· <a href="#" data-detail="${n.deliveryId}">pedido #${n.deliveryId}</a>` : ''} · ${esc(n.courier || n.reportedBy)} · ${formatTime(n.opened)}</div>
      <div>${esc(n.description)}</div>
      ${n.followUp.map(c => `<div class="small text-soft">↳ ${esc(c.username)}: ${esc(c.comment)}</div>`).join('')}
      <div class="d-flex gap-2 mt-1 flex-wrap">
        <select class="form-select form-select-sm w-auto" data-decide="${n.id}"><option value="">Decidir…</option>
          ${Object.entries(DECISIONS).filter(([k]) => n.deliveryId || k === 'NO_ACTION').map(([k, v]) => `<option value="${k}">${v}</option>`).join('')}</select>
        <button class="btn btn-sm btn-outline-secondary" data-comment="${n.id}">Comentar</button>
        ${n.hasPhoto ? `<button class="btn btn-sm btn-outline-secondary" data-photo="${n.id}"><i class="bi bi-image"></i> Foto</button>` : ''}
      </div></div>`).join('') : '<p class="text-soft m-0">No hay novedades abiertas.</p>';
}

$('incidents').onclick = async ev => {
  const t = ev.target.closest('[data-detail],[data-comment],[data-photo]');
  if (!t) return;
  ev.preventDefault();
  if (t.dataset.detail) return openDetail(+t.dataset.detail);
  if (t.dataset.comment) { const c = await askText({ title: 'Comentario de seguimiento', label: 'Comentario', button: 'Guardar' }); if (c && await action(api.comment(+t.dataset.comment, c))) loadIncidents(); }
  if (t.dataset.photo) { const f = await api.incidentPhoto(+t.dataset.photo); sidePanel('Foto de la novedad').body.innerHTML = `<img src="${esc(f.photo)}" class="img-fluid" alt="Foto">`; }
};
$('incidents').onchange = async ev => {
  const s = ev.target.closest('[data-decide]');
  if (!s?.value) return;
  const r = await askText({ title: DECISIONS[s.value], label: 'Resolución', button: 'Confirmar', required: false });
  if (r !== null && await action(api.decide(+s.dataset.decide, s.value, r), 'Novedad cerrada')) { incidentsVersion = -1; refresh(); }
  else s.value = '';
};
