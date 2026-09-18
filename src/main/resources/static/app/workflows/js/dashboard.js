async function hospitalResponseError(response, fallback) {
    try { const body = await response.clone().json(); window.HospitalValidation?.applyServerErrors(body.fieldErrors); return body.message || fallback; }
    catch { return fallback; }
}

// DOM Elements
const navItems = document.querySelectorAll('.nav-item');
const contentSections = document.querySelectorAll('.content-section');
const logoutBtn = document.getElementById('logoutBtn');
const userGreeting = document.getElementById('userGreeting');
const notificationBell = document.getElementById('notificationBell');
const notificationBadge = document.getElementById('notificationBadge');
const notificationDropdown = document.getElementById('notificationDropdown');
const notificationDropdownList = document.getElementById('notificationDropdownList');
const settingsInfo = document.getElementById('settingsInfo');
const pharmacyTabs = document.querySelectorAll('.pharmacy-tab');
const pharmacyTabContents = document.querySelectorAll('.pharmacy-tab-content');
const consultationTabs = document.querySelectorAll('.consultation-tab');
const consultationTabContents = document.querySelectorAll('.consultation-tab-content');
const openConsultationBtn = document.getElementById('openConsultationBtn');
const labTabs = document.querySelectorAll('.lab-tab');
const labTabContents = document.querySelectorAll('.lab-tab-content');
const bedTabs = document.querySelectorAll('.bed-tabs .pharmacy-tab');
const sidebarToggle = document.getElementById('sidebarToggle');

// Get user info from localStorage
let currentUser = null;
let currentViewedAppointmentId = null;
let currentConsultationAppointment = null;
let currentDispensePrescription = null;
let currentConsultationPrescriptionId = null;
let currentInvoiceIdForPayment = null;
let billingInitialized = false;
let labCache = {
    tests: [],
    orders: [],
    reports: [],
    ordersByStatus: {
        PENDING: [],
        IN_PROGRESS: [],
        COMPLETED: []
    }
};
let labSearchValue = '';
let pharmacyCache = {
    medicines: [],
    batches: [],
    transactions: [],
    prescriptions: [],
    prescriptionItems: [],
    dispensedItems: [],
    appointments: [],
    patients: [],
    staff: []
};

// Initialize Dashboard
window.initializeHospitalDashboard = async function () {
    if (!await checkUserAuthentication()) return;
    setupNavigation();
    setupLogout();
    displayUserInfo();
    setupPharmacyTabs();
    setupConsultationTabs();
    setupPharmacyForms();
    setupPharmacySearch();
    setupBilling();
    setupLabTabs();
    setupLabReportForm();
    setupLabSearch();
    setupBedTabs();
    setupBedForms();
    setupNotificationSettings();
    setupNotificationBell();
    setupSidebarToggle();

    if (openConsultationBtn) {
        openConsultationBtn.addEventListener('click', () => {
            if (currentViewedAppointmentId) {
                openConsultation(currentViewedAppointmentId);
            }
        });
    }
};
document.addEventListener('DOMContentLoaded', window.initializeHospitalDashboard);

function setupSidebarToggle() {
    if (!sidebarToggle) return;

    sidebarToggle.addEventListener('click', () => {
        const isOpen = document.body.classList.toggle('sidebar-open');
        sidebarToggle.setAttribute('aria-expanded', String(isOpen));
    });

    navItems.forEach(item => item.addEventListener('click', () => {
        document.body.classList.remove('sidebar-open');
        sidebarToggle.setAttribute('aria-expanded', 'false');
    }));
}

/**
 * Check if user is authenticated
 */
async function checkUserAuthentication() {
    try {
        const response = await fetch('/api/auth/session');
        if (!response.ok) throw new Error('Session expired');
        currentUser = await response.json();
        localStorage.setItem('user', JSON.stringify(currentUser));
        return true;
    } catch (error) {
        localStorage.removeItem('user');
        window.location.replace('login.html');
        return false;
    }
}

/**
 * Display user information
 */
function displayUserInfo() {
    if (currentUser) {
        userGreeting.textContent = `Welcome, ${currentUser.username}! (${currentUser.role})`;
        
        // Display settings info
        const settingsHTML = `
            <strong>Username:</strong> ${currentUser.username}<br>
            <strong>Email:</strong> ${currentUser.email}<br>
            <strong>Role:</strong> ${currentUser.role}<br>
            <strong>Login Time:</strong> ${currentUser.loginTime ? new Date(currentUser.loginTime).toLocaleString() : 'Current session'}
        `;
        if (settingsInfo) {
            settingsInfo.innerHTML = CareflowHTML.sanitize(settingsHTML);
        }
    }
}

/**
 * Setup navigation
 */
function setupNavigation() {
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();

            // Remove active class from all items
            navItems.forEach(nav => nav.classList.remove('active'));
            contentSections.forEach(section => section.classList.remove('active'));

            // Add active class to clicked item
            item.classList.add('active');

            // Show corresponding section
            const sectionId = item.getAttribute('data-section') + 'Section';
            const section = document.getElementById(sectionId);
            if (section) {
                section.classList.add('active');
                
                // Load patient data when patients section is viewed
                if (sectionId === 'patientsSection') {
                    loadPatientData();
                }
                
                // Load staff data when staff section is viewed
                if (sectionId === 'staffSection') {
                    loadStaffData();
                }

                if (sectionId === 'settingsSection') { loadNotificationSettings(); loadNotificationQueue(); }
                if (sectionId === 'billingSection') {
                    initializeBillingSection();
                }

                // Load appointment data when appointments section is viewed
                if (sectionId === 'appointmentsSection') {
                    loadAppointmentData();
                }

                if (sectionId === 'pharmacySection') {
                    loadPharmacyData();
                }

                if (sectionId === 'laboratorySection') {
                    loadLabData();
                }

                if (sectionId === 'bedsSection') {
                    loadBedData();
                }
            }
        });
    });
}

/**
 * Load patient data from API
 */
function loadPatientData() {
    fetch('/api/patients')
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to fetch patients'));
            }
            return response.json();
        })
        .then(patients => {
            displayPatients(patients);
            updatePatientCount(patients.length);
        })
        .catch(error => {
            console.error('Error loading patients:', error);
            // Display error message in table
            const tableBody = document.querySelector('.data-table tbody');
            if (tableBody) {
                tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="5" style="text-align: center; color: red;">Failed to load patient data</td></tr>');
            }
        });
}

/**
 * Display patients in table
 */
function displayPatients(patients) {
    const tableBody = document.querySelector('.data-table tbody');
    if (!tableBody) return;

    if (patients.length === 0) {
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="5" style="text-align: center;">No patients found</td></tr>');
        return;
    }

    tableBody.innerHTML = CareflowHTML.sanitize(patients.map(patient => `
        <tr>
            <td>P${String(patient.id).padStart(3, '0')}</td>
            <td>${patient.name}</td>
            <td>${patient.email}</td>
            <td>${patient.phone}</td>
            <td>
                <button class="btn-small action-button" title="View patient" aria-label="View patient" data-action="view" data-cf-action="viewPatient(${patient.id})"><span class="action-label">View</span></button>
                <button class="btn-small btn-warning action-button" title="Edit patient" aria-label="Edit patient" data-action="edit" data-cf-action="editPatient(${patient.id})"><span class="action-label">Edit</span></button>
                <button class="btn-small btn-danger action-button" title="Deactivate patient" aria-label="Deactivate patient" data-action="delete" data-cf-action="deletePatient(${patient.id})"><span class="action-label">Deactivate</span></button>
            </td>
        </tr>
    `).join(''));
}

/**
 * Update patient count in dashboard
 */
function updatePatientCount(count) {
    const cards = document.querySelectorAll('.card');
    if (cards.length > 0) {
        cards[0].querySelector('.card-value').textContent = count;
    }
}

/**
 * View patient details
 */
function viewPatient(patientId) {
    // Redirect to the patient details page
    window.location.href = `/patient-details.html?id=${patientId}`;
}

/**
 * Display patient details in modal
 */
function displayPatientDetails(patient) {
    const detailsContent = document.getElementById('patientDetailsContent');
    const detailsHTML = `
        <div class="detail-item">
            <strong>Patient ID:</strong>
            <span>P${String(patient.id).padStart(3, '0')}</span>
        </div>
        <div class="detail-item">
            <strong>Full Name:</strong>
            <span>${patient.name}</span>
        </div>
        <div class="detail-item">
            <strong>Email:</strong>
            <span>${patient.email}</span>
        </div>
        <div class="detail-item">
            <strong>Phone:</strong>
            <span>${patient.phone}</span>
        </div>
        <div class="detail-item">
            <strong>Date of Birth:</strong>
            <span>${patient.dateOfBirth}</span>
        </div>
        <div class="detail-item">
            <strong>Address:</strong>
            <span>${patient.address || 'Not provided'}</span>
        </div>
        <div class="detail-item">
            <strong>Medical History:</strong>
            <span>${patient.medicalHistory || 'Not provided'}</span>
        </div>
    `;
    detailsContent.innerHTML = CareflowHTML.sanitize(detailsHTML);
}

/**
 * Close view patient modal
 */
function closeViewPatientModal() {
    const modal = document.getElementById('viewPatientModal');
    modal.classList.remove('show');
}

/**
 * Edit patient
 */
function editPatient(patientId) {
    fetch(`/api/patients/${patientId}`)
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to fetch patient details'));
            }
            return response.json();
        })
        .then(patient => {
            // Populate form with patient data
            document.getElementById('editPatientName').value = patient.name;
            document.getElementById('editPatientEmail').value = patient.email;
            document.getElementById('editPatientPhone').value = patient.phone;
            document.getElementById('editPatientDOB').value = patient.dateOfBirth;
            document.getElementById('editPatientAddress').value = patient.address || '';
            document.getElementById('editPatientMedical').value = patient.medicalHistory || '';
            
            // Store patient ID for update
            document.getElementById('editPatientForm').dataset.patientId = patientId;
            
            // Setup form submission
            const form = document.getElementById('editPatientForm');
            form.onsubmit = handleEditPatient;
            
            // Show modal
            const modal = document.getElementById('editPatientModal');
            modal.classList.add('show');
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error loading patient details: ' + error.message);
        });
}

/**
 * Close edit patient modal
 */
function closeEditPatientModal() {
    const modal = document.getElementById('editPatientModal');
    modal.classList.remove('show');
    document.getElementById('editPatientForm').reset();
}

/**
 * Handle edit patient form submission
 */
function handleEditPatient(event) {
    event.preventDefault();

    const patientId = document.getElementById('editPatientForm').dataset.patientId;
    const formData = new FormData(event.target);
    const patient = {
        name: formData.get('name'),
        email: formData.get('email'),
        phone: formData.get('phone'),
        dateOfBirth: formData.get('dateOfBirth'),
        address: formData.get('address'),
        medicalHistory: formData.get('medicalHistory')
    };

    fetch(`/api/patients/${patientId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(patient)
    })
    .then(async response => {
        if (!response.ok) {
            throw new Error(await hospitalResponseError(response, 'Failed to update patient'));
        }
        return response.json();
    })
    .then(data => {
        alert('Patient updated successfully!');
        closeEditPatientModal();
        loadPatientData();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error updating patient: ' + error.message);
    });
}

/**
 * Delete patient
 */
function deletePatient(patientId) {
    if (confirm('Deactivate this patient?')) {
        fetch(`/api/patients/${patientId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(async response => {
            if (response.ok) {
                alert('Patient deactivated successfully');
                loadPatientData();
            } else {
                alert('Failed to deactivate patient');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error deactivating patient');
        });
    }
}

/**
 * Setup logout functionality
 */
function setupLogout() {
    logoutBtn.addEventListener('click', async () => {
        const confirmLogout = confirm('Are you sure you want to logout?');
        
        if (confirmLogout) {
            try {
                const response = await fetch('/api/auth/logout', { method: 'POST' });
                if (!response.ok) throw new Error('Logout failed');
            } catch (error) {
                alert('Could not log out. Please try again.');
                return;
            }
            // Clear user data
            localStorage.removeItem('user');
            
            // Redirect to login page
            window.location.href = 'login.html';
        }
    });
}

// Optional: Add keyboard shortcut for logout (Ctrl + L)
document.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.key === 'l') {
        logoutBtn.click();
    }
});

/**
 * Setup billing section handlers
 */
function setupBilling() {
    const patientFilter = document.getElementById('billingPatientFilter');
    const statusFilter = document.getElementById('billingStatusFilter');
    if (patientFilter) {
        patientFilter.addEventListener('change', loadBillingData);
    }
    if (statusFilter) {
        statusFilter.addEventListener('change', loadBillingData);
    }

    const generateForm = document.getElementById('generateInvoiceForm');
    if (generateForm) {
        generateForm.onsubmit = handleGenerateInvoice;
    }

    const paymentForm = document.getElementById('paymentForm');
    if (paymentForm) {
        paymentForm.onsubmit = handlePaymentSubmit;
    }

    const paymentMethod = document.getElementById('paymentMethod');
    if (paymentMethod) {
        paymentMethod.addEventListener('change', updatePaymentMethodUI);
    }
    const paymentAmount = document.getElementById('paymentAmount');
    if (paymentAmount) {
        paymentAmount.addEventListener('input', updateUpiQrPreview);
    }
    const upiIdInput = document.getElementById('upiId');
    if (upiIdInput) {
        upiIdInput.addEventListener('input', updateUpiQrPreview);
    }

    // Load patient filter early so "All Patients" list is ready when opening Billing.
    // Billing filters load when the billing workspace opens.
}

/**
 * Load staff data from API
 */
function loadStaffData() {
    fetch('/api/staff')
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to fetch staff'));
            }
            return response.json();
        })
        .then(staff => {
            displayStaff(staff);
            updateStaffCount(staff.length);
        })
        .catch(error => {
            console.error('Error loading staff:', error);
            const tableBody = document.querySelector('#staffSection .data-table tbody');
            if (tableBody) {
                tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="6" style="text-align: center; color: red;">Failed to load staff data</td></tr>');
            }
        });
}

/**
 * Display staff in table
 */
