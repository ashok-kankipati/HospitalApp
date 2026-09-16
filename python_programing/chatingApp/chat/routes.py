import hashlib
import hmac
import re
import secrets
import sqlite3
import time
from functools import wraps
from flask import Blueprint, Response, abort, current_app, jsonify, render_template, request, session
from werkzeug.security import check_password_hash, generate_password_hash
from .db import get_db

bp = Blueprint('chat', __name__)
PRESENCE_TIMEOUT_SECONDS = 120


def key_digest(key):
    secret = current_app.config['SECRET_KEY']
    if isinstance(secret, str):
        secret = secret.encode()
    return hmac.new(secret, key.encode(), hashlib.sha256).hexdigest()


def chosen_login_key(body):
    key = body.get('login_key')
    if not isinstance(key, str) or not 8 <= len(key) <= 128 or any(c.isspace() for c in key):
        abort(400, 'Enter a unique key with 8-128 characters and no spaces.')
    return key, key_digest(key)


@bp.post('/api/admin/reset-key')
def reset_key():
    if not authenticated():
        abort(401, 'Please sign in.')
    db = get_db()
    admin = db.execute('SELECT is_admin,password FROM users WHERE id=?', (session['user_id'],)).fetchone()
    if not admin['is_admin']:
        abort(403, 'Administrator access required.')
    rate_limit(f"reset:{session['user_id']}", 10, 300)
    body = data()
    current = body.get('admin_password')
    if not isinstance(current, str) or len(current) > 128 or not check_password_hash(admin['password'], current):
        abort(403, 'Your administrator password is incorrect.')
    username = field(body.get('username'), 24)
    target = db.execute('SELECT id,is_admin FROM users WHERE username=? AND is_removed=0', (username,)).fetchone()
    if not target:
        abort(404, 'Account not found.')
    if target['is_admin']:
        abort(403, 'Keys are for regular user accounts only.')
    key, digest = chosen_login_key(body)
    try:
        with db:
            if db.execute('SELECT 1 FROM users WHERE login_key_hash IN (?,?) AND id!=?',
                          (digest, hashlib.sha256(key.encode()).hexdigest(), target['id'])).fetchone():
                abort(409, 'That unique key is already assigned. Choose another key.')
            db.execute('UPDATE users SET login_key_hash=?,session_version=session_version+1 WHERE id=?',
                       (digest, target['id']))
            db.execute('DELETE FROM presence WHERE user_id=?', (target['id'],))
            db.execute('DELETE FROM typing_status WHERE user_id=?', (target['id'],))
    except sqlite3.IntegrityError:
        abort(409, 'That unique key is already assigned. Choose another key.')
    return jsonify(ok=True, login_key=key, username=username)


def authenticated():
    token = session.get('browser_token')
    if not (session.get('user_id') and token and
            secrets.compare_digest(token, request.headers.get('X-Browser-Token', ''))):
        return False
    user = get_db().execute('SELECT session_version FROM users WHERE id=? AND is_removed=0', (session['user_id'],)).fetchone()
    return bool(user and user['session_version'] == session.get('session_version'))


def login_required(function):
    @wraps(function)
    def wrapped(*args, **kwargs):
        if not authenticated():
            abort(401, 'Please sign in.')
        return function(*args, **kwargs)
    return wrapped


def data():
    value = request.get_json(silent=True)
    if not isinstance(value, dict):
        abort(400, 'Expected a JSON object.')
    return value


def field(value, maximum):
    if not isinstance(value, str) or not value.strip() or len(value) > maximum:
        abort(400, f'Enter text between 1 and {maximum} characters.')
    return value.strip()


def rate_limit(key, limit, window):
    db = get_db()
    now = time.time()
    with db:
        db.execute('DELETE FROM attempts WHERE start < ?', (now - 3600,))
        db.execute('''INSERT INTO attempts(key,count,start) VALUES (?,1,?)
            ON CONFLICT(key) DO UPDATE SET
            count=CASE WHEN start < ? THEN 1 ELSE count+1 END,
            start=CASE WHEN start < ? THEN excluded.start ELSE start END''',
                   (key, now, now-window, now-window))
        count = db.execute('SELECT count FROM attempts WHERE key=?', (key,)).fetchone()[0]
    if count > limit:
        abort(429, 'Too many requests. Please try again later.')


