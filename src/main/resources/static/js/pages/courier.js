// App del domiciliario: estado, ofertas con temporizador, recoger/entregar con pruebas, GPS, novedades,
// mensajes, turno, radio y cola sin conexión (los "Recogí/Entregué" se guardan y se mandan al volver la señal).
import { apiCourier } from '../api.js';
import { $, notice, esc } from '../common.js';
import { formatTime, pesos, liveClock, navigationLink, distanceKm, distanceText } from '../util.js';
import { createMap } from '../map.js';
import { mountRadio } from '../radio.js';
import { connectRealtime } from '../stomp.js';
import { COURIER_STATUSES, INCIDENT_TYPES, PAYMENT_METHODS, chip } from '../labels.js';

let v = null, map = null, shownOffer = null, gpsId = null, lastGps = 0, pendingGps = null, gpsTimer = null, lastPosition = null;
const QUEUE = 'serviasigna-queue-' + (document.querySelector('.greeting b')?.textContent || 'me');

liveClock($('clock'));
mountRadio($('radio'));

async function load() {
  try { v = await apiCourier.me(); render(); mark(true); }
  catch (e) { if (e.code === 0) mark(false); else notice(e.message); }
}
function mark(ok) {
  $('liveChip').className = 'chip ' + (ok ? 'chip-ok' : 'chip-red');
  $('liveChip').innerHTML = ok ? '<i class="bi bi-wifi"></i>' : '<i class="bi bi-wifi-off"></i> sin señal';
}

connectRealtime({ '/user/queue/notices': a => {
  if (a.type === 'MESSAGE') notice('Mensaje de la central: ' + (a.text || ''), 'info', 7000);
  else if (a.text) notice(a.text, a.type === 'CANCELLED' || a.type === 'REVOKED' ? 'error' : 'info', 7000);
  if (a.type === 'OFFER' && navigator.vibrate) navigator.vibrate([300, 150, 300]);
  load();
} }, ok => { mark(ok); if (ok) { load(); sendQueue(); } });
load();
setInterval(load, 5000);                // respaldo y "señal de vida"

// ---------------- Pintar ----------------
function render() {
  const d = v.courier, cfg = v.config;
  $('statusChip').innerHTML = chip(COURIER_STATUSES[d.courierStatus]);
  document.querySelectorAll('#statuses button').forEach(b => b.classList.toggle('active',
    b.dataset.status === d.courierStatus || (b.dataset.status === 'AVAILABLE' && d.courierStatus === 'ON_ROUTE')));
  $('statusText').textContent = d.courierStatus === 'OFF_DUTY' ? 'No recibes servicios y no se registra tu ubicación.'
    : d.courierStatus === 'BUSY' ? 'Pausa: conservas tus pedidos pero no te llegan nuevos.' : 'Te pueden llegar servicios.';
  $('courierNotices').innerHTML = (v.notices || []).map(a => `<div class="alert alert-warning py-2 small mb-2">${esc(a)}</div>`).join('');

  if (!map) { map = createMap($('map'), cfg); }
  const onMap = { time: v.time, couriers: [d], deliveries: v.deliveries };
  map.renderOperation(onMap);
  handleGps(d.connected);

  renderHero();
  $('route').innerHTML = d.route.map(p => {
    const s = v.deliveries.find(x => x.id === p.deliveryId) || {};
    return `<li>#${p.deliveryId} <b>${p.type === 'PICKUP' ? 'Recoger' : 'Entregar'}</b> ${esc(p.type === 'PICKUP' ? s.pickupAddress || '' : s.dropoffAddress || '')} <span class="text-soft">~${formatTime(p.arrives)}</span></li>`;
  }).join('') || '<li class="text-soft">Sin paradas.</li>';
  const t = d.shift;
  $('shift').innerHTML = t ? `<span>${t.completed} entregas</span><span>ganado ${pesos(t.earnings)}</span><span>cobrado ${pesos(t.collected)}</span><span>por entregar a la central ${pesos(t.pending)}</span>`
    : '<span class="text-soft">Ponte disponible para empezar tu turno.</span>';
  $('chat').innerHTML = v.messages.slice().reverse().map(m => `<div class="bubble ${m.fromCentral ? '' : 'mine'}"><span>${esc(m.text)}</span><time>${formatTime(m.date)}</time></div>`).join('') || '<p class="text-soft small">Sin mensajes.</p>';
  if (!$('templates').dataset.ready) {
    $('templates').dataset.ready = 1;
    $('templates').innerHTML = v.templates.map(p => `<button type="button" class="btn btn-sm btn-outline-secondary">${esc(p)}</button>`).join('');
  }
  renderOffer();
}

