# Update an existing PythonAnywhere deployment

## Unique-key sign-in update

Regular users now sign in with their unique key only. After Web > Reload, refresh the page and choose Administrator sign-in using your existing administrator username and password.

- New user: Create account > enter username and your chosen unique key > create the account and share the key privately.
- Existing user: Issue / reset key > enter their username, your chosen replacement key, and your admin password > save and share the key. This preserves the account and chats, invalidates any old key, and signs out existing sessions.
- Users: refresh their chat page and paste their key. No username or password is needed for regular sign-in.

Keys must contain 8-128 characters with no spaces and must not be assigned to another account. A lost key can be replaced by the administrator. Preserve the existing signing secret; it protects stored key digests. Previously generated keys continue to work. Existing administrators keep their username/password access. The update automatically adds key storage without deleting accounts or creating backup files. All existing regular users need an issued key to sign in again. No new packages are required for this update.


These commands assume your existing project is ~/chatingApp and virtualenv is gather-env. If your Web tab lists different locations, use those existing locations instead. Keep the existing instance directory; it contains your accounts, database, and signing key.

1. Upload gather-deploy.zip using the Files tab into your home directory (/home/YOUR_USERNAME).
2. Open a Bash console, activate your existing environment, and update without creating a backup:

```bash
workon gather-env
cd ~/chatingApp
unzip -o ~/gather-deploy.zip -d ~/chatingApp
python -m pip install -r requirements.txt
python -m unittest discover -s tests -v
```


3. In Web, confirm the Source code and Working directory both point to /home/YOUR_USERNAME/chatingApp. Keep your existing virtualenv, whose Python version must match the web app.
4. In the WSGI configuration file linked from Web, use the following (replace YOUR_USERNAME):

```python
import sys
project = '/home/YOUR_USERNAME/chatingApp'
if project not in sys.path:
    sys.path.insert(0, project)
from wsgi import application
```

Keep any existing custom SECRET_KEY or database configuration you previously used. The bundled wsgi.py enables secure cookies; use HTTPS and enable Force HTTPS in Web.

5. Confirm the static mapping: URL /static/ -> directory /home/YOUR_USERNAME/chatingApp/chat/static. Never map instance as a static directory.
6. Click Reload in the Web tab. Do not run python app.py or flask run to serve the hosted website.
7. Open your HTTPS site. Hard-refresh desktop pages (Ctrl+Shift+R). On mobile, close the old chat tab and reopen the site; if old styling persists, clear that site's cached files. Sign in again after this update.
8. Test two users in separate tabs, sending/replying, header online/last-seen status, and logout in one tab without logging out the other.

Database changes are applied automatically when the web app loads. Existing accounts and messages are retained, subject to the existing message-expiry policy. This ZIP excludes local databases, keys, logs, virtualenvs, and caches.

If you already have an administrator, use that account. Only if you need a new dedicated administrator, run in the activated environment:

```bash
cd ~/chatingApp
flask --app app create-admin
```

Then sign in as the administrator and use Create account. Public sign-up is disabled. There is no default admin password.

If Reload fails, read the error log linked in Web. Check the project path, matching virtualenv, and that chat/tab_sessions.py was extracted.

Official references:
- https://help.pythonanywhere.com/pages/Flask
- https://help.pythonanywhere.com/pages/StaticFiles
- https://help.pythonanywhere.com/pages/ReloadWebApp/

Voice messages are supported after this update. The database adds audio storage automatically; no extra packages or upload directory are needed. Use HTTPS and allow microphone access in the browser. Tap the microphone beside Send, record up to two minutes (5 MB maximum), then stop, preview, and send. On mobile the microphone is a compact round button; on laptops it also shows a Voice label. Audio follows the same private conversation access and message deletion rules as text.


## Camera and gallery photo update

Upload the new gather-deploy.zip and follow the update commands above. Run `python -m pip install -r requirements.txt` before Web > Reload: this update adds Pillow for validating and resizing photos. The photo storage table is created automatically in the existing database; preserve instance and your existing configuration.

Use the camera icon beside the microphone/Send controls. Choose Take photo or Gallery / files, preview the image, optionally enter a caption, then Send photo. Desktop Take photo uses a webcam and requires HTTPS or localhost plus camera permission. Mobile Take photo requests the rear camera using the device file picker; the browser controls whether it opens the camera or offers a chooser. Gallery remains available if camera permission is denied. JPEG, PNG, and WebP are supported; HEIC/HEIF only works when the browser can decode it (otherwise convert to JPEG or take a new photo).

Select one photo per message, up to 20 MB locally. The browser compresses it to at most 1600 pixels per side and a maximum 5 MB upload. The server validates the image, re-encodes it as JPEG, and strips metadata. Photos are stored in SQLite and served through authenticated endpoints, never a public static directory. Reply, seen, clear-chat, and five-minute expiry behavior also apply to photos. The read timer starts when the downloaded image is visible in the focused conversation. Expired media rows are removed on the next request; database backups can still contain prior photos. SQLite may retain freed disk pages for reuse, so logical deletion does not immediately shrink the database file.

Verification: backend tests cover image validation, private access, caption limits, expiry, and cascade cleanup. Browser checks use desktop Chrome with a simulated webcam and a mobile viewport/file chooser; actual camera availability and prompts depend on the phone/browser.


## Voice-only calls

The latest ZIP includes voice calls. Upload and extract it over the existing project, install requirements, then Web > Reload. No new dependency beyond the photo update is needed. Open a private conversation with an online person and use Call in the header. The recipient sees an incoming call panel even if they are viewing a different chat. Accept, Decline, Mute/Unmute, and End call are supported. Both people must keep the app open and allow microphone access over HTTPS (localhost also works). The plain HTTP Wi-Fi address does not provide microphone access in most browsers. Phone screen locking, background tabs, and browser suspension can interrupt a call; this is not a background telephone/push-notification service.