function displayStaff(staff) {
    const tableBody = document.querySelector('#staffSection .data-table tbody');
    if (!tableBody) return;

    if (staff.length === 0) {
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="6" style="text-align: center;">No staff found</td></tr>');
        return;
    }

    tableBody.innerHTML = CareflowHTML.sanitize(staff.map(member => `
        <tr>
            <td>S${String(member.id).padStart(3, '0')}</td>
            <td>${member.name}</td>
            <td>${member.position}</td>
            <td>${member.department}</td>
            <td>${member.email}</td>
            <td>
                <button class="btn-small action-button" title="View staff member" aria-label="View staff member" data-action="view" data-cf-action="viewStaff(${member.id})"><span class="action-label">View</span></button>
                <button class="btn-small btn-warning action-button" title="Edit staff member" aria-label="Edit staff member" data-action="edit" data-cf-action="editStaff(${member.id})"><span class="action-label">Edit</span></button>
                <button class="btn-small btn-danger action-button" title="Delete staff member" aria-label="Delete staff member" data-action="delete" data-cf-action="deleteStaff(${member.id})"><span class="action-label">Delete</span></button>
            </td>
        </tr>
    `).join(''));
}

/**
 * Update staff count in dashboard
 */
function updateStaffCount(count) {
    const cards = document.querySelectorAll('.card');
    if (cards.length > 2) {
        cards[2].querySelector('.card-value').textContent = count;
    }
}

function loadBedsSummary() {
    const target = document.getElementById('dashboardAvailableBeds');
    if (!target) return;
    fetch('/api/beds/summary')
        .then(response => response.ok ? response.json() : null)
        .then(summary => {
            if (!summary || typeof summary.available === 'undefined') return;
            target.textContent = summary.available;
        })
        .catch(error => {
            console.error('Error loading bed summary:', error);
        });
}

/**
 * Initialize billing section data (only once)
 */
function initializeBillingSection() {
    if (!billingInitialized) {
        billingInitialized = true;
    }
    loadBillingFilters();
    loadBillingData();
}

/**
 * Load billing filters (patients)
 */
function loadBillingFilters() {
    fetch('/api/patients?includeInactive=true')
        .then(response => response.ok ? response.json() : [])
        .then(patients => {
            const select = document.getElementById('billingPatientFilter');
            if (!select) return;
            select.innerHTML = CareflowHTML.sanitize('<option value="">All Patients</option>' +
                patients.map(p => `<option value="${p.id}">${p.name}</option>`).join(''));
        })
        .catch(error => {
            console.error('Error loading billing filters:', error);
        });
}

/**
 * Load invoices for billing section
 */
function loadBillingData() {
    const tableBody = document.querySelector('#billingTable tbody');
    if (!tableBody) return;

    const patientFilter = document.getElementById('billingPatientFilter');
    const statusFilter = document.getElementById('billingStatusFilter');
    const patientId = patientFilter ? patientFilter.value : '';
    const status = statusFilter ? statusFilter.value : '';

    const invoicesRequest = patientId
        ? fetch(`/api/billing/invoices/patient/${patientId}`)
        : fetch('/api/billing/invoices');

    Promise.all([
        invoicesRequest.then(r => r.ok ? r.json() : []),
        fetch('/api/patients?includeInactive=true').then(r => r.ok ? r.json() : []),
        fetch('/api/billing/invoices/payment-options').then(r => r.ok ? r.json() : { enabled: false, keyId: '' })
    ])
        .then(([invoices, patients, paymentOptions]) => {
            const patientMap = {};
            patients.forEach(p => patientMap[p.id] = p.name);
            const onlineEnabled = paymentOptions && paymentOptions.enabled;

            let filtered = invoices || [];
            if (status) {
                filtered = filtered.filter(inv => inv.status === status);
            }

            if (!filtered.length) {
                tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="9" style="text-align: center;">No invoices found</td></tr>');
                return;
            }

            tableBody.innerHTML = CareflowHTML.sanitize(filtered.map(inv => `
                <tr>
                    <td>${inv.invoiceNumber || '-'}</td>
                    <td>${patientMap[inv.patientId] || 'Unknown'}</td>
                    <td>${inv.appointmentId ? `A${String(inv.appointmentId).padStart(3, '0')}` : '-'}</td>
                    <td><span class="badge ${getInvoiceStatusBadgeClass(inv.status)}">${inv.status || 'PENDING'}</span></td>
                    <td>${formatCurrency(inv.total)}</td>
                    <td>${formatCurrency(inv.amountPaid)}</td>
                    <td>${formatCurrency(inv.balanceDue)}</td>
                    <td>${getInvoiceEmailBadge(inv)}</td>
                    <td>
                        <div class="billing-actions">
                            <button class="btn-small btn-info" data-cf-action="viewInvoice(${inv.id})">View</button>
                            <button class="btn-small" data-cf-action="downloadInvoicePdf(${inv.id})">PDF</button>
                            ${inv.balanceDue > 0 ? `<button class="btn-small btn-warning" data-cf-action="openPaymentModal(${inv.id}, ${inv.balanceDue})">Pay</button>` : ''}
                            ${onlineEnabled && inv.balanceDue > 0 ? `<button class="btn-small btn-primary" data-cf-action="openPaymentModal(${inv.id}, ${inv.balanceDue})">Pay Online</button>` : ''}
                        </div>
                    </td>
                </tr>
            `).join(''));
        })
        .catch(error => {
            console.error('Error loading invoices:', error);
            tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="9" style="text-align: center; color: red;">Failed to load invoices</td></tr>');
        });
}

function getInvoiceStatusBadgeClass(status) {
    switch (status) {
        case 'PAID':
            return 'badge-success';
        case 'PARTIAL':
            return 'badge-warning';
        case 'PENDING':
            return 'badge-warning';
        default:
            return '';
    }
}

function formatCurrency(value) {
    const numberValue = Number(value);
    if (Number.isNaN(numberValue)) return '0.00';
    return numberValue.toFixed(2);
}

function formatDateTime(value) {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleString();
}

function getInvoiceEmailBadge(invoice) {
    if (invoice && invoice.emailSentAt) {
        const sentAt = formatDateTime(invoice.emailSentAt);
        const title = sentAt ? ` title="Sent ${sentAt}"` : '';
        return `<span class="badge badge-success"${title}>Sent</span>`;
    }
    if (invoice && invoice.status === 'PAID') {
        return '<span class="badge badge-warning">Pending</span>';
    }
    return '-';
}

/**
 * Open generate invoice modal
 */
function openGenerateInvoiceModal() {
    const modal = document.getElementById('generateInvoiceModal');
    if (!modal) return;
    modal.classList.add('show');

    const appointmentSelect = document.getElementById('invoiceAppointment');
    if (!appointmentSelect) return;

    Promise.all([
        fetch('/api/appointments').then(r => r.ok ? r.json() : []),
        fetch('/api/patients').then(r => r.ok ? r.json() : [])
    ])
        .then(([appointments, patients]) => {
            const patientMap = {};
            patients.forEach(p => patientMap[p.id] = p.name);
            appointmentSelect.innerHTML = CareflowHTML.sanitize('<option value="">Select Appointment</option>' +
                appointments.map(a => `<option value="${a.id}">A${String(a.id).padStart(3, '0')} - ${patientMap[a.patientId] || 'Patient'}</option>`).join(''));
        })
        .catch(error => {
            console.error('Error loading appointments:', error);
            appointmentSelect.innerHTML = CareflowHTML.sanitize('<option value="">Failed to load appointments</option>');
        });
}

function closeGenerateInvoiceModal() {
    const modal = document.getElementById('generateInvoiceModal');
    if (modal) {
        modal.classList.remove('show');
    }
    const form = document.getElementById('generateInvoiceForm');
    if (form) {
        form.reset();
    }
}

function handleGenerateInvoice(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const appointmentId = parseInt(formData.get('appointmentId'), 10);
    if (!appointmentId) {
        alert('Select an appointment to generate invoice.');
        return;
    }

    const payload = {
        appointmentId: appointmentId,
        consultationFee: parseFloat(formData.get('consultationFee')) || 0,
        labFee: parseFloat(formData.get('labFee')) || 0,
        tax: parseFloat(formData.get('tax')) || 0,
        discount: parseFloat(formData.get('discount')) || 0,
        notes: formData.get('notes')
    };

    fetch('/api/billing/invoices/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to generate invoice'));
            return response.json();
        })
        .then(() => {
            alert('Invoice generated successfully!');
            closeGenerateInvoiceModal();
            loadBillingData();
        })
        .catch(error => {
            console.error('Error generating invoice:', error);
            alert('Error generating invoice: ' + error.message);
        });
}

/**
 * View invoice details
 */
function viewInvoice(invoiceId) {
    fetch(`/api/billing/invoices/${invoiceId}`)
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to fetch invoice details'));
            }
            return response.json();
        })
        .then(data => {
            renderInvoiceDetails(data);
            const modal = document.getElementById('viewInvoiceModal');
            if (modal) modal.classList.add('show');
            const downloadBtn = document.getElementById('downloadInvoiceBtn');
            if (downloadBtn) {
                downloadBtn.onclick = () => downloadInvoicePdf(invoiceId);
            }
        })
        .catch(error => {
            console.error('Error loading invoice:', error);
            alert('Error loading invoice: ' + error.message);
        });
}

function renderInvoiceDetails(data) {
    const container = document.getElementById('invoiceDetailsContent');
    if (!container) return;
    const invoice = data.invoice || {};
    const items = data.items || [];
    const payments = data.payments || [];

    const itemsRows = items.length ? items.map(item => `
        <tr>
            <td>${item.itemType || ''}</td>
            <td>${item.description || ''}</td>
            <td>${item.quantity || 0}</td>
            <td>${formatCurrency(item.unitPrice)}</td>
            <td>${formatCurrency(item.lineTotal)}</td>
        </tr>
    `).join('') : '<tr><td colspan="5" style="text-align:center;">No items</td></tr>';

    const paymentsRows = payments.length ? payments.map(pay => `
        <tr>
            <td>${formatCurrency(pay.amount)}</td>
            <td>${pay.paymentMethod || '-'}</td>
            <td>${pay.reference || '-'}</td>
            <td>${pay.paidAt || '-'}</td>
        </tr>
    `).join('') : '<tr><td colspan="4" style="text-align:center;">No payments</td></tr>';

    container.innerHTML = CareflowHTML.sanitize(`
        <div class="detail-item"><strong>Invoice #:</strong> <span>${invoice.invoiceNumber || '-'}</span></div>
        <div class="detail-item"><strong>Status:</strong> <span>${invoice.status || 'PENDING'}</span></div>
        <div class="detail-item"><strong>Email Sent:</strong> <span>${invoice.emailSentAt ? formatDateTime(invoice.emailSentAt) : '-'}</span></div>
        <div class="detail-item"><strong>Total:</strong> <span>${formatCurrency(invoice.total)}</span></div>
        <div class="detail-item"><strong>Paid:</strong> <span>${formatCurrency(invoice.amountPaid)}</span></div>
        <div class="detail-item"><strong>Balance:</strong> <span>${formatCurrency(invoice.balanceDue)}</span></div>
        <h3 style="margin-top: 16px;">Items</h3>
        <div class="table-container">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Type</th>
                        <th>Description</th>
                        <th>Qty</th>
                        <th>Unit</th>
                        <th>Total</th>
                    </tr>
                </thead>
                <tbody>
                    ${itemsRows}
                </tbody>
            </table>
        </div>
        <h3 style="margin-top: 16px;">Payments</h3>
        <div class="table-container">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Amount</th>
                        <th>Method</th>
                        <th>Reference</th>
                        <th>Paid At</th>
                    </tr>
                </thead>
                <tbody>
                    ${paymentsRows}
                </tbody>
            </table>
        </div>
    `);
}

function closeViewInvoiceModal() {
    const modal = document.getElementById('viewInvoiceModal');
    if (modal) modal.classList.remove('show');
}

function downloadInvoicePdf(invoiceId) {
    window.open(`/api/billing/invoices/${invoiceId}/pdf`, '_blank');
}

function openPaymentModal(invoiceId, balanceDue) {
    currentInvoiceIdForPayment = invoiceId;
    const modal = document.getElementById('paymentModal');
    if (modal) modal.classList.add('show');
    const amountField = document.getElementById('paymentAmount');
    if (amountField) {
        amountField.value = Number(balanceDue || 0).toFixed(2);
        amountField.max = Number(balanceDue || 0).toFixed(2);
    }
    const paymentMethod = document.getElementById('paymentMethod');
    if (paymentMethod) {
        paymentMethod.value = 'Cash';
    }
    updatePaymentMethodUI();
    fetch('/api/billing/invoices/payment-options')
        .then(async response => {
            if (!response.ok) return null;
            return response.json();
        })
        .then(config => {
            if (!config || !config.enabled) return;
            const option = document.getElementById('paymentMethod');
            if (!option) return;
            const hasOnline = Array.from(option.options).some(opt => opt.value === 'Razorpay');
            if (!hasOnline) {
                const onlineOption = document.createElement('option');
                onlineOption.value = 'Razorpay';
                onlineOption.textContent = 'Razorpay Online';
                option.appendChild(onlineOption);
            }
        })
        .catch(() => {});
}

function closePaymentModal() {
    const modal = document.getElementById('paymentModal');
    if (modal) modal.classList.remove('show');
    const form = document.getElementById('paymentForm');
    if (form) form.reset();
    currentInvoiceIdForPayment = null;
}

