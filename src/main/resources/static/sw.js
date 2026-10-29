// Service worker de la app del domiciliario (PWA): guarda los archivos estáticos para abrir más rápido
// y para que la pantalla cargue aunque la señal esté mala. Los datos (/api) siempre van a la red.
const CACHE = 'despacho-v3';
const ESTATICOS = ['/css/theme.css', '/vendor/bootstrap/bootstrap.min.css', '/vendor/bootstrap-icons/bootstrap-icons.min.css',
  '/vendor/leaflet/leaflet.css', '/vendor/leaflet/leaflet.js', '/js/api.js', '/js/common.js', '/js/util.js', '/js/map.js',
  '/js/labels.js', '/js/stomp.js', '/js/radio.js', '/js/pages/courier.js'];

self.addEventListener('install', e => e.waitUntil(caches.open(CACHE).then(c => c.addAll(ESTATICOS)).then(() => self.skipWaiting())));
self.addEventListener('activate', e => e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())));
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  if (e.request.method !== 'GET' || url.origin !== location.origin || url.pathname.startsWith('/api/') || url.pathname.startsWith('/ws/')) return;
  // Red primero; si no hay señal, lo guardado
  e.respondWith(fetch(e.request).then(r => {
    if (r.ok && (url.pathname.startsWith('/js/') || url.pathname.startsWith('/css/') || url.pathname.startsWith('/vendor/'))) {
      const copia = r.clone(); caches.open(CACHE).then(c => c.put(e.request, copia));
    }
    return r;
  }).catch(() => caches.match(e.request)));
});
