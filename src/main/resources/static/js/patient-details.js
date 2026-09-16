async function hospitalResponseError(response, fallback) {
    try { const body = await response.clone().json(); window.HospitalValidation?.applyServerErrors(body.fieldErrors); return body.message || fallback; }
    catch { return fallback; }
}

// Same-origin custom header prevents cross-site forms from making changes.
const patientOriginalFetch = window.fetch.bind(window);
window.fetch = (input, init) => {
    const url = new URL(input instanceof Request ? input.url : String(input), location.href);
    if (url.origin === location.origin && url.pathname.startsWith('/api/')) {
        const headers = new Headers(init?.headers || (input instanceof Request ? input.headers : undefined));
        headers.set('X-Requested-With', 'Careflow');
        init = { ...init, headers };
    }
    return patientOriginalFetch(input, init);
};
// Global variables
let currentPatientId = null;
let currentEditingHistoryId = null;
let currentEditingAllergyId = null;
let currentEditingConditionId = null;

// Get patient ID from URL
function getPatientIdFromURL() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('id');
}

// Initialize page
document.addEventListener('DOMContentLoaded', function() {
    currentPatientId = getPatientIdFromURL();
    
    if (!currentPatientId) {
        alert('Patient ID not found!');
        window.location.href = '/app/#/patients';
        return;
    }

    // Tab switching
    const tabButtons = document.querySelectorAll('.tab-button');
    tabButtons.forEach(button => {
        button.addEventListener('click', switchTab);
    });

    // Modal close buttons
    document.querySelectorAll('.close').forEach(closeBtn => {
        closeBtn.addEventListener('click', function() {
            this.closest('.modal').style.display = 'none';
        });
    });

    // Back button
    document.getElementById('backBtn').addEventListener('click', function() {
        window.location.href = '/app/#/patients';
    });

    // Button listeners
    document.getElementById('addHistoryBtn').addEventListener('click', openHistoryModal);
    document.getElementById('addAllergyBtn').addEventListener('click', openAllergyModal);
    document.getElementById('addConditionBtn').addEventListener('click', openConditionModal);

    // Form submissions
    document.getElementById('historyForm').addEventListener('submit', saveHistory);
    document.getElementById('allergyForm').addEventListener('submit', saveAllergy);
    document.getElementById('conditionForm').addEventListener('submit', saveCondition);

    // Load patient data
    loadPatientData();
    loadMedicalHistory();
    loadAllergies();
    loadChronicConditions();
    loadPrescriptionHistory();

    const printBtn = document.getElementById('printPrescriptionHistoryBtn');
    if (printBtn) {
        printBtn.addEventListener('click', printPrescriptionHistory);
    }
});

// Tab switching
function switchTab(e) {
    const tabName = e.target.getAttribute('data-tab');
    
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });

    // Remove active class from all buttons
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });

    // Show selected tab
    document.getElementById(tabName).classList.add('active');
    e.target.classList.add('active');
}

// Load patient data
function loadPatientData() {
    fetch(`/api/patients/${currentPatientId}`)
        .then(response => response.json())
        .then(data => {
            document.getElementById('patientName').innerText = data.name || 'N/A';
            document.getElementById('patientEmail').innerText = data.email || 'N/A';
            document.getElementById('patientPhone').innerText = data.phone || 'N/A';
            document.getElementById('patientDOB').innerText = data.dateOfBirth || 'N/A';
            document.getElementById('patientAddress').innerText = data.address || 'N/A';
        })
        .catch(error => console.error('Error loading patient data:', error));
}

// Load Medical History
function loadMedicalHistory() {
    fetch(`/api/patients/${currentPatientId}/medical-history`)
        .then(response => response.json())
        .then(data => {
            const historyList = document.getElementById('historyList');
            if (data.length === 0) {
                historyList.innerHTML = CareflowHTML.sanitize('<p class="no-data">No medical history records found.</p>');
                return;
            }

            historyList.innerHTML = CareflowHTML.sanitize(data.map(history => `
                <div class="record-card">
                    <div class="record-header">
                        <div class="record-title">${history.historyType}</div>
                        <div class="record-actions">
                            <button class="btn btn-edit" data-cf-action="editHistory(${history.historyId})">Edit</button>
                            <button class="btn btn-danger" data-cf-action="deleteHistory(${history.historyId})">Delete</button>
                        </div>
                    </div>
                    <div class="record-details">
                        <div class="detail">
                            <span class="detail-label">Description:</span>
                            <span class="detail-value">${history.description || 'N/A'}</span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Date:</span>
                            <span class="detail-value">${history.historyDate || 'N/A'}</span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Notes:</span>
                            <span class="detail-value">${history.notes || 'N/A'}</span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Recorded At:</span>
                            <span class="detail-value">${history.recordedAt || 'N/A'}</span>
                        </div>
                    </div>
                </div>
            `).join(''));
        })
        .catch(error => console.error('Error loading medical history:', error));
}

