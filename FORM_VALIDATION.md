# Form validation

Validation runs in the browser for immediate feedback and on the server before saving. Forms retain entered values, show errors beside fields, and focus the first invalid field on submission. API validation failures return HTTP 400 with `message` and `fieldErrors`; database uniqueness/reference conflicts return HTTP 409 without database internals.

## Rules

| Field | Rule |
| --- | --- |
| Patient/staff name | Required, up to 100 characters, must contain a Unicode letter. Single names, initials, accents, apostrophes and hyphens are accepted. No requirement for separate first/last names. Control characters and markup delimiters are rejected. |
| Email | Required where already required; valid email syntax and domain, at most 254 characters. Syntax validation does not verify ownership or delivery. |
| Phone | 7–15 digits, up to 30 formatted characters. Optional leading `+`, spaces, parentheses and hyphens. WhatsApp retains its stricter country-code rules. |
| Address | Optional; when provided, at least 5 and at most 255 characters, including a letter or digit. International address formats are accepted. |
| Date of birth | Real calendar date, today or earlier, no more than 150 years ago. |
| History/diagnosis date | Optional real calendar date, never in the future. |
| Staff joining date | Required real calendar date. Future start dates are allowed. |
| Appointment | Required patient/doctor, valid date and time. New or rescheduled appointments must be in the future. Updating an existing appointment without changing its original time remains possible. |
| Medicine batch | Required medicine and batch number (up to 100 characters), positive whole quantity, nonnegative price, real expiry date. Newly entered batches cannot already be expired. |
| Money | Nonnegative charges; payments/UPI amounts must be at least 0.01. Up to 8 integer digits and 2 decimal places. Payments cannot exceed the balance; discounts cannot exceed subtotal plus tax. |
| Quantities/record IDs | Positive whole numbers; fractional JSON quantities and IDs are rejected instead of truncated. Zero is allowed for a dispensing row that is not being dispensed. Selected entity references must carry positive IDs. |
| Account username | New accounts: 3–100 letters/digits/dots/underscores/hyphens, beginning with a letter or digit. Existing login usernames remain compatible. |
| Account password | Existing 8-character minimum and 72 UTF-8 byte maximum; confirmation must match. Password whitespace is never trimmed, including when the password is revealed. |
| Lab reports | Required visit, file name and complete HTTP/HTTPS URL; optional order ID. Unsafe URL schemes are rejected. |
| Free text | Bounded to the current entity storage limits (generally 255 characters); whitespace-only required values and unsupported control characters are rejected. |
| Selections | Required selections and supported status/type options are checked on the server. Nested dispensing items and lab-test IDs are validated. |

## Coverage and maintenance

The shared browser implementation is `src/main/resources/static/js/form-validation.js`, imported by the React app and loaded on the standalone patient-details page. It observes dynamically inserted forms and validates clinical actions without a form (prescribing, dispensing, consultation notes and lab results). Additional business rules remain enforced in their controllers/services.

Server constraints live on request DTOs/models and reusable constraints under `com.hospital.app.validation`. Controllers use Bean Validation. `NormalizeInput` trims surrounding whitespace on mutable request fields without changing passwords; account creation already trims its username/email in the service.

Optional fields stay optional. No new mandatory first-name, last-name, postal-code or address-format fields were added. Names and contact details cannot be verified as belonging to a real person through syntax validation alone.

Tests: `frontend/tests/validation.spec.ts` and `src/test/java/com/hospital/app/validation/` cover invalid submissions, correction, international names, dates, nested items, money, passwords, and HTTP rejection before persistence.

References: [MDN client/server form validation](https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Forms/Form_validation) and [W3C international personal names](https://www.w3.org/International/wiki/Personal_names).

## Verification results

- Production frontend build passed.
- 21 targeted backend validation/security/account/appointment tests passed.
- 5 browser validation/analytics tests passed.
- The broader suite is not fully green: `/app` has conflicting controller mappings, an account-access test supplies an incomplete authenticated session, and the pharmacist-navigation test expects a scheduling button that is absent. These are outside the validation changes.
