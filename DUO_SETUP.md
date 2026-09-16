# Duo authentication

The application uses Cisco Duo Universal Prompt after username/password verification.
Create a Web SDK application in the Duo Admin Panel and enroll users with usernames
matching their HospitalApp usernames. See https://duo.com/docs/duoweb.

Set these environment variables in the process or IDE run configuration:

```text
DUO_ENABLED=true
DUO_CLIENT_ID=<client ID from Duo>
DUO_CLIENT_SECRET=<client secret from Duo>
DUO_API_HOST=api-xxxxxxxx.duosecurity.com
DUO_REDIRECT_URI=http://localhost:8080/api/auth/duo/callback
```

Register that exact redirect URI in Duo. For deployment, use the application's
public HTTPS URL and set `SESSION_COOKIE_SECURE=true`. Configure trusted proxy
forwarding when TLS terminates at a reverse proxy so request origin checks see
the public scheme/host. Keep secrets in environment variables, not source files.
Restart the application after configuration changes.

Open the login page through Spring Boot (http://localhost:8080/login.html).
After a correct password, approve Duo and return to the dashboard. Test denial,
cancel, expired login, and logout as well. Live verification requires your Duo
account and an enrolled device; automated tests mock Duo.

Duo defaults to disabled until configured. Even when disabled, protected APIs
require a server session. When enabled, missing/invalid configuration prevents
startup and Duo errors never fall back to password-only access. Pending logins
expire after five minutes; callbacks are single-use. Sessions expire after
30 minutes of inactivity. Existing browser-only logins must log in again.

Registration now requires a logged-in session. Signed WhatsApp media links remain
accessible for Twilio and continue using their existing signature/expiry checks.
This integration does not change the existing password storage or implement
role-based authorization.
