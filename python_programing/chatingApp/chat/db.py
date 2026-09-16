import sqlite3
from flask import current_app, g


def get_db():
    if 'db' not in g:
        g.db = sqlite3.connect(current_app.config['DATABASE'], timeout=15)
        g.db.row_factory = sqlite3.Row
        g.db.execute('PRAGMA foreign_keys = ON')
    return g.db


def close_db(error=None):
    db = g.pop('db', None)
    if db is not None:
        db.close()


def init_db():
    get_db().executescript('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY, username TEXT UNIQUE COLLATE NOCASE NOT NULL,
            password TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS rooms (
            id INTEGER PRIMARY KEY, name TEXT NOT NULL,
            direct_key TEXT UNIQUE
        );
        CREATE TABLE IF NOT EXISTS members (
            room_id INTEGER REFERENCES rooms(id), user_id INTEGER REFERENCES users(id),
            PRIMARY KEY (room_id, user_id)
        );
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY, room_id INTEGER NOT NULL REFERENCES rooms(id),
            user_id INTEGER NOT NULL REFERENCES users(id), body TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
        );
        CREATE INDEX IF NOT EXISTS messages_room_id ON messages(room_id, id);
        CREATE TABLE IF NOT EXISTS message_reactions (
            message_id INTEGER NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            emoji TEXT NOT NULL, PRIMARY KEY (message_id, user_id)
        );
        CREATE TABLE IF NOT EXISTS attempts (key TEXT PRIMARY KEY, count INTEGER, start REAL);
        INSERT OR IGNORE INTO rooms(id, name) VALUES (1, 'general'), (2, 'introductions'), (3, 'random');
    ''')
    db = get_db()
    db.execute('''CREATE TABLE IF NOT EXISTS calls (
        id TEXT PRIMARY KEY, room_id INTEGER NOT NULL REFERENCES rooms(id),
        caller INTEGER NOT NULL REFERENCES users(id), callee INTEGER NOT NULL REFERENCES users(id),
        caller_token TEXT NOT NULL, callee_token TEXT,
        caller_version INTEGER NOT NULL, callee_version INTEGER NOT NULL,
        status TEXT NOT NULL, offer TEXT, answer TEXT, reason TEXT,
        created REAL NOT NULL, updated REAL NOT NULL, caller_ping REAL NOT NULL, callee_ping REAL NOT NULL)''')
    db.execute('''CREATE TABLE IF NOT EXISTS photo_messages (
        message_id INTEGER PRIMARY KEY REFERENCES messages(id) ON DELETE CASCADE,
        image BLOB NOT NULL)''')
    db.execute('''CREATE TABLE IF NOT EXISTS voice_messages (
        message_id INTEGER PRIMARY KEY REFERENCES messages(id) ON DELETE CASCADE,
        mime TEXT NOT NULL, audio BLOB NOT NULL)''')
    columns = {row['name'] for row in db.execute('PRAGMA table_info(users)')}
    for name in ('is_admin', 'session_version', 'is_removed'):
        if name not in columns:
            db.execute(f'ALTER TABLE users ADD COLUMN {name} INTEGER NOT NULL DEFAULT 0')
    if 'login_key_hash' not in columns:
        db.execute('ALTER TABLE users ADD COLUMN login_key_hash TEXT')
    db.execute('CREATE UNIQUE INDEX IF NOT EXISTS users_login_key ON users(login_key_hash)')
    if 'avatar' not in {row['name'] for row in db.execute('PRAGMA table_info(users)')}:
        db.execute("ALTER TABLE users ADD COLUMN avatar TEXT NOT NULL DEFAULT 'fox'")
    if 'seen_at' not in {row['name'] for row in db.execute('PRAGMA table_info(messages)')}:
        db.execute('ALTER TABLE messages ADD COLUMN seen_at REAL')
    if 'default_room_id' not in columns:
        db.execute('ALTER TABLE users ADD COLUMN default_room_id INTEGER')
    if 'last_seen' not in columns:
        db.execute('ALTER TABLE users ADD COLUMN last_seen REAL')
    if 'reply_to_id' not in {row['name'] for row in db.execute('PRAGMA table_info(messages)')}:
        db.execute('ALTER TABLE messages ADD COLUMN reply_to_id INTEGER')
    db.execute('''CREATE TABLE IF NOT EXISTS presence (token TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id), session_version INTEGER NOT NULL, updated_at REAL NOT NULL)''')
    db.execute('CREATE INDEX IF NOT EXISTS messages_seen ON messages(seen_at)')
    db.execute('CREATE TABLE IF NOT EXISTS message_sequence (id INTEGER PRIMARY KEY CHECK(id=1), value INTEGER NOT NULL)')
    db.execute('INSERT OR IGNORE INTO message_sequence VALUES (1, (SELECT COALESCE(MAX(id),0) FROM messages))')
    db.execute('CREATE TABLE IF NOT EXISTS tab_sessions (id TEXT PRIMARY KEY, data TEXT NOT NULL, expires_at REAL NOT NULL)')
    db.execute('''CREATE TABLE IF NOT EXISTS typing_status (
        token TEXT PRIMARY KEY, room_id INTEGER NOT NULL REFERENCES rooms(id),
        user_id INTEGER NOT NULL REFERENCES users(id), session_version INTEGER NOT NULL,
        updated_at REAL NOT NULL)''')
    db.commit()
