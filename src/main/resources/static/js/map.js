// Mapa con Leaflet + OpenStreetMap (sin clave). Búsqueda de direcciones con Nominatim y rutas por calles con OSRM.
// Leaflet se carga como <script> normal (variable global L) antes de los módulos.
import { courierColor, initials } from './labels.js';

const icon = (html, cssClass, size = 28) => L.divIcon({ html, className: 'mk ' + cssClass, iconSize: [size, size], iconAnchor: [size / 2, size / 2] });

export function createMap(element, config) {
  const map = L.map(element, { zoomControl: true }).setView([config.center.lat, config.center.lng], 14);
  L.tileLayer(config.tilesUrl || 'https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19, attribution: '&copy; colaboradores de <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  }).addTo(map);
  L.rectangle([[config.southWest.lat, config.southWest.lng], [config.northEast.lat, config.northEast.lng]],
    { color: '#0071e3', weight: 1, fill: false, dashArray: '4 6', interactive: false }).addTo(map);

  const operationLayer = L.layerGroup().addTo(map);
  const editLayer = L.layerGroup().addTo(map);
  const editing = {};

  return {
    map,
    onClick(fn) { map.on('click', e => fn({ lat: e.latlng.lat, lng: e.latlng.lng })); },

    /** Domiciliarios, servicios abiertos y la ruta planeada de cada domiciliario. */
    renderOperation(status, { courierOnly = null } = {}) {
      operationLayer.clearLayers();
      const open = status.deliveries.filter(s => !['DELIVERED', 'CANCELLED', 'FAILED'].includes(s.status));
      for (const d of status.couriers) {
        if (!d.connected || !d.position || (courierOnly && d.id !== courierOnly)) continue;
        const color = d.noSignal ? '#9a9aa0' : courierColor(d.id);
        if (d.route?.length) {
          const points = [[d.position.lat, d.position.lng]];
          for (const p of d.route) {
            const s = status.deliveries.find(x => x.id === p.deliveryId);
            if (s) { const q = p.type === 'PICKUP' ? s.pickup : s.dropoff; points.push([q.lat, q.lng]); }
          }
          L.polyline(points, { color, weight: 3, opacity: .7, dashArray: '6 6' }).addTo(operationLayer);
        }
        L.marker([d.position.lat, d.position.lng], { icon: icon(`<span style="background:${color}">${initials(d.name)}</span>`, 'mk-courier', 32), zIndexOffset: 1000 })
          .bindTooltip(`${d.name} · ${d.courierStatus.replaceAll('_', ' ').toLowerCase()}`).addTo(operationLayer);
      }
      for (const s of open) {
        if (courierOnly && s.courierId !== courierOnly) continue;
        const color = s.courierId ? courierColor(s.courierId) : '#8e8e93';
        L.marker([s.pickup.lat, s.pickup.lng], { icon: icon(`<span style="border-color:${color}">${s.id}</span>`, 'mk-pickup', 26) })
          .bindTooltip(`#${s.id} recoger: ${s.pickupAddress || ''}`).addTo(operationLayer);
        L.marker([s.dropoff.lat, s.dropoff.lng], { icon: icon(`<span style="background:${color}">${s.id}</span>`, 'mk-dropoff', 24) })
          .bindTooltip(`#${s.id} entregar: ${s.dropoffAddress || ''}`).addTo(operationLayer);
      }
    },

    /** Marcador arrastrable del formulario (tipo 'R' recogida o 'E' entrega). */
    set(type, point, onMove) {
      if (editing[type]) editLayer.removeLayer(editing[type]);
      if (!point) { delete editing[type]; return; }
      const m = L.marker([point.lat, point.lng], { draggable: true, icon: icon(type === 'R' ? 'R' : 'E', 'mk-new mk-new-' + type, 30), zIndexOffset: 2000 });
      m.on('dragend', () => onMove?.({ lat: m.getLatLng().lat, lng: m.getLatLng().lng }));
      m.addTo(editLayer);
      editing[type] = m;
    },

    line(points, color = '#0071e3') {
      const l = L.polyline(points.map(p => [p.lat, p.lng]), { color, weight: 4 }).addTo(editLayer);
      return () => editLayer.removeLayer(l);
    },

    adjust(points) { if (points.length) map.fitBounds(L.latLngBounds(points.map(p => [p.lat, p.lng])).pad(0.25), { maxZoom: 16 }); },
    center(p, zoom = 16) { map.setView([p.lat, p.lng], zoom); },
    refreshSize() { setTimeout(() => map.invalidateSize(), 50); },
  };
}

/** Busca una dirección dentro del área de operación (Nominatim). Devuelve [{nombre, punto}]. */
export async function searchAddress(text, config) {
  const base = (config.nominatimUrl || 'https://nominatim.openstreetmap.org').replace(/\/+$/, '');
  const box = [config.southWest.lng, config.northEast.lat, config.northEast.lng, config.southWest.lat].join(',');
  const url = `${base}/search?format=jsonv2&limit=5&countrycodes=co&bounded=1&viewbox=${box}&accept-language=es&q=${encodeURIComponent(text + ', Valledupar')}`;
  const r = await fetch(url);
  if (!r.ok) throw new Error('El buscador de direcciones no respondió');
  return (await r.json()).map(x => ({ name: x.display_name.split(',').slice(0, 3).join(','), point: { lat: +x.lat, lng: +x.lon } }));
}

/** Dirección aproximada de un punto (para cuando marcan en el mapa). */
export async function addressOf(point, config) {
  try {
    const base = (config.nominatimUrl || 'https://nominatim.openstreetmap.org').replace(/\/+$/, '');
    const r = await fetch(`${base}/reverse?format=jsonv2&zoom=18&accept-language=es&lat=${point.lat}&lon=${point.lng}`);
    const x = await r.json();
    const a = x.address || {};
    return [a.road, a.house_number, a.neighbourhood || a.suburb].filter(Boolean).join(' ') || null;
  } catch { return null; }
}

/** Ruta por calles entre dos o más puntos (OSRM). Devuelve {puntos, km, min} o null si no hay servicio. */
export async function streetRoute(points, config) {
  try {
    const base = (config.osrmUrl || 'https://router.project-osrm.org').replace(/\/+$/, '');
    const coords = points.map(p => `${p.lng.toFixed(6)},${p.lat.toFixed(6)}`).join(';');
    const r = await fetch(`${base}/route/v1/driving/${coords}?overview=full&geometries=geojson`);
    const x = await r.json();
    if (x.code !== 'Ok') return null;
    return { points: x.routes[0].geometry.coordinates.map(([lng, lat]) => ({ lat, lng })), km: x.routes[0].distance / 1000, min: x.routes[0].duration / 60 };
  } catch { return null; }
}
