const $ = id => document.getElementById(id);
const avatars = { fox: '🦊', cat: '🐱', dog: '🐶', panda: '🐼', koala: '🐨', lion: '🦁', rabbit: '🐰', bear: '🐻', owl: '🦉', penguin: '🐧', frog: '🐸', unicorn: '🦄' };
const avatarIcon = key => avatars[key] || avatars.fox;
const tabId = Array.from(crypto.getRandomValues(new Uint8Array(32)), byte => byte.toString(16).padStart(2, '0')).join('');
let browserToken = '';
let privacyLocked = false;
let csrf = '', user = null, rooms = [], active = null, last = 0, first = 0;
let defaultRoomId = null;
let generation = 0, timer, dialogMode = 'room', polling = false, roomPoll = 0;
async function api(path, options = {}, retried = false) {
  if (privacyLocked) throw new Error('Chat locked. Please sign in again.');
  const response = await fetch('/api' + path, { ...options, headers: { ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }), 'X-CSRF-Token': csrf, 'X-Browser-Token': browserToken, 'X-Tab-ID': tabId }, credentials: 'same-origin' });
  const body = await response.json();
  if (privacyLocked) throw new Error('Chat locked. Please sign in again.');
  if (response.status === 403 && body.code === 'csrf_expired' && !retried) {
    const previousUser = user?.id;
    const fresh = await api('/session');
    csrf = fresh.csrf;
    if (previousUser && fresh.user?.id !== previousUser) {
      resetPhoto(); resetVoice(); releaseAudio(); window.GatherCalls?.cleanup(); user = null; generation++; clearTimeout(timer);
      $('app').hidden = true; $('auth').hidden = false;
      $('auth-error').textContent = 'Please sign in again. Your message draft is still here.';
      throw new Error('Please sign in again. Your message draft is still here.');
    }
    return api(path, options, true);
  }
  if (!response.ok) {
    if (response.status === 401 && user) { resetPhoto(); resetVoice(); releaseAudio(); window.GatherCalls?.cleanup(); user = null; generation++; clearTimeout(timer); $('app').hidden = true; $('auth').hidden = false; $('username').value = ''; $('password').value = ''; $('login-key').value = ''; browserToken = ''; }
    throw new Error(body.error || 'Something went wrong. Please try again.');
  }
  return body;
}
const post = (path, body) => api(path, { method: 'POST', body: JSON.stringify(body) });
function element(tag, className, text) { const node = document.createElement(tag); node.className = className; if (text !== undefined) node.textContent = text; return node; }
async function refreshRooms() {
  const result = await api('/rooms');
  rooms = result.rooms; defaultRoomId = result.default_room_id;
  $('room-list').replaceChildren(); $('direct-list').replaceChildren();
  rooms.forEach(room => {
    const button = element('button', active?.id === room.id ? 'active' : '');
    button.append(element('span', '', room.private ? '◉' : '#'), document.createTextNode(room.name));
    button.onclick = () => selectRoom(room);
    $(room.private ? 'direct-list' : 'room-list').append(button);
  });
}
function addMessages(messages, prepend = false) {
  const fragment = document.createDocumentFragment();
  messages.forEach(message => {
    if ($('messages').querySelector(`[data-id="${message.id}"]`)) return;
    const row = element('article', 'message-row' + (message.user_id === user.id ? ' mine' : ''));
    row.dataset.id = message.id; row.dataset.sender = message.user_id;
    const content = element('div', 'message-content');
    content.append(element('div', 'message-sender', message.user_id === user.id ? `You (${message.username})` : message.username));
    const meta = element('div', 'message-meta');
    const date = new Date(message.created_at);
    const time = element('time', '', date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
    time.dateTime = message.created_at; time.title = date.toLocaleString();
    meta.append(time);
    if (message.user_id === user.id) {
      const receipt = element('span', 'message-receipt', '✓ Sent');
      receipt.setAttribute('aria-label', 'Sent; not seen yet');
      meta.append(receipt);
    }
    if (message.reply_to_id) {
      const quote = element('blockquote', 'reply-quote', message.reply_body === null ? 'Original message unavailable' : `${message.reply_username}: ${message.reply_body}`);
      quote.dataset.replyId = message.reply_to_id; content.append(quote);
    }
    const bubble = element('button', 'bubble message-tap', message.body);
    bubble.type = 'button'; bubble.title = 'Reply to this message';
    bubble.setAttribute('aria-label', `Reply to ${message.username}: ${message.body}`);
    bubble.onclick = () => { setReply(message); $('message').focus(); };
    const replyButton = element('button', 'reply-action', 'Reply');
    replyButton.type = 'button'; replyButton.onclick = bubble.onclick; meta.append(replyButton);
    content.append(bubble);
    if (message.is_voice) {
      const roomId = active.id;
      const play = element('button', 'voice-load', 'Load voice message');
      play.type = 'button';
      play.onclick = async () => {
        play.disabled = true;
        try {
          const response = await fetch(`/api/rooms/${roomId}/messages/${message.id}/audio`, {
            headers: { 'X-Browser-Token': browserToken, 'X-Tab-ID': tabId }, credentials: 'same-origin'
          });
          if (!response.ok) throw new Error('Voice message unavailable. Please refresh or sign in again.');
          const blob = await response.blob();
          if (!row.isConnected) return;
          const player = element('audio', 'voice-player');
          player.controls = true; player.setAttribute('aria-label', `Voice message from ${message.username}`);
          row.audioUrl = URL.createObjectURL(blob); player.src = row.audioUrl;
          play.replaceWith(player);
          player.play().catch(() => {});
        } catch (error) { $('chat-error').textContent = error.message; play.disabled = false; }
      };
      content.append(play);
    }
    if (message.is_photo) attachPhoto(row, content, message, active.id);
    content.append(meta);
    attachReactions(row, content, meta, message, active.id);
    const avatar = element('span', 'avatar', avatarIcon(message.avatar));
    avatar.setAttribute('role', 'img');
    avatar.setAttribute('aria-label', message.username + ' avatar');
    row.append(avatar, content);
    fragment.append(row);
    last = Math.max(last, message.id); first = first ? Math.min(first, message.id) : message.id;
  });
  if (prepend) $('messages').prepend(fragment); else $('messages').append(fragment);
}
async function selectRoom(room) {
  stopTyping(); clearTypingDisplay();
  resetPhoto(); resetVoice(); releaseAudio();
  setReply(null);
  active = room; const version = ++generation; last = 0; first = 0;
  closeSidebar(); $('messages').replaceChildren(); $('older').hidden = true;
  $('chat-error').textContent = ''; $('room-title').textContent = room.name;
  $('room-icon').textContent = room.private ? '◉' : '#';
  $('room-description').textContent = room.private ? 'A private conversation between the two of you.' : 'A public room. Everyone can read and join in.';
  updatePresenceHeader();
  $('welcome-title').textContent = room.private ? `Your conversation with ${room.name}` : `Welcome to #${room.name}`;
  for (const nav of [$('room-list'), $('direct-list')]) for (const button of nav.children) button.classList.toggle('active', button.textContent.slice(1) === room.name && (nav.id === 'direct-list') === Boolean(room.private));
  try {
    const result = await api(`/rooms/${room.id}/messages`);
    if (version !== generation) return;
    addMessages(result.messages); applyExpirations(result); $('older').hidden = result.messages.length < 100;
    $('message-scroll').scrollTop = $('message-scroll').scrollHeight;
    $('connection').textContent = '● Connected';
  } catch (error) { if (version === generation) $('chat-error').textContent = error.message; }
}
async function poll() {
  if (polling || !user) return;
  polling = true;
  try {
    if (!document.hidden && active) {
      await refreshPresence();
      const version = generation;
      const result = await api(`/rooms/${active.id}/messages?after=${last}`);
      if (version === generation) {
        const scroll = $('message-scroll'); const nearBottom = scroll.scrollHeight - scroll.scrollTop - scroll.clientHeight < 120;
        addMessages(result.messages); applyExpirations(result); if (nearBottom) scroll.scrollTop = scroll.scrollHeight;
        $('connection').textContent = '● Connected';
      }
      if (++roomPoll % 5 === 0) await refreshRooms();
    }
  } catch (error) { $('connection').textContent = 'Reconnecting…'; }
  finally { polling = false; if (user) timer = setTimeout(poll, document.hidden ? 15000 : 3000); }
}
async function enterApp() {
  $('admin-personal').hidden = !user.is_admin;
  $('admin-remove').hidden = !user.is_admin;
  $('admin-create').hidden = !user.is_admin;
  $('admin-reset').hidden = !user.is_admin;
  await refreshPresence();
  $('sending-as').textContent = `Sending as ${user.username}`;
  $('auth').hidden = true; $('app').hidden = false; $('my-name').textContent = user.username; $('my-avatar').textContent = avatarIcon(user.avatar);
  $('selected-avatar-icon').textContent = avatarIcon(user.avatar);
  await refreshRooms(); await selectRoom(rooms.find(room => room.id === defaultRoomId) || rooms[0]); clearTimeout(timer); timer = setTimeout(poll, 3000);
}
$('auth-form').onsubmit = async event => {
  event.preventDefault(); $('auth-submit').disabled = true; $('auth-error').textContent = '';
  try { const credentials = $('admin-login-fields').hidden ? { login_key: $('login-key').value.trim() } : { username: $('username').value, password: $('password').value }; const result = await post('/login', credentials); csrf = result.csrf; browserToken = result.browser_token; user = result.user; $('password').value = ''; $('login-key').value = ''; await enterApp(); }
  catch (error) { $(user ? 'chat-error' : 'auth-error').textContent = error.message; }
  finally { $('auth-submit').disabled = false; }
};
$('chat-logout').onclick = $('logout').onclick = async () => {
  $('chat-logout').disabled = true; $('logout').disabled = true;
  try { await post('/logout', {}); resetPhoto(); resetVoice(); releaseAudio(); window.GatherCalls?.cleanup(); user = null; generation++; clearTimeout(timer); $('app').hidden = true; $('auth').hidden = false; $('username').value = ''; $('password').value = ''; $('login-key').value = ''; browserToken = ''; csrf = (await api('/session')).csrf; }
  catch (error) { $('chat-error').textContent = error.message; }
  finally { $('chat-logout').disabled = false; $('logout').disabled = false; }
};
$('composer').onsubmit = async event => {
  event.preventDefault(); const text = $('message').value.trim(); if (!text || !active || $('send').disabled) return;
  stopTyping();
  const reply = replying; const roomId = active.id; const draft = $('message').value; $('send').disabled = true; $('chat-error').textContent = '';
  try { await post(`/rooms/${roomId}/messages`, { body: text, reply_to_id: reply?.id || null }); if (active?.id === roomId && replying === reply) setReply(null); if (active?.id === roomId && $('message').value === draft) { $('message').value = ''; $('character-count').textContent = '0 / 2000'; } clearTimeout(timer); await poll(); }
  catch (error) { $('chat-error').textContent = error.message; }
  finally { $('send').disabled = false; $('message').focus(); }
};
$('message').onkeydown = event => { if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) { event.preventDefault(); $('composer').requestSubmit(); } };
$('message').oninput = () => {
  $('character-count').textContent = `${$('message').value.length} / 2000`;
  notifyTyping();
};
$('older').onclick = async () => {
  const version = generation; $('older').disabled = true;
  try { const result = await api(`/rooms/${active.id}/messages?before=${first}`); if (version !== generation) return; const scroll = $('message-scroll'); const oldHeight = scroll.scrollHeight; addMessages(result.messages, true); applyExpirations(result); scroll.scrollTop += scroll.scrollHeight - oldHeight; $('older').hidden = result.messages.length < 100; }
  catch (error) { $('chat-error').textContent = error.message; } finally { $('older').disabled = false; }
};
function openDialog(mode) {
  dialogMode = mode; $('dialog-title').textContent = mode === 'room' ? 'Create a room' : 'Start a conversation';
  $('dialog-description').textContent = mode === 'room' ? 'Public rooms are visible to everyone. Give yours a name.' : 'Enter the exact username of someone with a chat account.';
  $('dialog-label').textContent = mode === 'room' ? 'Room name' : 'Username'; $('dialog-input').value = ''; $('dialog-error').textContent = ''; $('create-dialog').showModal();
}
$('new-room').onclick = () => openDialog('room'); $('new-direct').onclick = () => openDialog('direct');
$('close-dialog').onclick = () => $('create-dialog').close();
$('create-form').onsubmit = async event => {
  event.preventDefault(); const button = event.submitter; button.disabled = true;
  try { const value = $('dialog-input').value; const result = await post(dialogMode === 'room' ? '/rooms' : '/direct', dialogMode === 'room' ? { name: value } : { username: value }); await refreshRooms(); $('create-dialog').close(); await selectRoom(rooms.find(room => room.id === result.id)); }
  catch (error) { $('dialog-error').textContent = error.message; } finally { button.disabled = false; }
};
$('menu').onclick = () => {
  const open = $('app').classList.toggle('show-sidebar');
  $('sidebar-backdrop').hidden = !open; $('menu').setAttribute('aria-expanded', String(open));
};
function closeSidebar() {
  $('app').classList.remove('show-sidebar'); $('sidebar-backdrop').hidden = true;
  $('menu').setAttribute('aria-expanded', 'false');
}
$('sidebar-backdrop').onclick = closeSidebar;
document.addEventListener('keydown', event => { if (event.key === 'Escape') closeSidebar(); });
async function boot() { try { const result = await api('/session'); csrf = result.csrf; user = result.user; if (user) await enterApp(); } catch (error) { $(user ? 'chat-error' : 'auth-error').textContent = 'Unable to connect. Refresh to retry. ' + error.message; } }
boot();

