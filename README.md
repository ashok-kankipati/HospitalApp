# Household Expense Tracker

A tiny local web app for logging daily household purchases and
viewing the total spent for any month. Built with Flask + SQLite,
served in production by `waitress`, and designed to run as a
background Windows service via **NSSM**.

## Features
- Separate password-protected accounts
- BitLocker-style 48-digit recovery keys for forgotten usernames and passwords
- Add a purchase (date, description, category, amount)
- See today's purchases and a running total for the current month
- Jump to any month and see: total spent, breakdown by category,
  and the full list of purchases for that month
- Delete a mistaken entry
- Data is stored in a local SQLite file (`expenses.db`), so it
  survives service restarts and PC reboots

## 1. Prerequisites (on the Windows machine)
- Python 3.9+ installed and on PATH (https://www.python.org/downloads/windows/)
  - During install, tick "Add python.exe to PATH"
- NSSM (https://nssm.cc/download) — unzip it somewhere, e.g. `C:\nssm`

## 2. Get the app onto the machine
Copy this whole `household-expenses` folder to somewhere permanent, e.g.:

```
C:\Apps\household-expenses
```

## 3. Install dependencies
Open PowerShell / Command Prompt in that folder:

```powershell
cd C:\Apps\household-expenses
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

## 4. Test it manually first

Configure a unique application secret before starting the app:

```powershell
$env:EXPENSES_SECRET_KEY = "replace-with-a-long-random-value"
```

Keep the application secret private and unchanged after accounts are created.

```powershell
venv\Scripts\python.exe serve.py
```
Then open a browser to **http://localhost:8085**. Add a test
expense, check it appears, then check the "This Month's Summary"
page. Stop it with Ctrl+C once you've confirmed it works.

(To change the port, edit `PORT` in `serve.py`.)

## 5. Install as a Windows service with NSSM

Open an **elevated (Administrator)** PowerShell/Command Prompt:

```powershell
cd C:\nssm\win64      REM or win32, depending on your OS

nssm install HouseholdExpenses "C:\Apps\household-expenses\venv\Scripts\python.exe" "C:\Apps\household-expenses\serve.py"

nssm set HouseholdExpenses AppDirectory "C:\Apps\household-expenses"
nssm set HouseholdExpenses DisplayName "Household Expense Tracker"
nssm set HouseholdExpenses Description "Local web app for tracking household purchases"
nssm set HouseholdExpenses Start SERVICE_AUTO_START

REM Required for secure sessions
nssm set HouseholdExpenses AppEnvironmentExtra EXPENSES_SECRET_KEY="replace-with-a-long-random-value"

REM Optional but recommended: capture logs
nssm set HouseholdExpenses AppStdout "C:\Apps\household-expenses\logs\stdout.log"
nssm set HouseholdExpenses AppStderr "C:\Apps\household-expenses\logs\stderr.log"

nssm start HouseholdExpenses
```

Check it's running:
```powershell
nssm status HouseholdExpenses
```

Then open **http://localhost:8085** (or `http://<machine-ip>:8085`
from another device on your home network) any time — the service
starts automatically on boot.

## 6. Managing the service
```powershell
nssm stop HouseholdExpenses
nssm restart HouseholdExpenses
nssm remove HouseholdExpenses confirm   REM uninstall
```
You can also manage it visually via `services.msc` (look for
"Household Expense Tracker").

## 7. Backing up your data
All data lives in one file: `expenses.db` in the app folder. Just
copy that file somewhere safe periodically (or set up a scheduled
task to copy it nightly).

## Notes / possible extensions
- Currency symbol (₹) is hardcoded in the templates — change it in
  `templates/index.html` and `templates/month.html` if needed.
- No login/auth is included since this is meant for a trusted home
  network. If you expose it beyond your LAN, put it behind a
  reverse proxy with authentication (e.g. IIS or nginx) — don't
  expose port 8085 directly to the internet.
- To back it with a different DB (e.g. multi-user), swap SQLite
  for something like PostgreSQL and adjust `get_db()` in `app.py`.
