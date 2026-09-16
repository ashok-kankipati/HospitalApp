/* DOMPurify is vendored from the locked frontend dependency during UI builds. */
(() => {
    'use strict';
    const actions = new Set(['deleteAppointment', 'deleteBatch', 'deleteMedicine', 'deletePatient', 'deleteStaff',
        'dischargeAdmission', 'downloadInvoicePdf', 'editAppointment', 'editPatient', 'editStaff',
        'markNotificationRead', 'openAdmitPatientModal', 'openDispenseModal', 'openLabReportModal',
        'openLabResultsModal', 'openPaymentModal', 'saveLabResult', 'sendAppointmentWhatsApp',
        'updateBedStatus', 'updateLabOrderStatus', 'viewAppointment', 'viewInvoice', 'viewPatient', 'viewStaff',
        'deleteAllergy', 'deleteCondition', 'deleteHistory', 'editAllergy', 'editCondition', 'editHistory']);
    window.CareflowHTML = Object.freeze({
        sanitize: value => {
            const config = { USE_PROFILES: { html: true }, FORBID_TAGS: ['style'], FORBID_ATTR: ['srcdoc'] };
            // HTML parsers need a table context to preserve row/cell fragments.
            const text = String(value ?? '');
            if (/^\s*<tr[\s>]/i.test(text)) {
                const fragment = window.DOMPurify.sanitize('<table><tbody>' + text + '</tbody></table>', { ...config, RETURN_DOM_FRAGMENT: true });
                return fragment.querySelector('tbody')?.innerHTML || '';
            }
            if (/^\s*<t[dh][\s>]/i.test(text)) {
                const fragment = window.DOMPurify.sanitize('<table><tbody><tr>' + text + '</tr></tbody></table>', { ...config, RETURN_DOM_FRAGMENT: true });
                return fragment.querySelector('tr')?.innerHTML || '';
            }
            return window.DOMPurify.sanitize(text, config);
        }
    });
    // Data-driven actions replace executable event attributes in dynamic markup.
    document.addEventListener('click', event => {
        const button = event.target.closest?.('button[data-cf-action]');
        if (!button || button.disabled) return;
        const action = button.dataset.cfAction;
        if (action === 'removeRow()') { button.parentElement.remove(); return; }
        const match = /^([A-Za-z]+)\((.*)\)$/.exec(action);
        if (!match || !actions.has(match[1])) return;
        const args = [];
        for (const value of match[2].split(',').map(value => value.trim())) {
            if (/^-?\d+(\.\d+)?$/.test(value)) args.push(Number(value));
            else if (value === 'this') args.push(button);
            else if (/^'(AVAILABLE|MAINTENANCE|COMPLETED|IN_PROGRESS)'$/.test(value)) args.push(value.slice(1, -1));
            else return;
        }
        if (typeof window[match[1]] === 'function') window[match[1]](...args);
    });
})();
