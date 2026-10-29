// Cliente STOMP mínimo sobre WebSocket (sin librerías). Se conecta a /ws/stomp con la cookie de sesión,
// se suscribe a destinos y se reconecta solo si se cae la conexión.
//   const t = connectRealtime({ '/topic/central': datos => ..., '/user/queue/avisos': aviso => ... }, estado => ...)
const NUL = '\u0000';

export function connectRealtime(subscriptions, onStatusChange = () => {}) {
  let ws = null, heartbeat = null, watcher = null, lastData = 0, waitTime = 1000, closedManually = false;

  function frame(command, headers = {}, body = '') {
    let s = command + '\n';
    for (const [k, v] of Object.entries(headers)) s += k + ':' + v + '\n';
    return s + '\n' + body + NUL;
  }

  function connect() {
    ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws/stomp');
    ws.onopen = () => ws.send(frame('CONNECT', { 'accept-version': '1.2', host: location.hostname, 'heart-beat': '10000,10000' }));
    ws.onmessage = e => {
      lastData = Date.now();
      for (const raw of String(e.data).split(NUL)) {
        const text = raw.replace(/^\n+/, '');
        if (!text) continue;                                 // latido del servidor
        const end = text.indexOf('\n\n');
        const lines = text.slice(0, end < 0 ? text.length : end).split('\n');
        const command = lines.shift();
        const header = Object.fromEntries(lines.map(l => [l.slice(0, l.indexOf(':')), l.slice(l.indexOf(':') + 1)]));
        const body = end < 0 ? '' : text.slice(end + 2);
        if (command === 'CONNECTED') {
          waitTime = 1000;
          Object.keys(subscriptions).forEach((destination, i) => ws.send(frame('SUBSCRIBE', { id: 'sub-' + i, destination: destination })));
          clearInterval(heartbeat);
          heartbeat = setInterval(() => { if (ws.readyState === 1) ws.send('\n'); }, 10000);
          clearInterval(watcher);
          watcher = setInterval(() => { if (Date.now() - lastData > 35000) ws.close(); }, 5000);   // el servidor dejó de latir
          onStatusChange(true);
        } else if (command === 'MESSAGE') {
          const handler = subscriptions[header.subscription ? Object.keys(subscriptions)[+header.subscription.split('-')[1]] : header.destination];
          if (handler) { try { handler(JSON.parse(body)); } catch (err) { console.error('Mensaje en tiempo real inválido', err); } }
        } else if (command === 'ERROR') {
          console.warn('STOMP:', header.message || body);
        }
      }
    };
    ws.onclose = () => {
      clearInterval(heartbeat); clearInterval(watcher);
      onStatusChange(false);
      if (!closedManually) { setTimeout(connect, waitTime); waitTime = Math.min(waitTime * 2, 15000); }
    };
    ws.onerror = () => {};
  }
  connect();
  return { close() { closedManually = true; ws?.close(); } };
}