function applyExpirations(result) {
  if (result.typing) showTyping(result.typing, result.server_time);
  if (result.reactions) {
    for (const row of $('messages').children) row.updateReactions?.(result.reactions[row.dataset.id] || []);
  }
  for (const id of result.unavailable_replies || []) {
    const quote = $('messages').querySelector(`[data-id="${id}"] .reply-quote`);
    if (quote) quote.textContent = 'Original message unavailable';
  }
  for (const expiry of result.expirations || []) {
    const row = $('messages').querySelector(`[data-id="${expiry.id}"]`);
    if (row) {
      row.dataset.expires = Date.now() + Math.max(0, expiry.expires_at - result.server_time) * 1000;
      const receipt = row.querySelector('.message-receipt');
      if (receipt) {
        receipt.textContent = '✓✓ Seen';
        receipt.classList.add('is-seen');
        receipt.setAttribute('aria-label', 'Seen by another person');
        receipt.title = 'Seen by another person. Deletes 5 minutes after first seen.';
      }
    }
  }
}
let reading = false;
setInterval(async () => {
  for (const row of $('messages').children) {
    if (row.dataset.expires && Number(row.dataset.expires) <= Date.now()) removeMessage(row);
  }
  if (!user || !active || !document.hasFocus() || reading) return;
  const bounds = $('message-scroll').getBoundingClientRect();
  const visible = [...$('messages').children].filter(row => {
    const rect = row.getBoundingClientRect();
    return row.dataset.photoLoading !== '1' && Number(row.dataset.sender) !== user.id && !row.dataset.seen && rect.bottom > bounds.top && rect.top < bounds.bottom;
  }).slice(0, 100);
  if (!visible.length) return;
  reading = true;
  try {
    await post(`/rooms/${active.id}/seen`, { ids: visible.map(row => Number(row.dataset.id)) });
    visible.forEach(row => { row.dataset.seen = '1'; if (!row.dataset.expires) row.dataset.expires = Date.now() + 300000; });
  } catch (_) { /* Retry visible receipts on the next tick. */ }
  finally { reading = false; }
}, 1000);
window.addEventListener('pageshow', event => {
  $('username').value = ''; $('password').value = ''; $('login-key').value = '';
  if (privacyLocked) leaveMobileChat();
  else if (event.persisted) location.reload();
});