// Load Allergies
function loadAllergies() {
    fetch(`/api/patients/${currentPatientId}/allergies`)
        .then(response => response.json())
        .then(data => {
            const allergyList = document.getElementById('allergyList');
            if (data.length === 0) {
                allergyList.innerHTML = CareflowHTML.sanitize('<p class="no-data">No allergy records found.</p>');
                return;
            }

            allergyList.innerHTML = CareflowHTML.sanitize(data.map(allergy => `
                <div class="record-card">
                    <div class="record-header">
                        <div class="record-title">${allergy.allergyName}</div>
                        <div class="record-actions">
                            <button class="btn btn-edit" data-cf-action="editAllergy(${allergy.allergyId})">Edit</button>
                            <button class="btn btn-danger" data-cf-action="deleteAllergy(${allergy.allergyId})">Delete</button>
                        </div>
                    </div>
                    <div class="record-details">
                        <div class="detail">
                            <span class="detail-label">Type:</span>
                            <span class="detail-value">${allergy.allergyType || 'N/A'}</span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Reaction:</span>
                            <span class="detail-value">${allergy.reaction || 'N/A'}</span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Severity:</span>
                            <span class="detail-value">
                                ${allergy.severity ? `<span class="severity-badge severity-${allergy.severity.toLowerCase()}">${allergy.severity}</span>` : 'N/A'}
                            </span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Notes:</span>
                            <span class="detail-value">${allergy.notes || 'N/A'}</span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Recorded At:</span>
                            <span class="detail-value">${allergy.recordedAt || 'N/A'}</span>
                        </div>
                    </div>
                </div>
            `).join(''));
        })
        .catch(error => console.error('Error loading allergies:', error));
}

// Load Chronic Conditions
function loadChronicConditions() {
    fetch(`/api/patients/${currentPatientId}/chronic-conditions`)
        .then(response => response.json())
        .then(data => {
            const conditionList = document.getElementById('conditionList');
            if (data.length === 0) {
                conditionList.innerHTML = CareflowHTML.sanitize('<p class="no-data">No chronic condition records found.</p>');
                return;
            }

            conditionList.innerHTML = CareflowHTML.sanitize(data.map(condition => `
                <div class="record-card">
                    <div class="record-header">
                        <div class="record-title">${condition.conditionName}</div>
                        <div class="record-actions">
                            <button class="btn btn-edit" data-cf-action="editCondition(${condition.conditionId})">Edit</button>
                            <button class="btn btn-danger" data-cf-action="deleteCondition(${condition.conditionId})">Delete</button>
                        </div>
                    </div>
                    <div class="record-details">
                        <div class="detail">
                            <span class="detail-label">Diagnosed Date:</span>
                            <span class="detail-value">${condition.diagnosedDate || 'N/A'}</span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Status:</span>
                            <span class="detail-value">
                                <span class="status-badge status-${condition.status.toLowerCase()}">${condition.status}</span>
                            </span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Notes:</span>
                            <span class="detail-value">${condition.notes || 'N/A'}</span>
                        </div>
                        <div class="detail">
                            <span class="detail-label">Recorded At:</span>
                            <span class="detail-value">${condition.recordedAt || 'N/A'}</span>
                        </div>
                    </div>
                </div>
            `).join(''));
        })
        .catch(error => console.error('Error loading chronic conditions:', error));
}

// History Modal Functions
function openHistoryModal() {
    currentEditingHistoryId = null;
    document.getElementById('historyForm').reset();
    document.getElementById('historyModalTitle').innerText = 'Add Medical History';
    document.getElementById('historyModal').style.display = 'block';
}

function closeHistoryModal() {
    document.getElementById('historyModal').style.display = 'none';
}

function editHistory(historyId) {
    currentEditingHistoryId = historyId;
    fetch(`/api/patients/${currentPatientId}/medical-history/${historyId}`)
        .then(response => response.json())
        .then(data => {
            document.getElementById('historyType').value = data.historyType;
            document.getElementById('description').value = data.description;
            document.getElementById('historyDate').value = data.historyDate;
            document.getElementById('historyNotes').value = data.notes;
            document.getElementById('historyModalTitle').innerText = 'Edit Medical History';
            document.getElementById('historyModal').style.display = 'block';
        })
        .catch(error => console.error('Error loading history:', error));
}