function handlePaymentSubmit(event) {
    event.preventDefault();
    if (!currentInvoiceIdForPayment) {
        alert('No invoice selected for payment.');
        return;
    }
    const formData = new FormData(event.target);
    const paymentMethod = formData.get('paymentMethod');
    const amount = parseFloat(formData.get('amount')) || 0;

    if (paymentMethod === 'Razorpay') {
        submitRazorpayPayment(amount);
        return;
    }

    const payload = {
        amount: amount,
        paymentMethod: paymentMethod,
        reference: formData.get('reference')
    };

    fetch(`/api/billing/invoices/${currentInvoiceIdForPayment}/payments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to save payment'));
            }
            return response.json();
        })
        .then(() => {
            alert('Payment recorded successfully!');
            closePaymentModal();
            loadBillingData();
        })
        .catch(error => {
            console.error('Error saving payment:', error);
            alert('Error saving payment: ' + error.message);
        });
}

function submitRazorpayPayment(amount) {
    console.log('Razorpay flow started', { invoiceId: currentInvoiceIdForPayment, amount, hasScript: !!window.Razorpay });
    fetch('/api/billing/invoices/payment-options')
        .then(async response => {
            console.log('Payment options response status:', response.status);
            if (!response.ok) {
                throw new Error('Razorpay is not available right now.');
            }
            return response.json();
        })
        .then(paymentOptions => {
            console.log('Payment options loaded:', { enabled: !!(paymentOptions && paymentOptions.enabled), hasKey: !!(paymentOptions && paymentOptions.keyId) });
            if (!paymentOptions || !paymentOptions.enabled || !paymentOptions.keyId) {
                throw new Error('Razorpay is disabled or not configured.');
            }
            return fetch(`/api/billing/invoices/${currentInvoiceIdForPayment}/razorpay/order`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ amount: amount })
            });
        })
        .then(async response => {
            console.log('Order creation response status:', response.status);
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to create Razorpay order'));
            }
            return response.json();
        })
        .then(order => {
            console.log('Razorpay order created:', order);
            if (!window.Razorpay) {
                throw new Error('Razorpay script is not loaded.');
            }
            return fetch('/api/billing/invoices/payment-options')
                .then(async res => res.ok ? res.json() : null)
                .then(config => {
                    console.log('Checkout config is available:', !!(config && config.keyId));
                    const isAndroid = /Android/i.test(navigator.userAgent);
                    const options = {
                        key: config && config.keyId ? config.keyId : '',
                        amount: order.amount,
                        currency: order.currency || 'INR',
                        name: 'Hospital Management',
                        description: 'Invoice payment',
                        order_id: order.id,
                        handler: function (response) {
                            console.log('Razorpay success callback payload:', response);
                            fetch(`/api/billing/invoices/${currentInvoiceIdForPayment}/razorpay/verify`, {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    razorpay_order_id: response.razorpay_order_id,
                                    razorpay_payment_id: response.razorpay_payment_id,
                                    razorpay_signature: response.razorpay_signature,
                                    amount: amount
                                })
                            })
                                .then(async verifyResponse => {
                                    console.log('Verification response status:', verifyResponse.status);
                                    if (!verifyResponse.ok) {
                                        throw new Error(await hospitalResponseError(verifyResponse, 'Payment verification failed'));
                                    }
                                    return verifyResponse.json();
                                })
                                .then(() => {
                                    alert('Payment verified successfully!');
                                    closePaymentModal();
                                    loadBillingData();
                                })
                                .catch(error => {
                                    console.error('Razorpay verification failed:', error);
                                    alert('Payment verification failed: ' + error.message);
                                });
                        },
                        theme: { color: '#1d4ed8' },
                        ...(isAndroid ? { method: { upi: true } } : {})
                    };
                    const razorpayInstance = new window.Razorpay(options);
                    console.log('Opening Razorpay checkout');
                    razorpayInstance.open();
                });
        })
        .catch(error => {
            console.error('Error creating Razorpay order:', error);
            alert('Error starting Razorpay payment: ' + error.message);
        });
}

function updatePaymentMethodUI() {
    const paymentMethod = document.getElementById('paymentMethod');
    const upiSection = document.getElementById('upiSection');
    if (!paymentMethod || !upiSection) return;

    document.getElementById('upiId').disabled = paymentMethod.value !== 'UPI';
    if (paymentMethod.value === 'UPI') {
        upiSection.style.display = 'block';
        updateUpiQrPreview();
    } else {
        upiSection.style.display = 'none';
    }
}

function updateUpiQrPreview() {
    const qrImg = document.getElementById('upiQrImage');
    const amountField = document.getElementById('paymentAmount');
    if (!qrImg || !amountField || !currentInvoiceIdForPayment) return;

    if (window.HospitalValidation.message(amountField)) return;
    const amount = Number(amountField.value || 0).toFixed(2);

    fetch(`/api/billing/invoices/${currentInvoiceIdForPayment}/upi-qr`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount: parseFloat(amount) || 0 })
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to generate UPI QR'));
            return response.json();
        })
        .then(doc => {
            qrImg.src = doc.fileUrl || '';
            qrImg.alt = 'UPI QR Code';
        })
        .catch(error => {
            console.error('Error generating UPI QR:', error);
            qrImg.src = '';
            qrImg.alt = error.message || 'Failed to generate QR';
        });
}

/**
 * Open Add Patient Modal
 */
function openAddPatientModal() {
    const modal = document.getElementById('addPatientModal');
    modal.classList.add('show');
    
    // Setup form submission
    const form = document.getElementById('addPatientForm');
    form.onsubmit = handleAddPatient;
}

/**
 * Close Add Patient Modal
 */
function closeAddPatientModal() {
    const modal = document.getElementById('addPatientModal');
    modal.classList.remove('show');
    document.getElementById('addPatientForm').reset();
}

/**
 * Open Add Staff Modal
 */
function openAddStaffModal() {
    const modal = document.getElementById('addStaffModal');
    modal.classList.add('show');
    
    // Setup form submission
    const form = document.getElementById('addStaffForm');
    form.onsubmit = handleAddStaff;
}

/**
 * Close Add Staff Modal
 */
function closeAddStaffModal() {
    const modal = document.getElementById('addStaffModal');
    modal.classList.remove('show');
    document.getElementById('addStaffForm').reset();
}

/**
 * Handle Add Patient Form Submission
 */
function handleAddPatient(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const patient = {
        name: formData.get('name'),
        email: formData.get('email'),
        phone: formData.get('phone'),
        dateOfBirth: formData.get('dateOfBirth'),
        address: formData.get('address'),
        medicalHistory: formData.get('medicalHistory')
    };

    fetch('/api/patients', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(patient)
    })
    .then(async response => {
        if (!response.ok) {
            throw new Error(await hospitalResponseError(response, 'Failed to add patient'));
        }
        return response.json();
    })
    .then(data => {
        alert('Patient added successfully!');
        closeAddPatientModal();
        loadPatientData();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error adding patient: ' + error.message);
    });
}

/**
 * Handle Add Staff Form Submission
 */
function handleAddStaff(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const staff = {
        name: formData.get('name'),
        email: formData.get('email'),
        phone: formData.get('phone'),
        position: formData.get('position'),
        department: formData.get('department'),
        specialization: formData.get('specialization'),
        joiningDate: formData.get('joiningDate'),
        isActive: true
    };

    fetch('/api/staff', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(staff)
    })
    .then(async response => {
        if (!response.ok) {
            throw new Error(await hospitalResponseError(response, 'Failed to add staff'));
        }
        return response.json();
    })
    .then(data => {
        alert('Staff member added successfully!');
        closeAddStaffModal();
        loadStaffData();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error adding staff: ' + error.message);
    });
}

/**
 * View staff details
 */
function viewStaff(staffId) {
    fetch(`/api/staff/${staffId}`)
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to fetch staff details'));
            }
            return response.json();
        })
        .then(staff => {
            displayStaffDetails(staff);
            const modal = document.getElementById('viewStaffModal');
            modal.classList.add('show');
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error loading staff details: ' + error.message);
        });
}

/**
 * Display staff details in modal
 */
function displayStaffDetails(staff) {
    const detailsContent = document.getElementById('staffDetailsContent');
    const detailsHTML = `
        <div class="detail-item">
            <strong>Staff ID:</strong>
            <span>S${String(staff.id).padStart(3, '0')}</span>
        </div>
        <div class="detail-item">
            <strong>Full Name:</strong>
            <span>${staff.name}</span>
        </div>
        <div class="detail-item">
            <strong>Email:</strong>
            <span>${staff.email}</span>
        </div>
        <div class="detail-item">
            <strong>Phone:</strong>
            <span>${staff.phone}</span>
        </div>
        <div class="detail-item">
            <strong>Position:</strong>
            <span>${staff.position}</span>
        </div>
        <div class="detail-item">
            <strong>Department:</strong>
            <span>${staff.department}</span>
        </div>
        <div class="detail-item">
            <strong>Specialization:</strong>
            <span>${staff.specialization}</span>
        </div>
        <div class="detail-item">
            <strong>Joining Date:</strong>
            <span>${staff.joiningDate}</span>
        </div>
        <div class="detail-item">
            <strong>Status:</strong>
            <span><span class="badge ${staff.isActive ? 'badge-success' : 'badge-danger'}">${staff.isActive ? 'Active' : 'Inactive'}</span></span>
        </div>
    `;
    detailsContent.innerHTML = CareflowHTML.sanitize(detailsHTML);
}

/**
 * Close view staff modal
 */
function closeViewStaffModal() {
    const modal = document.getElementById('viewStaffModal');
    modal.classList.remove('show');
}

/**
 * Edit staff
 */
function editStaff(staffId) {
    fetch(`/api/staff/${staffId}`)
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to fetch staff details'));
            }
            return response.json();
        })
        .then(staff => {
            // Populate form with staff data
            document.getElementById('editStaffName').value = staff.name;
            document.getElementById('editStaffEmail').value = staff.email;
            document.getElementById('editStaffPhone').value = staff.phone;
            document.getElementById('editStaffPosition').value = staff.position;
            document.getElementById('editStaffDepartment').value = staff.department;
            document.getElementById('editStaffSpecialization').value = staff.specialization;
            document.getElementById('editStaffJoiningDate').value = staff.joiningDate;
            
            // Store staff ID for update
            document.getElementById('editStaffForm').dataset.staffId = staffId;
            
            // Setup form submission
            const form = document.getElementById('editStaffForm');
            form.onsubmit = handleEditStaff;
            
            // Show modal
            const modal = document.getElementById('editStaffModal');
            modal.classList.add('show');
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error loading staff details: ' + error.message);
        });
}

/**
 * Close edit staff modal
 */
function closeEditStaffModal() {
    const modal = document.getElementById('editStaffModal');
    modal.classList.remove('show');
    document.getElementById('editStaffForm').reset();
}

/**
 * Handle edit staff form submission
 */
function handleEditStaff(event) {
    event.preventDefault();

    const staffId = document.getElementById('editStaffForm').dataset.staffId;
    const formData = new FormData(event.target);
    const staff = {
        name: formData.get('name'),
        email: formData.get('email'),
        phone: formData.get('phone'),
        position: formData.get('position'),
        department: formData.get('department'),
        specialization: formData.get('specialization'),
        joiningDate: formData.get('joiningDate'),
        isActive: true
    };

    fetch(`/api/staff/${staffId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(staff)
    })
    .then(async response => {
        if (!response.ok) {
            throw new Error(await hospitalResponseError(response, 'Failed to update staff'));
        }
        return response.json();
    })
    .then(data => {
        alert('Staff member updated successfully!');
        closeEditStaffModal();
        loadStaffData();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error updating staff: ' + error.message);
    });
}

/**
 * Delete staff
 */
