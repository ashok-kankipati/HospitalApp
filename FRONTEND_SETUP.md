# Careflow React frontend

The workspace uses React 19, TypeScript, Vite 8, Tailwind CSS 4,
TanStack Query, and Lucide icons. Exact dependencies are locked in
`frontend/package-lock.json`. Use a current Node.js LTS version supported by Vite.

## Run with Spring Boot

The production frontend is built into `src/main/resources/static/app/` and served
by Spring Boot on the same origin as the API. No second production server is needed.

```powershell
npm.cmd --prefix frontend ci
npm.cmd run ui:build
mvn package -DskipTests
```

Start Spring Boot with your existing database and Duo environment variables,
then open **http://localhost:8080/app/**. The existing `/`, `/login.html`,
`/dashboard.html`, and `/register.html` links forward into the React workspace.
Restart the application after rebuilding if it is running from a packaged JAR.

Keep Duo's existing callback `http://localhost:8080/api/auth/duo/callback`.
The callback redirects through `login.html`, preserving the Duo result, and the
React app reads the authenticated server session before displaying patient data.
No credentials or user records are built into the frontend assets.

## Frontend development

Run Spring Boot on port 8080, then:

```powershell
npm.cmd run ui:dev
```

Open http://localhost:5173/app/. Vite proxies API requests to Spring Boot.
Use localhost consistently for both servers so the session cookie is available.
With the production Duo redirect URI, a successful Duo flow returns to port 8080
and the latest built UI. To remain on Vite during Duo development, configure and
register `http://localhost:5173/api/auth/duo/callback` as the development redirect.

## Migration boundary

React and TypeScript own login, session handling, navigation, global search,
notifications, overview charts, patient directory/CRUD, and staff account creation.
Forms use native constraints and accessible dialogs; API data is cached and refreshed
with TanStack Query. Layouts work on desktop, tablet, and phone.

Appointments, consultations, pharmacy, labs, billing, beds, staff management, and
notification settings retain their existing JavaScript controllers inside a styled
React workspace adapter. This preserves their existing actions and backend contracts.
The single workflow template is `static/workflows/dashboard.html`; its original JS
and CSS are bundled by Vite. `LegacyWorkspace.tsx` mounts it once and explicitly
initializes it. Patient clinical history remains a separate existing page, linked
from the directory and styled to match. These modules are not yet native React
components. They can be migrated individually without changing the API.

Account management APIs (including legacy registration) now require an active Admin.
Clinical module APIs now enforce server-side role permissions. See SECURITY_SETUP.md for the role matrix, remaining record-assignment limitations, and production setup.

## Validation

```powershell
npm.cmd run ui:build
npm.cmd run ui:test
```

The frontend Playwright suite uses Microsoft Edge, starts Vite preview on port 4173,
and mocks APIs with synthetic patient data. It does not modify a live database or
approve a live Duo challenge. Preview screenshots are generated in
`frontend/artifacts/`. Existing repository tests target the former UI selectors;
the new frontend suite covers the React routes and workflow integration.


## Bootstrap administrator

1. Restart the backend with the updated code. Existing sessions must sign in again.
2. Open `sql/create_admin.sql`, replace the example email and password, and run it in the HospitalApp PostgreSQL database. It requires permission to install `pgcrypto` (or have your database administrator install it). The returned row confirms creation; no row means the username/email already exists.
3. Sign in as `hospital_admin` with your chosen password. If Duo is enabled, enroll that exact username in Duo and grant access to the Web SDK application.
4. Open **Staff accounts** to create logins, assign/change roles, or delete accounts. Your own admin account cannot be deleted or demoted.

New passwords use BCrypt. Existing plaintext passwords upgrade after successful login. Account lists never return passwords. Deleted/inactive users lose access on their next API request, and role changes are checked against the database on each request. Reload the UI to refresh navigation after a role change. Deleting an account does not delete clinical staff records or Duo users.

See [SECURITY_SETUP.md](SECURITY_SETUP.md) before deploying with real patient data. API mutations require the X-Requested-With: Careflow header.