let syncing = false;
setInterval(async () => {
  if (!user || !active || syncing) return;
  syncing = true;
  const version = generation, roomId = active.id;
  const rows = [...$('messages').children];
  try {
    for (let i = 0; i < rows.length; i += 100) {
      const batch = rows.slice(i, i + 100);
      const result = await post(`/rooms/${roomId}/state`, { ids: batch.map(row => Number(row.dataset.id)) });
      if (version !== generation) return;
      batch.forEach(row => { if (!result.alive.includes(Number(row.dataset.id))) removeMessage(row); });
      applyExpirations(result);
    }
  } catch (_) { /* Reconcile again when the connection returns. */ }
  finally { syncing = false; }
}, 3000);

$('clear-chat').onclick = async () => {
  if (!active || !user) return;
  const room = active;
  if (!window.confirm(`Clear all messages in ${room.name}? This permanently deletes the chat history for everyone in this conversation and cannot be undone.`)) return;
  $('clear-chat').disabled = true;
  $('chat-error').textContent = '';
  try {
    await api(`/rooms/${room.id}/messages`, { method: 'DELETE' });
    if (active?.id === room.id) await selectRoom(room);
  } catch (error) { $('chat-error').textContent = error.message; }
  finally { $('clear-chat').disabled = false; }
};

