# Gather chat

A responsive chat app for a small group, built for PythonAnywhere free hosting.

- **Frontend:** HTML, CSS, vanilla JavaScript; mobile conversation menu, public rooms, private conversations, earlier-message loading.
- **Backend:** Flask JSON API with session authentication, hashed passwords, CSRF protection, input validation, and database-backed request limits.
- **Database:** SQLite using Python's built-in sqlite3; automatically initialized on startup. Parameterized queries and indexed message history.
- **Updates:** short HTTP polling every 3 seconds while the tab is visible; conversation list refreshes about every 15 seconds. No Node build, Redis, background worker, or WebSocket service needed.

## Run locally

Use Python 3.10 or newer. From the project directory:

```powershell
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt
.venv\Scripts\python app.py
```

Open http://127.0.0.1:5000. Create an administrator with `flask --app app create-admin`, sign in, and use **Create account** to add users. Sign in with two accounts in separate browser profiles or a private window to test conversations. Click the plus next to Direct Messages and enter the other account's username. Public rooms are readable by every signed-in user.

On macOS/Linux, use `.venv/bin/python` instead of `.venv\Scripts\python`.

## Deploy on PythonAnywhere free

1. Create a free PythonAnywhere account. Upload the project into `/home/YOUR_USERNAME/chatingApp` using the Files tab or clone your own repository in a Bash console. Upload `app.py`, `wsgi.py`, `requirements.txt`, and the entire `chat` directory. Do not upload your local `.venv` or `instance` directory.
2. In a PythonAnywhere **Bash console**, run:

   ```bash
   cd /home/YOUR_USERNAME/chatingApp
   mkvirtualenv --python=/usr/bin/python3.13 gather-env
   pip install -r requirements.txt
   ```

   Choose an available Python version if 3.13 is not offered; use that same version in the next step.
3. In **Web → Add a new web app**, choose the free domain, **Manual configuration**, and the matching Python version.
4. Set **Source code** and **Working directory** to `/home/YOUR_USERNAME/chatingApp`. Set **Virtualenv** to `/home/YOUR_USERNAME/.virtualenvs/gather-env`.
5. Open the WSGI configuration file linked in the Web tab. Replace its contents with:

   ```python
   import sys
   project = '/home/YOUR_USERNAME/chatingApp'
   if project not in sys.path:
       sys.path.insert(0, project)
   from wsgi import application
   ```

6. Add a static mapping: URL `/static/` → directory `/home/YOUR_USERNAME/chatingApp/chat/static`. Do not expose `instance` as a static directory.
7. Enable **Force HTTPS** in the Web tab. The supplied `wsgi.py` enables secure session cookies, which require HTTPS.
8. Click **Reload** and open `https://YOUR_USERNAME.pythonanywhere.com`. Create your administrator using the console command below, then sign in. If there is an error, inspect the error log linked in the Web tab.

The database and random signing key are created in `instance/` automatically. Keep this directory during code updates. Optionally set a stable `SECRET_KEY` environment variable in your WSGI configuration before importing the application; otherwise the generated key persists on disk. Never share the signing key.

PythonAnywhere currently lists 512 MiB storage, one web worker, and a one-month expiry for free web apps. Check the Web tab and renew before expiry. SQLite is available to everyone; new free accounts do not receive MySQL. This setup is intended for small groups and light traffic. Polling, message storage, and password hashing consume hosting resources. It has no attachments, email password recovery, moderation dashboard, or end-to-end encryption. Private-message access is enforced by the server; the hosting/database owner can access stored messages.

Deployment references: [Flask setup](https://help.pythonanywhere.com/pages/Flask), [database availability](https://help.pythonanywhere.com/pages/KindsOfDatabases), [free account limits](https://help.pythonanywhere.com/pages/FreeAccountsFeatures).

## Session privacy

Each page uses a random key held only in memory to select its server-side session, plus a separate authentication token. Different tabs can sign in as different users without sharing their session. Logout affects only the current tab. Inactive server session records are removed after 24 hours. Closing, refreshing, or opening a new tab requires sign-in, even if cookies are restored. Logout clears the session. Stale CSRF tokens recover automatically while the authenticated page remains open. Mobile browsers may suspend and restore a live tab when the app is backgrounded; this is not reliably distinguishable from closing the browser app. Online status has a separate two-minute timeout.

## Data and maintenance

Back up the database regularly from a PythonAnywhere Bash console using SQLite's backup API:

```bash
cd /home/YOUR_USERNAME/chatingApp
python -c "import sqlite3; source=sqlite3.connect('instance/chat.sqlite3'); backup=sqlite3.connect('instance/chat-backup.sqlite3'); source.backup(backup); backup.close(); source.close()"
```

Download the backup to private storage and retain `instance/secret.key` securely. Messages expire for everyone five minutes after the first other user sees them in a focused, visible conversation. Unread messages remain. Expired rows are deleted on the next server request; no background worker is needed. Existing backups may retain messages. Monitor storage usage. Rate limits are stored in SQLite; authentication uses the WSGI remote address, so users sharing an address may share the login limit.

## Verify

```bash
python -m unittest discover -s tests -v
```

Tests cover account sessions, CSRF, message validation, public history cursors, private conversation authorization, and message throttling.

Project layout: `chat/db.py` owns persistence, `chat/routes.py` owns API behavior, `chat/__init__.py` configures security and startup, `chat/templates` and `chat/static` contain the frontend. `app.py` is the local entry point; `wsgi.py` is the hosting entry point.

The Clear chat button in the conversation header asks for confirmation, then permanently deletes that conversation?s messages for everyone. Any signed-in user can clear a public room; either participant can clear a private conversation. Other conversations are unaffected.


## Sign-in keys and administrator access

Regular users sign in with a unique key only. Usernames remain their display names in chat and the names used to start direct conversations. Administrators use the Administrator sign-in button with their existing username and password.

Create account asks the administrator for a username and their chosen unique key (8-128 characters, no whitespace). The server rejects keys already assigned to another account. Share the key privately with the user. Only a keyed HMAC-SHA256 digest is stored, using the existing application signing secret; preserve that secret during deployment. Previously generated keys remain accepted. Keys are case-sensitive and should be hard to guess. Anyone holding a key can sign in as its user.

For existing accounts or lost keys, choose Issue / reset key, enter the user's username, a new unique key, and your administrator password. This preserves the user's account and conversations, invalidates their previous key, and signs out their active sessions. Administrator accounts cannot use key sign-in or be reset through this panel. The trusted server-console create-admin command remains available for creating dedicated administrators.

After deploying this update, existing regular users must receive a key from an administrator before signing in again; their old username/password login is disabled. Existing administrators retain their credentials. The schema migration adds a nullable indexed key-hash column automatically without deleting accounts or messages. Refresh all open chat tabs after deploying.