def room_access(room_id):
    db = get_db()
    room = db.execute('SELECT * FROM rooms WHERE id=?', (room_id,)).fetchone()
    if not room:
        abort(404, 'Conversation not found.')
    if room['direct_key'] and not db.execute('SELECT 1 FROM members WHERE room_id=? AND user_id=?',
                                            (room_id, session['user_id'])).fetchone():
        abort(403, 'This conversation is private.')


@bp.get('/')
def index():
    return render_template('index.html')


@bp.get('/api/session')
def current_session():
    session.setdefault('csrf', secrets.token_hex(32))
    user = get_db().execute('SELECT id,username,avatar,is_admin FROM users WHERE id=?',
                           (session.get('user_id') if authenticated() else None,)).fetchone()
    return jsonify(csrf=session['csrf'], user=dict(user) if user else None)


@bp.post('/api/<action>')
def auth(action):
    if action not in ('login', 'register', 'logout'):
        abort(404)
    if action == 'register':
        abort(403, 'Only administrators can create accounts.')
    if action == 'logout':
        if authenticated():
            db = get_db()
            with db:
                from .calls import finish
                for call in db.execute("SELECT id FROM calls WHERE status!='ended' AND (caller_token=? OR callee_token=?)",
                                       (session['browser_token'], session['browser_token'])).fetchall():
                    finish(db, call['id'], 'Signed out')
                db.execute('DELETE FROM presence WHERE token=?', (session['browser_token'],))
                db.execute('DELETE FROM typing_status WHERE token=?', (session['browser_token'],))
                db.execute('UPDATE users SET last_seen=? WHERE id=?', (time.time(), session['user_id']))
        session.clear()
        return jsonify(ok=True)
    rate_limit('auth:' + (request.remote_addr or 'unknown'), 20, 300)
    body = data()
    db = get_db()
    if 'login_key' in body:
        key = body.get('login_key')
        if not isinstance(key, str) or not 8 <= len(key) <= 128 or any(c.isspace() for c in key):
            abort(401, 'Invalid sign-in key.')
        digest = key_digest(key)
        user = db.execute('SELECT * FROM users WHERE login_key_hash=? AND is_removed=0 AND is_admin=0', (digest,)).fetchone()
        if not user and re.fullmatch(r'[A-Za-z0-9_-]{43}', key):
            user = db.execute('SELECT * FROM users WHERE login_key_hash=? AND is_removed=0 AND is_admin=0',
                              (hashlib.sha256(key.encode()).hexdigest(),)).fetchone()
        if not user:
            abort(401, 'Invalid sign-in key.')
    else:
        username = body.get('username')
        password = body.get('password')
        if not isinstance(username, str) or len(username) > 24 or not isinstance(password, str) or not 8 <= len(password) <= 128:
            abort(401, 'Incorrect administrator username or password.')
        user = db.execute('SELECT * FROM users WHERE username=? AND is_removed=0 AND is_admin=1', (username.strip(),)).fetchone()
        if not user or not check_password_hash(user['password'], password):
            abort(401, 'Incorrect administrator username or password.')
    user_id = user['id']
    session.clear()
    session['user_id'] = user_id
    session['csrf'] = secrets.token_hex(32)
    session['browser_token'] = secrets.token_hex(32)
    session.permanent = False
    session['session_version'] = db.execute('SELECT session_version FROM users WHERE id=? AND is_removed=0', (user_id,)).fetchone()[0]
    touch_presence()
    user = db.execute('SELECT id,username,avatar,is_admin FROM users WHERE id=?', (user_id,)).fetchone()
    return jsonify(csrf=session['csrf'], browser_token=session['browser_token'], user=dict(user))


@bp.get('/api/rooms')
@login_required
def rooms():
    rows = get_db().execute('''SELECT r.id,
        CASE WHEN r.direct_key IS NULL THEN r.name ELSE
        (SELECT u.username FROM members m JOIN users u ON u.id=m.user_id
         WHERE m.room_id=r.id AND m.user_id != ?) END AS name,
        r.direct_key IS NOT NULL AS private
        FROM rooms r WHERE r.direct_key IS NULL OR r.id IN
        (SELECT room_id FROM members WHERE user_id=?) ORDER BY r.id''',
        (session['user_id'], session['user_id'])).fetchall()
    default = get_db().execute('SELECT default_room_id FROM users WHERE id=?', (session['user_id'],)).fetchone()[0]
    return jsonify(rooms=[dict(row) for row in rows], default_room_id=default)


