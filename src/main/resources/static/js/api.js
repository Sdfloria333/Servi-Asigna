// Única capa que habla con el servidor. Si cambia una URL, se cambia solo aquí.
//
// Seguridad: la sesión viaja en una cookie HttpOnly con el token JWT (el JavaScript no la ve).
// Las peticiones que modifican datos llevan el token CSRF que Spring deja en <meta name="_csrf"> (ver fragmentos.html).
const token = document.querySelector('meta[name="_csrf"]')?.content;
const headerName = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-XSRF-TOKEN';

export class ErrorApi extends Error {
  constructor(message, code) { super(message); this.code = code; }
}

export async function request(route, method = 'GET', body) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET' && token) headers[headerName] = token;
  let reply;
  try {
    reply = await fetch(route, { method: method, headers: headers, body: body === undefined ? undefined : JSON.stringify(body), credentials: 'same-origin' });
  } catch (e) {
    throw new ErrorApi('Sin conexión con el servidor', 0);
  }
  if (reply.status === 401) {                     // el token venció o se abrió la sesión en otro celular
    location.href = '/login?expired';
    throw new ErrorApi('Tu sesión terminó', 401);
  }
  const type = reply.headers.get('Content-Type') || '';
  const data = type.includes('json') ? await reply.json().catch(() => null) : await reply.text();
  if (reply.status === 403 && data?.error?.includes('contraseña')) location.href = '/change-password';
  if (reply.status === 403 && data?.error?.includes('política')) location.href = '/policy';
  if (!reply.ok) throw new ErrorApi(data?.error || 'Error ' + reply.status, reply.status);
  return data;
}

const q = obj => {
  const p = new URLSearchParams();
  for (const [k, v] of Object.entries(obj || {})) if (v !== undefined && v !== null && v !== '') p.set(k, v);
  const s = p.toString();
  return s ? '?' + s : '';
};

// ---- Central (/api/central) ----
const C = '/api/central';
export const api = {
  status:        ()               => request(C + '/status'),
  mode:          mode             => request(C + '/mode', 'POST', { mode }),
  createDelivery: data            => request(C + '/deliveries', 'POST', data),
  suggestedValue: (r, e, size)   => request(C + '/deliveries/value' + q({ plat: r.lat, plng: r.lng, dlat: e.lat, dlng: e.lng, size })),
  edit:        (id, data)      => request(`${C}/deliveries/${id}`, 'PUT', data),
  assign:       (id, courierId, reason) => request(`${C}/deliveries/${id}/assign`, 'POST', { courierId, reason }),
  revoke:       (id, reason)     => request(`${C}/deliveries/${id}/revoke`, 'POST', { reason }),
  cancel:      (id, reason)     => request(`${C}/deliveries/${id}/cancel`, 'POST', { reason }),
  search:        filters          => request(C + '/deliveries/search' + q(filters)),
  detail:       id               => request(`${C}/deliveries/${id}`),
  messages:      courierId           => request(`${C}/couriers/${courierId}/messages`),
  write:      (courierId, text)  => request(`${C}/couriers/${courierId}/messages`, 'POST', { text }),
  collected:       (courierId, value)  => request(`${C}/couriers/${courierId}/collection`, 'POST', { value }),
  track:     (courierId, hours)  => request(`${C}/couriers/${courierId}/track` + q({ hours })),
  shifts:        range            => request(C + '/shifts' + q(range)),
  incidents:     filters          => request(C + '/incidents' + q(filters)),
  incidentPhoto:   id               => request(`${C}/incidents/${id}/photo`),
  reportIncident: data          => request(C + '/incidents', 'POST', data),
  comment:      (id, text)      => request(`${C}/incidents/${id}/comment`, 'POST', { text }),
  decide:       (id, decision, resolution) => request(`${C}/incidents/${id}/decision`, 'POST', { decision, resolution }),
  customers:      text            => request(C + '/customers' + q({ q: text })),
  customerByPhone: tel         => request(`${C}/customers/phone/${encodeURIComponent(tel)}`),
  customer:       id               => request(`${C}/customers/${id}`),
  saveCustomer: data           => request(C + '/customers', 'POST', data),
  removeCustomer: id               => request(`${C}/customers/${id}`, 'DELETE'),
  addAddress: (id, data)   => request(`${C}/customers/${id}/addresses`, 'POST', data),
  removeAddress: id             => request(`${C}/addresses/${id}`, 'DELETE'),
  templates:    ()               => request(C + '/templates'),
  indicators:   range            => request(C + '/indicators' + q(range)),
  csvUrl:        (type, range)    => `${C}/reports/${type}.csv` + q(range),
};

// ---- Administración (/api/central/admin, solo ADMIN) ----
const A = C + '/admin';
export const apiAdmin = {
  accounts:       ()               => request(A + '/users'),
  createAccount:   data            => request(A + '/users', 'POST', data),
  approve:       id               => request(`${A}/users/${id}/approve`, 'POST', {}),
  reject:      id               => request(`${A}/users/${id}/reject`, 'POST', {}),
  accountStatus:  (id, status)     => request(`${A}/users/${id}/status`, 'POST', { status }),
  reset:   id               => request(`${A}/users/${id}/password`, 'POST', {}),
  editCourier:    (id, data)      => request(`${A}/users/${id}`, 'PUT', data),
  vehicle:      data            => request(A + '/vehicles', 'POST', data),
  removeVehicle: id              => request(`${A}/vehicles/${id}`, 'DELETE'),
  settings:    ()               => request(A + '/settings'),
  setting:     (key, value)        => request(`${A}/settings/${encodeURIComponent(key)}`, 'PUT', { value }),
  rate:        ()               => request(A + '/rate'),
  newRate:   data            => request(A + '/rate', 'POST', data),
  policy:      ()               => request(A + '/policy'),
  publishPolicy: data         => request(A + '/policy', 'POST', data),
  addTemplate: data         => request(A + '/templates', 'POST', data),
  removeTemplate: id             => request(`${A}/templates/${id}`, 'DELETE'),
  audit:     range            => request(A + '/audit' + q(range)),
};

// ---- Domiciliario (/api/domi): el servidor sabe quién soy por el token ----
const D = '/api/courier';
export const apiCourier = {
  me:         ()                 => request(D + '/me'),
  status:     (status, pos)      => request(D + '/status', 'POST', { status, ...(pos || {}) }),
  location:  pos                => request(D + '/location', 'POST', pos),
  reply:  (id, accept, reason) => request(`${D}/offers/${id}`, 'POST', { accept, reason }),
  confirm:  data              => request(D + '/confirm', 'POST', data),
  incident:    data              => request(D + '/incident', 'POST', data),
  messages:   ()                 => request(D + '/messages'),
  write:   text              => request(D + '/messages', 'POST', { text }),
};
