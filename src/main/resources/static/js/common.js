// Lo que comparten TODAS las pantallas de la central: estado en vivo (STOMP con respaldo por consulta),
// barra superior, avisos emergentes, alertas nuevas, radio y utilidades de diálogos.
import { api } from './api.js';
import { liveClock } from './util.js';
import { mountRadio } from './radio.js';
import { connectRealtime } from './stomp.js';
import { MODES } from './labels.js';

export const $ = id => document.getElementById(id);

/** Escapa texto antes de meterlo en HTML (nombres, notas, direcciones...). */
export function esc(text) {
  const r = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  return String(text ?? '').replace(/[&<>"']/g, c => r[c]);
}

// ---- Avisos emergentes (abajo a la derecha) ----
export function notice(text, type = 'error', ms = 4500) {
  let stack = $('noticeStack');
  if (!stack) { stack = document.createElement('div'); stack.id = 'noticeStack'; stack.className = 'notice-stack'; document.body.append(stack); }
  if (!text) return;
  const box = document.createElement('div');
  box.className = 'toast-notice toast-' + type;
  box.setAttribute('role', type === 'error' ? 'alert' : 'status');
  box.innerHTML = `<i class="bi ${type === 'ok' ? 'bi-check-circle-fill' : type === 'info' ? 'bi-bell-fill' : 'bi-exclamation-triangle-fill'}"></i><span>${esc(text)}</span>`;
  box.onclick = () => box.remove();
  stack.append(box);
  setTimeout(() => box.classList.add('leaving'), ms);
  setTimeout(() => box.remove(), ms + 400);
}

/** Ejecuta una llamada; si falla muestra el error. Devuelve el resultado o null. */
export async function action(promise, okText) {
  try {
    const r = await promise;
    if (okText) notice(okText, 'ok');
    return r ?? true;
  } catch (e) {
    notice(e.message, 'error');
    return null;
  }
}

/** Ventana para pedir un texto (motivo de cancelación, etc.). Devuelve el texto o null si cancela. */
export function askText({ title, label = 'Motivo', button = 'Aceptar', danger = false, required = true, value = '' }) {
  return new Promise(resolver => {
    const background = document.createElement('div');
    background.className = 'dialog-backdrop';
    background.innerHTML = `<form class="dialog" role="dialog" aria-modal="true">
        <h2>${esc(title)}</h2>
        <label class="form-label" for="textDialog">${esc(label)}</label>
        <textarea id="textDialog" class="form-control" rows="3" maxlength="300">${esc(value)}</textarea>
        <div class="dialog-buttons">
          <button type="button" class="btn btn-outline-secondary" data-no>Volver</button>
          <button type="submit" class="btn ${danger ? 'btn-danger' : 'btn-primary'}">${esc(button)}</button>
        </div></form>`;
    document.body.append(background);
    const area = background.querySelector('textarea');
    area.focus();
    const close = v => { background.remove(); resolver(v); };
    background.querySelector('[data-no]').onclick = () => close(null);
    background.onkeydown = e => { if (e.key === 'Escape') close(null); };
    background.querySelector('form').onsubmit = e => {
      e.preventDefault();
      const t = area.value.trim();
      if (required && t.length < 3) { area.classList.add('is-invalid'); area.focus(); return; }
      close(t);
    };
  });
}

/** Panel lateral (detalle de un servicio, conversación...). Devuelve {cuerpo, cerrar}. */
export function sidePanel(title, onClose) {
  document.querySelector('.side-panel')?.remove();
  const p = document.createElement('aside');
  p.className = 'side-panel';
  p.innerHTML = `<header><h2>${esc(title)}</h2><button type="button" class="btn btn-sm btn-outline-secondary" aria-label="Cerrar"><i class="bi bi-x-lg"></i></button></header><div class="panel-body"></div>`;
  document.body.append(p);
  const close = () => { p.remove(); onClose?.(); };
  p.querySelector('header button').onclick = close;
  return { body: p.querySelector('.panel-body'), close, title: t => (p.querySelector('h2').textContent = t) };
}

// ---- Estado en vivo ----
let lastStatus = null;
const seenAlerts = new Set();
let firstTime = true;

/**
 * Arranca la pantalla: pinta con cada estado nuevo que llega (por STOMP al instante; por consulta cada 5 s si el
 * tiempo real se cae). Devuelve una función para pedir el estado de inmediato (después de una acción).
 */
export function start(renderPage) {
  liveClock($('clock'));
  if ($('radio')) mountRadio($('radio'), { spaceKey: true });
  let live = false;

  const receive = status => {
    lastStatus = status;
    renderBar(status);
    notifyNewAlerts(status.alerts || []);
    try { renderPage(status); } catch (e) { console.error(e); }
  };
  const query = async () => {
    try { receive(await api.status()); }
    catch (e) { if (e.code === 0) markConnection(false); }
  };

  connectRealtime({
    '/topic/central': receive,
    '/topic/central/notices': a => notice(a.text || a.type, a.type === 'ENGINE' ? 'error' : 'info', 7000),
  }, connected => { live = connected; markConnection(connected); if (connected) query(); });

  query();
  setInterval(() => { if (!live) query(); }, 5000);
  return query;
}

export const currentStatus = () => lastStatus;

function markConnection(live) {
  const c = $('liveChip');
  if (!c) return;
  c.className = 'chip ' + (live ? 'chip-ok' : 'chip-red');
  c.innerHTML = live ? '<i class="bi bi-broadcast"></i> En vivo' : '<i class="bi bi-wifi-off"></i> Reconectando';
}

function renderBar(status) {
  const m = $('engineBadge');
  if (m) {
    const [name] = MODES[status.mode] || [status.mode];
    m.innerHTML = `<i class="bi bi-cpu"></i> Modo ${name}${status.engineFailure ? ' · <b>falla</b>' : ''}`;
    m.className = 'chip ' + (status.engineFailure ? 'chip-red' : status.mode === 'MANUAL' ? 'chip-gray' : 'chip-ok');
    m.title = status.engineFailure || '';
  }
  const badge = (id, n) => { const b = $(id); if (b) { b.textContent = n; b.classList.toggle('d-none', !n); } };
  badge('badgePending', status.pendingAccounts);
  badge('badgeIncidents', status.openIncidents);
  badge('badgeAlerts', (status.alerts || []).filter(a => a.level >= 2).length);
}

/** Muestra una sola vez cada alerta nueva urgente (RF-M9-02). */
function notifyNewAlerts(alerts) {
  for (const a of alerts) {
    if (seenAlerts.has(a.id)) continue;
    seenAlerts.add(a.id);
    if (!firstTime && a.level >= 2) notice(a.text, a.level >= 3 ? 'error' : 'info', 7000);
  }
  firstTime = false;
}