function next() {
  const p = v.courier.route[0];
  if (!p) return null;
  return { p, s: v.deliveries.find(x => x.id === p.deliveryId) };
}

function renderHero() {
  const n = next(), d = v.courier, cfg = v.config;
  $('confirmForm').classList.add('d-none');
  $('btnNavigate').classList.add('d-none'); $('btnCall').classList.add('d-none');
  if (!n || !n.s) {
    $('heroTitle').textContent = d.connected ? 'Esperando servicios' : 'Estás fuera de servicio';
    $('heroDetail').textContent = d.connected ? 'Cuando te llegue uno lo verás aquí.' : 'Toca "Disponible" para empezar.';
    return;
  }
  const { p, s } = n;
  const pickUp = p.type === 'PICKUP' && !s.returnTrip;
  const destination = p.type === 'PICKUP' ? s.pickup : s.dropoff;
  const far = d.position ? distanceKm(d.position, destination) : null;
  $('heroTitle').textContent = s.returnTrip ? `Devolver pedido #${s.id}` : `${pickUp ? 'Recoger' : 'Entregar'} pedido #${s.id}`;
  $('heroDetail').innerHTML = `${esc(p.type === 'PICKUP' ? s.pickupAddress || 'Punto en el mapa' : s.dropoffAddress || 'Punto en el mapa')}
    ${s.note ? `<br><span class="text-soft">${esc(s.note)}</span>` : ''}${!pickUp && s.customer ? `<br>${esc(s.customer)}` : ''}
    <br><span class="text-soft">${far != null ? 'a ' + distanceText(far) + ' · ' : ''}llegada estimada ${formatTime(p.arrives)}</span>
    ${!pickUp && s.paymentMethod !== 'PREPAID' ? `<br><b>Cobrar ${pesos(s.value)}</b> (${PAYMENT_METHODS[s.paymentMethod]})` : ''}`;
  $('btnNavigate').href = navigationLink(destination); $('btnNavigate').classList.remove('d-none');
  if (s.phone) { $('btnCall').href = 'tel:' + s.phone; $('btnCall').classList.remove('d-none'); }
  const dropoff = p.type === 'DROPOFF';
  $('confirmForm').classList.remove('d-none');
  $('btnConfirm').textContent = dropoff ? 'Entregué el pedido' : s.returnTrip ? 'Devolví el pedido' : 'Recogí el pedido';
  $('pinField').classList.toggle('d-none', !(dropoff && cfg.requirePin));
  $('amountField').classList.toggle('d-none', !(dropoff && s.paymentMethod !== 'PREPAID'));
  if (dropoff && !$('inAmount').value) $('inAmount').value = s.value;
  $('photoField').classList.toggle('d-none', !(dropoff && cfg.requirePhoto));
  $('signatureField').classList.toggle('d-none', !(dropoff && cfg.requireSignature));
}

// ---------------- Oferta ----------------
function renderOffer() {
  const o = v.offer;
  if (!o) { $('offer').classList.add('d-none'); shownOffer = null; return; }
  if (shownOffer !== o.id) {
    shownOffer = o.id;
    const s = o.delivery;
    $('offerDetail').innerHTML = `<p><b>Recoger:</b> ${esc(s.pickupAddress || 'punto en el mapa')}<br><b>Entregar:</b> ${esc(s.dropoffAddress || 'punto en el mapa')}</p>
      <p class="small">Te desvías ${o.kmExtra.toFixed(1)} km · ganas <b>${pesos(o.payment)}</b>${s.paymentMethod !== 'PREPAID' ? ` · cobras ${pesos(s.value)}` : ''}${s.note ? '<br>' + esc(s.note) : ''}</p>`;
  }
  $('offer').classList.remove('d-none');
  let end = Date.now() + o.secondsLeft * 1000;
  clearInterval(renderOffer.t);
  const tick = () => {
    const sec = Math.max(0, Math.round((end - Date.now()) / 1000));
    $('offerSec').textContent = sec + ' s';
    $('offerBar').style.width = (sec / o.totalSeconds * 100) + '%';
    if (sec <= 0) { clearInterval(renderOffer.t); $('offer').classList.add('d-none'); }
  };
  tick(); renderOffer.t = setInterval(tick, 500);
}
async function reply(accept) {
  if (!v.offer) return;
  let reason = null;
  if (!accept) reason = prompt('¿Por qué la rechazas? (opcional)') || null;
  try { v = await apiCourier.reply(v.offer.id, accept, reason); notice(accept ? 'Servicio aceptado' : 'Oferta rechazada', 'ok'); render(); }
  catch (e) { notice(e.message); load(); }
}
$('btnAccept').onclick = () => reply(true);
$('btnReject').onclick = () => reply(false);