function deleteStaff(staffId) {
    if (confirm('Are you sure you want to delete this staff member?')) {
        fetch(`/api/staff/${staffId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(async response => {
            if (response.ok) {
                alert('Staff member deleted successfully');
                loadStaffData();
            } else {
                alert('Failed to delete staff member');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error deleting staff member');
        });
    }
}

/**
 * Load patient options for dropdown
 */
function loadPatientOptions() {
    const select = document.getElementById('appointmentPatient');
    
    fetch('/api/patients')
        .then(response => response.json())
        .then(patients => {
            let options = '<option value="">Select Patient</option>';
            patients.forEach(patient => {
                options += `<option value="${patient.id}">P${String(patient.id).padStart(3, '0')} - ${patient.name}</option>`;
            });
            select.innerHTML = CareflowHTML.sanitize(options);
        })
        .catch(error => {
            console.error('Error loading patients:', error);
            select.innerHTML = CareflowHTML.sanitize('<option value="">Error loading patients</option>');
        });
}

/**
 * Load staff options for dropdown
 */
function isAppointmentDoctor(member) {
    return member.isActive !== false && String(member.position || '').trim().toLowerCase() === 'doctor';
}

function loadStaffOptions() {
    const select = document.getElementById('appointmentDoctor');
    
    fetch('/api/staff')
        .then(response => response.json())
        .then(staff => {
            let options = '<option value="">Select Doctor</option>';
            staff.filter(isAppointmentDoctor).forEach(member => {
                options += `<option value="${member.id}">Dr. ${member.name} (${member.position})</option>`;
            });
            select.innerHTML = CareflowHTML.sanitize(options);
        })
        .catch(error => {
            console.error('Error loading staff:', error);
            select.innerHTML = CareflowHTML.sanitize('<option value="">Error loading staff</option>');
        });
}

// Close modals when clicking outside
window.onclick = function(event) {
    const addPatientModal = document.getElementById('addPatientModal');
    const addStaffModal = document.getElementById('addStaffModal');
    const viewPatientModal = document.getElementById('viewPatientModal');
    const viewStaffModal = document.getElementById('viewStaffModal');
    const editPatientModal = document.getElementById('editPatientModal');
    const editStaffModal = document.getElementById('editStaffModal');
    const scheduleAppointmentModal = document.getElementById('scheduleAppointmentModal');
    const viewAppointmentModal = document.getElementById('viewAppointmentModal');
    const editAppointmentModal = document.getElementById('editAppointmentModal');
    const generateInvoiceModal = document.getElementById('generateInvoiceModal');
    const viewInvoiceModal = document.getElementById('viewInvoiceModal');
    const paymentModal = document.getElementById('paymentModal');
    
    if (event.target === addPatientModal) {
        addPatientModal.classList.remove('show');
    }
    if (event.target === addStaffModal) {
        addStaffModal.classList.remove('show');
    }
    if (event.target === viewPatientModal) {
        viewPatientModal.classList.remove('show');
    }
    if (event.target === viewStaffModal) {
        viewStaffModal.classList.remove('show');
    }
    if (event.target === editPatientModal) {
        editPatientModal.classList.remove('show');
    }
    if (event.target === editStaffModal) {
        editStaffModal.classList.remove('show');
    }
    if (event.target === scheduleAppointmentModal) {
        scheduleAppointmentModal.classList.remove('show');
    }
    if (event.target === viewAppointmentModal) {
        viewAppointmentModal.classList.remove('show');
    }
    if (event.target === editAppointmentModal) {
        editAppointmentModal.classList.remove('show');
    }
    if (event.target === generateInvoiceModal) {
        generateInvoiceModal.classList.remove('show');
    }
    if (event.target === viewInvoiceModal) {
        viewInvoiceModal.classList.remove('show');
    }
    if (event.target === paymentModal) {
        paymentModal.classList.remove('show');
    }
}

/**
 * Load appointment data from API
 */
function loadAppointmentData() {
    fetch('/api/appointments')
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to fetch appointments'));
            }
            return response.json();
        })
        .then(appointments => {
            displayAppointments(appointments);
            updateAppointmentCount(appointments.length);
        })
        .catch(error => {
            console.error('Error loading appointments:', error);
            const tableBody = document.querySelector('#appointmentsSection .data-table tbody');
            if (tableBody) {
                tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="7" style="text-align: center; color: red;">Failed to load appointment data</td></tr>');
            }
        });
}

/**
 * Display appointments in table
 */
function displayAppointments(appointments) {
    const tableBody = document.querySelector('#appointmentsSection .data-table tbody');
    if (!tableBody) return;

    if (appointments.length === 0) {
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="7" style="text-align: center;">No appointments found</td></tr>');
        return;
    }

    // Fetch patient and staff details for display
    Promise.all([
        fetch('/api/patients?includeInactive=true').then(r => r.json()),
        fetch('/api/staff').then(r => r.json())
    ]).then(([patients, staff]) => {
        const patientMap = {};
        const staffMap = {};
        
        patients.forEach(p => patientMap[p.id] = p.name);
        staff.forEach(s => staffMap[s.id] = s.name);

        tableBody.innerHTML = CareflowHTML.sanitize(appointments.map(appt => `
            <tr>
                <td>A${String(appt.id).padStart(3, '0')}</td>
                <td>${patientMap[appt.patientId] || 'Unknown'}</td>
                <td>${staffMap[appt.staffId] || 'Unknown'}</td>
                <td>${appt.appointmentDate} ${appt.appointmentTime}</td>
                <td>${appt.reason || '-'}</td>
                <td><span class="badge ${getStatusBadgeClass(appt.status)}">${appt.status}</span></td>
                <td>
                    <div class="appointments-actions">
                        <button class="btn-small action-button" title="View appointment" aria-label="View appointment" data-action="view" data-cf-action="viewAppointment(${appt.id})"><span class="action-label">View</span></button>
                        <button class="btn-small btn-warning action-button" title="Edit appointment" aria-label="Edit appointment" data-action="edit" data-cf-action="editAppointment(${appt.id})"><span class="action-label">Edit</span></button>
                        ${['PENDING', 'CONFIRMED', 'SCHEDULED'].includes((appt.status || '').toUpperCase()) ? `<button class="btn-small action-button whatsapp-action" title="Send WhatsApp reminder" aria-label="Send WhatsApp reminder" data-action="whatsapp" data-cf-action="sendAppointmentWhatsApp(${appt.id}, this)"><span class="action-label">WhatsApp</span></button>` : ''}
                        <button class="btn-small btn-danger action-button" title="Cancel appointment" aria-label="Cancel appointment" data-action="delete" data-cf-action="deleteAppointment(${appt.id})"><span class="action-label">Cancel</span></button>
                    </div>
                </td>
            </tr>
        `).join(''));
    }).catch(error => {
        console.error('Error loading related data:', error);
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="7" style="text-align: center; color: red;">Failed to load appointment details</td></tr>');
    });
}

/**
 * Get badge class based on status
 */
function getStatusBadgeClass(status) {
    switch(status) {
        case 'CONFIRMED':
            return 'badge-success';
        case 'PENDING':
            return 'badge-warning';
        case 'CANCELLED':
            return 'badge-danger';
        case 'COMPLETED':
            return 'badge-success';
        default:
            return '';
    }
}

/**
 * Update appointment count in dashboard
 */
function updateAppointmentCount(count) {
    const cards = document.querySelectorAll('.card');
    if (cards.length > 1) {
        cards[1].querySelector('.card-value').textContent = count;
    }
}

/**
 * Open Schedule Appointment Modal
 */
function openScheduleAppointmentModal() {
    const modal = document.getElementById('scheduleAppointmentModal');
    modal.classList.add('show');
    
    // Load patients for dropdown
    fetch('/api/patients')
        .then(r => r.json())
        .then(patients => {
            const select = document.getElementById('appointmentPatient');
            select.innerHTML = CareflowHTML.sanitize('<option value="">Select Patient</option>' + 
                patients.map(p => `<option value="${p.id}">${p.name}</option>`).join(''));
        });
    
    // Load staff for dropdown
    fetch('/api/staff')
        .then(r => r.json())
        .then(staff => {
            const select = document.getElementById('appointmentDoctor');
            select.innerHTML = CareflowHTML.sanitize('<option value="">Select Doctor</option>' + 
                staff.filter(isAppointmentDoctor).map(s => `<option value="${s.id}">${s.name} - ${s.position}</option>`).join(''));
        });
    
    // Setup form submission
    const form = document.getElementById('scheduleAppointmentForm');
    form.onsubmit = handleScheduleAppointment;
}

/**
 * Close Schedule Appointment Modal
 */
function closeScheduleAppointmentModal() {
    const modal = document.getElementById('scheduleAppointmentModal');
    modal.classList.remove('show');
    document.getElementById('scheduleAppointmentForm').reset();
}

/**
 * Handle Schedule Appointment Form Submission
 */
function handleScheduleAppointment(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const appointment = {
        patientId: parseInt(formData.get('patientId')),
        staffId: parseInt(formData.get('staffId')),
        appointmentDate: formData.get('appointmentDate'),
        appointmentTime: formData.get('appointmentTime'),
        reason: formData.get('reason'),
        status: formData.get('status'),
        notes: formData.get('notes')
    };

    fetch('/api/appointments', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(appointment)
    })
    .then(async response => {
        if (!response.ok) {
            throw new Error(await hospitalResponseError(response, 'Failed to schedule appointment'));
        }
        return response.json();
    })
    .then(data => {
        alert('Appointment scheduled successfully!');
        closeScheduleAppointmentModal();
        loadAppointmentData();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error scheduling appointment: ' + error.message);
    });
}

/**
 * View appointment details
 */
function viewAppointment(appointmentId) {
    fetch(`/api/appointments/${appointmentId}`)
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to fetch appointment details'));
            }
            return response.json();
        })
        .then(appt => {
            // Fetch related patient and staff info
            return Promise.all([
                Promise.resolve(appt),
                fetch(`/api/patients/${appt.patientId}`).then(r => r.json()),
                fetch(`/api/staff/${appt.staffId}`).then(r => r.json())
            ]);
        })
        .then(([appt, patient, staff]) => {
            currentViewedAppointmentId = appt.id;
            displayAppointmentDetails(appt, patient, staff);
            const modal = document.getElementById('viewAppointmentModal');
            modal.classList.add('show');
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error loading appointment details: ' + error.message);
        });
}

/**
 * Display appointment details in modal
 */
function displayAppointmentDetails(appt, patient, staff) {
    const detailsContent = document.getElementById('appointmentDetailsContent');
    const detailsHTML = `
        <div class="detail-item">
            <strong>Appointment ID:</strong>
            <span>A${String(appt.id).padStart(3, '0')}</span>
        </div>
        <div class="detail-item">
            <strong>Patient:</strong>
            <span>${patient.name}</span>
        </div>
        <div class="detail-item">
            <strong>Doctor/Staff:</strong>
            <span>${staff.name} - ${staff.position}</span>
        </div>
        <div class="detail-item">
            <strong>Department:</strong>
            <span>${staff.department}</span>
        </div>
        <div class="detail-item">
            <strong>Date:</strong>
            <span>${appt.appointmentDate}</span>
        </div>
        <div class="detail-item">
            <strong>Time:</strong>
            <span>${appt.appointmentTime}</span>
        </div>
        <div class="detail-item">
            <strong>Reason:</strong>
            <span>${appt.reason || 'Not specified'}</span>
        </div>
        <div class="detail-item">
            <strong>Status:</strong>
            <span><span class="badge ${getStatusBadgeClass(appt.status)}">${appt.status}</span></span>
        </div>
        <div class="detail-item">
            <strong>Notes:</strong>
            <span>${appt.notes || 'No notes'}</span>
        </div>
    `;
    detailsContent.innerHTML = CareflowHTML.sanitize(detailsHTML);
}

/**
 * Close view appointment modal
 */
function closeViewAppointmentModal() {
    const modal = document.getElementById('viewAppointmentModal');
    modal.classList.remove('show');
}

/**
 * Edit appointment
 */
function editAppointment(appointmentId) {
    fetch(`/api/appointments/${appointmentId}`)
        .then(async response => {
            if (!response.ok) {
                throw new Error(await hospitalResponseError(response, 'Failed to fetch appointment details'));
            }
            return response.json();
        })
        .then(appt => {
            document.getElementById('editAppointmentId').value = appt.id;
            document.getElementById('editAppointmentPatientId').value = appt.patientId;
            document.getElementById('editAppointmentPatient').value = appt.patientId;
            document.getElementById('editAppointmentStaff').value = appt.staffId;
            document.getElementById('editAppointmentDate').value = appt.appointmentDate;
            document.getElementById('editAppointmentTime').value = appt.appointmentTime;
            document.getElementById('editAppointmentReason').value = appt.reason || '';
            document.getElementById('editAppointmentStatus').value = appt.status;
            document.getElementById('editAppointmentNotes').value = appt.notes || '';

            // Load staff dropdown
            fetch('/api/staff')
                .then(r => r.json())
                .then(staff => {
                    const select = document.getElementById('editAppointmentStaff');
                    select.innerHTML = CareflowHTML.sanitize('<option value="">Select Doctor</option>' + staff.filter(isAppointmentDoctor).map(s => `<option value="${s.id}">${s.name} - ${s.position}</option>`).join(''));
                    select.value = appt.staffId;
                });

            // Fetch patient name
            fetch(`/api/patients/${appt.patientId}`)
                .then(r => r.json())
                .then(patient => {
                    document.getElementById('editAppointmentPatient').value = patient.name;
                });

            const modal = document.getElementById('editAppointmentModal');
            modal.classList.add('show');

            const form = document.getElementById('editAppointmentForm');
            form.onsubmit = handleEditAppointment;
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error loading appointment: ' + error.message);
        });
}

/**
 * Close edit appointment modal
 */
function closeEditAppointmentModal() {
    const modal = document.getElementById('editAppointmentModal');
    modal.classList.remove('show');
    document.getElementById('editAppointmentForm').reset();
}

/**
 * Handle Edit Appointment Form Submission
 */
function handleEditAppointment(event) {
    event.preventDefault();

    const appointmentId = document.getElementById('editAppointmentId').value;
    const formData = new FormData(event.target);
    const appointment = {
        id: parseInt(appointmentId),
        patientId: parseInt(document.getElementById('editAppointmentPatientId').value),
        staffId: parseInt(formData.get('staffId')),
        appointmentDate: formData.get('appointmentDate'),
        appointmentTime: formData.get('appointmentTime'),
        reason: formData.get('reason'),
        status: formData.get('status'),
        notes: formData.get('notes')
    };

    fetch(`/api/appointments/${appointmentId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(appointment)
    })
    .then(async response => {
        if (!response.ok) {
            throw new Error(await hospitalResponseError(response, 'Failed to update appointment'));
        }
        return response.json();
    })
    .then(data => {
        alert('Appointment updated successfully!');
        closeEditAppointmentModal();
        loadAppointmentData();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error updating appointment: ' + error.message);
    });
}

/**
 * Delete appointment
 */
function deleteAppointment(appointmentId) {
    if (confirm('Cancel this appointment?')) {
        fetch(`/api/appointments/${appointmentId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(async response => {
            if (response.ok) {
                alert('Appointment cancelled successfully');
                loadAppointmentData();
            } else {
                alert('Failed to cancel appointment');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error cancelling appointment');
        });
    }
}


function setupPharmacyTabs() {
    if (!pharmacyTabs.length) return;
    pharmacyTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            pharmacyTabs.forEach(btn => btn.classList.remove('active'));
            pharmacyTabContents.forEach(section => section.classList.remove('active'));

            tab.classList.add('active');
            const targetId = `pharmacy-${tab.getAttribute('data-pharmacy-tab')}`;
            const section = document.getElementById(targetId);
            if (section) {
                section.classList.add('active');
            }
        });
    });
}

function setupLabTabs() {
    if (!labTabs.length) return;
    labTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            labTabs.forEach(btn => btn.classList.remove('active'));
            labTabContents.forEach(section => section.classList.remove('active'));

            tab.classList.add('active');
            const targetId = `lab-${tab.getAttribute('data-lab-tab')}`;
            const section = document.getElementById(targetId);
            if (section) {
                section.classList.add('active');
            }
        });
    });
}

function setupBedTabs() {
    if (!bedTabs.length) return;
    bedTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            bedTabs.forEach(btn => btn.classList.remove('active'));
            document.querySelectorAll('#bedsSection .pharmacy-tab-content').forEach(section => section.classList.remove('active'));

            tab.classList.add('active');
            const targetId = `beds-${tab.dataset.bedTab}`;
            const section = document.getElementById(targetId);
            if (section) {
                section.classList.add('active');
            }
        });
    });
}

function setupBedForms() {
    const addBedForm = document.getElementById('addBedForm');
    if (addBedForm) {
        addBedForm.onsubmit = handleAddBed;
    }
    const admitForm = document.getElementById('admitPatientForm');
    if (admitForm) {
        admitForm.onsubmit = handleAdmitPatient;
    }
}

function setupLabSearch() {
    const input = document.getElementById('labSearchInput');
    const btn = document.getElementById('labSearchBtn');
    if (btn) {
        btn.addEventListener('click', () => {
            labSearchValue = (input ? input.value : '').trim().toLowerCase();
            rerenderLabTables();
        });
    }
    if (input) {
        input.addEventListener('input', () => {
            labSearchValue = input.value.trim().toLowerCase();
            rerenderLabTables();
        });
    }
}

function setupConsultationTabs() {
    if (!consultationTabs.length) return;
    consultationTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            consultationTabs.forEach(btn => btn.classList.remove('active'));
            consultationTabContents.forEach(section => section.classList.remove('active'));
            tab.classList.add('active');
            const targetId = `consultation-${tab.getAttribute('data-consultation-tab')}`;
            const section = document.getElementById(targetId);
            if (section) {
                section.classList.add('active');
            }
        });
    });
}

function setupPharmacyForms() {
    const addMedicineForm = document.getElementById('addMedicineForm');
    const addBatchForm = document.getElementById('addBatchForm');

    if (addMedicineForm) {
        addMedicineForm.addEventListener('submit', handleAddMedicine);
    }

    if (addBatchForm) {
        addBatchForm.addEventListener('submit', handleAddBatch);
    }
}

function setupPharmacySearch() {
    const searchBtn = document.getElementById('prescriptionSearchBtn');
    if (searchBtn) {
        searchBtn.addEventListener('click', () => {
            renderPharmacyPrescriptions();
        });
    }
}

function loadPharmacyData() {
    Promise.all([
        loadMedicines(),
        loadBatches(),
        loadTransactions(),
        loadPharmacyPrescriptions()
    ]).then(() => {
        renderPharmacyAlerts();
    }).catch(error => {
        console.error('Error loading pharmacy data:', error);
    });
}

function loadMedicines() {
    return fetch('/api/pharmacy/medicines')
        .then(response => response.ok ? response.json() : [])
        .then(medicines => {
            pharmacyCache.medicines = medicines || [];
            renderMedicineTable();
            populateMedicineOptions();
        })
        .catch(error => {
            console.error('Error loading medicines:', error);
            const tableBody = document.querySelector('#medicineTable tbody');
            if (tableBody) {
                tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="4" style="text-align: center; color: red;">Failed to load medicines</td></tr>');
            }
        });
}

function loadBatches() {
    return fetch('/api/pharmacy/batches')
        .then(response => response.ok ? response.json() : [])
        .then(batches => {
            pharmacyCache.batches = batches || [];
            renderBatchTable();
        })
        .catch(error => {
            console.error('Error loading batches:', error);
            const tableBody = document.querySelector('#batchTable tbody');
            if (tableBody) {
                tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="7" style="text-align: center; color: red;">Failed to load batches</td></tr>');
            }
        });
}

function loadTransactions() {
    return fetch('/api/pharmacy/transactions')
        .then(response => response.ok ? response.json() : [])
        .then(transactions => {
            pharmacyCache.transactions = transactions || [];
            renderBatchTable();
        })
        .catch(error => {
            console.error('Error loading stock transactions:', error);
        });
}

function loadPharmacyPrescriptions() {
    return Promise.all([
        fetch('/api/pharmacy/prescriptions').then(r => r.ok ? r.json() : []),
        fetch('/api/pharmacy/prescription-items').then(r => r.ok ? r.json() : []),
        fetch('/api/pharmacy/dispensed-items').then(r => r.ok ? r.json() : []),
        fetch('/api/appointments').then(r => r.ok ? r.json() : []),
        fetch('/api/patients').then(r => r.ok ? r.json() : []),
        fetch('/api/staff').then(r => r.ok ? r.json() : [])
    ])
        .then(([prescriptions, items, dispensedItems, appointments, patients, staff]) => {
            pharmacyCache.prescriptions = prescriptions || [];
            pharmacyCache.prescriptionItems = items || [];
            pharmacyCache.dispensedItems = dispensedItems || [];
            pharmacyCache.appointments = appointments || [];
            pharmacyCache.patients = patients || [];
            pharmacyCache.staff = staff || [];
            renderPharmacyPrescriptions();
        })
        .catch(error => {
            console.error('Error loading prescriptions:', error);
            const tableBody = document.querySelector('#pharmacyPrescriptionTable tbody');
            if (tableBody) {
                tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="7" style="text-align: center; color: red;">Failed to load prescriptions</td></tr>');
            }
        });
}

function renderMedicineTable() {
    const tableBody = document.querySelector('#medicineTable tbody');
    if (!tableBody) return;

    if (!pharmacyCache.medicines.length) {
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="4" style="text-align: center;">No medicines found</td></tr>');
        return;
    }

    tableBody.innerHTML = CareflowHTML.sanitize(pharmacyCache.medicines.map(med => `
        <tr>
            <td>M${String(med.id).padStart(3, '0')}</td>
            <td>${med.name || '-'}</td>
            <td>${med.description || '-'}</td>
            <td>
                <button class="btn-small btn-danger" data-cf-action="deleteMedicine(${med.id})">Delete</button>
            </td>
        </tr>
    `).join(''));
}

function renderBatchTable() {
    const tableBody = document.querySelector('#batchTable tbody');
    if (!tableBody) return;

    if (!pharmacyCache.batches.length) {
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="7" style="text-align: center;">No batches found</td></tr>');
        return;
    }

    tableBody.innerHTML = CareflowHTML.sanitize(pharmacyCache.batches.map(batch => {
        const medicineName = resolveMedicineName(batch);
        const availableQty = calculateAvailableQuantity(batch);
        return `
            <tr>
                <td>B${String(batch.id).padStart(3, '0')}</td>
                <td>${medicineName}</td>
                <td>${batch.batchNo || '-'}</td>
                <td>${batch.expiryDate || '-'}</td>
                <td>${availableQty}</td>
                <td>${batch.price != null ? Number(batch.price).toFixed(2) : '-'}</td>
                <td>
                    <button class="btn-small btn-danger" data-cf-action="deleteBatch(${batch.id})">Delete</button>
                </td>
            </tr>
        `;
    }).join(''));
}

function renderPharmacyPrescriptions() {
    const tableBody = document.querySelector('#pharmacyPrescriptionTable tbody');
    if (!tableBody) return;

    const searchInput = document.getElementById('prescriptionSearchInput');
    const searchValue = searchInput ? searchInput.value.trim().toLowerCase() : '';

    const patientMap = {};
    pharmacyCache.patients.forEach(patient => {
        patientMap[patient.id] = patient.name || '';
    });

    const staffMap = {};
    pharmacyCache.staff.forEach(member => {
        staffMap[member.id] = member.name || '';
    });

    const appointmentMap = {};
    pharmacyCache.appointments.forEach(appt => {
        appointmentMap[appt.id] = appt;
    });

    const filtered = pharmacyCache.prescriptions.filter(presc => {
        if (!searchValue) return true;
        const appt = appointmentMap[presc.appointmentId] || {};
        const patientName = patientMap[appt.patientId] || '';
        const appointmentId = presc.appointmentId ? String(presc.appointmentId) : '';
        return patientName.toLowerCase().includes(searchValue) || appointmentId.includes(searchValue);
    });

    if (!filtered.length) {
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="7" style="text-align: center;">No prescriptions found</td></tr>');
        return;
    }

    tableBody.innerHTML = CareflowHTML.sanitize(filtered.map(presc => {
        const appt = appointmentMap[presc.appointmentId] || {};
        const patientName = patientMap[appt.patientId] || 'Unknown';
        const doctorName = staffMap[presc.doctorId] || 'Unknown';
        const dateStr = presc.date ? new Date(presc.date).toLocaleString() : '-';
        const isDispensed = isPrescriptionDispensed(presc.id);
        const status = isDispensed ? 'DISPENSED' : (presc.status || 'CREATED');

        return `
            <tr>
                <td>PR${String(presc.id).padStart(3, '0')}</td>
                <td>A${String(presc.appointmentId || '').padStart(3, '0')}</td>
                <td>${patientName}</td>
                <td>${doctorName}</td>
                <td>${dateStr}</td>
                <td><span class="badge ${isDispensed ? 'badge-success' : 'badge-warning'}">${status}</span></td>
                <td>
                    <button class="btn-small btn-info" data-cf-action="openDispenseModal(${presc.id})" ${isDispensed ? 'disabled' : ''}>Dispense</button>
                </td>
            </tr>
        `;
    }).join(''));
}

function renderPharmacyAlerts() {
    const lowStockList = document.getElementById('lowStockList');
    const expiredStockList = document.getElementById('expiredStockList');

    if (!lowStockList || !expiredStockList) return;

    const lowStockItems = [];
    const expiredItems = [];

    const today = new Date();
    const soonThreshold = new Date();
    soonThreshold.setDate(today.getDate() + 30);

    pharmacyCache.batches.forEach(batch => {
        const availableQty = calculateAvailableQuantity(batch);
        const medicineName = resolveMedicineName(batch);
        const expiryDate = batch.expiryDate ? new Date(batch.expiryDate) : null;

        if (availableQty <= 10) {
            lowStockItems.push(`${medicineName} (Batch ${batch.batchNo || '-'}) - Qty: ${availableQty}`);
        }

        if (expiryDate && expiryDate <= soonThreshold) {
            const label = expiryDate < today ? 'Expired' : 'Expiring Soon';
            expiredItems.push(`${medicineName} (Batch ${batch.batchNo || '-'}) - ${label}: ${batch.expiryDate}`);
        }
    });

    lowStockList.innerHTML = CareflowHTML.sanitize(lowStockItems.length
        ? `<ul>${lowStockItems.map(item => `<li>${item}</li>`).join('')}</ul>`
        : '<p class="no-data">No low stock alerts.</p>');

    expiredStockList.innerHTML = CareflowHTML.sanitize(expiredItems.length
        ? `<ul>${expiredItems.map(item => `<li>${item}</li>`).join('')}</ul>`
        : '<p class="no-data">No expired medicines.</p>');
}

function handleAddMedicine(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const name = (formData.get('name') || '').trim();
    if (!name) {
        alert('Medicine name is required.');
        return;
    }
    const medicine = {
        name: name,
        description: (formData.get('description') || '').trim()
    };

    fetch('/api/pharmacy/medicines', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(medicine)
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to add medicine'));
            return response.json();
        })
        .then(() => {
            alert('Medicine added successfully!');
            event.target.reset();
            loadMedicines();
        })
        .catch(error => {
            console.error('Error adding medicine:', error);
            alert('Error adding medicine: ' + error.message);
        });
}

function handleAddBatch(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const medicineId = parseInt(formData.get('medicineId'), 10);
    if (!medicineId || Number.isNaN(medicineId)) {
        alert('Please select a medicine for the batch.');
        return;
    }
    const batch = {
        medicine: { id: medicineId },
        batchNo: formData.get('batchNo'),
        expiryDate: formData.get('expiryDate'),
        quantity: parseInt(formData.get('quantity')),
        price: parseFloat(formData.get('price'))
    };

    fetch('/api/pharmacy/batches', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(batch)
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to add batch'));
            return response.json();
        })
        .then(() => {
            alert('Batch added successfully!');
            event.target.reset();
            loadBatches();
        })
        .catch(error => {
            console.error('Error adding batch:', error);
            alert('Error adding batch: ' + error.message);
        });
}

function deleteMedicine(medicineId) {
    if (!confirm('Delete this medicine?')) return;
    fetch(`/api/pharmacy/medicines/${medicineId}`, { method: 'DELETE' })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to delete medicine'));
            loadMedicines();
        })
        .catch(error => {
            console.error('Error deleting medicine:', error);
            alert('Error deleting medicine');
        });
}

function deleteBatch(batchId) {
    if (!confirm('Deactivate this batch?')) return;
    fetch(`/api/pharmacy/batches/${batchId}`, { method: 'DELETE' })
        .then(async response => {
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(text || 'Failed to delete batch');
                });
            }
            loadBatches();
        })
        .catch(error => {
            console.error('Error deleting batch:', error);
            alert(error.message || 'Error deactivating batch');
        });
}

function populateMedicineOptions() {
    const select = document.getElementById('batchMedicine');
    if (!select) return;

    const options = ['<option value="">Select Medicine</option>'];
    pharmacyCache.medicines.forEach(med => {
        options.push(`<option value="${med.id}">${med.name}</option>`);
    });
    select.innerHTML = CareflowHTML.sanitize(options.join(''));
}

function resolveMedicineName(batch) {
    if (batch.medicine && batch.medicine.name) {
        return batch.medicine.name;
    }
    if (batch.medicine && batch.medicine.id) {
        const med = pharmacyCache.medicines.find(item => item.id === batch.medicine.id);
        return med ? med.name : 'Unknown';
    }
    return 'Unknown';
}

function calculateAvailableQuantity(batch) {
    const baseQty = batch.quantity || 0;
    const transactions = pharmacyCache.transactions.filter(tx => tx.batch && tx.batch.id === batch.id);
    const adjustment = transactions.reduce((sum, tx) => {
        const qty = tx.quantity || 0;
        return tx.transactionType === 'IN' ? sum + qty : sum - qty;
    }, 0);
    return Math.max(baseQty + adjustment, 0);
}

function openConsultation(appointmentId) {
    Promise.all([
        fetch(`/api/appointments/${appointmentId}`).then(r => r.json()),
        fetch(`/api/patients`).then(r => r.json()),
        fetch(`/api/staff`).then(r => r.json()),
        fetch(`/api/visits/from-appointment/${appointmentId}`, {
            method: 'POST'
        }).then(r => r.json()),
        loadMedicines(),
        loadBatches(),
        loadLabTests(),
        loadConsultationPrescriptions(appointmentId)
    ]).then(([appt, patients, staff, visit]) => {
        const patient = patients.find(p => p.id === appt.patientId);
        const doctor = staff.find(s => s.id === appt.staffId);

        currentConsultationAppointment = appt;
        document.getElementById('consultationAppointmentId').value = appt.id;
        document.getElementById('consultationDoctorId').value = appt.staffId;
        if (!visit || !visit.id) {
            throw new Error('Visit not available. Please reopen consultation.');
        }
        document.getElementById('consultationVisitId').value = visit.id;

        const summary = document.getElementById('consultationSummary');
        if (summary) {
            summary.textContent = `Appointment A${String(appt.id).padStart(3, '0')} | ${patient ? patient.name : 'Patient'} | ${doctor ? doctor.name : 'Doctor'} | ${appt.appointmentDate} ${appt.appointmentTime}`;
        }

        loadVisitNotes(visit.id);
        loadLabReports(visit.id);
        resetConsultationTabs();
        resetPrescriptionItems();

        const modal = document.getElementById('consultationModal');
        modal.classList.add('show');
    }).catch(error => {
        console.error('Error opening consultation:', error);
        alert('Error loading consultation');
    });
}

function closeConsultationModal() {
    const modal = document.getElementById('consultationModal');
    modal.classList.remove('show');
}

function resetConsultationTabs() {
    consultationTabs.forEach(tab => tab.classList.remove('active'));
    consultationTabContents.forEach(section => section.classList.remove('active'));

    const defaultTab = document.querySelector('.consultation-tab[data-consultation-tab="symptoms"]');
    const defaultContent = document.getElementById('consultation-symptoms');
    if (defaultTab && defaultContent) {
        defaultTab.classList.add('active');
        defaultContent.classList.add('active');
    }
}

function resetPrescriptionItems() {
    const container = document.getElementById('prescriptionItemsContainer');
    if (!container) return;
    container.innerHTML = CareflowHTML.sanitize('');
    addPrescriptionItemRow();
}

function loadConsultationPrescriptions(appointmentId) {
    return fetch('/api/pharmacy/prescriptions')
        .then(response => response.ok ? response.json() : [])
        .then(prescriptions => {
            const matched = prescriptions.filter(p => p.appointmentId === appointmentId);
            if (matched.length) {
                const latest = matched.reduce((acc, item) => {
                    if (!acc) return item;
                    return (item.id || 0) > (acc.id || 0) ? item : acc;
                }, null);
                currentConsultationPrescriptionId = latest ? latest.id : null;
                updateConsultationPrescriptionInfo(latest);
            } else {
                currentConsultationPrescriptionId = null;
                updateConsultationPrescriptionInfo(null);
            }
        })
        .catch(error => {
            console.error('Error loading prescriptions:', error);
            currentConsultationPrescriptionId = null;
            updateConsultationPrescriptionInfo(null);
        });
}

function updateConsultationPrescriptionInfo(prescription) {
    const info = document.getElementById('consultationPrescriptionInfo');
    if (!info) return;
    if (!prescription || !prescription.id) {
        info.textContent = 'No prescription created for this appointment yet.';
        return;
    }
    const status = prescription.status || 'CREATED';
    info.textContent = `Current Prescription: PR${String(prescription.id).padStart(3, '0')} (${status})`;
}

function addPrescriptionItemRow() {
    const container = document.getElementById('prescriptionItemsContainer');
    if (!container) return;

    const options = pharmacyCache.batches.map(batch => {
        const medicineName = resolveMedicineName(batch);
        const availableQty = calculateAvailableQuantity(batch);
        const expiry = batch.expiryDate ? `Exp: ${batch.expiryDate}` : 'No expiry';
        return `<option value="${batch.id}">${medicineName} | Batch ${batch.batchNo || '-'} | ${expiry} | Qty ${availableQty}</option>`;
    }).join('');

    const row = document.createElement('div');
    row.className = 'prescription-item-row';
    row.innerHTML = CareflowHTML.sanitize(`
        <select class="prescription-batch">
            <option value="">Select batch</option>
            ${options}
        </select>
        <input type="number" class="prescription-qty" min="1" placeholder="Qty">
        <input type="text" class="prescription-instructions" placeholder="Instructions">
        <button type="button" class="btn-small btn-danger" data-cf-action="removeRow()">Remove</button>
    `);
    container.appendChild(row);
}

function createPrescription() {
    const prescriptionRoot = document.getElementById('prescriptionItemsContainer');
    prescriptionRoot.querySelectorAll('.prescription-batch, .prescription-qty').forEach(field => field.required = true);
    if (!window.HospitalValidation.validate(prescriptionRoot)) return;
    const appointmentId = document.getElementById('consultationAppointmentId').value;
    const doctorId = document.getElementById('consultationDoctorId').value;

    const rows = Array.from(document.querySelectorAll('#prescriptionItemsContainer .prescription-item-row'));
    const items = rows.map(row => {
        return {
            batchId: parseInt(row.querySelector('.prescription-batch').value),
            quantity: parseInt(row.querySelector('.prescription-qty').value),
            instructions: row.querySelector('.prescription-instructions').value
        };
    }).filter(item => item.batchId && item.quantity);

    if (!items.length) {
        alert('Add at least one medicine item.');
        return;
    }

    const prescription = {
        appointmentId: parseInt(appointmentId),
        doctorId: parseInt(doctorId),
        date: new Date().toISOString(),
        status: 'CREATED'
    };

    fetch('/api/pharmacy/prescriptions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(prescription)
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to create prescription'));
            return response.json();
        })
        .then(created => {
            currentConsultationPrescriptionId = created.id;
            updateConsultationPrescriptionInfo(created);
            const itemRequests = items.map(item => {
                return fetch('/api/pharmacy/prescription-items', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        prescription: { id: created.id },
                        medicineBatch: { id: item.batchId },
                        quantity: item.quantity,
                        instructions: item.instructions
                    })
                });
            });

            return Promise.all(itemRequests);
        })
        .then(() => {
            alert('Prescription created successfully!');
            loadPharmacyPrescriptions();
        })
        .catch(error => {
            console.error('Error creating prescription:', error);
            alert('Error creating prescription: ' + error.message);
        });
}

function finalizePrescription() {
    if (!currentConsultationPrescriptionId) {
        alert('Create a prescription before finalizing.');
        return;
    }

    fetch(`/api/pharmacy/prescriptions/${currentConsultationPrescriptionId}/finalize`, {
        method: 'POST'
    })
        .then(async response => {
            if (!response.ok) {
                return response.json().then(err => {
                    throw new Error(err.message || 'Failed to finalize prescription');
                });
            }
            return response.json();
        })
        .then(result => {
            alert(result.message || 'Prescription finalized and sent to billing.');
            if (result.invoiceId) {
                const billingNav = document.querySelector('.nav-item[data-section="billing"]');
                if (billingNav) {
                    billingNav.click();
                }
            }
            loadConsultationPrescriptions(currentConsultationAppointment ? currentConsultationAppointment.id : 0);
        })
        .catch(error => {
            console.error('Error finalizing prescription:', error);
            alert(error.message || 'Error finalizing prescription.');
        });
}

function saveConsultationNotes() {
    if (!window.HospitalValidation.validate(document.getElementById('consultationSymptoms').parentElement)
        || !window.HospitalValidation.validate(document.getElementById('consultationDiagnosis').parentElement)) return;
    const visitIdRaw = document.getElementById('consultationVisitId').value;
    const visitId = parseInt(visitIdRaw, 10);
    if (!visitId) {
        alert('Visit is not ready. Please reopen the consultation.');
        return;
    }

    const notes = {
        symptoms: document.getElementById('consultationSymptoms').value,
        diagnosis: document.getElementById('consultationDiagnosis').value
    };

    fetch(`/api/visits/${visitId}/notes`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(notes)
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to save consultation notes'));
            return response.json();
        })
        .then(() => {
            alert('Consultation notes saved.');
        })
        .catch(error => {
            console.error('Error saving notes:', error);
            alert('Error saving consultation notes.');
        });
}

function loadVisitNotes(visitId) {
    if (!visitId) return;
    fetch(`/api/visits/${visitId}`)
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to fetch visit'));
            return response.json();
        })
        .then(visit => {
            document.getElementById('consultationSymptoms').value = visit.symptoms || '';
            document.getElementById('consultationDiagnosis').value = visit.diagnosis || '';
            const priority = document.getElementById('consultationLabPriority');
            if (priority) {
                priority.value = visit.priority || 'NORMAL';
            }
        })
        .catch(error => {
            console.error('Error loading visit:', error);
        });
}

function openDispenseModal(prescriptionId) {
    const prescription = pharmacyCache.prescriptions.find(presc => presc.id === prescriptionId);
    if (!prescription) return;

    currentDispensePrescription = prescription;

    const appt = pharmacyCache.appointments.find(item => item.id === prescription.appointmentId) || {};
    const patient = pharmacyCache.patients.find(item => item.id === appt.patientId) || {};
    const doctor = pharmacyCache.staff.find(item => item.id === prescription.doctorId) || {};

    const summary = document.getElementById('dispenseSummary');
    if (summary) {
        summary.textContent = `Prescription PR${String(prescription.id).padStart(3, '0')} | ${patient.name || 'Patient'} | ${doctor.name || 'Doctor'}`;
    }

    const items = pharmacyCache.prescriptionItems.filter(item => item.prescription && item.prescription.id === prescriptionId);
    const container = document.getElementById('dispenseItemsContainer');
    if (!container) return;

    if (!items.length) {
        container.innerHTML = CareflowHTML.sanitize('<p class="no-data">No prescription items found.</p>');
    } else {
        container.innerHTML = CareflowHTML.sanitize(items.map(item => {
            const prescribedBatch = item.medicineBatch || {};
            const batch = resolveDispenseBatch(prescribedBatch, item.quantity || 0);
            const medicineName = resolveMedicineName(batch);
            const availableQty = calculateAvailableQuantity(batch);
            const existingDispensed = getDispensedQuantity(item.id);
            const remaining = Math.max((item.quantity || 0) - existingDispensed, 0);
            const maxDispense = Math.min(remaining, availableQty);
            const batchLabel = batch.id !== prescribedBatch.id ? `Batch ${batch.batchNo || '-'} (latest available)` : `Batch ${batch.batchNo || '-'}`;

            return `
                <div class="dispense-item">
                    <div class="dispense-label">${medicineName} (${batchLabel})</div>
                    <div class="dispense-meta">Prescribed: ${item.quantity || 0} | Available: ${availableQty}</div>
                    <input type="number" class="dispense-qty" min="0" max="${maxDispense}" value="${maxDispense}" data-item-id="${item.id}" data-batch-id="${batch.id}">
                </div>
            `;
        }).join(''));
    }

    const modal = document.getElementById('dispenseModal');
    modal.classList.add('show');
}

function resolveDispenseBatch(prescribedBatch, requestedQuantity) {
    const prescribedAvailable = calculateAvailableQuantity(prescribedBatch);
    const today = new Date().toISOString().slice(0, 10);
    if (prescribedBatch.id && prescribedBatch.isActive !== false
        && (!prescribedBatch.expiryDate || prescribedBatch.expiryDate >= today)
        && prescribedAvailable >= requestedQuantity) {
        return prescribedBatch;
    }

    const medicineId = prescribedBatch.medicine && prescribedBatch.medicine.id;
    return pharmacyCache.batches
        .filter(batch => batch.medicine && batch.medicine.id === medicineId)
        .filter(batch => batch.isActive !== false && (!batch.expiryDate || batch.expiryDate >= today))
        .filter(batch => calculateAvailableQuantity(batch) >= requestedQuantity)
        .sort((first, second) => Number(second.id || 0) - Number(first.id || 0))[0] || prescribedBatch;
}

function closeDispenseModal() {
    const modal = document.getElementById('dispenseModal');
    modal.classList.remove('show');
}

function confirmDispense() {
    const container = document.getElementById('dispenseItemsContainer');
    if (!container || !currentDispensePrescription) return;

    if (!window.HospitalValidation.validate(container)) return;
    const inputs = Array.from(container.querySelectorAll('.dispense-qty'));
    const dispenseItemsPayload = [];

    for (const input of inputs) {
        const qty = parseInt(input.value, 10) || 0;
        if (!qty) continue;

        const itemId = parseInt(input.dataset.itemId, 10);
        const batchId = parseInt(input.dataset.batchId, 10);
        dispenseItemsPayload.push({
            prescriptionItemId: itemId,
            medicineBatchId: batchId,
            quantity: qty
        });
    }

    if (!dispenseItemsPayload.length) {
        alert('Enter dispense quantities.');
        return;
    }

    fetch('/api/dispense', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    prescriptionId: currentDispensePrescription.id,
                    dispensedBy: currentUser && currentUser.id ? currentUser.id : 1,
                    items: dispenseItemsPayload
                })
            })
        .then(async response => {
            const result = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(result.message || 'Dispense rejected by stock validation.');
            return result;
        })
        .then(() => {
            alert('Dispense recorded successfully.');
            closeDispenseModal();
            loadPharmacyData();
            const billingNav = document.querySelector('.nav-item[data-section="billing"]');
            if (billingNav) {
                billingNav.click();
            }
        })
        .catch(error => {
            console.error('Error dispensing items:', error);
            alert(error.message || 'Error dispensing items.');
        });
}

function getDispensedQuantity(prescriptionItemId) {
    return pharmacyCache.dispensedItems
        .filter(item => item.prescriptionItem && item.prescriptionItem.id === prescriptionItemId)
        .reduce((sum, item) => sum + (item.quantity || 0), 0);
}

function isPrescriptionDispensed(prescriptionId) {
    const items = pharmacyCache.prescriptionItems.filter(item => item.prescription && item.prescription.id === prescriptionId);
    if (!items.length) return false;

    return items.every(item => {
        const dispensedQty = getDispensedQuantity(item.id);
        return dispensedQty >= (item.quantity || 0);
    });
}

function loadLabData() {
    Promise.all([
        loadLabTests(),
        loadLabOrders('PENDING'),
        loadLabOrders('IN_PROGRESS'),
        loadLabOrders('COMPLETED')
    ]).catch(error => {
        console.error('Error loading lab data:', error);
    });
}

function loadLabTests() {
    return fetch('/api/lab/tests')
        .then(response => response.ok ? response.json() : [])
        .then(tests => {
            labCache.tests = tests || [];
            renderConsultationLabTests();
            return tests;
        })
        .catch(error => {
            console.error('Error loading lab tests:', error);
            const container = document.getElementById('consultationLabTests');
            if (container) {
                container.innerHTML = CareflowHTML.sanitize('<span style="color: #d32f2f;">Failed to load lab tests.</span>');
            }
            return [];
        });
}

function renderConsultationLabTests() {
    const container = document.getElementById('consultationLabTests');
    if (!container) return;

    if (!labCache.tests.length) {
        container.innerHTML = CareflowHTML.sanitize('<span>No lab tests available.</span>');
        return;
    }

    container.innerHTML = CareflowHTML.sanitize(labCache.tests.map(test => `
        <label>
            <input type="checkbox" class="lab-test-checkbox" value="${test.id}">
            ${test.testName} (${formatCurrency(test.price || 0)})
        </label>
    `).join(''));
}

function createLabOrder() {
    if (!window.HospitalValidation.validate(document.getElementById('consultationLabNotes').parentElement)) return;
    const visitIdRaw = document.getElementById('consultationVisitId').value;
    const visitId = parseInt(visitIdRaw, 10);
    if (!visitId) {
        alert('Visit is not ready. Please reopen the consultation.');
        return;
    }

    const selected = Array.from(document.querySelectorAll('.lab-test-checkbox:checked'))
        .map(input => parseInt(input.value, 10))
        .filter(Boolean);

    if (!selected.length) {
        alert('Select at least one lab test.');
        return;
    }

    const priority = document.getElementById('consultationLabPriority').value;
    const notes = document.getElementById('consultationLabNotes').value;

    fetch(`/api/visits/${visitId}/lab-orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            orderedByDoctorId: parseInt(document.getElementById('consultationDoctorId').value, 10),
            notes: notes,
            testIds: selected,
            priority: priority
        })
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to create lab order'));
            return response.json();
        })
        .then(() => {
            alert('Lab order created.');
            loadLabData();
        })
        .catch(error => {
            console.error('Error creating lab order:', error);
            alert('Error creating lab order.');
        });
}

