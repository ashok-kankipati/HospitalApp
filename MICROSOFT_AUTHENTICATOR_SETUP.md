# Microsoft Authenticator setup

CareFlow uses standard TOTP codes, so Microsoft Authenticator does not require a paid Microsoft account or tenant.

## 1. Create the database table

Run `sql/totp-mfa.sql` against the production PostgreSQL database before enabling TOTP. The application also creates this table on startup when its database account has schema permissions.

## 2. Generate the encryption key

Generate one random 32-byte key and keep it stable for the lifetime of all enrollments:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Store the output in the deployment environment. Never commit it or replace it after users enroll; replacing it makes existing authenticator secrets unreadable.

## 3. Configure the application

```text
TOTP_ENABLED=true
TOTP_ISSUER=CareFlow
MFA_ENCRYPTION_KEY=<Base64 32-byte key>
DUO_ENABLED=false
```

Only one MFA provider may be enabled. CareFlow fails closed if Duo and TOTP are both enabled.

## 4. Enroll accounts

After deployment, each staff member signs in with their username and password. CareFlow shows a unique QR code. In Microsoft Authenticator, select **Add account**, then **Other account**, scan the QR, and enter the current six-digit code.

The eight recovery codes are shown once after confirmation. Each code works once. Store them outside the phone.

One test phone can enroll multiple CareFlow accounts, but each account must scan its own QR code. Production staff should enroll their own devices.

## Lost device

An administrator can open **Staff accounts** and use **Reset authenticator** for the affected account. The user will receive a new QR code after their next successful password sign-in.

## Forgotten password or username

The administrator can see each username in **Staff accounts**. To reset a forgotten password, use the **Set password** key action for that account and enter the new password twice. This changes only the password; username, email, role, and Authenticator enrollment stay unchanged. Share the new password with the staff member through a secure channel.

## Security notes

- Use HTTPS in production.
- Keep `MFA_ENCRYPTION_KEY` only in the deployment secret store.
- Rotate the previously exposed Duo client secret before using Duo again.
- Server time must be synchronized because codes use 30-second time windows.