@bp.post('/api/rooms')
@login_required
def create_room():
    rate_limit(f"rooms:{session['user_id']}", 10, 3600)
    name = field(data().get('name'), 40)
    db = get_db()
    with db:
        cursor = db.execute('INSERT INTO rooms(name) VALUES (?)', (name,))
    return jsonify(id=cursor.lastrowid), 201


@bp.post('/api/direct')
@login_required
def direct():
    username = field(data().get('username'), 24)
    db = get_db()
    other = db.execute('SELECT id FROM users WHERE username=? AND is_removed=0', (username,)).fetchone()
    if not other:
        abort(404, 'No account has that username yet.')
    user_id = session['user_id']
    if other['id'] == user_id:
        abort(400, 'Enter another person’s username.')
    key = ':'.join(map(str, sorted([user_id, other['id']])))
    with db:
        db.execute('INSERT OR IGNORE INTO rooms(name,direct_key) VALUES (?,?)', ('Direct message', key))
        room_id = db.execute('SELECT id FROM rooms WHERE direct_key=?', (key,)).fetchone()[0]
        db.executemany('INSERT OR IGNORE INTO members VALUES (?,?)', [(room_id, user_id), (room_id, other['id'])])
    return jsonify(id=room_id)


@bp.route('/api/rooms/<int:room_id>/messages', methods=['GET', 'POST'])
@login_required
def messages(room_id):
    room_access(room_id)
    db = get_db()
    if request.method == 'POST':
        audio = None
        if request.mimetype == 'multipart/form-data':
            upload = request.files.get('audio')
            if not upload or upload.mimetype not in ('audio/webm', 'audio/ogg', 'audio/mp4'):
                abort(400, 'Use a WebM, Ogg, or MP4 voice recording.')
            audio = upload.read(5 * 1024 * 1024 + 1)
            if not audio or len(audio) > 5 * 1024 * 1024:
                abort(400, 'Voice messages must be between 1 byte and 5 MB.')
            valid = (upload.mimetype == 'audio/webm' and audio.startswith(b'\x1a\x45\xdf\xa3') or
                     upload.mimetype == 'audio/ogg' and audio.startswith(b'OggS') or
                     upload.mimetype == 'audio/mp4' and audio[4:8] == b'ftyp')
            if not valid:
                abort(400, 'Invalid voice recording format.')
            payload = {}
            if request.form.get('reply_to_id'):
                try:
                    payload['reply_to_id'] = int(request.form['reply_to_id'])
                except ValueError:
                    abort(400, 'Invalid reply message.')
            body = 'Voice message'
        else:
            payload = data()
            body = field(payload.get('body'), 2000)
        reply_id = payload.get('reply_to_id')
        if reply_id is not None:
            if type(reply_id) is not int or reply_id < 1:
                abort(400, 'Invalid reply message.')
            if not db.execute('SELECT 1 FROM messages WHERE id=? AND room_id=?', (reply_id, room_id)).fetchone():
                abort(400, 'The message you are replying to is no longer available in this conversation.')
        rate_limit(f"send:{session['user_id']}", 30, 60)
        with db:
            db.execute('DELETE FROM typing_status WHERE token=?', (session['browser_token'],))
            db.execute('UPDATE message_sequence SET value=value+1 WHERE id=1')
            message_id = db.execute('SELECT value FROM message_sequence WHERE id=1').fetchone()[0]
            db.execute('INSERT INTO messages(id,room_id,user_id,body,reply_to_id) VALUES (?,?,?,?,?)',
                       (message_id, room_id, session['user_id'], body, reply_id))
            if audio is not None:
                db.execute('INSERT INTO voice_messages VALUES (?,?,?)', (message_id, upload.mimetype, audio))
        return jsonify(id=message_id), 201
    try:
        after = max(0, int(request.args.get('after', 0)))
        before = max(0, int(request.args.get('before', 0)))
    except ValueError:
        abort(400, 'Invalid message cursor.')
    query = '''SELECT m.id,m.body,m.created_at,m.user_id,u.username,u.avatar,m.reply_to_id,
               EXISTS(SELECT 1 FROM voice_messages v WHERE v.message_id=m.id) AS is_voice,
               EXISTS(SELECT 1 FROM photo_messages p WHERE p.message_id=m.id) AS is_photo,
               parent.body AS reply_body, author.username AS reply_username FROM messages m
               JOIN users u ON u.id=m.user_id
               LEFT JOIN messages parent ON parent.id=m.reply_to_id AND parent.room_id=m.room_id
               LEFT JOIN users author ON author.id=parent.user_id WHERE m.room_id=?'''
    args = [room_id]
    if after:
        query += ' AND m.id>?'
        args.append(after)
    if before:
        query += ' AND m.id<?'
        args.append(before)
    query += ' ORDER BY m.id ' + ('ASC' if after else 'DESC') + ' LIMIT 100'
    rows = [dict(row) for row in db.execute(query, args).fetchall()]
    if not after:
        rows.reverse()
    expirations = [dict(row) for row in db.execute(
        'SELECT id, seen_at + 300 AS expires_at FROM messages WHERE room_id=? AND seen_at IS NOT NULL',
        (room_id,))]
    return jsonify(messages=rows, typing=typing_users(room_id), reactions=reaction_state(room_id), expirations=expirations, server_time=time.time())