function saveHistory(e) {
    e.preventDefault();
    
    const historyData = {
        patientId: currentPatientId,
        historyType: document.getElementById('historyType').value,
        description: document.getElementById('description').value,
        historyDate: document.getElementById('historyDate').value,
        notes: document.getElementById('historyNotes').value
    };

    const url = currentEditingHistoryId 
        ? `/api/patients/${currentPatientId}/medical-history/${currentEditingHistoryId}`
        : `/api/patients/${currentPatientId}/medical-history`;
    
    const method = currentEditingHistoryId ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(historyData)
    })
    .then(async response => {
        if (response.ok) {
            closeHistoryModal();
            loadMedicalHistory();
            alert(currentEditingHistoryId ? 'History updated successfully!' : 'History added successfully!');
        }
    })
    .catch(error => console.error('Error saving history:', error));
}

function deleteHistory(historyId) {
    if (confirm('Are you sure you want to delete this medical history record?')) {
        fetch(`/api/patients/${currentPatientId}/medical-history/${historyId}`, {
            method: 'DELETE'
        })
        .then(async response => {
            if (response.ok) {
                loadMedicalHistory();
                alert('History deleted successfully!');
            }
        })
        .catch(error => console.error('Error deleting history:', error));
    }
}

// Allergy Modal Functions
function openAllergyModal() {
    currentEditingAllergyId = null;
    document.getElementById('allergyForm').reset();
    document.getElementById('allergyModalTitle').innerText = 'Add Allergy';
    document.getElementById('allergyModal').style.display = 'block';
}

function closeAllergyModal() {
    document.getElementById('allergyModal').style.display = 'none';
}

function editAllergy(allergyId) {
    currentEditingAllergyId = allergyId;
    fetch(`/api/patients/${currentPatientId}/allergies/${allergyId}`)
        .then(response => response.json())
        .then(data => {
            document.getElementById('allergyType').value = data.allergyType;
            document.getElementById('allergyName').value = data.allergyName;
            document.getElementById('reaction').value = data.reaction;
            document.getElementById('severity').value = data.severity;
            document.getElementById('allergyNotes').value = data.notes;
            document.getElementById('allergyModalTitle').innerText = 'Edit Allergy';
            document.getElementById('allergyModal').style.display = 'block';
        })
        .catch(error => console.error('Error loading allergy:', error));
}

function saveAllergy(e) {
    e.preventDefault();
    
    const allergyData = {
        patientId: currentPatientId,
        allergyType: document.getElementById('allergyType').value,
        allergyName: document.getElementById('allergyName').value,
        reaction: document.getElementById('reaction').value,
        severity: document.getElementById('severity').value,
        notes: document.getElementById('allergyNotes').value
    };

    const url = currentEditingAllergyId 
        ? `/api/patients/${currentPatientId}/allergies/${currentEditingAllergyId}`
        : `/api/patients/${currentPatientId}/allergies`;
    
    const method = currentEditingAllergyId ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(allergyData)
    })
    .then(async response => {
        if (response.ok) {
            closeAllergyModal();
            loadAllergies();
            alert(currentEditingAllergyId ? 'Allergy updated successfully!' : 'Allergy added successfully!');
        }
    })
    .catch(error => console.error('Error saving allergy:', error));
}

function deleteAllergy(allergyId) {
    if (confirm('Are you sure you want to delete this allergy record?')) {
        fetch(`/api/patients/${currentPatientId}/allergies/${allergyId}`, {
            method: 'DELETE'
        })
        .then(async response => {
            if (response.ok) {
                loadAllergies();
                alert('Allergy deleted successfully!');
            }
        })
        .catch(error => console.error('Error deleting allergy:', error));
    }
}

// Chronic Condition Modal Functions
function openConditionModal() {
    currentEditingConditionId = null;
    document.getElementById('conditionForm').reset();
    document.getElementById('conditionModalTitle').innerText = 'Add Chronic Condition';
    document.getElementById('conditionModal').style.display = 'block';
}

function closeConditionModal() {
    document.getElementById('conditionModal').style.display = 'none';
}

function editCondition(conditionId) {
    currentEditingConditionId = conditionId;
    fetch(`/api/patients/${currentPatientId}/chronic-conditions/${conditionId}`)
        .then(response => response.json())
        .then(data => {
            document.getElementById('conditionName').value = data.conditionName;
            document.getElementById('diagnosedDate').value = data.diagnosedDate;
            document.getElementById('status').value = data.status;
            document.getElementById('conditionNotes').value = data.notes;
            document.getElementById('conditionModalTitle').innerText = 'Edit Chronic Condition';
            document.getElementById('conditionModal').style.display = 'block';
        })
        .catch(error => console.error('Error loading condition:', error));
}