PythonAnywhere handles only short HTTP signaling requests. Browser WebRTC carries audio, and no audio is recorded or stored. Ringing expires after 60 seconds; missing call heartbeats expire after 45 seconds, checked on signaling requests. Ended call signaling data is cleared and short-lived status rows are removed on subsequent signaling requests after two minutes. Resetting passwords, removing accounts, signing out, or closing the call stops access; ungraceful browser closures are caught by the heartbeat timeout.

The default connection setup uses a public Google STUN server. Direct peer connections may fail between certain mobile networks, routers, VPNs, or corporate networks. For reliable cross-network calling, obtain an external TURN service and configure it in the PythonAnywhere WSGI file BEFORE `from wsgi import application`:

```python
import os
os.environ['TURN_URLS'] = 'turn:YOUR_RELAY_HOST:3478,turns:YOUR_RELAY_HOST:5349'
os.environ['TURN_USERNAME'] = 'YOUR_RELAY_USERNAME'
os.environ['TURN_PASSWORD'] = 'YOUR_RELAY_PASSWORD'
```

Use actual URLs/credentials supplied by your provider, not these placeholders. TURN relays use bandwidth and may have a cost; no relay service is provisioned by this update. Credentials are delivered to signed-in browsers so WebRTC can authenticate to the relay; use limited/rotated provider credentials and usage limits. Keep credentials out of source control. Reload after changing settings. These calls are tested locally with simulated microphones and direct browser connectivity; TURN and real cellular network behavior require testing with your configured provider/devices.


## Call audio clarity update

Calls now explicitly request echo cancellation, noise suppression, automatic gain control, mono capture, and a preferred 48 kHz sample rate. Audio tracks are marked as speech and Opus is preferred while retaining fallback codecs. Other in-app audio playback pauses when a call starts. These are browser-supported preferences rather than guarantees; some browsers already enabled processing by default, and microphone hardware, speaker feedback, and packet loss still affect quality. Avoid testing two speakerphones next to each other; use headphones or separate rooms and moderate speaker volume.

Upload the updated ZIP, extract it, and Reload. BOTH callers must close/reopen or refresh their chat tabs before starting a new call to load the new audio settings. Browser tests confirm processing is enabled in Chrome and calling/mute/end still work using simulated microphones; actual acoustic quality requires testing on your devices.


## Speaker / headset selection

During a call, tap Speaker / headset. Supported browsers list audio output devices; select Speakers or a headset such as Jabra and press Use this output. Choose device invokes the browser permission picker where available. System default follows the operating system output and does not necessarily mean loudspeaker. This changes playback only, not the microphone. Some phone browsers do not expose earpiece/loudspeaker routing; the app displays guidance rather than simulating a speaker toggle. Use phone audio/Bluetooth controls or headphones on those devices. Choosing speaker output will not by itself remove echo; keep devices apart and avoid excessive speaker volume.

Update the ZIP and Reload, then reopen both chat tabs. Browser testing verified selecting the default output and the unsupported-browser fallback, along with call connection, mute, end, and decline; actual Jabra and phone hardware routing requires testing on those devices.


Mobile speaker visibility update: Speaker / headset now has its own full-width row in the call panel and appears while ringing as well as during a connected call. Reload the web app and close/reopen old mobile tabs after uploading; the frontend asset version has changed to refresh cached files. Device output selection still depends on browser support.


## Emoji reactions update

The latest ZIP includes heart, like, laugh, surprised, sad, and thanks reactions. Tap React under any message; selecting your current reaction removes it. Each person has one reaction per message. Counts refresh automatically. The reaction table is created on app startup and reactions are deleted with their messages. Upload the ZIP and run the update commands above, then Web > Reload and refresh the chat page. No additional dependency is required for reactions.


## Typing indicator

The conversation heading shows who is typing, refreshed with the existing three-second message polling. Draft text is never sent. The indicator stays active while an unsent draft exists, including pauses and tab switches. It stops on Send, clearing the draft, or switching conversations. A heartbeat refreshes the status every two seconds; if the browser closes, loses connection, or suspends background timers, stale status expires after six seconds. Upload gather-deploy.zip, extract over the existing project, then Web > Reload. Refresh both users' chat tabs and sign in again. The typing table is created automatically. The update commands do not create backups.


## Mobile header update

On screens up to 760px wide, the header places the conversation name beside the menu, Call, and options buttons. Typing status uses a separate readable line below, replacing presence while someone is typing; connection status stays on its own line. Laptop styling remains unchanged. Upload gather-deploy.zip, extract over the existing project, then Web > Reload and refresh both chat tabs.


## Mobile privacy lock

On a phone or touch device, hiding the chat tab (switching tabs/apps, locking the screen, or leaving the browser) immediately covers the app, clears the local key/draft/messages, stops media, and attempts to sign out that tab. The tab navigates to about:blank using history replacement, showing neither chat nor sign-in when reopened. Open the app URL again from your saved link to reach sign-in. This applies even when leaving the sign-in page. Local credentials are forgotten even if the background logout request did not reach the server. Other signed-in tabs are independent. Desktop-size mouse/trackpad browsers retain their existing behavior.

This deliberately prioritizes privacy: using an external camera/gallery picker or switching apps during a call can also lock the chat and discard the draft. A browser cannot reliably distinguish closing its window from temporarily switching away. Browser/OS-controlled recent-app thumbnails are outside the app's control; check this behavior on your phone. Upload the ZIP, reload the web app, then refresh existing tabs to enable the lock.