$('choose-avatar').onclick = () => {
  $('avatar-error').textContent = '';
  $('avatar-options').replaceChildren();
  for (const [key, icon] of Object.entries(avatars)) {
    const button = element('button', 'avatar-option', icon);
    button.type = 'button'; button.setAttribute('aria-label', key);
    button.setAttribute('aria-pressed', String(user.avatar === key));
    button.onclick = async () => {
      const buttons = [...$('avatar-options').children];
      buttons.forEach(item => item.disabled = true);
      try {
        await post('/avatar', { avatar: key });
        user.avatar = key;
        $('my-avatar').textContent = icon; $('selected-avatar-icon').textContent = icon;
        for (const row of $('messages').children) {
          if (Number(row.dataset.sender) === user.id) row.querySelector('.avatar').textContent = icon;
        }
        $('avatar-dialog').close();
      } catch (error) { $('avatar-error').textContent = error.message; }
      finally { buttons.forEach(item => item.disabled = false); }
    };
    $('avatar-options').append(button);
  }
  $('avatar-dialog').showModal();
};
$('close-avatar').onclick = () => $('avatar-dialog').close();

$('admin-reset').onclick = () => {
  $('admin-form').reset(); $('reset-status').textContent = '';
  $('admin-dialog').showModal();
};
$('close-admin').onclick = () => $('admin-dialog').close();
$('admin-dialog').addEventListener('close', () => { $('admin-form').reset(); $('reset-status').replaceChildren(); });
$('admin-form').onsubmit = async event => {
  event.preventDefault(); $('reset-submit').disabled = true; $('reset-status').textContent = '';
  try {
    const result = await post('/admin/reset-key', {
      username: $('reset-username').value,
      login_key: $('reset-key').value,
      admin_password: $('admin-password').value
    });
    $('admin-form').reset();
    showIssuedKey($('reset-status'), result);
  } catch (error) { $('reset-status').textContent = error.message; }
  finally { $('admin-password').value = ''; $('reset-submit').disabled = false; }
};

let replying = null, people = [];
function setReply(message) {
  replying = message; $('reply-preview').hidden = !message;
  $('reply-text').textContent = message ? `Replying to ${message.username}: ${message.body}` : '';
}
$('cancel-reply').onclick = () => setReply(null);
function removeMessage(row) {
  const id = Number(row.dataset.id);
  if (replying?.id === id) setReply(null);
  for (const quote of document.querySelectorAll(`[data-reply-id="${id}"]`)) quote.textContent = 'Original message unavailable';
  releaseAudio(row);
  row.remove();
}
function statusText(person) {
  return person.online ? 'Online' : person.last_seen ? `Last seen ${new Date(person.last_seen * 1000).toLocaleString()}` : 'Offline';
}
function updatePresenceHeader() {
  if (!$('header-presence')) {
    const status = element('span', '', '');
    status.id = 'header-presence'; status.setAttribute('role', 'status');
    $('connection').before(status);
  }
  if (!active) return;
  if (!active.private) {
    const others = people.filter(person => person.id !== user?.id);
    $('header-presence').replaceChildren(...others.map(person => element('span', person.online ? 'person-online' : '', `${person.username}: ${statusText(person)}`)));
    if (!others.length) $('header-presence').textContent = 'No other chat accounts';
    $('room-description').textContent = 'A public room. Everyone can read and join in.';
    return;
  }
  const person = people.find(person => person.username === active.name);
  $('header-presence').textContent = person ? `${person.username}: ${statusText(person)}` : '';
  $('room-description').textContent = 'A private conversation between the two of you.';
}
async function refreshPresence() {
  const result = await post('/presence', {});
  if (!user) return;
  people = result.users; $('people-list').replaceChildren();
  for (const person of people.filter(person => person.id !== user.id)) {
    const row = element('div', 'person');
    row.append(element('strong', '', person.username), element('small', person.online ? 'person-online' : '', statusText(person)));
    $('people-list').append(row);
  }
  updatePresenceHeader();
}
document.addEventListener('visibilitychange', () => {
  if (!document.hidden && user) { clearTimeout(timer); poll(); }
});
$('admin-create').onclick = () => {
  $('account-form').reset(); $('account-status').textContent = ''; $('account-dialog').showModal();
};
$('close-account').onclick = () => $('account-dialog').close();
$('account-dialog').addEventListener('close', () => { $('account-form').reset(); $('account-status').replaceChildren(); });
$('account-form').onsubmit = async event => {
  event.preventDefault(); $('account-submit').disabled = true; $('account-status').textContent = '';
  try {
    const result = await post('/admin/users', { username: $('account-username').value, login_key: $('account-key').value });
    $('account-form').reset(); showIssuedKey($('account-status'), result);
    await refreshPresence().catch(() => {});
  } catch (error) { $('account-status').textContent = error.message; }
  finally { $('account-submit').disabled = false; }
};

$('admin-remove').onclick = async () => {
  $('remove-form').reset(); $('remove-status').textContent = '';
  $('remove-user').replaceChildren(); $('remove-submit').disabled = true;
  $('remove-dialog').showModal();
  try {
    const result = await api('/admin/users');
    for (const account of result.users) {
      const option = element('option', '', account.username); option.value = account.id;
      $('remove-user').append(option);
    }
    $('remove-submit').disabled = !result.users.length;
    if (!result.users.length) $('remove-status').textContent = 'No users available to remove.';
  } catch (error) { $('remove-status').textContent = error.message; }
};
$('close-remove').onclick = () => $('remove-dialog').close();
$('remove-dialog').addEventListener('close', () => $('remove-form').reset());
$('remove-form').onsubmit = async event => {
  event.preventDefault();
  const selected = $('remove-user').selectedOptions[0];
  if (!selected || !confirm(`Remove ${selected.textContent}? They will lose access to all chats.`)) return;
  $('remove-submit').disabled = true;
  try {
    await api(`/admin/users/${selected.value}`, { method: 'DELETE', body: JSON.stringify({ admin_password: $('remove-password').value }) });
    selected.remove(); $('remove-status').textContent = 'User removed. Their existing sessions can no longer access chats.';
    await refreshPresence();
  } catch (error) { $('remove-status').textContent = error.message; }
  finally { $('remove-password').value = ''; $('remove-submit').disabled = !$('remove-user').options.length; }
};

