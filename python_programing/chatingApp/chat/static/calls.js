(() => {
  let current = null, checking = false;
  const button = $('voice-call'), panel = $('call-panel'), remote = $('call-audio');
  function render(text) {
    panel.hidden = false;
    $('call-name').textContent = current?.name || 'Voice call';
    $('call-status').textContent = text;
    const incoming = current?.incoming && !current?.accepting && !current?.pc;
    $('call-accept').hidden = !incoming;
    $('call-end').textContent = incoming ? 'Decline' : 'End call';
    $('call-mute').hidden = !current?.stream;
    $('call-output').hidden = !current;
    $('call-mute').textContent = current?.muted ? 'Unmute' : 'Mute';
    $('call-mute').setAttribute('aria-pressed', String(!!current?.muted));
  }
  function dispose(call) {
    if (!call) return;
    clearTimeout(call.connectTimer); clearTimeout(call.disconnectTimer);
    call.pc?.close(); call.stream?.getTracks().forEach(track => track.stop());
    remote.pause(); remote.srcObject = null; $('call-play').hidden = true;
  }
  function cleanup() {
    const call = current; current = null; dispose(call); panel.hidden = true;
    $('call-output-dialog').close();
  }
  async function end(reason = 'Call ended') {
    const call = current; cleanup();
    if (call?.id && user) {
      try { await post(`/calls/${call.id}/end`, {}); } catch (_) { /* Server timeout also releases the call. */ }
    }
    if (reason) $('chat-error').textContent = reason;
  }
  window.GatherCalls = { cleanup, end, busy: () => !!current };
  function checkSupport() {
    if (!navigator.mediaDevices?.getUserMedia || !window.RTCPeerConnection) throw new Error('Voice calls need HTTPS (or localhost) and microphone support.');
  }
  async function setup(call) {
    checkSupport(); resetVoice();
    // Stop voice-message playback from feeding back into the microphone.
    document.querySelectorAll('audio').forEach(player => { if (player !== remote) player.pause(); });
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: { ideal: true },
        noiseSuppression: { ideal: true },
        autoGainControl: { ideal: true },
        channelCount: { ideal: 1 },
        sampleRate: { ideal: 48000 }
      },
      video: false
    });
    if (current !== call) { stream.getTracks().forEach(track => track.stop()); return false; }
    call.stream = stream;
    const settings = await api('/calls/config');
    if (current !== call) return false;
    const pc = new RTCPeerConnection(settings); call.pc = pc;
    stream.getAudioTracks().forEach(track => {
      if ('contentHint' in track) track.contentHint = 'speech';
      pc.addTrack(track, stream);
    });
    // Prefer Opus while retaining other codecs for browser interoperability.
    const codecs = window.RTCRtpReceiver?.getCapabilities?.('audio')?.codecs;
    if (codecs?.some(codec => codec.mimeType.toLowerCase() === 'audio/opus')) {
      const preferred = [...codecs.filter(codec => codec.mimeType.toLowerCase() === 'audio/opus'),
                         ...codecs.filter(codec => codec.mimeType.toLowerCase() !== 'audio/opus')];
      for (const transceiver of pc.getTransceivers()) {
        try { transceiver.setCodecPreferences?.(preferred); } catch (_) { /* Keep browser defaults when unsupported. */ }
      }
    }
    pc.ontrack = event => {
      if (current !== call) return;
      remote.srcObject = event.streams[0] || new MediaStream([event.track]);
      remote.play().catch(() => { $('call-play').hidden = false; });
    };
    pc.onconnectionstatechange = () => {
      if (current !== call) return;
      if (pc.connectionState === 'connected') {
        clearTimeout(call.connectTimer); clearTimeout(call.disconnectTimer); render('Connected');
      } else if (pc.connectionState === 'failed') {
        end('Could not connect the call. This network may need a TURN relay.');
      } else if (pc.connectionState === 'disconnected') {
        render('Reconnecting...');
        clearTimeout(call.disconnectTimer);
        call.disconnectTimer = setTimeout(() => { if (current === call) end('Call disconnected.'); }, 15000);
      }
    };
    return true;
  }
  async function gather(call, description) {
    await call.pc.setLocalDescription(description);
    if (call.pc.iceGatheringState !== 'complete') await new Promise(resolve => {
      const done = () => { clearTimeout(timeout); call.pc.removeEventListener('icegatheringstatechange', changed); resolve(); };
      const changed = () => { if (call.pc.iceGatheringState === 'complete') done(); };
      const timeout = setTimeout(done, 8000);
      call.pc.addEventListener('icegatheringstatechange', changed);
    });
    if (current !== call) throw new Error('Call cancelled.');
    return call.pc.localDescription.sdp;
  }
  function waitForConnection(call) {
    if (call.pc?.connectionState === 'connected') return;
    call.connectTimer = setTimeout(() => {
      if (current === call && call.pc?.connectionState !== 'connected') end('Call could not connect. A TURN relay may be needed on this network.');
    }, 30000);
  }
  button.onclick = async () => {
    if (current || !active?.private || !user) return;
    const call = { roomId: active.id, name: active.name }; current = call;
    $('chat-error').textContent = ''; render('Allow microphone access...');
    try {
      if (!await setup(call)) return;
      render('Preparing voice call...');
      const offer = await gather(call, await call.pc.createOffer());
      const result = await post('/calls', { room_id: call.roomId, offer });
      call.id = result.call.id;
      if (current !== call) { await post(`/calls/${call.id}/end`, {}); return; }
      render('Ringing...');
    } catch (error) {
      if (current === call) await end(error.name === 'NotAllowedError' ? 'Microphone access denied. Allow it in browser settings to call.' : error.message);
    }
  };
  $('call-accept').onclick = async () => {
    const call = current;
    if (!call || call.accepting) return;
    call.accepting = true; render('Allow microphone access...');
    try {
      if (!await setup(call)) return;
      await call.pc.setRemoteDescription({ type: 'offer', sdp: call.offer });
      render('Connecting...');
      const answer = await gather(call, await call.pc.createAnswer());
      const result = await post(`/calls/${call.id}/accept`, { answer });
      if (current !== call) return;
      if (result.call.status === 'ended') { cleanup(); $('chat-error').textContent = result.call.reason; return; }
      call.accepted = true; waitForConnection(call);
    } catch (error) {
      if (current === call) await end(error.name === 'NotAllowedError' ? 'Microphone access denied. The call was declined.' : error.message);
    }
  };
  $('call-end').onclick = () => end('');
  $('call-mute').onclick = () => {
    if (!current?.stream) return;
    current.muted = !current.muted;
    current.stream.getAudioTracks().forEach(track => track.enabled = !current.muted);
    $('call-mute').textContent = current.muted ? 'Unmute' : 'Mute';
    $('call-mute').setAttribute('aria-pressed', String(current.muted));
  };
  $('call-play').onclick = () => remote.play().then(() => $('call-play').hidden = true).catch(() => {});
  const outputSelect = $('call-output-device'), outputStatus = $('call-output-status');
  async function listOutputs(call) {
    const devices = await navigator.mediaDevices.enumerateDevices();
    if (current !== call || !$('call-output-dialog').open) return;
    outputSelect.replaceChildren();
    const defaultOption = element('option', '', 'System default (speaker or headset)');
    defaultOption.value = ''; outputSelect.append(defaultOption);
    for (const device of devices.filter(device => device.kind === 'audiooutput' && device.deviceId && device.deviceId !== 'default')) {
      const option = element('option', '', device.label || `Audio output ${outputSelect.options.length}`);
      option.value = device.deviceId; outputSelect.append(option);
    }
    outputSelect.value = remote.sinkId === 'default' ? '' : remote.sinkId || '';
    if (outputSelect.selectedIndex < 0) outputSelect.selectedIndex = 0;
    outputStatus.textContent = outputSelect.options.length > 1
      ? 'Select Speakers for loudspeaker playback, or your Jabra/headset. Only devices exposed by your browser appear here.'
      : 'Only the system default is available. Use your device’s sound or Bluetooth controls to change the output.';
  }
  $('call-output').onclick = async () => {
    const call = current;
    if (!call) return;
    $('call-output-dialog').showModal();
    const supported = typeof remote.setSinkId === 'function';
    outputSelect.disabled = !supported;
    $('call-output-apply').hidden = !supported;
    $('call-output-pick').hidden = !supported || typeof navigator.mediaDevices.selectAudioOutput !== 'function';
    outputSelect.replaceChildren();
    if (!supported) {
      outputStatus.textContent = 'This browser cannot switch the phone’s earpiece and loudspeaker from a webpage. Use your phone’s audio/Bluetooth controls if available, or connect earphones. Speakerphone mode cannot be forced here.';
      return;
    }
    outputStatus.textContent = 'Finding audio outputs...';
    $('call-output-apply').disabled = true;
    try { await listOutputs(call); }
    catch (_) { outputStatus.textContent = 'Could not list audio devices. Use your system sound settings or Choose device if available.'; }
    finally { $('call-output-apply').disabled = outputSelect.options.length === 0; }
  };
  async function switchOutput(deviceId, label, call) {
    if (current !== call || !call) return;
    await remote.setSinkId(deviceId);
    if (current !== call) return;
    outputStatus.textContent = `Audio output selected: ${label}.`;
    $('call-output').title = `Audio output: ${label}`;
    if (remote.srcObject) remote.play().catch(() => { $('call-play').hidden = false; });
  }
  function outputError(error) {
    outputStatus.textContent = error.name === 'NotAllowedError'
      ? 'Output selection was cancelled or permission was denied. Your previous output is unchanged.'
      : 'Could not switch output. Reconnect the device or choose a different output.';
  }
  $('call-output-apply').onclick = async () => {
    const call = current, selected = outputSelect.selectedOptions[0];
    if (!selected) return;
    $('call-output-apply').disabled = true;
    try { await switchOutput(selected.value, selected.textContent, call); }
    catch (error) { if (current === call) outputError(error); }
    finally { $('call-output-apply').disabled = false; }
  };
  $('call-output-pick').onclick = async () => {
    const call = current;
    try {
      const device = await navigator.mediaDevices.selectAudioOutput();
      if (current !== call) return;
      await switchOutput(device.deviceId, device.label || 'Chosen device', call);
    } catch (error) { if (current === call) outputError(error); }
  };
  $('call-output-close').onclick = () => $('call-output-dialog').close();
  async function tick() {
    button.hidden = !user || !active?.private;
    const person = people.find(person => person.username === active?.name);
    button.disabled = !!current || !person?.online;
    button.title = person?.online ? 'Start a voice call' : 'The other person needs to be online';
    if (checking || !user || (!current && document.hidden)) return;
    checking = true;
    try {
      if (current?.id) {
        const call = current;
        const result = await post(`/calls/${call.id}/poll`, {});
        if (current !== call) return;
        call.lastSignal = Date.now();
        if (result.call.status === 'ended') { cleanup(); $('chat-error').textContent = result.call.reason; return; }
        if (!call.incoming && result.call.answer && !call.answered) {
          call.answered = true;
          await call.pc.setRemoteDescription({ type: 'answer', sdp: result.call.answer });
          render('Connecting...'); waitForConnection(call);
        }
      } else if (!current) {
        const result = await api('/calls');
        if (result.call && !current && user) {
          current = { ...result.call, incoming: true, lastSignal: Date.now() };
          render('Incoming voice call');
        }
      }
    } catch (error) {
      if (current) {
        current.lastSignal ??= Date.now();
        if (error.message.includes('another tab') || error.message.includes('no longer available')) { cleanup(); $('chat-error').textContent = error.message; }
        else if (Date.now() - current.lastSignal > 20000) await end('Call ended because the server connection was lost.');
      }
    } finally { checking = false; }
  }
  setInterval(tick, 2000);
  window.addEventListener('pagehide', () => {
    if (current?.id && user) fetch(`/api/calls/${current.id}/end`, { method: 'POST', body: '{}', keepalive: true,
      headers: { 'Content-Type': 'application/json', 'X-Tab-ID': tabId, 'X-Browser-Token': browserToken, 'X-CSRF-Token': csrf } }).catch(() => {});
    cleanup();
  });
})();
