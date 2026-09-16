import hashlib
import secrets
import io
import tempfile
import time
import unittest
from unittest.mock import patch
from chat.db import get_db
from pathlib import Path
from chat import create_app


class ChatTest(unittest.TestCase):
    def test_typing_access_expiry_and_send(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        eve, e = self.account('eve')
        room = alice.post('/api/direct', json={'username': 'bob'}, headers=a).json['id']
        path = f'/api/rooms/{room}'
        self.assertEqual(eve.post(path+'/typing', json={'typing': True}, headers=e).status_code, 403)
        self.assertEqual(alice.post(path+'/typing', json={'typing': True}).status_code, 403)
        self.assertEqual(alice.post(path+'/typing', json={'typing': 'true'}, headers=a).status_code, 400)
        def names(client=bob, headers=b):
            return [u['username'] for u in client.get(path+'/messages?after=999', headers=headers).json['typing']]
        alice.post(path+'/typing', json={'typing': True}, headers=a)
        self.assertEqual(names(), ['alice'])
        self.assertEqual(names(alice, a), [])
        self.assertEqual(bob.get('/api/rooms/1/messages', headers=b).json['typing'], [])
        with patch('chat.routes.time.time', return_value=time.time()+7):
            self.assertEqual(names(), [])
        alice.post(path+'/typing', json={'typing': True}, headers=a)
        alice.post(path+'/typing', json={'typing': False}, headers=a)
        self.assertEqual(names(), [])
        alice.post(path+'/typing', json={'typing': True}, headers=a)
        alice.post(path+'/messages', json={'body': 'Hi'}, headers=a)
        self.assertEqual(names(), [])
        alice.post(path+'/typing', json={'typing': True}, headers=a)
        alice.post('/api/logout', json={}, headers=a)
        self.assertEqual(names(), [])


    def test_reactions_toggle_replace_access_and_cleanup(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        eve, e = self.account('eve')
        room = alice.post('/api/direct', json={'username': 'bob'}, headers=a).json['id']
        path = f'/api/rooms/{room}/messages'
        mid = alice.post(path, json={'body': 'Hello'}, headers=a).json['id']
        endpoint = f'{path}/{mid}/reaction'
        heart, like = '\u2764\ufe0f', '\U0001f44d'
        self.assertEqual(eve.post(endpoint, json={'emoji': heart}, headers=e).status_code, 403)
        self.assertEqual(alice.post(endpoint, json={'emoji': heart}).status_code, 403)
        self.assertEqual(alice.post(endpoint, json={'emoji': []}, headers=a).status_code, 400)
        self.assertEqual(alice.post(endpoint, json={'emoji': 'invalid'}, headers=a).status_code, 400)
        self.assertEqual(alice.post(f'/api/rooms/1/messages/{mid}/reaction', json={'emoji': heart}, headers=a).status_code, 404)
        alice.post(endpoint, json={'emoji': heart}, headers=a)
        result = bob.post(endpoint, json={'emoji': heart}, headers=b).json
        self.assertEqual(result['reactions'][str(mid)], [{'emoji': heart, 'count': 2, 'mine': True}])
        alice.post(endpoint, json={'emoji': like}, headers=a)
        result = bob.get(path + f'?after={mid}', headers=b).json
        self.assertEqual(result['messages'], [])
        self.assertEqual(len(result['reactions'][str(mid)]), 2)
        bob.post(endpoint, json={'emoji': heart}, headers=b)
        result = bob.get(path, headers=b).json
        self.assertEqual(result['reactions'][str(mid)], [{'emoji': like, 'count': 1, 'mine': False}])
        alice.delete(path, headers=a)
        self.assertEqual(alice.post(endpoint, json={'emoji': heart}, headers=a).status_code, 404)
        with self.app.app_context():
            self.assertEqual(get_db().execute('SELECT COUNT(*) FROM message_reactions').fetchone()[0], 0)


    def test_photo_upload_access_and_cleanup(self):
        from PIL import Image
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        eve, e = self.account('eve')
        room = alice.post('/api/direct', json={'username': 'bob'}, headers=a).json['id']
        path = f'/api/rooms/{room}/photos'
        image = io.BytesIO()
        source = Image.new('RGB', (1800, 900), 'red')
        exif = Image.Exif(); exif[270] = 'Private camera metadata'
        source.save(image, format='JPEG', exif=exif)
        def upload(client=alice, headers=a, raw=None, **fields):
            return client.post(path, data=dict(photo=(io.BytesIO(image.getvalue() if raw is None else raw), 'camera.jpg'), **fields), headers=headers)
        self.assertEqual(upload(eve, e).status_code, 403)
        self.assertEqual(upload(headers={}).status_code, 403)
        self.assertEqual(upload(raw=b'<svg>not a photo</svg>').status_code, 400)
        self.assertEqual(upload(raw=b'x'*(5*1024*1024+1)).status_code, 400)
        self.assertEqual(upload(caption='x'*2001).status_code, 400)
        self.assertEqual(upload(reply_to_id='9999').status_code, 400)
        result = upload(caption='My photo')
        self.assertEqual(result.status_code, 201)
        message_id = result.json['id']
        photo_path = f'/api/rooms/{room}/messages/{message_id}/photo'
        response = bob.get(photo_path, headers=b)
        self.assertEqual(response.mimetype, 'image/jpeg')
        self.assertEqual(response.headers['Cache-Control'], 'no-store')
        with Image.open(io.BytesIO(response.data)) as saved:
            self.assertEqual(saved.size, (1600, 800))
            self.assertEqual(dict(saved.getexif()), {})
        self.assertEqual(eve.get(photo_path, headers=e).status_code, 403)
        self.assertEqual(bob.get(photo_path).status_code, 401)
        listing = bob.get(f'/api/rooms/{room}/messages', headers=b).json['messages'][0]
        self.assertTrue(listing['is_photo'])
        self.assertEqual(listing['body'], 'My photo')
        bob.post(f'/api/rooms/{room}/seen', json={'ids': [message_id]}, headers=b)
        with patch('chat.time.time', return_value=time.time()+301):
            self.assertEqual(bob.get(photo_path, headers=b).status_code, 404)
        with self.app.app_context():
            self.assertEqual(get_db().execute('SELECT COUNT(*) FROM photo_messages').fetchone()[0], 0)
        self.assertEqual(upload().status_code, 201)
        alice.delete(f'/api/rooms/{room}/messages', headers=a)
        with self.app.app_context():
            self.assertEqual(get_db().execute('SELECT COUNT(*) FROM photo_messages').fetchone()[0], 0)

    def setUp(self):
        self.keys = {}
        self.temp = tempfile.TemporaryDirectory()
        self.app = create_app({'TESTING': True, 'SECRET_KEY': 'test',
                               'DATABASE': str(Path(self.temp.name) / 'test.sqlite3')})

    def tearDown(self):
        self.temp.cleanup()

    def account(self, name):
        from werkzeug.security import generate_password_hash
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute('INSERT INTO users(username,password) VALUES (?,?)', (name, generate_password_hash('test-password')))
        self.keys[name] = secrets.token_urlsafe(32)
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute('UPDATE users SET login_key_hash=? WHERE username=?', (hashlib.sha256(self.keys[name].encode()).hexdigest(), name))
        client = self.app.test_client()
        token = client.get('/api/session').json['csrf']
        result = client.post('/api/login', json={'login_key': self.keys[name]},
                             headers={'X-CSRF-Token': token})
        self.assertEqual(result.status_code, 200)
        return client, {'X-CSRF-Token': result.json['csrf'], 'X-Browser-Token': result.json['browser_token']}

    def test_voice_messages_access_validation_and_cleanup(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        eve, e = self.account('eve')
        room = alice.post('/api/direct', json={'username': 'bob'}, headers=a).json['id']
        path = f'/api/rooms/{room}/messages'
        audio = b'\x1a\x45\xdf\xa3' + b'test audio'
        def upload(client, headers, content=audio, mime='audio/webm', **fields):
            return client.post(path, data=dict(audio=(io.BytesIO(content), 'voice', mime), **fields), headers=headers)
        self.assertEqual(upload(eve, e).status_code, 403)
        self.assertEqual(upload(alice, {}).status_code, 403)
        self.assertEqual(upload(alice, a, b'fake').status_code, 400)
        self.assertEqual(upload(alice, a, mime='text/html').status_code, 400)
        self.assertEqual(upload(alice, a, reply_to_id='999').status_code, 400)
        self.assertEqual(upload(alice, a, audio + b'x' * (5 * 1024 * 1024)).status_code, 400)
        result = upload(alice, a)
        self.assertEqual(result.status_code, 201)
        audio_path = f"{path}/{result.json['id']}/audio"
        self.assertEqual(bob.get(audio_path, headers=b).data, audio)
        self.assertEqual(bob.get(audio_path, headers=b).mimetype, 'audio/webm')
        self.assertEqual(eve.get(audio_path, headers=e).status_code, 403)
        self.assertEqual(alice.get(audio_path).status_code, 401)
        listing = bob.get(path, headers=b).json['messages'][0]
        self.assertTrue(listing['is_voice'])
        self.assertNotIn('audio', listing)
        bob.post(f'/api/rooms/{room}/seen', json={'ids': [result.json['id']]}, headers=b)
        with patch('chat.time.time', return_value=time.time() + 301):
            self.assertEqual(bob.get(audio_path, headers=b).status_code, 404)
        upload(alice, a)
        bob.delete(path, headers=b)
        with self.app.app_context():
            self.assertEqual(get_db().execute('SELECT COUNT(*) FROM voice_messages').fetchone()[0], 0)

    def test_admin_only_creation(self):
        admin, a = self.account('admin')
        regular, r = self.account('regular')
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute("UPDATE users SET is_admin=1 WHERE username='admin'")
        body = {'username': 'newuser', 'login_key': 'AdminChosenKey123', 'is_admin': True}
        self.assertEqual(regular.post('/api/register', json=body, headers=r).status_code, 403)
        self.assertEqual(regular.post('/api/admin/users', json=body, headers=r).status_code, 403)
        self.assertEqual(admin.post('/api/admin/users', json=body, headers=a).status_code, 201)
        self.assertEqual(admin.post('/api/admin/users', json=body, headers=a).status_code, 409)
        self.assertEqual(admin.get('/api/session', headers=a).json['user']['username'], 'admin')
        with self.app.app_context():
            self.assertEqual(get_db().execute("SELECT is_admin FROM users WHERE username='newuser'").fetchone()[0], 0)
        self.assertNotIn('auth-toggle', regular.get('/').text)

    def test_presence_logout_timeout_and_multiple_sessions(self):
        self.app.config['SESSION_REFRESH_EACH_REQUEST'] = False
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        def alice_status():
            return next(u for u in bob.post('/api/presence', json={}, headers=b).json['users'] if u['username'] == 'alice')
        self.assertTrue(alice_status()['online'])
        now = time.time()
        with patch('chat.routes.time.time', return_value=now + 119):
            self.assertTrue(alice_status()['online'])
        with patch('chat.routes.time.time', return_value=now + 121):
            self.assertFalse(alice_status()['online'])
            self.assertIsNotNone(alice_status()['last_seen'])
        alice.post('/api/presence', json={}, headers=a)
        second = self.app.test_client()
        csrf = second.get('/api/session').json['csrf']
        second.post('/api/login', json={'login_key': self.keys['alice']}, headers={'X-CSRF-Token': csrf})
        alice.post('/api/logout', headers=a)
        self.assertTrue(alice_status()['online'])
        bob.post('/api/logout', headers=b)
        self.assertEqual(bob.post('/api/presence', json={}, headers=b).status_code, 403)

    def test_replies_scope_and_expiry(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        original = alice.post('/api/rooms/1/messages', json={'body': 'original'}, headers=a).json['id']
        reply = bob.post('/api/rooms/1/messages', json={'body': 'answer', 'reply_to_id': original}, headers=b)
        self.assertEqual(reply.status_code, 201)
        messages = alice.get('/api/rooms/1/messages', headers=a).json['messages']
        self.assertEqual(messages[-1]['reply_body'], 'original')
        self.assertEqual(messages[-1]['reply_username'], 'alice')
        for invalid in [True, '1', -1, 999]:
            self.assertEqual(bob.post('/api/rooms/1/messages', json={'body': 'bad', 'reply_to_id': invalid}, headers=b).status_code, 400)
        self.assertEqual(bob.post('/api/rooms/2/messages', json={'body': 'bad', 'reply_to_id': original}, headers=b).status_code, 400)
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute('UPDATE messages SET seen_at=? WHERE id=?', (time.time()-301, original))
        remaining = alice.get('/api/rooms/1/messages', headers=a).json['messages']
        self.assertEqual(len(remaining), 1)
        self.assertIsNone(remaining[0]['reply_body'])
        self.assertEqual(remaining[0]['reply_to_id'], original)

    def test_tab_session_and_csrf_recovery(self):
        alice, a = self.account('alice')
        with alice.session_transaction() as saved:
            self.assertFalse(saved.permanent)
        cookie = alice.get_cookie('gather_session')
        self.assertIsNone(cookie.expires)
        self.assertTrue(cookie.http_only)
        reopened = self.app.test_client()
        reopened.set_cookie('gather_session', cookie.value)
        self.assertIsNone(reopened.get('/api/session').json['user'])
        self.assertEqual(reopened.get('/api/rooms').status_code, 401)
        rejected = alice.post('/api/rooms/1/messages', json={'body': 'draft'}, headers=dict(a, **{'X-CSRF-Token': 'old-token'}))
        self.assertEqual(rejected.status_code, 403)
        self.assertEqual(rejected.json['code'], 'csrf_expired')
        current = alice.get('/api/session', headers=a).json
        self.assertEqual(current['user']['username'], 'alice')
        self.assertEqual(alice.post('/api/rooms/1/messages', json={'body': 'draft'}, headers=dict(a, **{'X-CSRF-Token': current['csrf']})).status_code, 201)
        alice.post('/api/logout', headers=a)
        self.assertIsNone(alice.get('/api/session', headers=a).json['user'])

    def test_two_users_in_same_browser_tabs(self):
        self.account('alice')
        self.account('bob')
        browser = self.app.test_client()
        def login_tab(name, key):
            headers = {'X-Tab-ID': key}
            headers['X-CSRF-Token'] = browser.get('/api/session', headers=headers).json['csrf']
            result = browser.post('/api/login', json={'login_key': self.keys[name]}, headers=headers)
            self.assertEqual(result.status_code, 200)
            self.assertNotIn('Set-Cookie', result.headers)
            headers.update({'X-CSRF-Token': result.json['csrf'], 'X-Browser-Token': result.json['browser_token']})
            return headers
        a = login_tab('alice', 'a' * 64)
        b = login_tab('bob', 'b' * 64)
        for headers, name in [(a, 'alice'), (b, 'bob'), (a, 'alice')]:
            self.assertEqual(browser.get('/api/session', headers=headers).json['user']['username'], name)
            self.assertEqual(browser.post('/api/rooms/1/messages', json={'body': name}, headers=headers).status_code, 201)
        messages = browser.get('/api/rooms/1/messages', headers=a).json['messages']
        self.assertEqual([m['username'] for m in messages], ['alice', 'bob', 'alice'])
        browser.post('/api/logout', headers=b)
        self.assertIsNone(browser.get('/api/session', headers=b).json['user'])
        self.assertEqual(browser.get('/api/session', headers=a).json['user']['username'], 'alice')
        self.assertIsNone(browser.get('/api/session', headers={'X-Tab-ID': 'c' * 64}).json['user'])
        self.assertEqual(browser.get('/api/rooms', headers=dict(a, **{'X-Tab-ID': 'c' * 64})).status_code, 401)

    def test_admin_remove_user(self):
        admin, a = self.account('admin')
        alice, b = self.account('alice')
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute("UPDATE users SET is_admin=1 WHERE username='admin'")
        user_id = alice.get('/api/session', headers=b).json['user']['id']
        admin_id = admin.get('/api/session', headers=a).json['user']['id']
        path = f'/api/admin/users/{user_id}'
        body = {'admin_password': 'test-password'}
        self.assertEqual(alice.get('/api/admin/users', headers=b).status_code, 403)
        self.assertEqual(alice.delete(path, json=body, headers=b).status_code, 403)
        self.assertEqual(admin.delete(path, json=body).status_code, 403)
        self.assertEqual(admin.delete(path, json={'admin_password': 'wrong'}, headers=a).status_code, 403)
        self.assertEqual(admin.delete(f'/api/admin/users/{admin_id}', json=body, headers=a).status_code, 403)
        alice.post('/api/rooms/1/messages', json={'body': 'keep history'}, headers=b)
        self.assertEqual(admin.delete(path, json=body, headers=a).status_code, 200)
        self.assertEqual(alice.get('/api/rooms', headers=b).status_code, 401)
        self.assertIsNone(alice.get('/api/session', headers=b).json['user'])
        self.assertEqual(alice.post('/api/login', json={'login_key': self.keys['alice']}, headers=b).status_code, 401)
        self.assertNotIn(user_id, [u['id'] for u in admin.get('/api/admin/users', headers=a).json['users']])
        self.assertNotIn(user_id, [u['id'] for u in admin.post('/api/presence', json={}, headers=a).json['users']])
        self.assertEqual(admin.post('/api/direct', json={'username': 'alice'}, headers=a).status_code, 404)
        self.assertEqual(admin.get('/api/rooms/1/messages', headers=a).json['messages'][0]['body'], 'keep history')
        self.assertEqual(admin.delete(path, json=body, headers=a).status_code, 404)

    def test_admin_personal_chat_and_hidden_presence(self):
        admin, a = self.account('admin')
        alice, b = self.account('alice')
        bob, c = self.account('bob')
        eve, e = self.account('eve')
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute("UPDATE users SET is_admin=1 WHERE username='admin'")
        aid = alice.get('/api/session', headers=b).json['user']['id']
        bid = bob.get('/api/session', headers=c).json['user']['id']
        payload = {'user_ids': [aid, bid]}
        self.assertEqual(alice.post('/api/admin/personal-chat', json=payload, headers=b).status_code, 403)
        for ids in [[aid], [aid, aid], [aid, bid, 999], [aid, 999], [True, bid]]:
            self.assertEqual(admin.post('/api/admin/personal-chat', json={'user_ids': ids}, headers=a).status_code, 400)
        result = admin.post('/api/admin/personal-chat', json=payload, headers=a)
        self.assertEqual(result.status_code, 201)
        room = result.json['id']
        self.assertEqual(admin.post('/api/admin/personal-chat', json=payload, headers=a).json['id'], room)
        for client, headers in [(alice, b), (bob, c)]:
            listing = client.get('/api/rooms', headers=headers).json
            self.assertEqual(listing['default_room_id'], room)
            self.assertIn(room, [r['id'] for r in listing['rooms']])
            self.assertEqual(client.get('/api/rooms/1/messages', headers=headers).status_code, 200)
        for client, headers in [(admin, a), (eve, e)]:
            self.assertEqual(client.get(f'/api/rooms/{room}/messages', headers=headers).status_code, 403)
            self.assertNotIn(room, [r['id'] for r in client.get('/api/rooms', headers=headers).json['rooms']])
        self.assertNotIn('admin', [u['username'] for u in alice.post('/api/presence', json={}, headers=b).json['users']])
        with self.app.app_context():
            self.assertEqual(get_db().execute('SELECT COUNT(*) FROM members WHERE room_id=?', (room,)).fetchone()[0], 2)

    def test_private_senders_and_avatars_stay_distinct(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        alice.post('/api/avatar', json={'avatar': 'lion'}, headers=a)
        bob.post('/api/avatar', json={'avatar': 'panda'}, headers=b)
        alice_id = alice.get('/api/session', headers=a).json['user']['id']
        bob_id = bob.get('/api/session', headers=b).json['user']['id']
        room = alice.post('/api/direct', json={'username': 'bob'}, headers=a).json['id']
        path = f'/api/rooms/{room}/messages'
        alice.post(path, json={'body': 'hello', 'user_id': bob_id}, headers=a)
        bob.post(path, json={'body': 'reply', 'user_id': alice_id}, headers=b)
        for client, headers, own in [(alice, a, alice_id), (bob, b, bob_id)]:
            messages = client.get(path, headers=headers).json['messages']
            self.assertEqual([(m['user_id'], m['username'], m['avatar']) for m in messages],
                             [(alice_id, 'alice', 'lion'), (bob_id, 'bob', 'panda')])
            self.assertEqual(sum(m['user_id'] == own for m in messages), 1)

    def test_public_chat_and_history(self):
        alice, headers = self.account('alice')
        bob, b = self.account('bob')
        for index in range(3):
            self.assertEqual(alice.post('/api/rooms/1/messages', json={'body': f'hello {index}'}, headers=headers).status_code, 201)
        messages = bob.get('/api/rooms/1/messages', headers=b).json['messages']
        self.assertEqual(len(messages), 3)
        self.assertEqual(messages[0]['username'], 'alice')
        self.assertEqual(len(bob.get(f"/api/rooms/1/messages?after={messages[0]['id']}", headers=b).json['messages']), 2)
        self.assertEqual(len(bob.get(f"/api/rooms/1/messages?before={messages[-1]['id']}", headers=b).json['messages']), 2)

    def test_private_access_and_unique_conversation(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        eve, e = self.account('eve')
        room = alice.post('/api/direct', json={'username': 'bob'}, headers=a).json['id']
        self.assertEqual(bob.post('/api/direct', json={'username': 'alice'}, headers=b).json['id'], room)
        self.assertEqual(alice.post(f'/api/rooms/{room}/messages', json={'body': 'secret'}, headers=a).status_code, 201)
        self.assertEqual(bob.get(f'/api/rooms/{room}/messages', headers=b).json['messages'][0]['body'], 'secret')
        self.assertEqual(eve.get(f'/api/rooms/{room}/messages', headers=e).status_code, 403)
        self.assertEqual(eve.post(f'/api/rooms/{room}/messages', json={'body': 'intrude'}, headers=e).status_code, 403)
        self.assertNotIn(room, [r['id'] for r in eve.get('/api/rooms', headers=e).json['rooms']])

    def test_auth_csrf_validation_and_logout(self):
        client, headers = self.account('alice')
        self.assertEqual(client.post('/api/rooms/1/messages', json={'body': 'hello'}).status_code, 403)
        self.assertEqual(client.post('/api/rooms/1/messages', json={'body': ' '}, headers=headers).status_code, 400)
        self.assertEqual(client.post('/api/rooms/1/messages', json={'body': 'x'*2001}, headers=headers).status_code, 400)
        self.assertEqual(client.get('/api/rooms/1/messages?after=bad', headers=headers).status_code, 400)
        client.post('/api/logout', headers=headers)
        self.assertEqual(client.get('/api/rooms').status_code, 401)
        token = client.get('/api/session').json['csrf']
        result = client.post('/api/login', json={'login_key': self.keys['alice']}, headers={'X-CSRF-Token': token})
        self.assertEqual(result.status_code, 200)
        self.assertNotEqual(result.json['csrf'], token)

    def test_rate_limit_and_page(self):
        client, headers = self.account('alice')
        for _ in range(30):
            self.assertEqual(client.post('/api/rooms/1/messages', json={'body': 'hi'}, headers=headers).status_code, 201)
        self.assertEqual(client.post('/api/rooms/1/messages', json={'body': 'hi'}, headers=headers).status_code, 429)
        self.assertEqual(client.get('/').status_code, 200)
        with client.get('/static/app.js') as response:
            self.assertEqual(response.status_code, 200)
        self.assertIn("default-src 'self'", client.get('/').headers['Content-Security-Policy'])

    def test_admin_key_reset(self):
        result = self.app.test_cli_runner().invoke(args=['create-admin', '--username', 'admin'],
                                                  input='admin-password\nadmin-password\n')
        self.assertEqual(result.exit_code, 0, result.output)
        admin = self.app.test_client()
        csrf = admin.get('/api/session').json['csrf']
        login = admin.post('/api/login', json={'username': 'ADMIN', 'password': 'admin-password'},
                           headers={'X-CSRF-Token': csrf}).json
        headers = {'X-CSRF-Token': login['csrf'], 'X-Browser-Token': login['browser_token']}
        alice, a = self.account('alice')
        body = {'username': 'alice', 'login_key': 'ReplacementKey123', 'admin_password': 'admin-password'}
        self.assertEqual(alice.post('/api/admin/reset-key', json=body, headers=a).status_code, 403)
        self.assertEqual(admin.post('/api/admin/reset-key', json=body).status_code, 403)
        wrong = dict(body, admin_password='wrong')
        self.assertEqual(admin.post('/api/admin/reset-key', json=wrong, headers=headers).status_code, 403)
        reset = admin.post('/api/admin/reset-key', json=body, headers=headers)
        self.assertEqual(reset.status_code, 200)
        self.assertNotEqual(reset.json['login_key'], self.keys['alice'])
        self.assertEqual(alice.get('/api/rooms', headers=a).status_code, 401)
        self.assertEqual(alice.post('/api/login', json={'login_key': self.keys['alice']}, headers=a).status_code, 401)
        self.assertEqual(alice.post('/api/login', json={'login_key': reset.json['login_key']}, headers=a).status_code, 200)
        self.assertEqual(admin.post('/api/admin/reset-key', json=dict(body, username='admin'), headers=headers).status_code, 403)

    def test_key_creation_and_legacy_migration(self):
        admin, a = self.account('admin')
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute("UPDATE users SET is_admin=1 WHERE username='admin'")
        created = admin.post('/api/admin/users', json={'username': 'newuser', 'login_key': 'ChosenKey123'}, headers=a)
        self.assertEqual(created.status_code, 201)
        key = created.json['login_key']
        self.assertEqual(key, 'ChosenKey123')
        self.assertEqual(admin.post('/api/admin/users', json={'username': 'otheruser', 'login_key': key}, headers=a).status_code, 409)
        for invalid in ['', 'short', 'key with spaces', 'x'*129, None]:
            self.assertEqual(admin.post('/api/admin/users', json={'username': 'otheruser', 'login_key': invalid}, headers=a).status_code, 400)
        client = self.app.test_client()
        csrf = client.get('/api/session').json['csrf']
        h = {'X-CSRF-Token': csrf}
        for invalid in [None, [], 'bad', secrets.token_urlsafe(32)]:
            self.assertEqual(client.post('/api/login', json={'login_key': invalid}, headers=h).status_code, 401)
        login = client.post('/api/login', json={'login_key': key}, headers=h)
        self.assertEqual(login.status_code, 200)
        self.assertEqual(login.json['user']['username'], 'newuser')
        self.assertNotIn('login_key_hash', login.json['user'])
        self.assertNotIn('login_key', login.json)
        with self.app.app_context():
            db = get_db()
            digest = db.execute("SELECT login_key_hash FROM users WHERE username='newuser'").fetchone()[0]
            self.assertNotEqual(key, digest)
            from chat.routes import key_digest
            self.assertEqual(digest, key_digest(key))
            from chat.db import init_db
            init_db()
            self.assertEqual(db.execute("SELECT login_key_hash FROM users WHERE username='newuser'").fetchone()[0], digest)
        legacy, old = self.account('legacy')
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute("UPDATE users SET login_key_hash=NULL WHERE username='legacy'")
        self.assertEqual(legacy.post('/api/login', json={'username': 'legacy', 'password': 'test-password'}, headers=old).status_code, 401)
        collision = admin.post('/api/admin/reset-key', json={'username': 'legacy', 'login_key': key, 'admin_password': 'test-password'}, headers=a)
        self.assertEqual(collision.status_code, 409)
        issued = admin.post('/api/admin/reset-key', json={'username': 'legacy', 'login_key': 'LegacyChosenKey123', 'admin_password': 'test-password'}, headers=a)
        self.assertEqual(issued.status_code, 200)
        self.assertEqual(legacy.post('/api/login', json={'login_key': issued.json['login_key']}, headers=old).status_code, 200)
        self.assertNotIn(key, admin.get('/api/admin/users', headers=a).text)

    def test_avatar_saved_and_shared(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        self.assertEqual(alice.post('/api/avatar', json={'avatar': 'invalid'}, headers=a).status_code, 400)
        self.assertEqual(alice.post('/api/avatar', json={'avatar': 'panda'}).status_code, 403)
        self.assertEqual(alice.post('/api/avatar', json={'avatar': 'panda'}, headers=a).status_code, 200)
        self.assertEqual(alice.get('/api/session', headers=a).json['user']['avatar'], 'panda')
        alice.post('/api/rooms/1/messages', json={'body': 'hello'}, headers=a)
        self.assertEqual(bob.get('/api/rooms/1/messages', headers=b).json['messages'][0]['avatar'], 'panda')

    def test_clear_chat_scope_and_authorization(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        eve, e = self.account('eve')
        room = alice.post('/api/direct', json={'username': 'bob'}, headers=a).json['id']
        path = f'/api/rooms/{room}/messages'
        alice.post(path, json={'body': 'private'}, headers=a)
        alice.post('/api/rooms/1/messages', json={'body': 'keep'}, headers=a)
        self.assertEqual(eve.delete(path, headers=e).status_code, 403)
        self.assertEqual(alice.delete(path).status_code, 403)
        self.assertEqual(bob.delete(path, headers=b).status_code, 200)
        self.assertEqual(alice.get(path, headers=a).json['messages'], [])
        self.assertEqual(len(alice.get('/api/rooms/1/messages', headers=a).json['messages']), 1)

    def test_seen_expiry_and_cookie_alone_cannot_login(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        self.assertIsNone(alice.get('/api/session').json['user'])
        self.assertEqual(alice.get('/api/rooms').status_code, 401)
        message_id = alice.post('/api/rooms/1/messages', json={'body': 'temporary'}, headers=a).json['id']
        now = time.time()
        with patch('chat.routes.time.time', return_value=now):
            alice.post('/api/rooms/1/seen', json={'ids': [message_id]}, headers=a)
            with self.app.app_context():
                self.assertIsNone(get_db().execute('SELECT seen_at FROM messages WHERE id=?', (message_id,)).fetchone()[0])
            bob.get('/api/rooms/1/messages', headers=b)
            with self.app.app_context():
                self.assertIsNone(get_db().execute('SELECT seen_at FROM messages WHERE id=?', (message_id,)).fetchone()[0])
            bob.post('/api/rooms/1/seen', json={'ids': [message_id]}, headers=b)
        with patch('chat.routes.time.time', return_value=now + 299):
            bob.post('/api/rooms/1/seen', json={'ids': [message_id]}, headers=b)
            self.assertEqual(len(alice.get('/api/rooms/1/messages', headers=a).json['messages']), 1)
        with patch('chat.routes.time.time', return_value=now + 300):
            self.assertEqual(alice.get('/api/rooms/1/messages', headers=a).json['messages'], [])
            self.assertEqual(alice.post('/api/rooms/1/state', json={'ids': [message_id]}, headers=a).json['alive'], [])
            next_id = alice.post('/api/rooms/1/messages', json={'body': 'next'}, headers=a).json['id']
            self.assertGreater(next_id, message_id)


if __name__ == '__main__':
    unittest.main()
