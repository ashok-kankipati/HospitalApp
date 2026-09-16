/* Shared by React forms, clinical workflows and the patient history page. */
(() => {
    if (window.HospitalValidation) return;
    const selector = 'input, select, textarea';
    const errors = new WeakMap();
    let sequence = 0;
    let lastForm;
    const dateKey = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    const isPassword = field => field.type === 'password' || /password|confirm/i.test(field.name || '');
    function configure(field) {
        if (!(field instanceof HTMLInputElement || field instanceof HTMLSelectElement || field instanceof HTMLTextAreaElement)) return;
        const key = field.name || field.id;
        const form = field.form;
        if (form) form.noValidate = true;
        if (field instanceof HTMLInputElement && ['hidden', 'checkbox', 'radio', 'submit', 'button'].includes(field.type)) return;
        if (!(field instanceof HTMLSelectElement) && !field.hasAttribute('maxlength') && ['text', 'email', 'tel', 'password', 'url', 'textarea'].includes(field.type)) field.maxLength = field.type === 'password' ? 72 : 255;
        if (/^(name|firstName|lastName)$/.test(key) && !/Medicine/i.test(form?.id || '')) { field.dataset.validationKind = 'name'; field.maxLength = 100; }
        if (key === 'username' && field.minLength === 3) field.dataset.validationKind = 'username';
        if (field.type === 'email') { field.dataset.validationKind = 'email'; field.maxLength = 254; }
        if (field.type === 'tel') { field.dataset.validationKind = 'phone'; field.maxLength = 30; }
        if (/address/i.test(key)) field.dataset.validationKind = 'address';
        if (key === 'batchNo') field.maxLength = 100;
        if (key === 'bedNumber') field.maxLength = 50;
        if (['ward', 'department', 'position'].includes(key)) field.maxLength = 100;
        if (key === 'upiId') field.dataset.validationKind = 'upi';
        if (key === 'labReportFileUrl') field.dataset.validationKind = 'url';
        if (key === 'labReportMimeType') field.maxLength = 100;
        if (field.classList.contains('lab-result-unit')) field.maxLength = 50;
        if (field.type === 'date') {
            field.min = '0001-01-01';
            if (/dateOfBirth|DOB|historyDate|diagnosedDate/i.test(key)) field.max = dateKey(new Date());
            if (/dateOfBirth|DOB/i.test(key)) { const earliest = new Date(); earliest.setFullYear(earliest.getFullYear() - 150); field.min = dateKey(earliest); }
            if (key === 'expiryDate' || key === 'batchExpiry' || form?.id === 'scheduleAppointmentForm') field.min = dateKey(new Date());
        }
        if (field.type === 'number') {
            if (!field.hasAttribute('min')) field.min = /Id$/.test(key) ? '1' : '0';
            if (!field.hasAttribute('step')) field.step = '1';
            if (!field.hasAttribute('max')) field.max = field.step === '0.01' ? '99999999.99' : '2147483647';
        }
    }
    function message(field) {
        configure(field);
        field.setCustomValidity('');
        const value = field.value;
        const text = value.trim();
        if (field.disabled || field.type === 'hidden' || ['checkbox', 'radio', 'submit', 'button'].includes(field.type)) return '';
        if (field.validity.badInput) return 'Enter a valid number or date.';
        if (field.required && !text) return 'This field is required.';
        if (!text) return '';
        if (field.maxLength > 0 && value.length > field.maxLength) return `Use at most ${field.maxLength} characters.`;
        if (field.minLength > 0 && text.length < field.minLength) return `Use at least ${field.minLength} characters.`;
        if (!isPassword(field) && /[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/u.test(value)) return 'Remove unsupported control characters.';
        switch (field.dataset.validationKind) {
            case 'username': if (!/^[A-Za-z0-9][A-Za-z0-9._-]{2,99}$/.test(text)) return 'Use 3 to 100 letters, digits, dots, underscores or hyphens; start with a letter or digit.'; break;
            case 'name': if (!/\p{L}/u.test(text) || /[<>\r\n\t]/u.test(text)) return 'Enter a name containing letters; single and international names are welcome.'; break;
            case 'email': if (!/^[^\s@]+@[^\s@.]+(?:\.[^\s@.]+)+$/.test(text) || text.startsWith('.') || text.includes('..') || text.split('@')[0].endsWith('.')) return 'Enter a valid email address, such as name@example.com.'; break;
            case 'phone': { const digits = text.replace(/\D/g, ''); if (!/^\+?[0-9 ()-]+$/.test(text) || digits.length < 7 || digits.length > 15) return 'Enter 7 to 15 digits; an optional +, spaces, parentheses and hyphens are allowed.'; break; }
            case 'address': if (text.length < 5 || !/[\p{L}\p{N}]/u.test(text)) return 'Enter an address of at least 5 characters.'; break;
            case 'upi': if (!/^[A-Za-z0-9._-]{2,}@[A-Za-z][A-Za-z0-9.-]{1,}$/.test(text)) return 'Enter a valid UPI ID, such as hospital@bank.'; break;
            case 'url': try { const url = new URL(text); if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password) return 'Enter a complete http or https URL.'; } catch { return 'Enter a complete http or https URL.'; } break;
        }
        if (isPassword(field) && new TextEncoder().encode(value).length > 72) return 'Password must be at most 72 UTF-8 bytes.';
        if (field.name === 'confirm' && value !== field.form?.querySelector('[name="password"]')?.value) return 'Passwords must match.';
        if (field.type === 'date' && (!/^\d{4}-\d{2}-\d{2}$/.test(text) || !Number.isFinite(Date.parse(text + 'T12:00:00')))) return 'Enter a valid calendar date.';
        if (field.validity.rangeUnderflow) return field.type === 'date' ? `Choose a date on or after ${field.min}.` : `Enter a value of at least ${field.min}.`;
        if (field.validity.rangeOverflow) return field.type === 'date' ? `Choose a date on or before ${field.max}.` : `Enter a value no greater than ${field.max}.`;
        if (field.validity.stepMismatch) return field.step === '0.01' ? 'Use no more than 2 decimal places.' : 'Enter a whole number.';
        if (field.type === 'number' && !Number.isFinite(Number(text))) return 'Enter a valid number.';
        if (field.name === 'appointmentTime' && field.form?.id === 'scheduleAppointmentForm') {
            const day = field.form.querySelector('[name="appointmentDate"]')?.value;
            if (day && new Date(`${day}T${text}`) < new Date()) return 'Choose an appointment time in the future.';
        }
        if (field.validity.typeMismatch || field.validity.patternMismatch) return 'Enter a value in the required format.';
        return '';
    }
    function show(field, error) {
        let node = errors.get(field);
        if (error && !node) {
            // Keep an input's accessible name stable when its label gains error text.
            if (!field.hasAttribute('aria-label') && !field.hasAttribute('aria-labelledby') && field.labels?.length) {
                const label = field.labels[0].cloneNode(true);
                label.querySelectorAll('input, select, textarea, button, .hospital-field-error').forEach(node => node.remove());
                field.setAttribute('aria-label', label.textContent.trim());
            }
            node = document.createElement('span'); node.className = 'hospital-field-error'; node.id = `hospital-error-${++sequence}`; node.setAttribute('aria-live', 'polite');
            field.insertAdjacentElement('afterend', node); errors.set(field, node);
            field.setAttribute('aria-describedby', [field.getAttribute('aria-describedby'), node.id].filter(Boolean).join(' '));
        }
        if (node) { node.textContent = error; node.hidden = !error; }
        field.setCustomValidity(error);
        if (error) field.setAttribute('aria-invalid', 'true'); else field.removeAttribute('aria-invalid');
    }
    function validate(root) {
        let first;
        root.querySelectorAll(selector).forEach(field => {
            if (field.disabled || field.type === 'hidden') return;
            if (!isPassword(field) && !['checkbox', 'radio', 'file'].includes(field.type)) field.value = field.value.trim();
            const error = message(field); show(field, error); if (error && !first) first = field;
        });
        first?.focus(); return !first;
    }
    function scan(root) {
        if (root.matches?.(selector)) configure(root);
        root.querySelectorAll?.(selector).forEach(configure);
    }
    document.addEventListener('submit', event => {
        if (event.target instanceof HTMLFormElement) {
            lastForm = event.target;
            if (!validate(event.target)) { event.preventDefault(); event.stopImmediatePropagation(); }
        }
    }, true);
    document.addEventListener('input', event => {
        const field = event.target;
        if (field.matches?.(selector)) show(field, message(field));
    }, true);
    document.addEventListener('change', event => { if (event.target.matches?.(selector)) show(event.target, message(event.target)); }, true);
    document.addEventListener('reset', event => { event.target.querySelectorAll(selector).forEach(field => show(field, '')); }, true);
    function start() { scan(document); new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(node => { if (node.nodeType === 1) scan(node); }))).observe(document.body, { childList: true, subtree: true }); }
    function applyServerErrors(fieldErrors) {
        if (!lastForm?.isConnected || !fieldErrors) return;
        for (const field of lastForm.querySelectorAll(selector)) {
            const error = fieldErrors[field.name || field.id];
            if (error) show(field, error);
        }
    }
    window.HospitalValidation = { validate, message, applyServerErrors };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
})();
