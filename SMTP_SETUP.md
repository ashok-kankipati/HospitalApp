# SMTP on paid Render

Deploy this code, then configure the web service environment in Render:

```text
MAIL_PROVIDER=smtp
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<sending Gmail address>
MAIL_PASSWORD=<new Gmail app password>
MAIL_FROM=<same sending Gmail address>
```

Spring Boot variable names are also supported when already configured:

```text
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<sending Gmail address>
SPRING_MAIL_PASSWORD=<Gmail app password>
MAIL_FROM=<same sending Gmail address>
```

Use one naming set consistently. Do not configure both sets with different values.

Replace an existing MAIL_PROVIDER=brevo value; environment overrides the default.
Remove BREVO_API_KEY if it is no longer used. Save and redeploy the web service.
Use a paid web service instance, not just a paid database or workspace.

Use a newly generated Gmail app password with two-step verification enabled.
Revoke the old app password previously embedded in application.properties.
Keep credentials in Render environment variables, never source code or chat.
MAIL_FROM must be your authenticated sender or an authorized sending alias.
For another provider, use its SMTP host, credentials and STARTTLS port 587.
This configuration requires STARTTLS and checks the server certificate identity.

Confirm a normal application notification reaches the recipient and check its
notification status. SENT means the SMTP server accepted the message, not that
it reached the inbox. This change does not resend older PENDING/FAILED emails.