def typing_users(room_id):
    return [dict(row) for row in get_db().execute('''SELECT u.id,u.username,
        MAX(t.updated_at)+6 AS expires_at FROM typing_status t JOIN users u ON u.id=t.user_id
        WHERE t.room_id=? AND t.updated_at>? AND u.id!=? AND u.is_removed=0
        AND t.session_version=u.session_version GROUP BY u.id,u.username''',
        (room_id, time.time()-6, session['user_id']))]


@bp.post('/api/rooms/<int:room_id>/typing')
@login_required
def typing(room_id):
    room_access(room_id)
    value = data().get('typing')
    if type(value) is not bool:
        abort(400, 'Typing must be true or false.')
    db = get_db()
    with db:
        db.execute('DELETE FROM typing_status WHERE updated_at<=?', (time.time()-6,))
        if value:
            db.execute('INSERT OR REPLACE INTO typing_status VALUES (?,?,?,?,?)',
                       (session['browser_token'], room_id, session['user_id'], session['session_version'], time.time()))
        else:
            db.execute('DELETE FROM typing_status WHERE token=? AND room_id=?', (session['browser_token'], room_id))
    return jsonify(ok=True)


REACTION_EMOJIS = ('❤️', '👍', '😂', '😮', '😢', '🙏')


def reaction_state(room_id):
    grouped = {}
    for row in get_db().execute('''SELECT r.message_id,r.emoji,COUNT(*) AS count,
            MAX(r.user_id=?) AS mine FROM message_reactions r
            JOIN messages m ON m.id=r.message_id WHERE m.room_id=?
            GROUP BY r.message_id,r.emoji ORDER BY r.message_id,r.emoji''',
            (session['user_id'], room_id)):
        grouped.setdefault(str(row['message_id']), []).append(
            dict(emoji=row['emoji'], count=row['count'], mine=bool(row['mine'])))
    return grouped


@bp.post('/api/rooms/<int:room_id>/messages/<int:message_id>/reaction')
@login_required
def react(room_id, message_id):
    room_access(room_id)
    emoji = data().get('emoji')
    if not isinstance(emoji, str) or emoji not in REACTION_EMOJIS:
        abort(400, 'Choose a supported reaction.')
    rate_limit(f"react:{session['user_id']}", 60, 60)
    db = get_db()
    with db:
        if not db.execute('SELECT 1 FROM messages WHERE id=? AND room_id=?', (message_id, room_id)).fetchone():
            abort(404, 'Message is no longer available.')
        removed = db.execute('DELETE FROM message_reactions WHERE message_id=? AND user_id=? AND emoji=?',
                             (message_id, session['user_id'], emoji)).rowcount
        if not removed:
            db.execute('''INSERT INTO message_reactions VALUES (?,?,?)
                ON CONFLICT(message_id,user_id) DO UPDATE SET emoji=excluded.emoji''',
                (message_id, session['user_id'], emoji))
    return jsonify(reactions=reaction_state(room_id))