// ---------------- Estado ----------------
document.querySelectorAll('#statuses button').forEach(b => b.onclick = async () => {
  try {
    let pos = null;
    if (b.dataset.status === 'AVAILABLE' && v.courier.courierStatus === 'OFF_DUTY') pos = await currentPosition();
    v = await apiCourier.status(b.dataset.status, pos); render();
  } catch (e) { notice(e.message); }
});

// ---------------- GPS (RF-M6-10) ----------------
function currentPosition() {
  return new Promise((ok, invalid) => navigator.geolocation
    ? navigator.geolocation.getCurrentPosition(p => ok({ lat: p.coords.latitude, lng: p.coords.longitude, precision: p.coords.accuracy }),
        () => invalid(new Error('Activa el permiso de ubicación del navegador')), { enableHighAccuracy: true, timeout: 15000 })
    : invalid(new Error('Este navegador no tiene GPS')));
}
// Manda la última posición leída (nunca se pierde: si llega antes del intervalo, se envía al cumplirse).
async function sendGps() {
  gpsTimer = null;
  if (!pendingGps) return;
  const item = pendingGps; pendingGps = null; lastGps = Date.now();
  try { await apiCourier.location(item); } catch (e) { if (e.code === 0) enqueue('location', item); }
}
function handleGps(active) {
  if (active && gpsId == null && navigator.geolocation) {
    gpsId = navigator.geolocation.watchPosition(p => {
      $('gpsStatus').className = 'chip chip-ok'; $('gpsStatus').innerHTML = `<i class="bi bi-geo-alt-fill"></i> ±${Math.round(p.coords.accuracy)} m`;
      pendingGps = { lat: p.coords.latitude, lng: p.coords.longitude, precision: p.coords.accuracy, capturedMs: p.timestamp };
      lastPosition = pendingGps;
      const waitTime = (v.config.gpsIntervalSec || 10) * 1000 - (Date.now() - lastGps);
      if (waitTime <= 0) sendGps();
      else if (!gpsTimer) gpsTimer = setTimeout(sendGps, waitTime);   // la última posición sale apenas se cumpla el intervalo
    }, () => { $('gpsStatus').className = 'chip chip-red'; $('gpsStatus').textContent = 'GPS sin permiso'; }, { enableHighAccuracy: true, maximumAge: 5000 });
  } else if (!active && gpsId != null) { navigator.geolocation.clearWatch(gpsId); gpsId = null; clearTimeout(gpsTimer); gpsTimer = null; pendingGps = null; lastPosition = null; $('gpsStatus').className = 'chip chip-gray'; $('gpsStatus').textContent = 'GPS apagado'; }
}

// ---------------- Confirmar con pruebas (RF-M6-06) y cola sin conexión (RF-M6-11) ----------------
const readQueue = () => { try { return JSON.parse(localStorage.getItem(QUEUE) || '[]'); } catch { return []; } };
const saveQueue = c => { try { localStorage.setItem(QUEUE, JSON.stringify(c)); } catch { /* lleno */ } };
function enqueue(type, data) { const c = readQueue(); c.push({ type, data }); saveQueue(c.slice(-200)); }
async function sendQueue() {
  const c = readQueue();
  if (!c.length) return;
  const rest = [];
  for (const x of c) {
    try { x.type === 'confirm' ? await apiCourier.confirm(x.data) : await apiCourier.location(x.data); }
    catch (e) { if (e.code === 0) rest.push(x); else notice('Un evento guardado sin señal no se pudo aplicar: ' + e.message); }
  }
  saveQueue(rest);
  if (c.length !== rest.length) { notice('Se enviaron los eventos guardados sin señal', 'ok'); load(); }
}
window.addEventListener('online', sendQueue);