$('admin-personal').onclick = async () => {
  $('personal-status').textContent = ''; $('personal-submit').disabled = true;
  $('personal-first').replaceChildren(); $('personal-second').replaceChildren();
  $('personal-dialog').showModal();
  try {
    const result = await api('/admin/users');
    for (const account of result.users) {
      for (const id of ['personal-first', 'personal-second']) {
        const option = element('option', '', account.username); option.value = account.id; $(id).append(option);
      }
    }
    if (result.users.length >= 2) { $('personal-second').selectedIndex = 1; $('personal-submit').disabled = false; }
    else $('personal-status').textContent = 'Create at least two regular user accounts first.';
  } catch (error) { $('personal-status').textContent = error.message; }
};
$('close-personal').onclick = () => $('personal-dialog').close();
$('personal-form').onsubmit = async event => {
  event.preventDefault(); $('personal-submit').disabled = true;
  try {
    await post('/admin/personal-chat', { user_ids: [Number($('personal-first').value), Number($('personal-second').value)] });
    $('personal-status').textContent = 'Personal chat ready for both users. It will open by default at their next sign-in.';
  } catch (error) { $('personal-status').textContent = error.message; }
  finally { $('personal-submit').disabled = false; }
};

const mobileLayout = matchMedia('(max-width: 760px)');
const mobileActionIds = ['choose-avatar', 'admin-personal', 'admin-create', 'admin-remove', 'admin-reset', 'clear-chat'];
const actionPositions = mobileActionIds.map(id => {
  const node = $(id), anchor = document.createComment(id);
  node.before(anchor);
  node.addEventListener('click', () => $('mobile-actions-dialog').close());
  return { node, anchor };
});
function applyMobileLayout() {
  $('mobile-actions-dialog').close(); closeSidebar();
  for (const { node, anchor } of actionPositions) {
    if (mobileLayout.matches) $('mobile-actions-list').append(node);
    else anchor.after(node);
  }
}
mobileLayout.addEventListener('change', applyMobileLayout);
applyMobileLayout();
$('mobile-actions').onclick = () => $('mobile-actions-dialog').showModal();
$('close-mobile-actions').onclick = () => $('mobile-actions-dialog').close();
$('mobile-signout').onclick = () => { $('mobile-actions-dialog').close(); $('chat-logout').click(); };


// Keep microphone streams and temporary audio URLs scoped to this conversation.
let voice = null;
function releaseAudio(root = $('messages')) {
  for (const row of [root, ...root.querySelectorAll('[data-id]')]) {
    if (row.photoUrl) { URL.revokeObjectURL(row.photoUrl); delete row.photoUrl; }
    row.querySelectorAll('.chat-photo').forEach(image => image.removeAttribute('src'));
  }
  for (const player of root.querySelectorAll('audio')) { player.pause(); player.removeAttribute('src'); player.load(); }
  for (const row of [root, ...root.querySelectorAll('[data-id]')]) {
    if (row.audioUrl) { URL.revokeObjectURL(row.audioUrl); delete row.audioUrl; }
  }
}
function resetVoice() {
  const previous = voice; voice = null;
  if (previous) {
    clearInterval(previous.timer);
    if (previous.recorder?.state !== 'inactive') previous.recorder?.stop();
    previous.stream?.getTracks().forEach(track => track.stop());
    if (previous.url) URL.revokeObjectURL(previous.url);
  }
  $('voice-preview').pause(); $('voice-preview').removeAttribute('src'); $('voice-preview').load();
  $('voice-panel').hidden = true; $('voice-record').disabled = false;
}
$('voice-record').onclick = async () => {
  if (!active || voice) return;
  if (window.GatherCalls?.busy()) { $('chat-error').textContent = 'End the voice call before recording a voice message.'; return; }
  $('chat-error').textContent = '';
  if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
    $('chat-error').textContent = 'Voice recording needs HTTPS (or localhost) and a browser with microphone recording support.'; return;
  }
  const current = { roomId: active.id, replyId: replying?.id }; voice = current;
  $('voice-record').disabled = true;
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    if (voice !== current) { stream.getTracks().forEach(track => track.stop()); return; }
    current.stream = stream;
    const mimeType = ['audio/webm;codecs=opus', 'audio/mp4', 'audio/ogg;codecs=opus'].find(type => MediaRecorder.isTypeSupported(type));
    if (!mimeType) throw new Error('This browser cannot record a supported audio format.');
    const recorder = new MediaRecorder(stream, { mimeType }); current.recorder = recorder;
    const chunks = []; let size = 0;
    recorder.ondataavailable = event => {
      if (event.data.size) { chunks.push(event.data); size += event.data.size; }
      if (size > 5 * 1024 * 1024 && voice === current) { resetVoice(); $('chat-error').textContent = 'Recording is too large. Please record a shorter message.'; }
    };
    recorder.onerror = () => { if (voice === current) { resetVoice(); $('chat-error').textContent = 'Recording failed. Please try again.'; } };
    recorder.onstop = () => {
      stream.getTracks().forEach(track => track.stop()); clearInterval(current.timer);
      if (voice !== current) return;
      current.blob = new Blob(chunks, { type: recorder.mimeType });
      if (!current.blob.size) { resetVoice(); $('chat-error').textContent = 'No audio recorded. Please try again.'; return; }
      current.url = URL.createObjectURL(current.blob);
      $('voice-preview').src = current.url; $('voice-preview').hidden = false;
      $('voice-stop').hidden = true; $('voice-send').hidden = false; $('voice-send').disabled = false;
      $('voice-cancel').disabled = false; $('voice-status').textContent = 'Preview your voice message';
    };
    $('voice-panel').hidden = false; $('voice-preview').hidden = true;
    $('voice-stop').hidden = false; $('voice-send').hidden = true; $('voice-cancel').disabled = false;
    recorder.start(1000); const started = Date.now();
    const tick = () => {
      const seconds = Math.floor((Date.now() - started) / 1000);
      $('voice-status').textContent = `Recording ${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')} / 2:00`;
      if (seconds >= 120 && recorder.state === 'recording') recorder.stop();
    };
    tick(); current.timer = setInterval(tick, 250);
  } catch (error) {
    if (voice !== current) return;
    resetVoice(); $('chat-error').textContent = error.name === 'NotAllowedError' ? 'Microphone access was denied. Allow microphone access in your browser and try again.' : error.message;
  }
};
$('voice-stop').onclick = () => { if (voice?.recorder?.state === 'recording') voice.recorder.stop(); };
$('voice-cancel').onclick = resetVoice;
$('voice-send').onclick = async () => {
  const current = voice;
  if (!current?.blob || $('voice-send').disabled) return;
  $('voice-send').disabled = true; $('voice-cancel').disabled = true;
  $('voice-status').textContent = 'Sending voice message?';
  const form = new FormData(); form.append('audio', current.blob, 'voice');
  if (current.replyId) form.append('reply_to_id', current.replyId);
  try {
    await api(`/rooms/${current.roomId}/messages`, { method: 'POST', body: form });
    if (voice === current) { resetVoice(); if (replying?.id === current.replyId) setReply(null); }
    clearTimeout(timer); await poll();
  } catch (error) {
    if (voice === current) { $('voice-send').disabled = false; $('voice-cancel').disabled = false; $('voice-status').textContent = 'Unable to send. Your recording is ready to retry.'; }
    $('chat-error').textContent = error.message;
  }
};
window.addEventListener('pagehide', () => { resetPhoto(); resetVoice(); releaseAudio(); });