@bp.get('/api/rooms/<int:room_id>/messages/<int:message_id>/audio')
@login_required
def voice_audio(room_id, message_id):
    room_access(room_id)
    voice = get_db().execute('''SELECT v.audio,v.mime FROM voice_messages v
        JOIN messages m ON m.id=v.message_id WHERE m.id=? AND m.room_id=?''',
        (message_id, room_id)).fetchone()
    if not voice:
        abort(404, 'Voice message is no longer available.')
    return Response(voice['audio'], mimetype=voice['mime'])


@bp.delete('/api/rooms/<int:room_id>/messages')
@login_required
def clear_messages(room_id):
    room_access(room_id)
    db = get_db()
    with db:
        db.execute('DELETE FROM messages WHERE room_id=?', (room_id,))
    return jsonify(ok=True)


@bp.post('/api/rooms/<int:room_id>/state')
@bp.post('/api/rooms/<int:room_id>/seen')
@login_required
def seen(room_id):
    room_access(room_id)
    ids = data().get('ids')
    if not isinstance(ids, list) or len(ids) > 100 or any(type(i) is not int or i < 1 for i in ids):
        abort(400, 'Provide up to 100 message IDs.')
    db = get_db()
    now = time.time()
    if request.path.endswith('/state'):
        placeholders = ','.join('?' for _ in ids)
        rows = db.execute(f'SELECT id, seen_at + 300 AS expires_at FROM messages WHERE room_id=? AND id IN ({placeholders})',
                          [room_id] + ids).fetchall()
        missing = db.execute(f'''SELECT m.id FROM messages m LEFT JOIN messages p ON p.id=m.reply_to_id
            WHERE m.room_id=? AND m.id IN ({placeholders}) AND m.reply_to_id IS NOT NULL AND p.id IS NULL''', [room_id] + ids).fetchall()
        return jsonify(unavailable_replies=[row['id'] for row in missing], alive=[row['id'] for row in rows],
                       expirations=[dict(row) for row in rows if row['expires_at'] is not None], server_time=now)
    with db:
        db.executemany('''UPDATE messages SET seen_at=? WHERE id=? AND room_id=?
                          AND user_id != ? AND seen_at IS NULL''',
                       [(now, i, room_id, session['user_id']) for i in ids])
    return jsonify(ok=True)


@bp.post('/api/avatar')
@login_required
def update_avatar():
    avatar = data().get('avatar')
    if not isinstance(avatar, str) or avatar not in ('fox', 'cat', 'dog', 'panda', 'koala', 'lion', 'rabbit', 'bear', 'owl', 'penguin', 'frog', 'unicorn'):
        abort(400, 'Choose an avatar from the list.')
    db = get_db()
    with db:
        db.execute('UPDATE users SET avatar=? WHERE id=?', (avatar, session['user_id']))
    return jsonify(avatar=avatar)


def touch_presence():
    db = get_db()
    now = time.time()
    with db:
        db.execute('DELETE FROM presence WHERE updated_at < ?', (now - PRESENCE_TIMEOUT_SECONDS,))
        db.execute('INSERT OR REPLACE INTO presence VALUES (?,?,?,?)',
                   (session['browser_token'], session['user_id'], session['session_version'], now))
        db.execute('UPDATE users SET last_seen=? WHERE id=?', (now, session['user_id']))


@bp.post('/api/presence')
@login_required
def presence():
    touch_presence()
    rows = get_db().execute("""SELECT u.id,u.username,u.last_seen, EXISTS(
        SELECT 1 FROM presence p WHERE p.user_id=u.id AND p.session_version=u.session_version
        AND p.updated_at >= ?) AS online FROM users u WHERE u.is_removed=0 AND u.is_admin=0 ORDER BY u.username""", (time.time()-PRESENCE_TIMEOUT_SECONDS,)).fetchall()
    return jsonify(users=[dict(row) for row in rows])