function compressedImage(file) {
  return new Promise((ok, invalid) => {
    if (!file) return ok(null);
    const img = new Image();
    img.onload = () => {
      const k = Math.min(1, 900 / Math.max(img.width, img.height));
      const c = document.createElement('canvas'); c.width = img.width * k; c.height = img.height * k;
      c.getContext('2d').drawImage(img, 0, 0, c.width, c.height);
      ok(c.toDataURL('image/jpeg', 0.7));
    };
    img.onerror = () => invalid(new Error('No se pudo leer la foto'));
    img.src = URL.createObjectURL(file);
  });
}

// Firma con el dedo
const canvas = $('signature'), ctx = canvas.getContext('2d');
let signing = false, hasSignature = false;
const point = e => { const r = canvas.getBoundingClientRect(); return [(e.clientX - r.left) * canvas.width / r.width, (e.clientY - r.top) * canvas.height / r.height]; };
canvas.onpointerdown = e => { signing = true; hasSignature = true; ctx.beginPath(); ctx.moveTo(...point(e)); canvas.setPointerCapture(e.pointerId); };
canvas.onpointermove = e => { if (signing) { ctx.lineWidth = 2; ctx.lineTo(...point(e)); ctx.stroke(); } };
canvas.onpointerup = () => { signing = false; };
$('clearSignature').onclick = () => { ctx.clearRect(0, 0, canvas.width, canvas.height); hasSignature = false; };

$('confirmForm').onsubmit = async ev => {
  ev.preventDefault();
  const n = next();
  if (!n) return;
  const dropoff = n.p.type === 'DROPOFF';
  let photo = null;
  try { photo = dropoff ? await compressedImage($('inPhoto').files[0]) : null; } catch (e) { notice(e.message); return; }
  const data = {
    pin: dropoff ? $('inPin').value.trim() || null : null,
    amountReceived: dropoff && n.s.paymentMethod !== 'PREPAID' ? Number($('inAmount').value || 0) : null,
    photo, signature: dropoff && hasSignature ? canvas.toDataURL('image/png') : null,
    // la lectura más reciente del GPS (la del servidor puede tener unos segundos de atraso)
    lat: lastPosition?.lat ?? v.courier.position?.lat, lng: lastPosition?.lng ?? v.courier.position?.lng, eventTime: Date.now() / 60000,
    eventId: (crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random()).slice(0, 40),
  };
  $('btnConfirm').disabled = true;
  try {
    const r = await apiCourier.confirm(data);
    notice(r.message, 'ok');
    $('inPin').value = ''; $('inAmount').value = ''; $('inPhoto').value = ''; $('clearSignature').click();
    load();
  } catch (e) {
    if (e.code === 0) { enqueue('confirm', data); notice('Sin señal: se guardó y se enviará al volver la conexión', 'info'); }
    else notice(e.message);
  } finally { $('btnConfirm').disabled = false; }
};

// ---------------- Novedad (RF-M6-08) ----------------
$('incType').innerHTML = Object.entries(INCIDENT_TYPES).map(([k, t]) => `<option value="${k}">${t}</option>`).join('');
$('btnIncident').onclick = () => {
  $('incDelivery').innerHTML = '<option value="">Ninguno</option>' + v.deliveries.map(s => `<option value="${s.id}">#${s.id} ${esc(s.dropoffAddress || '')}</option>`).join('');
  const n = next(); if (n) $('incDelivery').value = n.s.id;
  $('incidentDialog').classList.remove('d-none');
};
$('incCancel').onclick = () => $('incidentDialog').classList.add('d-none');
$('incidentForm').onsubmit = async ev => {
  ev.preventDefault();
  try {
    await apiCourier.incident({ type: $('incType').value, deliveryId: $('incDelivery').value ? +$('incDelivery').value : null,
      description: $('incText').value.trim(), photo: await compressedImage($('incPhoto').files[0]) });
    notice('Novedad enviada a la central', 'ok');
    $('incidentDialog').classList.add('d-none'); ev.target.reset();
  } catch (e) { notice(e.message); }
};

// ---------------- Mensajes ----------------
$('templates').onclick = e => { const b = e.target.closest('button'); if (b) { $('messageForm').querySelector('input').value = b.textContent; } };
$('messageForm').onsubmit = async ev => {
  ev.preventDefault();
  const i = ev.target.querySelector('input');
  if (!i.value.trim()) return;
  try { await apiCourier.write(i.value.trim()); i.value = ''; load(); } catch (e) { notice(e.message); }
};

if ('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js').catch(() => {});
