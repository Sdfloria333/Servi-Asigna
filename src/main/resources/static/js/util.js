// Utilidades de horas (siempre America/Bogotá, RN-17) y de distancias reales.
const ZONE = 'America/Bogota';
const two = n => String(n).padStart(2, '0');

const fmtTime = new Intl.DateTimeFormat('es-CO', { timeZone: ZONE, hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' });
const fmtDay = new Intl.DateTimeFormat('en-CA', { timeZone: ZONE, year: 'numeric', month: '2-digit', day: '2-digit' });

function timeParts(ms) {
  const p = Object.fromEntries(fmtTime.formatToParts(new Date(ms)).map(x => [x.type, x.value]));
  return { h: +p.hour, m: +p.minute, s: +p.second };
}

// El servidor manda las horas como "minutos de reloj" (minutos desde 1970). -> "14:35" hora de Bogotá
export function formatTime(minutes) {
  if (minutes == null) return '—';
  const { h, m } = timeParts(minutes * 60000);
  return two(h) + ':' + two(m);
}

/** Hora del día (0-23) en Bogotá de unos minutos de reloj; sin argumento, la hora de ahora. */
export const bogotaTime = minutes => timeParts(minutes == null ? Date.now() : minutes * 60000).h;

// Duración en minutos -> "8 min" / "1 h 05 min"
export function duration(minutes) {
  const m = Math.max(0, Math.round(minutes));
  return m < 60 ? m + ' min' : Math.floor(m / 60) + ' h ' + two(m % 60) + ' min';
}

// ¿Esta hora (minutos de reloj) es de hoy en Bogotá?
export const isToday = minutes => fmtDay.format(new Date(minutes * 60000)) === fmtDay.format(new Date());

// Reloj de la barra: la hora real de Bogotá, segundo a segundo
export function liveClock(element) {
  if (!element) return;
  const render = () => { const { h, m, s } = timeParts(Date.now()); element.textContent = two(h) + ':' + two(m) + ':' + two(s); };
  render();
  setInterval(render, 1000);
}

// ---- Distancias ----
const R = 6371.0088;
/** Kilómetros en línea recta entre dos puntos {lat, lng} (Haversine). */
export function distanceKm(a, b) {
  const rad = x => x * Math.PI / 180;
  const h = Math.sin(rad(b.lat - a.lat) / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(rad(b.lng - a.lng) / 2) ** 2;
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(h)));
}

/** "850 m" o "1,3 km". */
export function distanceText(km) {
  return km < 1 ? Math.round(km * 1000 / 10) * 10 + ' m' : km.toFixed(1).replace('.', ',') + ' km';
}

/** Lee "10.4700, -73.2500" (lo que Google Maps copia con clic derecho) o devuelve null. */
export function readCoordinates(text) {
  const m = /^\s*(-?\d{1,2}(?:[.,]\d+)?)\s*[,;\s]\s*(-?\d{1,3}(?:[.,]\d+)?)\s*$/.exec(text || '');
  if (!m) return null;
  const lat = parseFloat(m[1].replace(',', '.')), lng = parseFloat(m[2].replace(',', '.'));
  return Math.abs(lat) <= 90 && Math.abs(lng) <= 180 ? { lat, lng } : null;
}

export const coordinatesText = p => p.lat.toFixed(5) + ', ' + p.lng.toFixed(5);

/** Texto para mostrar de un lugar: su dirección o, si no hay, sus coordenadas. */
export const place = (address, point) => address || coordinatesText(point);

/** Abre la app de mapas del celular (Google Maps, Waze, OsmAnd...) con el destino. */
export const navigationLink = p => /Android|iPhone|iPad/i.test(navigator.userAgent)
  ? `geo:${p.lat},${p.lng}?q=${p.lat},${p.lng}`
  : `https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=%3B${p.lat}%2C${p.lng}`;
/** Ver un punto en OpenStreetMap. */
export const viewLink = p => `https://www.openstreetmap.org/?mlat=${p.lat}&mlon=${p.lng}#map=18/${p.lat}/${p.lng}`;

// ---- Dinero y fechas ----
const fmtPesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
/** 12500 -> "$ 12.500" */
export const pesos = v => v == null ? '—' : fmtPesos.format(v);

const fmtDate = new Intl.DateTimeFormat('es-CO', { timeZone: ZONE, day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' });
/** Minutos de reloj -> "26 oct, 14:35" */
export const dateTime = minutes => minutes == null ? '—' : fmtDate.format(new Date(minutes * 60000));
/** Fecha ISO de hoy en Bogotá (2026-10-26), para los filtros de fecha. */
export const todayIso = () => fmtDay.format(new Date());
/** Fecha ISO de hace n días. */
export const daysAgo = n => fmtDay.format(new Date(Date.now() - n * 86400000));
/** Minutos de reloj de ahora. */
export const nowMin = () => Date.now() / 60000;