let photoDraft = null;
function stopPhotoCamera(draft = photoDraft) {
  if (draft) draft.revision++;
  draft?.stream?.getTracks().forEach(track => track.stop());
  if (draft) draft.stream = null;
  $('photo-video').srcObject = null; $('photo-video').hidden = true; $('photo-shutter').hidden = true;
}
function resetPhoto() {
  stopPhotoCamera();
  if (photoDraft?.url) URL.revokeObjectURL(photoDraft.url);
  photoDraft = null;
  $('photo-camera').disabled = false; $('photo-gallery').disabled = false;
  $('photo-preview').removeAttribute('src'); $('photo-preview').hidden = true;
  $('photo-caption').value = ''; $('photo-file').value = ''; $('photo-capture').value = '';
  $('photo-send').disabled = true; $('photo-dialog').close();
}
$('photo-add').onclick = () => {
  if (!active || !user) return;
  resetPhoto();
  photoDraft = { roomId: active.id, replyId: replying?.id, revision: 0 };
  $('photo-destination').textContent = `To ${active.name}`;
  $('photo-status').textContent = 'Choose a photo or take a new one. One photo per message.';
  $('photo-dialog').showModal();
};
$('photo-close').onclick = resetPhoto;
$('photo-discard').onclick = resetPhoto;
$('photo-dialog').addEventListener('close', () => { if (photoDraft) resetPhoto(); });
$('photo-gallery').onclick = () => { stopPhotoCamera(); $('photo-file').click(); };
async function preparePhoto(file) {
  const draft = photoDraft;
  if (!draft || !file) return;
  const revision = ++draft.revision;
  $('photo-send').disabled = true;
  draft.blob = null;
  if (draft.url) { URL.revokeObjectURL(draft.url); draft.url = null; }
  $('photo-preview').hidden = true; $('photo-preview').removeAttribute('src');
  if (file.size > 20 * 1024 * 1024) { $('photo-status').textContent = 'Choose a photo smaller than 20 MB.'; return; }
  $('photo-status').textContent = 'Preparing photo...';
  const url = URL.createObjectURL(file);
  try {
    const image = new Image(); image.src = url; await image.decode();
    if (photoDraft !== draft || revision !== draft.revision) return;
    if (image.naturalWidth * image.naturalHeight > 50_000_000) throw new Error('Choose a photo below 50 megapixels.');
    const scale = Math.min(1, 1600 / Math.max(image.naturalWidth, image.naturalHeight));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(image.naturalWidth * scale)); canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
    const context = canvas.getContext('2d'); context.fillStyle = '#fff'; context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(image, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.85));
    if (photoDraft !== draft || revision !== draft.revision) return;
    if (!blob || blob.size > 5 * 1024 * 1024) throw new Error('Photo is too large to send. Choose a smaller photo.');
    draft.blob = blob; draft.url = URL.createObjectURL(blob);
    $('photo-preview').src = draft.url; $('photo-preview').hidden = false;
    $('photo-send').disabled = false; $('photo-status').textContent = 'Ready to send. You can add a caption or choose another photo.';
  } catch (error) {
    if (photoDraft === draft && revision === draft.revision) $('photo-status').textContent = error.name === 'EncodingError' ? 'This image format is not supported by your browser. Choose JPEG, PNG, or WebP, or take a new photo.' : error.message;
  } finally { URL.revokeObjectURL(url); }
}
for (const id of ['photo-file', 'photo-capture']) {
  $(id).onchange = () => { const file = $(id).files[0]; $(id).value = ''; preparePhoto(file); };
}
$('photo-camera').onclick = async () => {
  const draft = photoDraft;
  if (!draft) return;
  if (matchMedia('(pointer: coarse)').matches) { $('photo-capture').click(); return; }
  if (!navigator.mediaDevices?.getUserMedia) { $('photo-status').textContent = 'Camera access needs HTTPS or localhost. You can still choose a photo from files.'; return; }
  stopPhotoCamera();
  const revision = ++draft.revision;
  $('photo-send').disabled = true; $('photo-status').textContent = 'Allow camera access to take a photo.';
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment', width: { ideal: 1600 } }, audio: false });
    if (photoDraft !== draft || draft.revision !== revision) { stream.getTracks().forEach(track => track.stop()); return; }
    draft.stream = stream; $('photo-video').srcObject = stream; $('photo-video').hidden = false;
    $('photo-preview').hidden = true; $('photo-shutter').hidden = false;
    $('photo-status').textContent = 'Frame your photo, then select Capture photo.';
  } catch (error) {
    if (photoDraft === draft && revision === draft.revision) {
      $('photo-status').textContent = 'Camera unavailable or permission denied. Allow camera access, or choose Gallery / files.';
      $('photo-send').disabled = !draft.blob;
    }
  }
};
$('photo-shutter').onclick = async () => {
  const draft = photoDraft, video = $('photo-video');
  if (!draft || !video.videoWidth) return;
  const canvas = document.createElement('canvas'); canvas.width = video.videoWidth; canvas.height = video.videoHeight;
  canvas.getContext('2d').drawImage(video, 0, 0);
  const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.85));
  if (photoDraft !== draft) return;
  stopPhotoCamera(); if (blob) await preparePhoto(blob);
};
$('photo-send').onclick = async () => {
  const draft = photoDraft;
  if (!draft?.blob || $('photo-send').disabled) return;
  $('photo-send').disabled = true; $('photo-status').textContent = 'Sending photo...';
  $('photo-camera').disabled = true; $('photo-gallery').disabled = true;
  const form = new FormData(); form.append('photo', draft.blob, 'photo.jpg');
  form.append('caption', $('photo-caption').value);
  if (draft.replyId) form.append('reply_to_id', draft.replyId);
  try {
    await api(`/rooms/${draft.roomId}/photos`, { method: 'POST', body: form });
    if (photoDraft === draft) { resetPhoto(); if (replying?.id === draft.replyId) setReply(null); }
    clearTimeout(timer); await poll();
  } catch (error) {
    if (photoDraft === draft) { $('photo-send').disabled = false; $('photo-camera').disabled = false; $('photo-gallery').disabled = false; $('photo-status').textContent = error.message + ' Your photo is ready to retry.'; }
  }
};
function attachPhoto(row, content, message, roomId) {
  row.dataset.photoLoading = '1';
  const load = element('button', 'photo-retry', 'Loading photo...'); load.type = 'button';
  content.append(load);
  const version = generation;
  load.onclick = async () => {
    load.disabled = true;
    try {
      const response = await fetch(`/api/rooms/${roomId}/messages/${message.id}/photo`, {
        headers: { 'X-Browser-Token': browserToken, 'X-Tab-ID': tabId }, credentials: 'same-origin'
      });
      if (!response.ok) throw new Error('Photo unavailable. Tap to retry.');
      const blob = await response.blob();
      if (generation !== version || !row.isConnected || !user) return;
      const image = element('img', 'chat-photo'); image.alt = message.body === 'Photo' ? `Photo from ${message.username}` : message.body;
      if (row.photoUrl) URL.revokeObjectURL(row.photoUrl);
      row.photoUrl = URL.createObjectURL(blob); image.src = row.photoUrl;
      await image.decode();
      if (generation !== version || !row.isConnected || !user) return;
      const scroll = $('message-scroll'), nearBottom = scroll.scrollHeight - scroll.scrollTop - scroll.clientHeight < 160;
      load.replaceWith(image); row.dataset.photoLoading = '0';
      if (nearBottom) scroll.scrollTop = scroll.scrollHeight;
    } catch (error) { load.textContent = error.message; load.disabled = false; }
  };
  load.click();
}