function loadLabOrders(status) {
    const url = status ? `/api/lab/orders?status=${encodeURIComponent(status)}` : '/api/lab/orders';
    return fetch(url)
        .then(response => response.ok ? response.json() : [])
        .then(orders => {
            if (status) {
                labCache.ordersByStatus[status] = orders || [];
            }
            if (status === 'PENDING') {
                renderLabOrders(orders || [], 'labPendingTable', true);
            } else if (status === 'IN_PROGRESS') {
                renderLabOrders(orders || [], 'labInProgressTable', true);
            } else if (status === 'COMPLETED') {
                renderLabOrders(orders || [], 'labCompletedTable', false);
            }
            return orders;
        })
        .catch(error => {
            console.error('Error loading lab orders:', error);
            return [];
        });
}

function renderLabOrders(orders, tableId, showActions) {
    const tableBody = document.querySelector(`#${tableId} tbody`);
    if (!tableBody) return;

    const filteredOrders = applyLabSearchFilter(orders);

    if (!filteredOrders.length) {
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="5" style="text-align: center;">No lab orders found</td></tr>');
        return;
    }

    const testMap = {};
    labCache.tests.forEach(test => {
        testMap[test.id] = test.testName;
    });

    const itemPromises = filteredOrders.map(order => {
        return fetch(`/api/lab/orders/${order.id}/items`)
            .then(response => response.ok ? response.json() : [])
            .then(items => ({
                order,
                tests: items.map(item => testMap[item.testId] || `Test ${item.testId}`).join(', ')
            }));
    });

    Promise.all(itemPromises)
        .then(rows => {
            const searched = rows.filter(({ order, tests }) => {
                if (!labSearchValue) return true;
                const needle = labSearchValue;
                const orderIdStr = String(order.id || '');
                const visitIdStr = String(order.visitId || '');
                const testsStr = (tests || '').toLowerCase();
                return orderIdStr.includes(needle) || visitIdStr.includes(needle) || testsStr.includes(needle);
            });

            if (!searched.length) {
                tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="5" style="text-align: center;">No lab orders found</td></tr>');
                return;
            }

            tableBody.innerHTML = CareflowHTML.sanitize(searched.map(({ order, tests }) => {
                const statusBadge = order.status === 'COMPLETED' ? 'badge-success' : 'badge-warning';
                const visitLabel = order.visitId ? `V${String(order.visitId).padStart(3, '0')}` : '-';

                if (showActions) {
                    return `
                        <tr>
                            <td>LO${String(order.id).padStart(3, '0')}</td>
                            <td>${visitLabel}</td>
                            <td>${tests || '-'}</td>
                            <td><span class="badge ${statusBadge}">${order.status || 'PENDING'}</span></td>
                            <td>
                                <button class="btn-small btn-warning" data-cf-action="updateLabOrderStatus(${order.id}, 'IN_PROGRESS')">In Progress</button>
                                <button class="btn-small btn-info" data-cf-action="updateLabOrderStatus(${order.id}, 'COMPLETED')">Complete</button>
                                <button class="btn-small" data-cf-action="openLabReportModal(${order.id}, ${order.visitId})">Upload</button>
                                <button class="btn-small btn-info" data-cf-action="openLabResultsModal(${order.id})">Results</button>
                            </td>
                        </tr>
                    `;
                }

                return `
                    <tr>
                        <td>LO${String(order.id).padStart(3, '0')}</td>
                        <td>${visitLabel}</td>
                        <td>${tests || '-'}</td>
                        <td><span class="badge ${statusBadge}">${order.status || 'COMPLETED'}</span></td>
                        <td><button class="btn-small btn-info" data-cf-action="openLabReportModal(${order.id}, ${order.visitId})">Upload</button></td>
                    </tr>
                `;
            }).join(''));
        })
        .catch(error => {
            console.error('Error loading lab order items:', error);
            tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="5" style="text-align: center; color: red;">Failed to load lab orders</td></tr>');
        });
}

