// Pantalla principal de la central: modo del motor, mapa, crear servicios y la cola con sus acciones.
import { api } from '../api.js';
import { $, start, action, notice, esc, askText } from '../common.js';
import { formatTime, pesos, distanceKm, distanceText, duration, nowMin } from '../util.js';
import { createMap, searchAddress, addressOf } from '../map.js';
import { DELIVERY_STATUSES, PRIORITIES, MODES, OPEN_STATUSES, chip, courierPoint, COURIER_DETAIL } from '../labels.js';
import { openDetail } from '../detail.js';

let status = null, map = null, filter = 'open';
const newStatus = { R: null, E: null };
let active = 'R', customerId = null;

const refresh = start(e => {
  status = e;
  if (!map) { map = createMap($('map'), e.config); map.onClick(onClick); }
  map.renderOperation(e);
  renderMode();
  renderTable();
});

// ---------------- Modo ----------------
function renderMode() {
  document.querySelectorAll('#modes button').forEach(b => b.classList.toggle('active', b.dataset.mode === status.mode));
  $('modeText').innerHTML = (MODES[status.mode]?.[1] || '') + (status.engineFailure ? `<br><b class="text-red-c">${esc(status.engineFailure)}</b>` : '');
  const queued = status.deliveries.filter(s => ['QUEUED', 'OFFERED'].includes(s.status)).length;
  const working = status.couriers.filter(d => d.connected);
  $('figures').innerHTML = `<div class="${queued ? 'figure-alert' : ''}"><b>${queued}</b><span>por asignar</span></div>
    <div><b>${working.length}</b><span>trabajando</span></div>
    <div><b>${working.filter(d => d.courierStatus === 'AVAILABLE').length}</b><span>disponibles</span></div>`;
}
document.querySelectorAll('#modes button').forEach(b => b.onclick = async () => {
  if (await action(api.mode(b.dataset.mode), 'Modo ' + MODES[b.dataset.mode][0])) refresh();
});

// ---------------- Formulario ----------------
async function onClick(p) {
  setPoint(active, p);
  const dir = await addressOf(p, status.config);
  if (dir) $(active === 'R' ? 'inPickup' : 'inDropoff').value = dir;
  if (active === 'R' && !newStatus.E) active = 'E';
}

function setPoint(type, p) {
  newStatus[type] = p;
  map.set(type, p, q => { newStatus[type] = q; estimate(); });
  if (p && !$(type === 'R' ? 'inPickup' : 'inDropoff').value) $(type === 'R' ? 'inPickup' : 'inDropoff').value = `${p.lat.toFixed(5)}, ${p.lng.toFixed(5)}`;
  estimate();
}

async function estimate() {
  if (!newStatus.R || !newStatus.E) { $('estimate').textContent = ''; return; }
  const km = distanceKm(newStatus.R, newStatus.E) * (status.config.detourFactor || 1.3);
  $('estimate').textContent = `≈ ${distanceText(km)} por calles · ${duration(km / status.config.kmPerMin)} de trayecto`;
  try {
    const v = await api.suggestedValue(newStatus.R, newStatus.E, $('size').value);
    $('value').placeholder = v.value;
    $('estimate').textContent += ` · tarifa sugerida ${pesos(v.value)}`;
  } catch { /* sin tarifa */ }
}
$('size').onchange = estimate;

async function search(type) {
  const input = $(type === 'R' ? 'inPickup' : 'inDropoff'), box = $('sug' + type);
  const text = input.value.trim();
  if (text.length < 3) return;
  const coords = /^\s*(-?\d+[.,]\d+)\s*[,;\s]\s*(-?\d+[.,]\d+)\s*$/.exec(text);
  if (coords) { setPoint(type, { lat: +coords[1].replace(',', '.'), lng: +coords[2].replace(',', '.') }); map.center(newStatus[type]); return; }
  box.innerHTML = '<div class="text-soft small">Buscando…</div>';
  try {
    const list = await searchAddress(text, status.config);
    box.innerHTML = list.length ? list.map((x, i) => `<button type="button" data-i="${i}">${esc(x.name)}</button>`).join('')
      : '<div class="text-soft small">No la encontré. Marca el punto en el mapa.</div>';
    box.querySelectorAll('button').forEach(b => b.onclick = () => {
      const x = list[+b.dataset.i];
      input.value = x.name; box.innerHTML = '';
      setPoint(type, x.point); map.center(x.point);
    });
  } catch (e) { box.innerHTML = `<div class="text-red-c small">${esc(e.message)}</div>`; }
}
document.querySelectorAll('[data-search]').forEach(b => b.onclick = () => search(b.dataset.search));
['R', 'E'].forEach(t => {
  const input = $(t === 'R' ? 'inPickup' : 'inDropoff');
  input.onfocus = () => { active = t; };
  input.onkeydown = e => { if (e.key === 'Enter') { e.preventDefault(); search(t); } };
});