function attachReactions(row, content, meta, message, roomId) {
  const choices = [['\u2764\ufe0f', 'Heart'], ['\u{1f44d}', 'Like'], ['\u{1f602}', 'Laugh'], ['\u{1f62e}', 'Surprised'], ['\u{1f622}', 'Sad'], ['\u{1f64f}', 'Thanks']];
  const trigger = element('button', 'reply-action', 'React');
  trigger.type = 'button'; trigger.setAttribute('aria-expanded', 'false');
  trigger.setAttribute('aria-label', `React to message from ${message.username}`);
  const picker = element('div', 'reaction-picker'); picker.hidden = true;
  picker.setAttribute('role', 'group'); picker.setAttribute('aria-label', 'Choose a reaction');
  const badges = element('div', 'reaction-badges');
  let busy = false;
  const close = () => { picker.hidden = true; trigger.setAttribute('aria-expanded', 'false'); };
  const react = async emoji => {
    if (busy) return;
    busy = true; close();
    const version = generation;
    try {
      const result = await post(`/rooms/${roomId}/messages/${message.id}/reaction`, { emoji });
      if (version === generation && row.isConnected) applyExpirations(result);
    } catch (error) { if (version === generation) $('chat-error').textContent = error.message; }
    finally { busy = false; }
  };
  for (const [emoji, label] of choices) {
    const button = element('button', 'reaction-choice', emoji); button.type = 'button';
    button.title = label; button.setAttribute('aria-label', label);
    button.onclick = () => { react(emoji); trigger.focus(); }; picker.append(button);
  }
  trigger.onclick = () => {
    picker.hidden = !picker.hidden; trigger.setAttribute('aria-expanded', String(!picker.hidden));
    if (!picker.hidden) picker.firstElementChild.focus();
  };
  picker.onkeydown = event => { if (event.key === 'Escape') { close(); trigger.focus(); } };
  picker.addEventListener('focusout', event => { if (!picker.contains(event.relatedTarget) && event.relatedTarget !== trigger) close(); });
  row.updateReactions = reactions => {
    const signature = JSON.stringify(reactions);
    if (badges.dataset.state === signature) return;
    badges.dataset.state = signature; badges.replaceChildren();
    for (const reaction of reactions) {
      const label = choices.find(choice => choice[0] === reaction.emoji)?.[1] || reaction.emoji;
      const button = element('button', 'reaction-badge' + (reaction.mine ? ' selected' : ''), `${reaction.emoji} ${reaction.count}`);
      button.type = 'button'; button.setAttribute('aria-pressed', String(reaction.mine));
      button.title = `${label}: ${reaction.count}${reaction.mine ? ' (yours; tap to remove)' : ' (tap to react)'}`;
      button.setAttribute('aria-label', button.title); button.onclick = () => react(reaction.emoji); badges.append(button);
    }
    for (const button of picker.children) button.setAttribute('aria-pressed', String(reactions.some(r => r.mine && r.emoji === button.textContent)));
  };
  meta.append(trigger); content.append(picker, badges);
}


