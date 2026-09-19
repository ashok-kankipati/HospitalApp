/* Shared by the React workspace and standalone patient forms. */
(() => {
    if (window.CareflowSubmissionFeedback) return;
    window.CareflowSubmissionFeedback = true;
    const originalFetch = window.fetch.bind(window);
    let pending = 0;
    let lastControl;
    let status;
    let failed = false;
    const controls = new Map();

    function show(message) {
        if (!status?.isConnected) {
            status = document.createElement('div');
            status.className = 'cf-submission-status';
            status.setAttribute('role', 'status');
            status.setAttribute('aria-live', 'polite');
            document.body.appendChild(status);
        }
        status.textContent = message;
        status.hidden = false;
    }

    function remember(control) {
        if (!control || controls.has(control)) return;
        controls.set(control, {
            busy: control.getAttribute('aria-busy'),
            disabled: control.getAttribute('aria-disabled')
        });
        control.setAttribute('aria-busy', 'true');
        control.setAttribute('aria-disabled', 'true');
        control.classList.add('cf-submission-busy');
    }

    function capture(event) {
        const control = event.type === 'submit'
            ? event.submitter || event.target.querySelector('button:not([type="button"]), input[type="submit"]')
            : event.target.closest?.('button, input[type="submit"], input[type="button"], [data-cf-action], [onclick]');
        if (pending && (event.type === 'submit' || control)) {
            event.preventDefault();
            event.stopImmediatePropagation();
            show('Saving... Please wait. Do not submit again.');
            return;
        }
        lastControl = control;
        // Avoid attributing unrelated background work to an old click.
        setTimeout(() => { if (lastControl === control) lastControl = null; }, 0);
    }
    document.addEventListener('click', capture, true);
    document.addEventListener('submit', capture, true);

    window.fetch = async (input, init) => {
        const url = new URL(input instanceof Request ? input.url : String(input), location.href);
        const method = (init?.method || (input instanceof Request ? input.method : 'GET')).toUpperCase();
        const mutation = url.origin === location.origin && url.pathname.startsWith('/api/')
            && !['GET', 'HEAD', 'OPTIONS'].includes(method);
        if (!mutation) return originalFetch(input, init);
        if (!pending) failed = false;
        pending++;
        document.body.classList.add('cf-submission-pending');
        remember(lastControl);
        show('Saving... Please wait. Do not submit again.');
        try {
            const response = await originalFetch(input, init);
            // Keep the guard through response-body delivery, not just response headers.
            await response.clone().arrayBuffer();
            if (!response.ok) failed = true;
            return response;
        } catch (error) {
            failed = true;
            throw error;
        } finally {
            // Let the caller process the response and start dependent requests first.
            setTimeout(() => {
                pending--;
                if (pending) return;
                document.body.classList.remove('cf-submission-pending');
                for (const [control, previous] of controls) {
                    for (const [attribute, value] of [['aria-busy', previous.busy], ['aria-disabled', previous.disabled]]) {
                        if (value === null) control.removeAttribute(attribute);
                        else control.setAttribute(attribute, value);
                    }
                    control.classList.remove('cf-submission-busy');
                }
                controls.clear();
                if (failed) show('Request could not be confirmed. Check the result before submitting again.');
                else status.hidden = true;
            }, 0);
        }
    };
})();