// Cliente frecuente: al escribir el teléfono se trae su nombre y su última dirección
$('phone').onchange = async () => {
  const tel = $('phone').value.trim();
  customerId = null; $('customerInfo').textContent = '';
  if (tel.length < 7) return;
  try {
    const c = await api.customerByPhone(tel);
    if (!c?.customer) return;
    customerId = c.customer.id;
    $('customer').value = c.customer.name;
    $('customerInfo').innerHTML = `Cliente frecuente · ${c.deliveries} servicios` + (c.addresses.length
      ? ' · direcciones: ' + c.addresses.map((d, i) => `<a href="#" data-dir="${i}">${esc(d.text)}</a>`).join(', ') : '');
    $('customerInfo').querySelectorAll('[data-dir]').forEach(a => a.onclick = ev => {
      ev.preventDefault();
      const d = c.addresses[+a.dataset.dir];
      $('inDropoff').value = d.text; setPoint('E', { lat: d.lat, lng: d.lng });
    });
  } catch { /* nuevo */ }
};

function clear() {
  $('deliveryForm').reset(); customerId = null; $('customerInfo').textContent = ''; $('value').placeholder = 'auto';
  setPoint('R', null); setPoint('E', null); active = 'R';
}
$('btnClear').onclick = clear;

$('deliveryForm').onsubmit = async ev => {
  ev.preventDefault();
  if (!newStatus.R || !newStatus.E) { notice('Marca la recogida y la entrega (búscalas o haz clic en el mapa)'); return; }
  const num = id => $(id).value === '' ? null : Number($(id).value);
  const prog = $('scheduled').value ? new Date($('scheduled').value).getTime() / 60000 : null;
  const r = await action(api.createDelivery({
    pickup: newStatus.R, pickupAddress: $('inPickup').value.trim(), dropoff: newStatus.E, dropoffAddress: $('inDropoff').value.trim(),
    customerId, customer: $('customer').value.trim(), phone: $('phone').value.trim(), note: $('note').value.trim(),
    size: $('size').value, priority: $('priority').value, paymentMethod: $('paymentMethod').value, value: num('value'),
    readyMin: num('readyMin'), deadlineMin: num('deadlineMin'), scheduledFor: prog, saveCustomer: $('saveCustomer').checked,
  }));
  if (r) { notice(`Servicio #${r.id} creado · PIN ${r.pin} · ${pesos(r.value)}`, 'ok', 8000); clear(); refresh(); }
};

// ---------------- Tabla ----------------
document.querySelectorAll('#filters button').forEach(b => b.onclick = () => {
  filter = b.dataset.filter;
  document.querySelectorAll('#filters button').forEach(x => x.classList.toggle('active', x === b));
  renderTable();
});

