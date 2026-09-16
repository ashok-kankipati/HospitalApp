"""Short HTTP signaling requests. Audio is transmitted by browser WebRTC."""
import os
import secrets
import time
from flask import Blueprint, abort, jsonify, session
from .db import get_db
from .routes import data, login_required, rate_limit, room_access

bp = Blueprint('calls', __name__)


def finish(db, call_id, reason):
    db.execute("UPDATE calls SET status='ended',offer=NULL,answer=NULL,reason=?,updated=? WHERE id=?",
               (reason, time.time(), call_id))


def cleanup(db):
    now = time.time()
    for call in db.execute("SELECT * FROM calls WHERE status != 'ended'").fetchall():
        users = db.execute('SELECT id,session_version,is_removed FROM users WHERE id IN (?,?)',
                           (call['caller'], call['callee'])).fetchall()
        revoked = any(u['is_removed'] or u['session_version'] != call['caller_version' if u['id'] == call['caller'] else 'callee_version'] for u in users)
        if revoked:
            finish(db, call['id'], 'Account session ended')
        elif call['status'] == 'ringing' and now-call['created'] > 60:
            finish(db, call['id'], 'No answer')
        elif now-call['caller_ping'] > 45 or (call['status'] == 'accepted' and now-call['callee_ping'] > 45):
            finish(db, call['id'], 'Connection lost')
    db.execute("DELETE FROM calls WHERE status='ended' AND updated<?", (now-120,))


def owned(call):
    return session['browser_token'] == call['caller_token' if session['user_id'] == call['caller'] else 'callee_token']


def public(call):
    other = call['callee'] if session['user_id'] == call['caller'] else call['caller']
    name = get_db().execute('SELECT username FROM users WHERE id=?', (other,)).fetchone()[0]
    return {key: call[key] for key in ('id', 'room_id', 'caller', 'callee', 'status', 'offer', 'answer', 'reason')} | {'name': name}


def get_call(db, call_id):
    call = db.execute('SELECT * FROM calls WHERE id=? AND (caller=? OR callee=?)',
                      (call_id, session['user_id'], session['user_id'])).fetchone()
    if not call:
        abort(404, 'Call is no longer available.')
    if call['callee_token'] or session['user_id'] == call['caller']:
        if not owned(call):
            abort(409, 'This call is open in another tab.')
    return call


def sdp(value):
    if not isinstance(value, str) or not value.startswith('v=0') or len(value) > 60000 or 'm=audio ' not in value or 'm=video ' in value:
        abort(400, 'Invalid voice call setup.')
    return value


@bp.get('/api/calls/config')
@login_required
def config():
    servers = [{'urls': 'stun:stun.l.google.com:19302'}]
    turn = os.environ.get('TURN_URLS', '')
    if turn:
        servers.append({'urls': [url.strip() for url in turn.split(',') if url.strip()],
                        'username': os.environ.get('TURN_USERNAME', ''), 'credential': os.environ.get('TURN_PASSWORD', '')})
    return jsonify(iceServers=servers)


@bp.get('/api/calls')
@login_required
def incoming():
    db = get_db()
    with db:
        cleanup(db)
    call = db.execute("SELECT * FROM calls WHERE callee=? AND status='ringing' AND callee_token IS NULL ORDER BY created LIMIT 1",
                      (session['user_id'],)).fetchone()
    return jsonify(call=public(call) if call else None)


@bp.post('/api/calls')
@login_required
def start():
    payload = data()
    room_id = payload.get('room_id')
    if type(room_id) is not int:
        abort(400, 'Choose a private conversation.')
    room_access(room_id)
    offer = sdp(payload.get('offer'))
    rate_limit(f"call:{session['user_id']}", 10, 300)
    db = get_db()
    room = db.execute('SELECT direct_key FROM rooms WHERE id=?', (room_id,)).fetchone()
    if not room['direct_key']:
        abort(400, 'Calls are available in private conversations only.')
    other = db.execute('''SELECT u.id,u.session_version FROM members m JOIN users u ON u.id=m.user_id
        WHERE m.room_id=? AND m.user_id!=? AND u.is_removed=0''', (room_id, session['user_id'])).fetchone()
    if not other:
        abort(404, 'The other person is unavailable.')
    now = time.time()
    with db:
        db.execute('BEGIN IMMEDIATE')
        cleanup(db)
        if not db.execute('SELECT 1 FROM presence WHERE user_id=? AND session_version=? AND updated_at>?',
                          (other['id'], other['session_version'], now-120)).fetchone():
            abort(409, 'The other person is offline. Ask them to open the chat.')
        if db.execute("SELECT 1 FROM calls WHERE status!='ended' AND (caller IN (?,?) OR callee IN (?,?))",
                      (session['user_id'], other['id'], session['user_id'], other['id'])).fetchone():
            abort(409, 'One of you is already on a call.')
        call_id = secrets.token_hex(16)
        db.execute('''INSERT INTO calls VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)''',
                   (call_id, room_id, session['user_id'], other['id'], session['browser_token'], None,
                    session['session_version'], other['session_version'], 'ringing', offer, None, None, now, now, now, now))
    return jsonify(call=public(db.execute('SELECT * FROM calls WHERE id=?', (call_id,)).fetchone())), 201


@bp.post('/api/calls/<call_id>/<action>')
@login_required
def update(call_id, action):
    if action not in ('poll', 'accept', 'end'):
        abort(404)
    payload = data()
    db = get_db()
    with db:
        db.execute('BEGIN IMMEDIATE')
        cleanup(db)
        call = get_call(db, call_id)
        if call['status'] != 'ended':
            if action == 'accept':
                if session['user_id'] != call['callee'] or call['status'] != 'ringing':
                    abort(409, 'Call cannot be accepted now.')
                answer = sdp(payload.get('answer'))
                db.execute("UPDATE calls SET status='accepted',answer=?,callee_token=?,callee_ping=?,updated=? WHERE id=?",
                           (answer, session['browser_token'], time.time(), time.time(), call_id))
            elif action == 'end':
                finish(db, call_id, 'Declined' if call['status'] == 'ringing' and session['user_id'] == call['callee'] else 'Call ended')
            elif owned(call):
                column = 'caller_ping' if session['user_id'] == call['caller'] else 'callee_ping'
                db.execute(f'UPDATE calls SET {column}=? WHERE id=?', (time.time(), call_id))
        result = public(db.execute('SELECT * FROM calls WHERE id=?', (call_id,)).fetchone())
    return jsonify(call=result)