let typingRoom = null, typingSentAt = 0, typingHeartbeat, typingDisplayTimer;
let typingQueue = Promise.resolve();
function sendTyping(roomId, value) {
  const version = generation, token = browserToken;
  typingQueue = typingQueue.catch(() => {}).then(() => {
    if (!user || token !== browserToken || (value && version !== generation)) return;
    return post(`/rooms/${roomId}/typing`, { typing: value });
  }).catch(() => { /* A missing heartbeat expires automatically. */ });
}
function stopTyping() {
  clearInterval(typingHeartbeat); typingHeartbeat = null;
  if (typingRoom !== null) sendTyping(typingRoom, false);
  typingRoom = null; typingSentAt = 0;
}
function notifyTyping() {
  if (!user || !active || !$('message').value.trim()) { stopTyping(); return; }
  if (typingRoom !== active.id) stopTyping();
  typingRoom = active.id;
  if (Date.now() - typingSentAt >= 2000) { sendTyping(typingRoom, true); typingSentAt = Date.now(); }
  if (!typingHeartbeat) typingHeartbeat = setInterval(notifyTyping, 2000);
}
function clearTypingDisplay() {
  clearTimeout(typingDisplayTimer);
  const status = $('typing-status'); if (status) { status.textContent = ''; status.hidden = true; }
}
function showTyping(people, serverTime) {
  clearTypingDisplay();
  let status = $('typing-status');
  if (!status) {
    status = element('p', 'typing-status'); status.id = 'typing-status';
    status.setAttribute('role', 'status'); $('room-description').after(status);
  }
  const received = Date.now();
  const render = () => {
    const remaining = people.filter(person => person.expires_at > serverTime + (Date.now() - received) / 1000);
    status.hidden = !remaining.length;
    status.textContent = remaining.length ? `${remaining.map(person => person.username).join(', ')} ${remaining.length === 1 ? 'is' : 'are'} typing...` : '';
    if (remaining.length) typingDisplayTimer = setTimeout(render, 500);
  };
  render();
}
// An unsent draft stays active even when the input loses focus.
document.addEventListener('visibilitychange', () => {
  if (!document.hidden && typingRoom !== null) notifyTyping();
});
window.addEventListener('pagehide', stopTyping);


$('login-mode').onclick = () => {
  const admin = $('admin-login-fields').hidden;
  $('admin-login-fields').hidden = !admin;
  for (const id of ['username', 'password']) { $(id).disabled = !admin; $(id).required = admin; $(id).value = ''; }
  $('login-key').disabled = admin; $('login-key').required = !admin; $('login-key').hidden = admin; $('login-key').value = '';
  document.querySelector('label[for="login-key"]').hidden = admin;
  $('login-mode').textContent = admin ? 'User key sign-in' : 'Administrator sign-in';
  $('login-mode').setAttribute('aria-pressed', String(admin));
  $('auth-description').textContent = admin ? 'Sign in with your administrator username and password.' : 'Paste your unique key to join the conversation.';
  $('auth-error').textContent = ''; $(admin ? 'username' : 'login-key').focus();
};
function showIssuedKey(container, result) {
  container.replaceChildren();
  container.append(element('span', '', `Key for ${result.username}. Save and share this key privately.`));
  const key = element('input', 'issued-key'); key.type = 'text'; key.readOnly = true; key.value = result.login_key;
  key.setAttribute('aria-label', `Sign-in key for ${result.username}`); key.autocomplete = 'off'; key.spellcheck = false;
  key.onclick = () => key.select();
  const copy = element('button', 'text-button', 'Copy key'); copy.type = 'button';
  copy.onclick = async () => {
    try { await navigator.clipboard.writeText(key.value); copy.textContent = 'Copied'; }
    catch (_) { key.focus(); key.select(); copy.textContent = 'Select and copy the key above'; }
  };
  container.append(key, copy);
}


// Mobile browsers can restore a live tab after closing the browser UI.
// Cover it synchronously, forget local credentials, and leave a blank tab.
function lockMobileChat() {
  if (privacyLocked || !matchMedia('(max-width: 760px), (pointer: coarse)').matches) return;
  privacyLocked = true;
  document.documentElement.classList.add('privacy-locked');
  document.title = '';
  const token = browserToken;
  user = null; browserToken = ''; generation++; clearTimeout(timer);
  if (token) fetch('/api/logout', {
    method: 'POST', body: '{}', keepalive: true, credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-Tab-ID': tabId, 'X-Browser-Token': token, 'X-CSRF-Token': csrf }
  }).catch(() => {});
  $('app').hidden = true; $('auth').hidden = true;
  stopTyping(); clearTypingDisplay();
  window.GatherCalls?.cleanup(); resetPhoto(); resetVoice(); releaseAudio();
  for (const dialog of document.querySelectorAll('dialog[open]')) dialog.close();
  for (const field of document.querySelectorAll('input, textarea')) field.value = '';
  $('messages').replaceChildren(); $('account-status').replaceChildren(); $('reset-status').replaceChildren();
  replying = null; active = null; people = []; rooms = [];
}
function leaveMobileChat() {
  if (privacyLocked) location.replace('about:blank');
}
document.addEventListener('visibilitychange', () => {
  try { if (document.hidden) lockMobileChat(); }
  finally { leaveMobileChat(); }
});
window.addEventListener('pagehide', lockMobileChat);
window.addEventListener('pageshow', () => { leaveMobileChat(); });