function applyLabSearchFilter(orders) {
    if (!labSearchValue) return orders;
    return orders.filter(order => {
        const needle = labSearchValue;
        const orderIdStr = String(order.id || '');
        const visitIdStr = String(order.visitId || '');
        return orderIdStr.includes(needle) || visitIdStr.includes(needle);
    });
}

function rerenderLabTables() {
    renderLabOrders(labCache.ordersByStatus.PENDING || [], 'labPendingTable', true);
    renderLabOrders(labCache.ordersByStatus.IN_PROGRESS || [], 'labInProgressTable', true);
    renderLabOrders(labCache.ordersByStatus.COMPLETED || [], 'labCompletedTable', false);
}

function loadBedData() {
    Promise.all([
        fetch('/api/beds/summary').then(r => r.ok ? r.json() : {}),
        fetch('/api/beds').then(r => r.ok ? r.json() : []),
        fetch('/api/admissions/active').then(r => r.ok ? r.json() : []),
        fetch('/api/patients').then(r => r.ok ? r.json() : []),
        fetch('/api/staff').then(r => r.ok ? r.json() : [])
    ])
        .then(([summary, beds, admissions, patients, staff]) => {
            renderBedSummary(summary);
            renderBedsTable(beds || []);
            renderAdmissionsTable(admissions || [], patients || [], staff || []);
            populateAdmitSelects(patients || [], staff || []);
        })
        .catch(error => {
            console.error('Error loading bed data:', error);
        });
}