function saveCondition(e) {
    e.preventDefault();
    
    const conditionData = {
        patientId: currentPatientId,
        conditionName: document.getElementById('conditionName').value,
        diagnosedDate: document.getElementById('diagnosedDate').value,
        status: document.getElementById('status').value,
        notes: document.getElementById('conditionNotes').value
    };

    const url = currentEditingConditionId 
        ? `/api/patients/${currentPatientId}/chronic-conditions/${currentEditingConditionId}`
        : `/api/patients/${currentPatientId}/chronic-conditions`;
    
    const method = currentEditingConditionId ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(conditionData)
    })
    .then(async response => {
        if (response.ok) {
            closeConditionModal();
            loadChronicConditions();
            alert(currentEditingConditionId ? 'Condition updated successfully!' : 'Condition added successfully!');
        }
    })
    .catch(error => console.error('Error saving condition:', error));
}

function deleteCondition(conditionId) {
    if (confirm('Are you sure you want to delete this chronic condition record?')) {
        fetch(`/api/patients/${currentPatientId}/chronic-conditions/${conditionId}`, {
            method: 'DELETE'
        })
        .then(async response => {
            if (response.ok) {
                loadChronicConditions();
                alert('Condition deleted successfully!');
            }
        })
        .catch(error => console.error('Error deleting condition:', error));
    }
}

// Close modals when clicking outside
window.onclick = function(event) {
    const historyModal = document.getElementById('historyModal');
    const allergyModal = document.getElementById('allergyModal');
    const conditionModal = document.getElementById('conditionModal');

    if (event.target == historyModal) {
        historyModal.style.display = 'none';
    }
    if (event.target == allergyModal) {
        allergyModal.style.display = 'none';
    }
    if (event.target == conditionModal) {
        conditionModal.style.display = 'none';
    }
}


function loadPrescriptionHistory() {
    fetch(`/api/patients/${currentPatientId}/prescriptions`)
        .then(response => response.json())
        .then(async data => {
            const historyList = document.getElementById('prescriptionHistoryList');
            if (!historyList) return;

            if (!Array.isArray(data) || data.length == 0) {
                historyList.innerHTML = CareflowHTML.sanitize('<p class="no-data">No prescriptions found.</p>');
                return;
            }

            let staffMap = {};
            let batchMap = {};
            let medicineMap = {};

            try {
                const [staff, batches, medicines] = await Promise.all([
                    fetch('/api/staff').then(r => r.json()),
                    fetch('/api/pharmacy/batches').then(r => r.json()),
                    fetch('/api/pharmacy/medicines').then(r => r.json())
                ]);

                staff.forEach(member => {
                    staffMap[member.id] = member.name;
                });

                medicines.forEach(med => {
                    medicineMap[med.id] = med.name;
                });

                batches.forEach(batch => {
                    batchMap[batch.id] = batch.medicine && batch.medicine.id
                        ? medicineMap[batch.medicine.id]
                        : (batch.medicine ? batch.medicine.name : null);
                });
            } catch (error) {
                console.warn('Error loading pharmacy references:', error);
            }

            historyList.innerHTML = CareflowHTML.sanitize(data.map(entry => {
                const date = entry.date ? new Date(entry.date).toLocaleString() : 'N/A';
                const doctorName = staffMap[entry.doctorId] || `Staff #${entry.doctorId || 'N/A'}`;
                const status = entry.status || 'CREATED';
                const items = Array.isArray(entry.medicines) ? entry.medicines : [];

                const medicineLines = items.map(item => {
                    const batchId = item.medicineBatch ? item.medicineBatch.id : null;
                    const medicineName = (item.medicineBatch && item.medicineBatch.medicine && item.medicineBatch.medicine.name)
                        ? item.medicineBatch.medicine.name
                        : (batchId ? batchMap[batchId] : null);
                    const displayName = medicineName || 'Unknown medicine';
                    const qty = item.quantity || 0;
                    const instructions = item.instructions ? ` - ${item.instructions}` : '';
                    return `<li>${displayName} (${qty})${instructions}</li>`;
                }).join('');

                return `
                    <div class="record-card prescription-card">
                        <div class="record-header">
                            <div class="record-title">${date}</div>
                            <div class="record-actions">
                                <span class="status-badge status-active">${status}</span>
                            </div>
                        </div>
                        <div class="record-details">
                            <div class="detail">
                                <span class="detail-label">Doctor:</span>
                                <span class="detail-value">${doctorName}</span>
                            </div>
                            <div class="detail">
                                <span class="detail-label">Prescription ID:</span>
                                <span class="detail-value">PR${String(entry.prescriptionId || '').padStart(3, '0')}</span>
                            </div>
                        </div>
                        <div class="prescription-meds">
                            <h4>Medicines</h4>
                            <ul>${medicineLines || '<li>No items</li>'}</ul>
                        </div>
                    </div>
                `;
            }).join(''));
        })
        .catch(error => console.error('Error loading prescription history:', error));
}

function printPrescriptionHistory() {
    window.print();
}