@bp.post('/api/admin/users')
@login_required
def create_user():
    db = get_db()
    if not db.execute('SELECT is_admin FROM users WHERE id=?', (session['user_id'],)).fetchone()['is_admin']:
        abort(403, 'Administrator access required.')
    rate_limit(f"create-user:{session['user_id']}", 20, 300)
    body = data()
    username = field(body.get('username'), 24)
    if not re.fullmatch(r'[A-Za-z0-9_]{3,24}', username):
        abort(400, 'Use 3-24 letters, numbers, or underscores for your username.')
    key, digest = chosen_login_key(body)
    try:
        with db:
            if db.execute('SELECT 1 FROM users WHERE login_key_hash IN (?,?)', (digest, hashlib.sha256(key.encode()).hexdigest())).fetchone():
                abort(409, 'That unique key is already assigned. Choose another key.')
            cursor = db.execute('INSERT INTO users(username,password,login_key_hash) VALUES (?,?,?)',
                                (username, generate_password_hash(secrets.token_urlsafe(32)), digest))
    except sqlite3.IntegrityError:
        abort(409, 'That username or unique key is already assigned. Choose another.')
    return jsonify(id=cursor.lastrowid, username=username, login_key=key), 201



@bp.get('/api/admin/users')
@login_required
def list_users():
    db = get_db()
    if not db.execute('SELECT is_admin FROM users WHERE id=?', (session['user_id'],)).fetchone()['is_admin']:
        abort(403, 'Administrator access required.')
    return jsonify(users=[dict(row) for row in db.execute(
        'SELECT id,username FROM users WHERE is_admin=0 AND is_removed=0 ORDER BY username')])


@bp.delete('/api/admin/users/<int:user_id>')
@login_required
def remove_user(user_id):
    db = get_db()
    admin = db.execute('SELECT is_admin,password FROM users WHERE id=?', (session['user_id'],)).fetchone()
    if not admin['is_admin']:
        abort(403, 'Administrator access required.')
    rate_limit(f"remove-user:{session['user_id']}", 10, 300)
    password = data().get('admin_password')
    if not isinstance(password, str) or len(password) > 128 or not check_password_hash(admin['password'], password):
        abort(403, 'Your administrator password is incorrect.')
    target = db.execute('SELECT is_admin FROM users WHERE id=? AND is_removed=0', (user_id,)).fetchone()
    if not target:
        abort(404, 'Account not found.')
    if target['is_admin'] or user_id == session['user_id']:
        abort(403, 'Administrator accounts cannot be removed here.')
    with db:
        db.execute('UPDATE users SET is_removed=1,password=?,session_version=session_version+1 WHERE id=?',
                   (generate_password_hash(secrets.token_hex(32)), user_id))
        db.execute('DELETE FROM presence WHERE user_id=?', (user_id,))
    return jsonify(ok=True)


@bp.post('/api/admin/personal-chat')
@login_required
def personal_chat():
    db = get_db()
    if not db.execute('SELECT is_admin FROM users WHERE id=?', (session['user_id'],)).fetchone()['is_admin']:
        abort(403, 'Administrator access required.')
    ids = data().get('user_ids')
    if not isinstance(ids, list) or len(ids) != 2 or any(type(i) is not int or i < 1 for i in ids) or ids[0] == ids[1]:
        abort(400, 'Select exactly two different users.')
    rate_limit(f"personal-chat:{session['user_id']}", 20, 300)
    with db:
        users = db.execute('SELECT id FROM users WHERE id IN (?,?) AND is_removed=0 AND is_admin=0', ids).fetchall()
        if len(users) != 2:
            abort(400, 'Select two existing regular users.')
        key = ':'.join(map(str, sorted(ids)))
        db.execute('INSERT OR IGNORE INTO rooms(name,direct_key) VALUES (?,?)', ('Personal chat', key))
        room_id = db.execute('SELECT id FROM rooms WHERE direct_key=?', (key,)).fetchone()[0]
        db.executemany('INSERT OR IGNORE INTO members VALUES (?,?)', [(room_id, i) for i in ids])
        db.executemany('UPDATE users SET default_room_id=? WHERE id=?', [(room_id, i) for i in ids])
    return jsonify(id=room_id), 201