function renderBedSummary(summary) {
    const totalEl = document.getElementById('bedsTotalCount');
    const availableEl = document.getElementById('bedsAvailableCount');
    const occupiedEl = document.getElementById('bedsOccupiedCount');
    const typeEl = document.getElementById('bedsTypeSummary');
    if (!summary) return;
    if (totalEl) totalEl.textContent = summary.total || 0;
    if (availableEl) availableEl.textContent = summary.available || 0;
    if (occupiedEl) occupiedEl.textContent = summary.occupied || 0;
    if (typeEl) {
        const icu = summary.icu || 0;
        const general = summary.general || 0;
        const privateCount = summary.private || 0;
        typeEl.textContent = `${icu} / ${general} / ${privateCount}`;
    }
    const dashboardAvailable = document.getElementById('dashboardAvailableBeds');
    if (dashboardAvailable && typeof summary.available !== 'undefined') {
        dashboardAvailable.textContent = summary.available;
    }
}

function renderBedsTable(beds) {
    const tableBody = document.querySelector('#bedsTable tbody');
    if (!tableBody) return;
    if (!beds.length) {
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="6" style="text-align: center;">No beds found</td></tr>');
        return;
    }
    tableBody.innerHTML = CareflowHTML.sanitize(beds.map(bed => `
        <tr>
            <td>${bed.bedNumber || '-'}</td>
            <td>${bed.ward || '-'}</td>
            <td>${bed.type || '-'}</td>
            <td><span class="badge ${bed.status === 'AVAILABLE' ? 'badge-success' : bed.status === 'OCCUPIED' ? 'badge-warning' : 'badge-error'}">${bed.status || '-'}</span></td>
            <td>${formatCurrency(bed.dailyCharge || 0)}</td>
            <td>
                <div class="appointments-actions">
                    ${bed.status === 'AVAILABLE' ? `<button class="btn-small btn-info" data-cf-action="openAdmitPatientModal(${bed.id})">Admit</button>` : ''}
                    <button class="btn-small btn-warning" data-cf-action="updateBedStatus(${bed.id}, 'MAINTENANCE')">Maintenance</button>
                    <button class="btn-small" data-cf-action="updateBedStatus(${bed.id}, 'AVAILABLE')">Available</button>
                </div>
            </td>
        </tr>
    `).join(''));
}

function renderAdmissionsTable(admissions, patients, staff) {
    const tableBody = document.querySelector('#admissionsTable tbody');
    if (!tableBody) return;

    const patientMap = {};
    patients.forEach(p => patientMap[p.id] = p.name);
    const staffMap = {};
    staff.forEach(s => staffMap[s.id] = s.name);

    if (!admissions.length) {
        tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="7" style="text-align: center;">No active admissions</td></tr>');
        return;
    }

    tableBody.innerHTML = CareflowHTML.sanitize(admissions.map(adm => `
        <tr>
            <td>AD${String(adm.id).padStart(3, '0')}</td>
            <td>${adm.bedNumber || '-'}</td>
            <td>${patientMap[adm.patientId] || 'Unknown'}</td>
            <td>${staffMap[adm.doctorId] || 'Unknown'}</td>
            <td>${adm.admittedAt ? new Date(adm.admittedAt).toLocaleString() : '-'}</td>
            <td><span class="badge badge-warning">${adm.status || 'ACTIVE'}</span></td>
            <td>
                <button class="btn-small btn-danger" data-cf-action="dischargeAdmission(${adm.id})">Discharge</button>
            </td>
        </tr>
    `).join(''));
}

function populateAdmitSelects(patients, staff) {
    const patientSelect = document.getElementById('admitPatientId');
    const doctorSelect = document.getElementById('admitDoctorId');
    if (patientSelect) {
        patientSelect.innerHTML = CareflowHTML.sanitize('<option value="">Select Patient</option>' +
            patients.map(p => `<option value="${p.id}">${p.name}</option>`).join(''));
        patientSelect.addEventListener('change', handleAdmitPatientChange);
    }
    if (doctorSelect) {
        doctorSelect.innerHTML = CareflowHTML.sanitize('<option value="">Select Doctor</option>' +
            staff.filter(s => s.position === 'Doctor').map(s => `<option value="${s.id}">${s.name}</option>`).join(''));
    }
}

function openAddBedModal() {
    const modal = document.getElementById('addBedModal');
    if (modal) modal.classList.add('show');
}

function closeAddBedModal() {
    const modal = document.getElementById('addBedModal');
    if (modal) modal.classList.remove('show');
    const form = document.getElementById('addBedForm');
    if (form) form.reset();
}

function handleAddBed(event) {
    event.preventDefault();
    const formData = new FormData(event.target);
    const payload = {
        bedNumber: formData.get('bedNumber'),
        ward: formData.get('ward'),
        type: formData.get('type'),
        status: 'AVAILABLE',
        dailyCharge: parseFloat(formData.get('dailyCharge'))
    };

    fetch('/api/beds', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to add bed'));
            return response.json();
        })
        .then(() => {
            alert('Bed added.');
            closeAddBedModal();
            loadBedData();
        })
        .catch(error => {
            console.error('Error adding bed:', error);
            alert('Error adding bed.');
        });
}

function openAdmitPatientModal(bedId) {
    const modal = document.getElementById('admitPatientModal');
    document.getElementById('admitBedId').value = bedId;
    if (modal) modal.classList.add('show');
}

function closeAdmitPatientModal() {
    const modal = document.getElementById('admitPatientModal');
    if (modal) modal.classList.remove('show');
    const form = document.getElementById('admitPatientForm');
    if (form) form.reset();
}

function handleAdmitPatient(event) {
    event.preventDefault();
    const bedId = parseInt(document.getElementById('admitBedId').value, 10);
    const patientId = parseInt(document.getElementById('admitPatientId').value, 10);
    const visitId = parseInt(document.getElementById('admitVisitId').value, 10);
    const doctorId = parseInt(document.getElementById('admitDoctorId').value, 10);
    const notes = document.getElementById('admitNotes').value;

    if (!bedId || !patientId || !visitId || !doctorId) {
        alert('Please fill all required fields.');
        return;
    }

    fetch('/api/admissions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            bedId: bedId,
            patientId: patientId,
            visitId: visitId,
            doctorId: doctorId,
            notes: notes
        })
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to admit patient'));
            return response.json();
        })
        .then(() => {
            alert('Patient admitted.');
            closeAdmitPatientModal();
            loadBedData();
        })
        .catch(error => {
            console.error('Error admitting patient:', error);
            alert('Error admitting patient.');
        });
}

function handleAdmitPatientChange(event) {
    const patientId = parseInt(event.target.value, 10);
    const visitInput = document.getElementById('admitVisitId');
    if (!patientId || !visitInput) return;

    fetch(`/api/visits/patient/${patientId}/latest`)
        .then(async response => {
            if (!response.ok) {
                visitInput.value = '';
                return null;
            }
            return response.json();
        })
        .then(visit => {
            if (visit && visit.id) {
                visitInput.value = visit.id;
            } else {
                visitInput.value = '';
            }
        })
        .catch(error => {
            console.error('Error fetching latest visit:', error);
            visitInput.value = '';
        });
}

