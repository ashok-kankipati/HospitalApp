"""Server-side sessions isolated by a random, page-memory tab key."""
import hashlib
import json
import re
import time
from flask.sessions import SecureCookieSessionInterface
from .db import get_db


class TabSessionInterface(SecureCookieSessionInterface):
    def open_session(self, app, request):
        key = request.headers.get('X-Tab-ID')
        if key is None:
            return super().open_session(app, request)
        if not re.fullmatch(r'[a-f0-9]{64}', key):
            return None
        digest = hashlib.sha256(key.encode()).hexdigest()
        row = get_db().execute('SELECT data FROM tab_sessions WHERE id=? AND expires_at>?',
                               (digest, time.time())).fetchone()
        saved = self.session_class(json.loads(row['data']) if row else {})
        saved.tab_key = digest
        return saved

    def save_session(self, app, session, response):
        key = getattr(session, 'tab_key', None)
        if key is None:
            return super().save_session(app, session, response)
        db = get_db()
        with db:
            db.execute('DELETE FROM tab_sessions WHERE expires_at<=?', (time.time(),))
            if not session:
                db.execute('DELETE FROM tab_sessions WHERE id=?', (key,))
            else:
                db.execute('INSERT OR REPLACE INTO tab_sessions VALUES (?,?,?)',
                           (key, json.dumps(dict(session)), time.time() + 86400))
        response.vary.add('X-Tab-ID')
