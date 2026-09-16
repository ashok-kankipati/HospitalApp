# Security controls and production setup

Browser DevTools can inspect any response delivered to that browser. Hiding DevTools,
obfuscating JavaScript, or encrypting JSON with a browser-held key does not protect
that data. The server must decide which records and fields a user may receive and
which operations they may perform. Local edits to a response do not change the
database or grant server permissions.

## Implemented controls

- Every protected API request checks the account's immutable ID, current active
  status, and current database role. Submitted roles and localStorage are not trusted.
- `ApiPermissions` enforces module permissions. Unknown roles and Patient accounts
  cannot access clinical APIs. There is no patient self-service portal yet.
- Admin manages accounts and staff, notification settings, and all clinical modules.
  Doctor/Surgeon/Nurse can manage patient care and appointments; only Doctor/Surgeon
  can create prescriptions. Receptionist manages patient demographics, appointments,
  billing and admissions. Pharmacist manages dispensing and inventory. Lab Technician
  and Radiologist manage laboratory results. Staff have shared patient/appointment
  read access to support these workflows. Staff-directory reads omit personal contact
  fields outside Admin. Patient demographic responses omit medical-history text for
  non-clinical roles; demographic updates cannot overwrite that text.
- Patient assignment/ownership restrictions are **not implemented**. Module access
  currently covers hospital-wide records within the allowed operations. Decide the
  assignment and emergency-access rules before using this for restricted care teams.
- Notification lists and mark-read operations are filtered by server-side role;
  email bodies and internal delivery error details are not serialized.
- Mutations require `X-Requested-With: Careflow`, same-origin checks, and no permissive
  CORS configuration. Browser cross-site forms cannot supply this header. It is a
  CSRF defense, not an authentication secret. Custom API clients must send it too.
- Nested patient history/allergy/condition mutations verify that the record belongs
  to the patient in the URL. POST entity creation rejects supplied primary keys.
- Login attempts are capped at 30 per remote IP per five minutes, in a bounded,
  per-process map. Restarting a node resets these counters. Shared NAT addresses can
  hit this limit; use a distributed ingress limit for production.
- Dynamic legacy clinical HTML is sanitized with DOMPurify. Dynamic click attributes
  use an explicit action allowlist and typed argument parsing; no eval is used.
- Staff accounts does not initialize the clinical workspace or preload patient data.
- Responses have anti-framing, MIME-sniffing, referrer, and browser-permission headers.
  HSTS is sent when the servlet request is HTTPS. The current CSP restricts framing,
  objects, base URLs, and forms. It is **not a strict script CSP**: trusted legacy
  static templates still contain inline handlers and require further migration.
- API responses are not cached. Default error pages omit exception/stack details.
  Mutation logs record actor ID, module, method, and result, without request bodies.
  These logs are a starting point, not a complete tamper-resistant medical audit trail.
- Passwords use BCrypt; old plaintext accounts migrate when they log in successfully.
  Remaining dormant plaintext accounts still require a password reset/migration.

## Before production

1. Run behind HTTPS with a trusted reverse proxy. Restrict direct backend access and
   database ports. Set forwarded-header handling only for trusted proxies. Do not
   trust arbitrary `X-Forwarded-For` headers; the application rate limiter intentionally
   uses the connection address unless trusted container forwarding is configured.
2. Set `SPRING_PROFILES_ACTIVE=prod`. Supply `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
   `MAIL_USERNAME`, `MAIL_PASSWORD`, `DUO_CLIENT_ID`, `DUO_CLIENT_SECRET`, `DUO_API_HOST`,
   and `DUO_REDIRECT_URI` through a secret manager/environment. Use an HTTPS Duo callback.
   Configure WhatsApp secrets separately if that integration is enabled. This profile
   requires Duo and secure HttpOnly session cookies, expires idle sessions after 15
   minutes, disables schema auto-updates, and reduces debug output. Apply reviewed
   database migrations before starting it. Do not use this profile over plain HTTP.
3. Rotate credentials previously stored in development properties or repository
   history. The development properties were not removed in this change to avoid
   silently breaking the existing local installation; they are not a production
   secrets-management solution. Do not commit real credentials or patient exports.
4. Agree on the role matrix and patient assignment restrictions. Independently test
   direct API access, cross-patient IDs, financial values, laboratory result changes,
   and record-level permissions with representative accounts.
5. Review remaining entity-binding endpoints and financial/clinical validation. This
   change is not a complete validation audit of every domain object. Extend DTOs and
   tests for each business invariant before exposure to untrusted users.
6. Scan Maven dependencies and move to a supported, patched Spring Boot release with
   regression testing. The current backend dependency stack has not been certified
   free of vulnerabilities. Frontend `npm audit` reported zero known issues during
   this change; that is not proof of application security. Keep DOMPurify current;
   UI builds copy the locked dependency into both static and bundled workflow assets.
7. Configure encrypted backups and restore tests, least-privilege DB credentials,
   central access/mutation audit retention, anomaly alerts, distributed rate limits,
   and an incident-response process. Obtain an independent penetration test and
   applicable healthcare/privacy review before using real patient data publicly.

## Validation

The targeted tests cover server role checks, stale/deleted sessions, missing request
headers, rate limiting, nested patient ownership checks, malicious rendered markup,
and account-page data loading. They do not simulate every attack or prove compliance.

References: [OWASP authorization](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html),
[OWASP CSRF](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html),
[DOMPurify](https://github.com/cure53/DOMPurify).