function updateBedStatus(bedId, status) {
    fetch(`/api/beds/${bedId}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: status })
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to update bed status'));
            return response.json();
        })
        .then(() => loadBedData())
        .catch(error => {
            console.error('Error updating bed status:', error);
            alert('Error updating bed status.');
        });
}

function dischargeAdmission(admissionId) {
    if (!confirm('Discharge this patient and release bed?')) return;
    fetch(`/api/admissions/${admissionId}/discharge`, {
        method: 'PUT'
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to discharge'));
            return response.json();
        })
        .then(() => {
            alert('Patient discharged.');
            loadBedData();
        })
        .catch(error => {
            console.error('Error discharging patient:', error);
            alert('Error discharging patient.');
        });
}

function setupNotificationSettings() {
    // Loaded when settings are opened.
    setupNotificationCenter();
}

function setupNotificationBell() {
    if (!notificationBell || !notificationDropdown) return;
    notificationBell.addEventListener('click', () => {
        notificationDropdown.classList.toggle('show');
    });
    document.addEventListener('click', (event) => {
        if (!notificationDropdown.contains(event.target) && !notificationBell.contains(event.target)) {
            notificationDropdown.classList.remove('show');
        }
    });
    // React notifications load on demand.
}

function loadNotificationBell() {
    if (!notificationDropdownList || !notificationBadge) return;
    fetch('/api/notifications/queue')
        .then(response => response.ok ? response.json() : [])
        .then(queue => {
            const filtered = filterNotificationsByRole(queue || []);
            const unreadCount = filtered.filter(item => !item.isRead).length;
            notificationBadge.textContent = unreadCount > 99 ? '99+' : String(unreadCount);

            if (!filtered.length) {
                notificationDropdownList.innerHTML = CareflowHTML.sanitize('<div class="notification-item">No notifications yet.</div>');
                return;
            }
            const items = filtered.slice(0, 8).map(item => {
                const time = item.createdAt ? new Date(item.createdAt).toLocaleString() : '-';
                const roleClass = roleClassName(item.role);
                const unreadClass = item.isRead ? '' : ' unread';
                return `
                    <div class="notification-item${unreadClass}">
                        <div><span class="notification-role ${roleClass}">${item.role || ''}</span><strong>${item.eventType || 'Notification'}</strong></div>
                        <div>${item.subject || ''}</div>
                        <div class="meta">${time} · ${item.status || ''}</div>
                        ${item.isRead ? '' : `<div class="notification-actions"><button class="notification-mark-read" data-cf-action="markNotificationRead(${item.id})">Mark as read</button></div>`}
                    </div>
                `;
            }).join('');
            notificationDropdownList.innerHTML = CareflowHTML.sanitize(items);
        })
        .catch(error => {
            console.error('Error loading notification bell:', error);
            notificationDropdownList.innerHTML = CareflowHTML.sanitize('<div class="notification-item">Failed to load notifications.</div>');
        });
}

function filterNotificationsByRole(queue) {
    if (!currentUser || !currentUser.role) return [];
    const role = currentUser.role;
    if (role === 'Admin') return queue;
    let targetRole = role;
    if (role === 'Lab Technician') targetRole = 'Lab';
    if (role === 'Receptionist') {
        return queue.filter(item => item.role === 'Receptionist' || item.role === 'Billing');
    }
    return queue.filter(item => item.role === targetRole);
}

function roleClassName(role) {
    if (!role) return 'admin';
    const normalized = role.toLowerCase();
    if (normalized.includes('doctor')) return 'doctor';
    if (normalized.includes('lab')) return 'lab';
    if (normalized.includes('billing')) return 'billing';
    if (normalized.includes('reception')) return 'receptionist';
    return 'admin';
}

function markNotificationRead(id) {
    fetch(`/api/notifications/queue/${id}/read`, {
        method: 'PUT'
    })
        .then(response => response.ok ? response.json() : null)
        .then(() => {
            // React notifications load on demand.
        })
        .catch(error => {
            console.error('Error marking notification as read:', error);
        });
}

function loadNotificationSettings() {
    const container = document.getElementById('notificationSettingsContainer');
    if (!container) return;
    fetch('/api/notifications/settings')
        .then(response => response.ok ? response.json() : [])
        .then(settings => {
            renderNotificationSettings(settings || []);
        })
        .catch(error => {
            console.error('Error loading notification settings:', error);
            container.textContent = 'Failed to load notification settings.';
        });
}

function renderNotificationSettings(settings) {
    const container = document.getElementById('notificationSettingsContainer');
    if (!container) return;

    const roles = ['Doctor', 'Lab', 'Billing', 'Receptionist'];
    const events = [
        { key: 'APPOINTMENT_REMINDER', label: 'Appointment Reminders' },
        { key: 'LAB_REPORT_READY', label: 'Lab Report Ready' },
        { key: 'BILLING_REMINDER', label: 'Billing Reminders' }
    ];
    const channels = ['EMAIL', 'SMS'];

    const settingMap = {};
    settings.forEach(s => {
        const key = `${s.role}-${s.eventType}-${s.channel}`;
        settingMap[key] = s.enabled;
    });

    container.innerHTML = CareflowHTML.sanitize(roles.map(role => {
        const rows = events.map(evt => {
            const channelToggles = channels.map(ch => {
                const key = `${role}-${evt.key}-${ch}`;
                const checked = settingMap[key] !== undefined ? settingMap[key] : true;
                return `
                    <label style="margin-right: 12px;">
                        <input type="checkbox" data-role="${role}" data-event="${evt.key}" data-channel="${ch}" ${checked ? 'checked' : ''}>
                        ${ch}
                    </label>
                `;
            }).join('');

            return `
                <div style="margin-bottom: 10px;">
                    <strong>${evt.label}</strong>
                    <div style="margin-top: 6px;">${channelToggles}</div>
                </div>
            `;
        }).join('');

        return `
            <div style="margin-bottom: 18px;">
                <h4 style="margin-bottom: 8px;">${role}</h4>
                ${rows}
            </div>
        `;
    }).join(''));

    container.querySelectorAll('input[type="checkbox"]').forEach(input => {
        input.addEventListener('change', handleNotificationSettingChange);
    });
}

function handleNotificationSettingChange(event) {
    const input = event.target;
    const payload = {
        role: input.dataset.role,
        eventType: input.dataset.event,
        channel: input.dataset.channel,
        enabled: input.checked
    };

    fetch('/api/notifications/settings', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(response => response.ok ? response.json() : null)
        .then(() => {
            // no-op
        })
        .catch(error => {
            console.error('Error updating notification setting:', error);
            alert('Failed to update setting.');
        });
}

function setupNotificationCenter() {
    const refreshBtn = document.getElementById('refreshNotificationsBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadNotificationQueue);
    }
    // Loaded on demand from Settings.
}

function loadNotificationQueue() {
    const tableBody = document.querySelector('#notificationQueueTable tbody');
    if (!tableBody) return;
    fetch('/api/notifications/queue')
        .then(response => response.ok ? response.json() : [])
        .then(queue => {
            if (!queue || !queue.length) {
                tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="6" style="text-align: center;">No notifications yet.</td></tr>');
                return;
            }
            tableBody.innerHTML = CareflowHTML.sanitize(queue.map(item => {
                const time = item.createdAt ? new Date(item.createdAt).toLocaleString() : '-';
                const statusClass = item.status === 'SENT' ? 'badge-success' : (item.status === 'FAILED' ? 'badge-error' : 'badge-warning');
                return `
                    <tr>
                        <td>${time}</td>
                        <td>${item.role || '-'}</td>
                        <td>${item.eventType || '-'}</td>
                        <td>${item.channel || '-'}</td>
                        <td>${item.recipient || '-'}</td>
                        <td><span class="badge ${statusClass}">${item.status || 'PENDING'}</span></td>
                    </tr>
                `;
            }).join(''));
        })
        .catch(error => {
            console.error('Error loading notifications:', error);
            tableBody.innerHTML = CareflowHTML.sanitize('<tr><td colspan="6" style="text-align: center; color: red;">Failed to load notifications.</td></tr>');
        });
}

function updateLabOrderStatus(orderId, status) {
    fetch(`/api/lab/orders/${orderId}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: status })
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to update status'));
            return response.json();
        })
        .then(() => {
            loadLabData();
        })
        .catch(error => {
            console.error('Error updating lab status:', error);
            alert('Error updating lab order status.');
        });
}

function setupLabReportForm() {
    const form = document.getElementById('labReportForm');
    if (!form) return;
    form.onsubmit = handleLabReportSubmit;
}

function openLabReportModal(orderId, visitId) {
    document.getElementById('labReportOrderId').value = orderId || '';
    document.getElementById('labReportVisitId').value = visitId || '';
    document.getElementById('labReportForm').reset();
    const modal = document.getElementById('labReportModal');
    modal.classList.add('show');
}

function closeLabReportModal() {
    const modal = document.getElementById('labReportModal');
    modal.classList.remove('show');
}

function openLabResultsModal(orderId) {
    const container = document.getElementById('labResultsContainer');
    if (!container) return;
    container.innerHTML = CareflowHTML.sanitize('Loading...');
    const modal = document.getElementById('labResultsModal');
    modal.classList.add('show');

    Promise.all([
        fetch(`/api/lab/orders/${orderId}/items`).then(r => r.ok ? r.json() : []),
        fetch('/api/lab/tests').then(r => r.ok ? r.json() : [])
    ])
        .then(([items, tests]) => {
            const testMap = {};
            (tests || []).forEach(test => testMap[test.id] = test);
            if (!items || !items.length) {
                container.innerHTML = CareflowHTML.sanitize('<p class="no-data">No lab items found.</p>');
                return;
            }

            container.innerHTML = CareflowHTML.sanitize(items.map(item => {
                const test = testMap[item.testId] || {};
                return `
                    <div class="prescription-item-row" data-lab-item-id="${item.id}">
                        <div>
                            <strong>${test.testName || 'Test'}</strong>
                            <div style="font-size: 12px; color: #666;">Ref: ${item.referenceRange || test.normalRangeText || '-'}</div>
                        </div>
                        <input type="text" class="lab-result-value" placeholder="Value" value="${item.resultValue || ''}">
                        <input type="text" class="lab-result-unit" placeholder="Unit" value="${item.resultUnit || ''}">
                        <input type="text" class="lab-result-range" placeholder="Reference" value="${item.referenceRange || test.normalRangeText || ''}">
                        <select class="lab-result-flag">
                            <option value="">Status</option>
                            <option value="NORMAL" ${item.resultFlag === 'NORMAL' ? 'selected' : ''}>NORMAL</option>
                            <option value="HIGH" ${item.resultFlag === 'HIGH' ? 'selected' : ''}>HIGH</option>
                            <option value="LOW" ${item.resultFlag === 'LOW' ? 'selected' : ''}>LOW</option>
                        </select>
                        <input type="text" class="lab-result-notes" placeholder="Notes" value="${item.resultNotes || ''}">
                        <button type="button" class="btn-small btn-primary" data-cf-action="saveLabResult(${item.id})">Save</button>
                    </div>
                `;
            }).join(''));
        })
        .catch(error => {
            console.error('Error loading lab items:', error);
            container.innerHTML = CareflowHTML.sanitize('<p class="no-data">Failed to load lab items.</p>');
        });
}

function closeLabResultsModal() {
    const modal = document.getElementById('labResultsModal');
    modal.classList.remove('show');
}

function saveLabResult(itemId) {
    const row = document.querySelector(`[data-lab-item-id="${itemId}"]`);
    if (!row || !window.HospitalValidation.validate(row)) return;
    const value = row.querySelector('.lab-result-value')?.value || '';
    const unit = row.querySelector('.lab-result-unit')?.value || '';
    const referenceRange = row.querySelector('.lab-result-range')?.value || '';
    const flag = row.querySelector('.lab-result-flag')?.value || '';
    const notes = row.querySelector('.lab-result-notes')?.value || '';

    fetch(`/api/lab/order-items/${itemId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            resultValue: value,
            resultUnit: unit,
            referenceRange: referenceRange,
            resultFlag: flag,
            resultNotes: notes,
            status: value ? 'COMPLETED' : 'IN_PROGRESS'
        })
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to save result'));
            return response.json();
        })
        .then(() => {
            alert('Result saved.');
            loadLabData();
        })
        .catch(error => {
            console.error('Error saving result:', error);
            alert('Error saving result.');
        });
}

function handleLabReportSubmit(event) {
    event.preventDefault();
    const visitId = parseInt(document.getElementById('labReportVisitId').value, 10);
    const orderId = parseInt(document.getElementById('labReportOrderId').value, 10);
    const fileName = document.getElementById('labReportFileName').value.trim();
    const fileUrl = document.getElementById('labReportFileUrl').value.trim();
    const mimeType = document.getElementById('labReportMimeType').value.trim();

    if (!visitId || !fileName || !fileUrl) {
        alert('Visit, file name, and URL are required.');
        return;
    }

    fetch('/api/lab/reports', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            visitId: visitId,
            labOrderId: orderId || null,
            fileName: fileName,
            fileUrl: fileUrl,
            mimeType: mimeType || null
        })
    })
        .then(async response => {
            if (!response.ok) throw new Error(await hospitalResponseError(response, 'Failed to upload report'));
            return response.json();
        })
        .then(() => {
            alert('Report uploaded.');
            closeLabReportModal();
            loadLabReports(visitId);
            loadLabData();
        })
        .catch(error => {
            console.error('Error uploading report:', error);
            alert('Error uploading report.');
        });
}

function loadLabReports(visitId) {
    const container = document.getElementById('consultationLabReports');
    if (!container || !visitId) return;

    fetch(`/api/visits/${visitId}/lab-reports`)
        .then(response => response.ok ? response.json() : [])
        .then(reports => {
            labCache.reports = reports || [];
            if (!labCache.reports.length) {
                container.textContent = 'No reports uploaded.';
                return;
            }
            container.replaceChildren();
            labCache.reports.forEach(report => {
                const row = document.createElement('div');
                const link = document.createElement('a');
                link.textContent = report.fileName || 'Report';
                link.href = `/api/lab/reports/${report.id}/pdf`;
                link.target = '_blank';
                link.rel = 'noopener';
                row.append(link);
                container.append(row);
            });
        })
        .catch(error => {
            console.error('Error loading lab reports:', error);
            container.textContent = 'Failed to load reports.';
        });
}

async function sendAppointmentWhatsApp(appointmentId, button) {
    let templateMode;
    try {
        const modeResponse = await fetch('/api/whatsapp/mode');
        if (!modeResponse.ok) throw new Error('Cannot read WhatsApp configuration.');
        templateMode = (await modeResponse.json()).template;
    } catch (error) { alert(error.message); return; }
    const description = templateMode
        ? 'Send the Twilio TEST appointment reminder? Its sample date/time is fixed and does NOT match this booking. Replies C/R do not update appointments. Use only a test patient and confirm consent.'
        : 'Send an appointment reminder with the saved booking date and time? Confirm the saved patient number and consent.';
    if (!confirm(description)) return;
    const password = prompt('Enter the WhatsApp operator password:');
    if (!password) return;
    const sessionConfirmed = !templateMode && confirm('Has the patient messaged this sender within the last 24 hours?');
    if (!templateMode && !sessionConfirmed) return;
    button.disabled = true;
    button.querySelector('.action-label').textContent = 'Submitting...';
    try {
        const response = await fetch(`/api/whatsapp/appointments/${appointmentId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'X-WhatsApp-Password': password},
            body: JSON.stringify({consentConfirmed: true, sessionConfirmed})
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || 'WhatsApp submission failed.');
        alert(`WhatsApp request accepted (${result.status}). This does not confirm delivery. Message ID: ${result.sid}`);
    } catch (error) {
        alert(error.message || 'Submission could not be confirmed. Check Twilio logs before retrying.');
    } finally {
        button.disabled = false;
        button.querySelector('.action-label').textContent = 'WhatsApp';
    }
}
