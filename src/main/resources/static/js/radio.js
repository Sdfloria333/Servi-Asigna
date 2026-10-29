// Radio de voz tipo walkie-talkie (como Zello): mantén el botón para hablar, suelta para escuchar.
// Un solo canal para la central y todos los domiciliarios en línea. Habla uno a la vez.
//
// Cómo viaja la voz: el micrófono se lee en pedacitos (~0,1 s), se pasa a 16 kHz / 16 bits y se manda
// por WebSocket (/ws/radio). El servidor la reenvía al instante a los demás, que la van reproduciendo.
// Ver RadioChannel.java.

const RATE = 16000;                  // muestras por segundo que viajan (voz clara y liviana: ~32 KB/s)

const esc = t => String(t ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/** spaceKey: en la central, mantener la barra espaciadora también sirve para hablar. */
export function mountRadio(container, { spaceKey = false } = {}) {
  // ---------- Dibujo ----------
  container.classList.add('radio');
  container.innerHTML = `
    <div class="radio-head">
      <i class="bi bi-broadcast"></i><b>Radio</b>
      <button type="button" class="radio-present" data-r="present" title="Quién está en el canal">0 en línea</button>
    </div>
    <div class="radio-body">
      <ul class="radio-list d-none" data-r="list"></ul>
      <div class="radio-status" data-r="status"><i class="bi bi-circle"></i> Conectando…</div>
      <button type="button" class="radio-ptt" data-r="ptt" disabled>
        <i class="bi bi-mic-fill"></i><span data-r="pttText">Mantén para hablar</span>
      </button>
      <div class="radio-note" data-r="note">${spaceKey ? 'O mantén la barra espaciadora' : ''}</div>
    </div>`;
  const el = name => container.querySelector(`[data-r="${name}"]`);

  // ---------- Estado ----------
  let ws = null;
  let present = [];
  let speaker = null;          // {nombre, usuario} de quien tiene el canal (otro)
  let me = 'nada';                // 'nada' | 'pidiendo' | 'hablando'
  let pressed = false;
  let ctx = null;                 // AudioContext (se crea con el primer toque: los navegadores lo exigen)
  let next = 0;              // cuándo empieza el próximo pedacito de voz que llegue
  let mic = null;                 // {stream, fuente, proceso}
  let muted = false;          // llegó voz pero el audio aún no está habilitado

  // ---------- Conexión con el canal ----------
  function connect() {
    ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws/radio');
    ws.binaryType = 'arraybuffer';
    ws.onopen = () => { el('ptt').disabled = false; render(); };
    ws.onclose = () => {
      el('ptt').disabled = true;
      present = []; speaker = null; releaseMicrophone(); me = 'nada';
      render('Sin conexión con la radio. Reintentando…');
      setTimeout(connect, 3000);
    };
    ws.onmessage = e => {
      if (typeof e.data !== 'string') { play(e.data); return; }
      const m = JSON.parse(e.data);
      if (m.t === 'present') present = m.list;
      else if (m.t === 'speaking') { speaker = m; next = 0; beep(880, 0.06); }
      else if (m.t === 'free') { if (speaker) beep(660, 0.08); speaker = null; }
      else if (m.t === 'granted') { if (pressed) { me = 'talking'; beep(1000, 0.05); } else send({ t: 'release' }); }
      else if (m.t === 'busy') { me = 'nada'; releaseMicrophone(); render('Canal ocupado: espera a que terminen'); return; }
      else if (m.t === 'cut') { me = 'nada'; releaseMicrophone(); render('Se cortó tu turno (máximo 1 minuto o sin audio)'); return; }
      render();
    };
  }

  function send(obj) { if (ws?.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj)); }

  // ---------- Audio: habilitarlo con el primer toque ----------
  function audio() {
    if (!ctx) ctx = new (window.AudioContext || window.webkitAudioContext)();
    if (ctx.state === 'suspended') ctx.resume();
    if (muted) { muted = false; render(); }
    return ctx;
  }
  ['pointerdown', 'keydown', 'touchstart'].forEach(t => document.addEventListener(t, audio, { passive: true }));

  function beep(frequency, seconds) {
    if (!ctx || ctx.state !== 'running') return;
    const o = ctx.createOscillator(), g = ctx.createGain();
    o.frequency.value = frequency; g.gain.value = 0.06;
    o.connect(g).connect(ctx.destination);
    o.start(); o.stop(ctx.currentTime + seconds);
  }

  // Llega un pedacito de voz: PCM 16 bits a 16 kHz -> se pone en fila para sonar sin cortes
  function play(data) {
    if (!ctx || ctx.state !== 'running') { if (!muted) { muted = true; render(); } return; }
    const pcm = new Int16Array(data);
    const buffer = ctx.createBuffer(1, pcm.length, RATE);
    const channel = buffer.getChannelData(0);
    for (let i = 0; i < pcm.length; i++) channel[i] = pcm[i] / 32768;
    const source = ctx.createBufferSource();
    source.buffer = buffer;
    source.connect(ctx.destination);
    const start = Math.max(next, ctx.currentTime + 0.15);     // 0,15 s de colchón contra la red
    source.start(start);
    next = start + buffer.duration;
  }

  // ---------- Hablar ----------
  async function press() {
    if (pressed || el('ptt').disabled) return;
    pressed = true;
    if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
      pressed = false;
      render('El micrófono solo funciona con https:// (o en localhost). Mira el README.');
      return;
    }
    audio();
    me = 'requesting';
    send({ t: 'talk' });
    render();
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true } });
      if (!pressed || me === 'nada') { stream.getTracks().forEach(t => t.stop()); return; }   // ya soltó o le negaron el turno
      const source = ctx.createMediaStreamSource(stream);
      const process = ctx.createScriptProcessor(4096, 1, 1);
      process.onaudioprocess = e => {
        if (me !== 'talking' || ws?.readyState !== WebSocket.OPEN) return;
        ws.send(a16k(e.inputBuffer.getChannelData(0), ctx.sampleRate));
      };
      source.connect(process);
      process.connect(ctx.destination);           // necesario para que el navegador procese (sale en silencio)
      mic = { stream, source, process };
    } catch {
      release();
      render('No se pudo usar el micrófono: revisa el permiso del navegador');
    }
  }

  function release() {
    pressed = false;
    if (me !== 'nada') send({ t: 'release' });
    me = 'nada';
    releaseMicrophone();
    render();
  }

  function releaseMicrophone() {
    if (!mic) return;
    mic.process.disconnect(); mic.source.disconnect();
    mic.stream.getTracks().forEach(t => t.stop());
    mic = null;
  }

  // Float32 a la tasa del equipo (44.1/48 kHz) -> Int16 a 16 kHz
  function a16k(input, sourceRate) {
    const reason = sourceRate / RATE;
    const length = Math.floor(input.length / reason);
    const output = new Int16Array(length);
    for (let i = 0; i < length; i++) {
      const from = Math.floor(i * reason), to = Math.min(input.length, Math.floor((i + 1) * reason));
      let sum = 0;
      for (let j = from; j < to; j++) sum += input[j];
      const v = Math.max(-1, Math.min(1, sum / Math.max(1, to - from)));
      output[i] = v < 0 ? v * 32768 : v * 32767;
    }
    return output.buffer;
  }

  // ---------- Pantalla ----------
  function render(message) {
    const ptt = el('ptt');
    el('present').textContent = present.length + ' en línea';
    el('list').innerHTML = present.map(p => `<li><i class="bi ${p.role === 'CENTRAL' ? 'bi-headset' : 'bi-bicycle'}"></i> ${esc(p.name)} <span>@${esc(p.username)}</span></li>`).join('')
      || '<li>Nadie más en línea</li>';

    let html, cssClass = '';
    if (message) { html = `<i class="bi bi-exclamation-circle"></i> ${esc(message)}`; cssClass = 'radio-alert'; }
    else if (me === 'talking') { html = '<i class="bi bi-record-circle-fill"></i> Estás al aire'; cssClass = 'radio-on-air'; }
    else if (me === 'requesting') html = '<i class="bi bi-hourglass-split"></i> Pidiendo el canal…';
    else if (speaker) { html = `<i class="bi bi-volume-up-fill"></i> Habla <b>${esc(speaker.name)}</b>`; cssClass = 'radio-listening'; }
    else if (ws?.readyState === WebSocket.OPEN) html = '<i class="bi bi-circle-fill"></i> Canal libre';
    else html = '<i class="bi bi-circle"></i> Conectando…';
    if (muted) { html += '<div class="radio-turn">Toca la pantalla para escuchar</div>'; }
    el('status').innerHTML = html;
    el('status').className = 'radio-status ' + cssClass;

    ptt.classList.toggle('on-air', me === 'talking');
    ptt.classList.toggle('busy', !!speaker && me === 'nada');
    el('pttText').textContent = me === 'talking' ? 'Suelta para terminar' : speaker ? 'Canal ocupado' : 'Mantén para hablar';
  }

  // ---------- Eventos ----------
  const ptt = el('ptt');
  ptt.addEventListener('pointerdown', e => { e.preventDefault(); ptt.setPointerCapture?.(e.pointerId); press(); });
  ['pointerup', 'pointercancel', 'lostpointercapture'].forEach(t => ptt.addEventListener(t, () => { if (pressed) release(); }));
  ptt.addEventListener('contextmenu', e => e.preventDefault());      // en celular, mantener no abre el menú

  if (spaceKey) {
    // Barra espaciadora = botón de hablar (si no se está escribiendo en un campo)
    const typing = e => ['INPUT', 'TEXTAREA', 'SELECT'].includes(e.target.tagName) || e.target.isContentEditable;
    document.addEventListener('keydown', e => { if (e.code === 'Space' && !e.repeat && !typing(e)) { e.preventDefault(); press(); } });
    document.addEventListener('keyup', e => { if (e.code === 'Space' && pressed) { e.preventDefault(); release(); } });
  }
  el('present').onclick = () => el('list').classList.toggle('d-none');
  window.addEventListener('blur', () => { if (pressed) release(); });

  connect();
  render();
}
