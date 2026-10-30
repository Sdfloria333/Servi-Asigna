// Administración: aprobar domiciliarios, estados de cuenta, claves, vehículos, parámetros, tarifa y auditoría.
import { apiAdmin } from '../api.js';
import { $, start, action, notice, esc, askText } from '../common.js';
import { dateTime, formatTime } from '../util.js';
import { ACCOUNT_STATUSES, ROLES, VEHICLE_TYPES, chip } from '../labels.js';

let previousPending = -1;
start(e => { if (e.pendingAccounts !== previousPending) { previousPending = e.pendingAccounts; loadAccounts(); } });

async function loadAccounts() {
  const accounts = await action(apiAdmin.accounts());
  if (!accounts) return;
  const pend = accounts.filter(c => c.accountStatus === 'PENDING');
  $('pending').innerHTML = pend.length ? pend.map(c => `<div class="d-flex gap-2 align-items-center mb-2">
      <div class="flex-grow-1"><b>${esc(c.name)}</b> @${esc(c.username)} · cédula ${esc(c.idNumber || '—')} · ${esc(c.phone || '')}</div>
      <button class="btn btn-sm btn-primary" data-approve="${c.id}">Aprobar</button><button class="btn btn-sm btn-outline-danger" data-reject="${c.id}">Rechazar</button></div>`).join('')
    : '<p class="text-soft m-0">No hay solicitudes.</p>';
  $('accountsTable').innerHTML = `<thead><tr><th>Nombre</th><th>Rol</th><th>Estado</th><th>Vehículo</th><th>Último ingreso</th><th></th></tr></thead><tbody>` +
    accounts.filter(c => c.accountStatus !== 'PENDING').map(c => {
      const courier = c.role === 'COURIER';
      return `<tr><td><b>${esc(c.name)}</b><div class="small text-soft">@${esc(c.username)}${c.idNumber ? ' · ' + esc(c.idNumber) : ''}</div></td>
        <td>${ROLES[c.role]}</td>
        <td><select class="form-select form-select-sm" data-status="${c.id}">${Object.entries(ACCOUNT_STATUSES).filter(([k]) => k !== 'PENDING').map(([k, v]) => `<option value="${k}" ${k === c.accountStatus ? 'selected' : ''}>${v[0]}</option>`).join('')}</select></td>
        <td class="small">${courier ? (c.vehicle ? `${VEHICLE_TYPES[c.vehicle.type]} ${esc(c.vehicle.plate)} (${c.vehicle.capacityUnits} u.) ` : '<span class="text-red-c">sin vehículo</span> ')
          + `<a href="#" data-vehicle="${c.id}">cambiar</a>` : ''}</td>
        <td class="small">${c.lastLogin ? c.lastLogin.replace('T', ' ').slice(0, 16) : '—'}</td>
        <td><button class="btn btn-sm btn-outline-secondary" data-password="${c.id}" title="Restablecer contraseña"><i class="bi bi-key"></i></button></td></tr>`;
    }).join('') + '</tbody>';
}

document.body.addEventListener('click', async ev => {
  const t = ev.target.closest('[data-approve],[data-reject],[data-password],[data-vehicle]');
  if (!t) return;
  ev.preventDefault();
  const d = t.dataset;
  if (d.approve && await action(apiAdmin.approve(+d.approve), 'Cuenta aprobada')) loadAccounts();
  if (d.reject && await action(apiAdmin.reject(+d.reject), 'Solicitud rechazada')) loadAccounts();
  if (d.password) { const r = await action(apiAdmin.reset(+d.password)); if (r) await askText({ title: 'Clave temporal (cópiala ahora)', label: 'Debe cambiarla al entrar', value: r.temporary, button: 'Listo', required: false }); }
  if (d.vehicle) {
    const v = await askText({ title: 'Vehículo', label: 'Placa y tipo, ej: ABC12D MOTO (MOTO, BICICLETA, CARRO, A_PIE)', button: 'Guardar' });
    if (v) { const [plate, type = 'MOTORCYCLE'] = v.trim().split(/\s+/); if (await action(apiAdmin.vehicle({ courierId: +d.vehicle, plate, type: type.toUpperCase() }), 'Vehículo guardado')) loadAccounts(); }
  }
});
$('accountsTable').onchange = async ev => {
  const s = ev.target.closest('[data-status]');
  if (s && !await action(apiAdmin.accountStatus(+s.dataset.status, s.value), 'Estado actualizado')) loadAccounts();
};
$('accountForm').onsubmit = async ev => {
  ev.preventDefault();
  const f = Object.fromEntries(new FormData(ev.target));
  if (await action(apiAdmin.createAccount(f), 'Cuenta creada (deberá cambiar la clave al entrar)')) { ev.target.reset(); loadAccounts(); }
};

// ---- Parámetros ----
async function loadSettings() {
  const ps = await action(apiAdmin.settings());
  if (!ps) return;
  $('settingsTable').innerHTML = '<tbody>' + ps.map(p => `<tr><td><code>${esc(p.key)}</code><div class="small text-soft">${esc(p.description)}</div></td>
    <td style="width:38%"><input class="form-control form-control-sm" data-param="${esc(p.key)}" value="${esc(p.value)}"></td></tr>`).join('') + '</tbody>';
}
$('settingsTable').onchange = async ev => {
  const i = ev.target.closest('[data-param]');
  if (i && !await action(apiAdmin.setting(i.dataset.param, i.value.trim()), 'Parámetro guardado')) loadSettings();
};

// ---- Tarifa ----
const FIELDS = [['name', 'Nombre', 'text'], ['base', 'Base', 'number'], ['perKm', 'Por km', 'number'], ['mediumSurcharge', 'Recargo mediano', 'number'],
  ['largeSurcharge', 'Recargo grande', 'number'], ['minimum', 'Mínimo', 'number'], ['courierPercentage', '% para el domiciliario', 'number']];
async function loadRate() {
  const t = await action(apiAdmin.rate()) || {};
  $('rateForm').innerHTML = FIELDS.map(([k, n, type]) => `<div class="col-6"><label class="form-label">${n}</label><input class="form-control form-control-sm" name="${k}" type="${type}" value="${esc(t[k] ?? '')}" required></div>`).join('')
    + '<div class="col-12"><button class="btn btn-sm btn-primary">Publicar nueva tarifa</button></div>';
}
$('rateForm').onsubmit = async ev => {
  ev.preventDefault();
  const f = Object.fromEntries(new FormData(ev.target));
  for (const k of Object.keys(f)) if (k !== 'name') f[k] = Number(f[k]);
  if (await action(apiAdmin.newRate(f), 'Tarifa publicada')) loadRate();
};

async function loadAudit() {
  const a = await action(apiAdmin.audit({})) || [];
  $('audit').innerHTML = a.slice(0, 100).map(x => `<li><time>${formatTime(x.date)}</time> <b>${esc(x.username)}</b> ${esc(x.action.toLowerCase().replaceAll('_', ' '))}
    ${x.entityId ? '#' + x.entityId : ''} <span class="text-soft">${esc(x.detail || '')}</span>${x.manualIntervention ? ' <span class="st st-amber">manual</span>' : ''}</li>`).join('') || '<li class="text-soft">Sin registros hoy.</li>';
}
loadSettings(); loadRate(); loadAudit();
