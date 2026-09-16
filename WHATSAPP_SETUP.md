# Appointment WhatsApp reminders

The WhatsApp action is now in the Appointments table, beside View/Edit. It is removed from lab reports. The POST endpoint is `/api/whatsapp/appointments/{id}` and the recipient comes from that appointment's patient. Only active appointments can send reminders.

Click **WhatsApp reminder** after saving an appointment to send it manually. There is no automatic scheduled delivery or inbound C/R handling yet. Replies do not confirm or reschedule appointments in the database.

With the current trial ContentSid, Twilio sends its fixed Appointment Reminders sample, including its sample date/time. The confirmation clearly warns that this does not reflect the booking. Use this only with a dummy patient.

After upgrading and configuring a suitable sender, clear TWILIO_CONTENT_SID to send text containing the actual saved appointment date/time within the 24-hour customer-service window. An approved customizable template is required for actual booking reminders outside that window.

# Trial predefined messages

To test the selected Appointment Reminders template, set this in the same PowerShell terminal used to start Java:

```powershell
$env:TWILIO_CONTENT_SID = "HXfe5ab5f00277942d4d4200328b4d403c"
mvn spring-boot:run
```

Stop Java first if running. Retain the other Twilio settings in that terminal. Refresh the dashboard after restarting.

When ContentSid is set, the app sends ONLY To, From and ContentSid. It does not generate or send lab results, Body, MediaUrl or ContentVariables. The confirmation explicitly identifies this as a predefined trial message. Use a dummy patient with your connected trial phone number. No ngrok is needed.

Clearing ContentSid selects custom report-text mode, which is not supported by the current Twilio trial. The following report-text instructions apply after upgrading and configuring a suitable sender.

# WhatsApp lab reports

The consultation lab-report list includes **Send on WhatsApp**. It sends test names, recorded results and units, reference ranges, status/flags and notes as plain text to the patient's saved number. It reads the same database records used to generate the app's PDF, not arbitrary uploaded PDF files. It does not interpret results or generate medical advice.

Messages exceeding 1600 characters are rejected before sending, without truncating results or sending partial messages. No PDF attachment or download link is sent. Ngrok/public HTTPS and a signing secret are not required for this outgoing text flow. Twilio credentials, sender, enabled flag and operator password remain required.

## Configuration

Set these environment variables before starting Spring Boot:

| Variable | Value |
| --- | --- |
| WHATSAPP_ENABLED | true |
| TWILIO_ACCOUNT_SID | Your AC-prefixed account SID |
| TWILIO_API_KEY | API key belonging to that account |
| TWILIO_API_SECRET | API key secret |
| TWILIO_WHATSAPP_FROM | Your approved sender, e.g. whatsapp:+... |
| WHATSAPP_PUBLIC_BASE_URL | Not required for report text |
| WHATSAPP_SIGNING_SECRET | Not required for report text |
| WHATSAPP_OPERATOR_PASSWORD | Separate strong password supplied to authorized staff |
| TWILIO_CONTENT_SID | Set for predefined trial messages; clear for custom report text |

Do not commit real credentials. Restart the app after changing its environment. For text-only local testing, no public tunnel is needed. Do not use real patient records in sandbox testing.

## Sandbox and templates

Activate the Twilio WhatsApp Sandbox and have the test recipient join it using the instructions in your Twilio console. Use that console's actual sender number, not numbers from the example document.

Text-only mode uses Twilio's `Body` parameter and does not send `MediaUrl`, `ContentSid`, or `ContentVariables`, even if an old template SID is configured. The recipient must have messaged the sender within the previous 24 hours; staff must confirm this before sending. Outside that window, ask the patient to message the sender first. Trial restrictions may still apply; acceptance and delivery require a live test.

Example message includes each test's recorded result, unit, reference range, status and notes. No additional interpretation is added.

## Behavior and limits

- Staff confirm the saved recipient number and patient consent before each submission.
- The new POST endpoint requires the configured operator password. It does not repair the existing application's lack of server-side authentication on other endpoints; full user authentication and authorization remain necessary before public patient-data use.
- A successful response means Twilio accepted the request, not that the patient received it. Check the Twilio console using the returned message SID for delivery status.
- No automatic retries or delivery webhooks are implemented. On timeout, check Twilio before retrying to avoid duplicates. The UI disables the button while a request is pending.
- No database migration is required. Consent confirmations and send history are not persisted by this initial integration.
- No real message is sent during automated tests.

References: https://www.twilio.com/docs/messaging/api/message-resource and https://www.twilio.com/docs/content/send-templates-created-with-the-content-template-builder