function renderTable() {
  if (document.activeElement?.tagName === 'SELECT' && document.activeElement.closest('#deliveriesTable')) return;
  const couriers = new Map(status.couriers.map(d => [d.id, d]));
  const open = status.deliveries.filter(s => OPEN_STATUSES.includes(s.status));
  $('nOpen').textContent = open.length || '';
  const list = filter === 'open' ? open : filter === 'closed' ? status.deliveries.filter(s => !OPEN_STATUSES.includes(s.status)) : status.deliveries;
  if (!list.length) { $('deliveriesTable').innerHTML = '<tbody><tr><td class="text-soft p-4 text-center">No hay servicios aquí.</td></tr></tbody>'; return; }
  const now = nowMin();
  const available = status.couriers.filter(d => d.connected);
  $('deliveriesTable').innerHTML = `<thead><tr><th>#</th><th>Estado</th><th>Recoger → Entregar</th><th>Cliente</th><th>Domiciliario</th><th>Límite</th><th>Valor</th><th></th></tr></thead><tbody>` +
    list.map(s => {
      const d = couriers.get(s.courierId);
      const remaining = s.deadline - (s.delivered ?? s.etaDropoff ?? now);
      const trafficLight = !OPEN_STATUSES.includes(s.status) ? '' : remaining < 0 ? 'red' : remaining < 10 ? 'amber' : 'green';
      const canAssign = ['QUEUED', 'OFFERED', 'ASSIGNED', 'CREATED'].includes(s.status);
      const offers = s.offers?.length ? `<div class="small text-soft">ofertado a ${s.offers.map(o => esc(couriers.get(o.courierId)?.name || '#' + o.courierId) + ' (' + o.secondsLeft + ' s)').join(', ')}</div>` : '';
      return `<tr>
        <td><a href="#" data-detail="${s.id}">#${s.id}</a></td>
        <td>${chip(DELIVERY_STATUSES[s.status])} ${s.priority !== 'NORMAL' ? chip(PRIORITIES[s.priority]) : ''}${s.returnTrip ? ' <span class="st st-red">Devolución</span>' : ''}${offers}</td>
        <td class="small">${esc(s.pickupAddress || 'punto en mapa')}<br><span class="text-soft">→ ${esc(s.dropoffAddress || 'punto en mapa')}</span></td>
        <td class="small">${esc(s.customer || '')}<br><span class="text-soft">${esc(s.phone || '')}</span></td>
        <td class="small">${d ? courierPoint(d.id) + esc(d.name) : '—'}${d && s.etaDropoff ? `<br><span class="text-soft">llega ~${formatTime(s.etaDropoff)}</span>` : ''}</td>
        <td><span class="traffic-light traffic-light-${trafficLight}"></span>${formatTime(s.deadline)}</td>
        <td class="small">${pesos(s.value)}</td>
        <td class="text-end actions">${canAssign ? `<select class="form-select form-select-sm" data-assign="${s.id}"><option value="">Asignar a…</option>
            ${available.filter(x => x.id !== s.courierId).map(x => `<option value="${x.id}" ${x.eligible ? '' : 'class="text-soft"'}>${esc(x.name)} · ${COURIER_DETAIL[x.status] || x.status}</option>`).join('')}</select>` : ''}
          ${s.courierId && ['ASSIGNED', 'OFFERED'].includes(s.status) ? `<button class="btn btn-sm btn-outline-secondary" data-revoke="${s.id}" title="Quitar al domiciliario"><i class="bi bi-arrow-counterclockwise"></i></button>` : ''}
          ${OPEN_STATUSES.includes(s.status) ? `<button class="btn btn-sm btn-outline-danger" data-cancel="${s.id}" title="Cancelar"><i class="bi bi-x-lg"></i></button>` : ''}</td></tr>`;
    }).join('') + '</tbody>';
}

$('deliveriesTable').onclick = async ev => {
  const t = ev.target.closest('[data-detail],[data-revoke],[data-cancel]');
  if (!t) return;
  ev.preventDefault();
  if (t.dataset.detail) return openDetail(+t.dataset.detail);
  if (t.dataset.revoke) {
    const m = await askText({ title: `Quitar el servicio #${t.dataset.revoke} al domiciliario`, button: 'Quitar' });
    if (m && await action(api.revoke(+t.dataset.revoke, m), 'Servicio devuelto a la cola')) refresh();
  }
  if (t.dataset.cancel) {
    const m = await askText({ title: `Cancelar el servicio #${t.dataset.cancel}`, button: 'Cancelar servicio', danger: true });
    if (m && await action(api.cancel(+t.dataset.cancel, m), 'Servicio cancelado')) refresh();
  }
};
$('deliveriesTable').onchange = async ev => {
  const sel = ev.target.closest('[data-assign]');
  if (!sel || !sel.value) return;
  const id = +sel.dataset.assign, courier = +sel.value;
  sel.blur();
  const m = await askText({ title: `Asignar #${id} a ${sel.selectedOptions[0].text.split(' · ')[0]}`, label: 'Motivo (opcional)', button: 'Asignar', required: false });
  if (m !== null && await action(api.assign(id, courier, m), 'Asignado')) refresh();
  else sel.value = '';
};
