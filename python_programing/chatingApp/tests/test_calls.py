import unittest
import test_app
from unittest.mock import patch
import time
from chat.db import get_db


class CallsTest(unittest.TestCase):
    setUp = test_app.ChatTest.setUp
    tearDown = test_app.ChatTest.tearDown
    account = test_app.ChatTest.account
    def test_call_lifecycle_and_authorization(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        eve, e = self.account('eve')
        room = alice.post('/api/direct', json={'username': 'bob'}, headers=a).json['id']
        offer = 'v=0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\n'
        body = {'room_id': room, 'offer': offer}
        self.assertEqual(eve.post('/api/calls', json=body, headers=e).status_code, 403)
        self.assertEqual(alice.post('/api/calls', json=body).status_code, 403)
        self.assertEqual(alice.post('/api/calls', json=dict(body, room_id=1), headers=a).status_code, 400)
        result = alice.post('/api/calls', json=body, headers=a)
        self.assertEqual(result.status_code, 201, result.json)
        call_id = result.json['call']['id']
        path = f'/api/calls/{call_id}'
        self.assertIsNone(eve.get('/api/calls', headers=e).json['call'])
        self.assertEqual(bob.get('/api/calls', headers=b).json['call']['id'], call_id)
        self.assertEqual(eve.post(path+'/poll', json={}, headers=e).status_code, 404)
        self.assertEqual(alice.post('/api/calls', json=body, headers=a).status_code, 409)
        self.assertEqual(alice.post(path+'/accept', json={'answer': offer}, headers=a).status_code, 409)
        self.assertEqual(bob.post(path+'/accept', json={'answer': offer}, headers=b).json['call']['status'], 'accepted')
        self.assertEqual(alice.post(path+'/poll', json={}, headers=a).json['call']['answer'], offer)
        bob.post(path+'/end', json={}, headers=b)
        ended = alice.post(path+'/poll', json={}, headers=a).json['call']
        self.assertEqual(ended['status'], 'ended')
        self.assertIsNone(ended['offer']); self.assertIsNone(ended['answer'])
        self.assertEqual(alice.post('/api/calls', json=body, headers=a).status_code, 201)
        alice.post('/api/logout', headers=a)
        self.assertIsNone(bob.get('/api/calls', headers=b).json['call'])

    def test_call_offline_timeout_and_revocation(self):
        alice, a = self.account('alice')
        bob, b = self.account('bob')
        room = alice.post('/api/direct', json={'username': 'bob'}, headers=a).json['id']
        body = {'room_id': room, 'offer': 'v=0\r\nm=audio 9 RTP/SAVPF 111\r\n'}
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute('DELETE FROM presence WHERE user_id=(SELECT id FROM users WHERE username=?)', ('bob',))
        self.assertEqual(alice.post('/api/calls', json=body, headers=a).status_code, 409)
        bob.post('/api/presence', json={}, headers=b)
        call = alice.post('/api/calls', json=body, headers=a).json['call']
        with patch('chat.calls.time.time', return_value=time.time()+61):
            result = alice.post(f"/api/calls/{call['id']}/poll", json={}, headers=a).json['call']
            self.assertEqual(result['reason'], 'No answer')
        call = alice.post('/api/calls', json=body, headers=a).json['call']
        with self.app.app_context():
            db = get_db()
            with db:
                db.execute("UPDATE users SET session_version=session_version+1 WHERE username='bob'")
        result = alice.post(f"/api/calls/{call['id']}/poll", json={}, headers=a).json['call']
        self.assertEqual(result['status'], 'ended')
