/* Shared outline action icons for the clinical workflows and patient history. */
(() => {
    const ns = 'http://www.w3.org/2000/svg';
    const icons = {
        view: ['M7 17 17 7', 'M7 7h10v10'],
        edit: ['m16 3 5 5', 'M4 16 16 4a2.1 2.1 0 0 1 3 3L7 19l-4 1z'],
        delete: ['M3 6h18', 'M9 6V4h6v2', 'M5 6l1 14h12l1-14', 'M10 10v6', 'M14 10v6'],
        archive: ['M3 3h18v4H3z', 'M5 7v14h14V7', 'M10 11h4'],
        cancel: ['M18 6 6 18', 'm6 6 12 12'],
        whatsapp: ['M21 11.5a8.5 8.5 0 0 1-12.8 7.3L3 21l2.2-5.2A8.5 8.5 0 1 1 21 11.5Z', 'M8 8c0 4 4 8 8 8', 'm8 8 2-1 1 3-2 1', 'm13 14 1-2 3 1-1 3'],
        pdf: ['M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z', 'M14 2v6h6', 'M12 11v7', 'm9 15 3 3 3-3'],
        pay: ['M3 5h18v14H3z', 'M3 10h18', 'M7 15h3'],
        dispense: ['m9 15 6-6', 'M7 17a4.25 4.25 0 0 1-6-6l10-10a4.25 4.25 0 0 1 6 6Z'],
        progress: ['m9 5 10 7-10 7z'],
        complete: ['m5 12 4 4L19 6'],
        upload: ['M12 16V3', 'm7 8 5-5 5 5', 'M4 16v5h16v-5'],
        results: ['M9 3H5v18h14V3h-4', 'M9 2h6v4H9z', 'M9 11h6', 'M9 15h6'],
        admit: ['M3 18v-7h18v7', 'M3 15h18', 'M5 11V6h14v5', 'M3 18v3', 'M21 18v3', 'M12 6v5'],
        maintenance: ['M14 6a5 5 0 0 0-6 6L3 17a2.8 2.8 0 0 0 4 4l5-5a5 5 0 0 0 6-6l-3 3-4-4z'],
        discharge: ['M9 3H4v18h5', 'M9 12h12', 'm16 7 5 5-5 5'],
        save: ['M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h12l4 4v12a2 2 0 0 1-2 2Z', 'M7 3v6h10V3', 'M7 21v-8h10v8'],
        plus: ['M12 5v14', 'M5 12h14'],
        search: ['M21 21l-5-5', 'M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0'],
        refresh: ['M20 7a8 8 0 1 0 1 8', 'M20 3v5h-5'],
        print: ['M6 9V3h12v6', 'M6 17H3V9h18v8h-3', 'M6 14h12v7H6z'],
        back: ['M19 12H5', 'm12 5-7 7 7 7'],
        loading: ['M21 12a9 9 0 1 1-6.2-8.55'],
    };
    const selector = 'button.btn-small, button.btn-primary, button.btn-secondary, button.btn-danger, button.btn, button.notification-mark-read, a.btn';
    const compactSelector = '.btn-small, .record-actions .btn, .notification-mark-read, .content-section > .btn-primary';
    function kind(label) {
        const text = label.toLowerCase().trim();
        if (/submitting|sending|saving|loading/.test(text)) return 'loading';
        if (/whatsapp/.test(text)) return 'whatsapp';
        if (/deactivate|archive/.test(text)) return 'archive';
        if (/^(delete|remove)/.test(text)) return 'delete';
        if (/^(cancel|close)/.test(text)) return 'cancel';
        if (/^(edit)/.test(text)) return 'edit';
        if (/^(view|open)/.test(text)) return 'view';
        if (/pdf|download/.test(text)) return 'pdf';
        if (/print/.test(text)) return 'print';
        if (/pay/.test(text)) return 'pay';
        if (/dispense/.test(text)) return 'dispense';
        if (/^(in progress|start)/.test(text)) return 'progress';
        if (/^(complete|finalize|available|mark as read)/.test(text)) return 'complete';
        if (/upload/.test(text)) return 'upload';
        if (/results/.test(text)) return 'results';
        if (/^admit/.test(text)) return 'admit';
        if (/maintenance/.test(text)) return 'maintenance';
        if (/discharge/.test(text)) return 'discharge';
        if (/^(save|update|confirm)/.test(text)) return 'save';
        if (/^(add|create|generate|schedule|register)/.test(text)) return 'plus';
        if (/search/.test(text)) return 'search';
        if (/refresh/.test(text)) return 'refresh';
        if (/back/.test(text)) return 'back';
        return null;
    }
    function decorate(button) {
        const label = button.querySelector('.cf-workflow-label')?.textContent || button.textContent;
        const type = kind(label || '');
        if (!type) return;
        if (button.dataset.workflowIcon === type && button.querySelector('.cf-workflow-icon')) return;
        let text = button.querySelector('.cf-workflow-label');
        if (!text) {
            text = document.createElement('span');
            text.className = 'cf-workflow-label';
            // Move, rather than replace, original label nodes and their event handlers.
            while (button.firstChild) text.appendChild(button.firstChild);
            button.appendChild(text);
        }
        button.querySelector(':scope > .cf-workflow-icon')?.remove();
        const svg = document.createElementNS(ns, 'svg');
        for (const [name, value] of Object.entries({ viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '1.7', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'aria-hidden': 'true', focusable: 'false', class: 'cf-workflow-icon' })) svg.setAttribute(name, value);
        for (const d of icons[type]) { const path = document.createElementNS(ns, 'path'); path.setAttribute('d', d); svg.appendChild(path); }
        button.prepend(svg);
        button.classList.add('cf-workflow-button');
        button.classList.toggle('cf-workflow-icon-only', button.matches(compactSelector));
        button.classList.toggle('cf-workflow-danger', ['delete', 'cancel', 'archive', 'discharge'].includes(type));
        button.dataset.workflowIcon = type;
        if (!button.dataset.workflowLabel) button.dataset.workflowLabel = button.getAttribute('aria-label') || button.title || label.trim();
        const accessibleLabel = type === 'loading' ? label.trim() : button.dataset.workflowLabel;
        button.setAttribute('aria-label', accessibleLabel);
        button.title = accessibleLabel;
        const parent = button.parentElement;
        if (parent?.tagName === 'TD' && [...parent.children].every(child => child.matches('button, a'))) {
            const group = document.createElement('div'); group.className = 'cf-workflow-actions';
            while (parent.firstChild) group.appendChild(parent.firstChild);
            parent.appendChild(group);
        }
    }
    let accountRole = null;
    const careRoles = ['Admin', 'Doctor', 'Surgeon', 'Nurse', 'Receptionist'];
    const clinicalRoles = ['Admin', 'Doctor', 'Surgeon', 'Nurse'];
    function actionRoles(button) {
        const action = button.getAttribute('data-cf-action') || button.getAttribute('onclick') || '';
        const name = action.split('(')[0].trim();
        if (/^(close|view|download|markNotificationRead)/.test(name)) return null;
        if (/^(openAddPatientModal|editPatient|openScheduleAppointmentModal|editAppointment|deleteAppointment|sendAppointmentWhatsApp)$/.test(name)) return careRoles;
        if (/^(deletePatient|openAddStaffModal|editStaff|deleteStaff)$/.test(name)) return ['Admin'];
        if (/^(openConsultation|saveConsultationNotes|createLabOrder|openHistoryModal|openAllergyModal|openConditionModal|editHistory|deleteHistory|editAllergy|deleteAllergy|editCondition|deleteCondition)$/.test(name)) return clinicalRoles;
        if (/^(addPrescriptionItemRow|createPrescription|finalizePrescription)$/.test(name)) return ['Admin', 'Doctor', 'Surgeon'];
        if (/^(openDispenseModal|confirmDispense|deleteMedicine|deleteBatch)$/.test(name)) return ['Admin', 'Pharmacist'];
        if (/^(openGenerateInvoiceModal|openPaymentModal)$/.test(name)) return ['Admin', 'Receptionist'];
        if (/^(openLabReportModal|saveLabResult|updateLabOrderStatus)$/.test(name)) return ['Admin', 'Lab Technician', 'Radiologist'];
        if (/^(openAddBedModal|openAdmitPatientModal|dischargeAdmission|updateBedStatus)$/.test(name)) return careRoles;
        if (button.id === 'openConsultationBtn') return clinicalRoles;
        if (/^add(History|Allergy|Condition)Btn$/.test(button.id)) return clinicalRoles;
        if (button.type !== 'submit') return null;
        const form = button.closest('form')?.id;
        if (['addPatientForm', 'editPatientForm', 'scheduleAppointmentForm', 'editAppointmentForm', 'addBedForm', 'admitPatientForm'].includes(form)) return careRoles;
        if (['addStaffForm', 'editStaffForm'].includes(form)) return ['Admin'];
        if (['addMedicineForm', 'addBatchForm'].includes(form)) return ['Admin', 'Pharmacist'];
        if (['generateInvoiceForm', 'paymentForm'].includes(form)) return ['Admin', 'Receptionist'];
        if (form === 'labReportForm') return ['Admin', 'Lab Technician', 'Radiologist'];
        if (['historyForm', 'allergyForm', 'conditionForm'].includes(form)) return clinicalRoles;
        return null;
    }
    function applyRole(button) {
        const roles = actionRoles(button);
        if (roles && !roles.includes(accountRole)) {
            if (!button.dataset.roleDisabled) {
                button.dataset.roleDisabled = 'true';
                button.dataset.previousDisabled = String(button.disabled);
                button.dataset.previousTitle = button.title;
            }
            button.disabled = true;
            button.title = 'Your role can view this information but cannot perform this action';
        } else if (button.dataset.roleDisabled) {
            button.disabled = button.dataset.previousDisabled === 'true';
            button.title = button.dataset.previousTitle || '';
            delete button.dataset.roleDisabled;
        }
    }
    function scan(root) {
        if (!(root instanceof Element)) return;
        if (root.matches('button')) applyRole(root);
        root.querySelectorAll('button').forEach(applyRole);
        if (root.matches(selector)) decorate(root);
        root.querySelectorAll(selector).forEach(decorate);
        if (root.matches('.badge, .status-badge')) decorateStatus(root);
        root.querySelectorAll('.badge, .status-badge').forEach(decorateStatus);
    }
    function decorateStatus(badge) {
        const raw = badge.textContent.trim();
        const status = raw.toLowerCase().replace(/[\s_]+/g, '-');
        if (badge.dataset.status === status && badge.classList.contains('cf-status-pill')) return;
        badge.dataset.status = status;
        badge.classList.add('cf-status-pill');
        badge.textContent = raw.toLowerCase().replace(/[_-]+/g, ' ').replace(/^\w/, letter => letter.toUpperCase());
    }
    const root = document.getElementById('legacy-workspace') || document.body;
    scan(root);
    fetch('/api/auth/session').then(response => response.ok ? response.json() : null).then(user => { accountRole = user?.role || null; scan(root); }).catch(() => {});
    new MutationObserver(records => {
        const changed = new Set();
        for (const record of records) {
            const element = record.target instanceof Element ? record.target : record.target.parentElement;
            const button = element?.closest(selector);
            if (button) changed.add(button);
            const badge = element?.closest('.badge, .status-badge');
            if (badge) changed.add(badge);
            for (const node of record.addedNodes) if (node instanceof Element) changed.add(node);
        }
        changed.forEach(scan);
    }).observe(root, { childList: true, subtree: true, characterData: true });
})();
