-- Create Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_totp_mfa (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    encrypted_secret TEXT NOT NULL,
    recovery_codes TEXT,
    last_used_counter BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Patients Table
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255) NOT NULL,
    date_of_birth VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    medical_history VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- Create Staff Table
CREATE TABLE IF NOT EXISTS staff (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    position VARCHAR(255) NOT NULL,
    department VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255) NOT NULL,
    specialization VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    joining_date VARCHAR(255)
);

-- Create Appointments Table
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    appointment_date VARCHAR(255) NOT NULL,
    appointment_time VARCHAR(255) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
);

-- Create Patient Allergies Table
CREATE TABLE IF NOT EXISTS patient_allergies (
    allergy_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    allergy_type VARCHAR(255) NOT NULL,
    allergy_name VARCHAR(255) NOT NULL,
    reaction VARCHAR(255),
    severity VARCHAR(255),
    notes VARCHAR(255),
    recorded_at VARCHAR(255),
    CONSTRAINT fk_allergies_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

-- Create Patient Chronic Conditions Table
CREATE TABLE IF NOT EXISTS patient_chronic_conditions (
    condition_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    condition_name VARCHAR(255) NOT NULL,
    diagnosed_date VARCHAR(255),
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(255),
    recorded_at VARCHAR(255),
    CONSTRAINT fk_chronic_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

-- Create Patient Medical History Table
CREATE TABLE IF NOT EXISTS patient_medical_history (
    history_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    history_type VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    history_date VARCHAR(255),
    notes VARCHAR(255),
    recorded_at VARCHAR(255),
    CONSTRAINT fk_history_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

-- Create Indexes for Patient Medical Tables
CREATE INDEX IF NOT EXISTS idx_allergies_patient ON patient_allergies(patient_id);
CREATE INDEX IF NOT EXISTS idx_chronic_patient ON patient_chronic_conditions(patient_id);
CREATE INDEX IF NOT EXISTS idx_history_patient ON patient_medical_history(patient_id);

-- ================= PHARMACY & PRESCRIPTION MODULE TABLES =================

-- Medicines master table
CREATE TABLE IF NOT EXISTS medicines (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500)
);

-- Medicine batches (for batch/expiry/stock tracking)
CREATE TABLE IF NOT EXISTS medicine_batches (
    id BIGSERIAL PRIMARY KEY,
    medicine_id BIGINT NOT NULL,
    batch_no VARCHAR(100) NOT NULL,
    expiry_date DATE NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT fk_batches_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);

-- Stock transactions (in/out for inventory management)
CREATE TABLE IF NOT EXISTS stock_transactions (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- IN/OUT
    quantity INT NOT NULL,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reference VARCHAR(255),
    CONSTRAINT fk_stock_batch FOREIGN KEY (batch_id) REFERENCES medicine_batches(id) ON DELETE CASCADE
);

-- Prescriptions (linked to visits/appointments)
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'CREATED',
    CONSTRAINT fk_presc_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_presc_doctor FOREIGN KEY (doctor_id) REFERENCES staff(id) ON DELETE CASCADE
);

-- Prescription items (medicines prescribed)
CREATE TABLE IF NOT EXISTS prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    medicine_batch_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    instructions VARCHAR(255),
    CONSTRAINT fk_item_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_batch FOREIGN KEY (medicine_batch_id) REFERENCES medicine_batches(id) ON DELETE CASCADE
);

-- Dispensed items (track dispensing by pharmacy)
CREATE TABLE IF NOT EXISTS dispensed_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_item_id BIGINT NOT NULL,
    dispensed_by BIGINT NOT NULL, -- staff id (pharmacist)
    dispensed_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    quantity INT NOT NULL,
    CONSTRAINT fk_dispense_item FOREIGN KEY (prescription_item_id) REFERENCES prescription_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_dispense_staff FOREIGN KEY (dispensed_by) REFERENCES staff(id) ON DELETE CASCADE
);

-- Indexes for pharmacy tables
CREATE INDEX IF NOT EXISTS idx_batches_medicine ON medicine_batches(medicine_id);
CREATE INDEX IF NOT EXISTS idx_stock_batch ON stock_transactions(batch_id);
CREATE INDEX IF NOT EXISTS idx_presc_appointment ON prescriptions(appointment_id);
CREATE INDEX IF NOT EXISTS idx_presc_doctor ON prescriptions(doctor_id);
CREATE INDEX IF NOT EXISTS idx_item_prescription ON prescription_items(prescription_id);
CREATE INDEX IF NOT EXISTS idx_item_batch ON prescription_items(medicine_batch_id);
CREATE INDEX IF NOT EXISTS idx_dispense_item ON dispensed_items(prescription_item_id);
CREATE INDEX IF NOT EXISTS idx_dispense_staff ON dispensed_items(dispensed_by);

-- ================= BILLING & INVOICES MODULE TABLES =================

-- Invoices master table
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PAID/PENDING/PARTIAL
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0,
    balance_due DECIMAL(10,2) NOT NULL DEFAULT 0,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date DATE,
    email_sent_at TIMESTAMP,
    notes VARCHAR(500),
    CONSTRAINT fk_invoices_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoices_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
);

-- Invoice line items (consultation/pharmacy/lab)
CREATE TABLE IF NOT EXISTS invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    item_type VARCHAR(50) NOT NULL, -- CONSULTATION/PHARMACY/LAB/OTHER
    description VARCHAR(255) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- Payments captured against invoices (supports partial payments)
CREATE TABLE IF NOT EXISTS invoice_payments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50),
    payment_status VARCHAR(20) DEFAULT 'PAID',
    paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gateway_order_id VARCHAR(255),
    gateway_payment_id VARCHAR(255),
    gateway_signature VARCHAR(255),
    gateway_status VARCHAR(50) DEFAULT 'PENDING',
    reference VARCHAR(255),
    CONSTRAINT fk_invoice_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT uq_invoice_payment_gateway_order UNIQUE (gateway_order_id),
    CONSTRAINT uq_invoice_payment_gateway_payment UNIQUE (gateway_payment_id)
);

ALTER TABLE invoice_payments ADD COLUMN IF NOT EXISTS gateway_order_id VARCHAR(255);
ALTER TABLE invoice_payments ADD COLUMN IF NOT EXISTS gateway_payment_id VARCHAR(255);
ALTER TABLE invoice_payments ADD COLUMN IF NOT EXISTS gateway_signature VARCHAR(255);
ALTER TABLE invoice_payments ADD COLUMN IF NOT EXISTS gateway_status VARCHAR(50) DEFAULT 'PENDING';

CREATE UNIQUE INDEX IF NOT EXISTS idx_invoice_payments_gateway_order ON invoice_payments(gateway_order_id) WHERE gateway_order_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_invoice_payments_gateway_payment ON invoice_payments(gateway_payment_id) WHERE gateway_payment_id IS NOT NULL;

-- Stored invoice documents (PDFs)
CREATE TABLE IF NOT EXISTS invoice_documents (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_docs_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- Indexes for billing tables
CREATE INDEX IF NOT EXISTS idx_invoices_patient ON invoices(patient_id);
CREATE INDEX IF NOT EXISTS idx_invoices_appointment ON invoices(appointment_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_issued_at ON invoices(issued_at);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_reference ON invoice_items(reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_invoice_payments_invoice ON invoice_payments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_docs_invoice ON invoice_documents(invoice_id);

-- ================= CONSULTATION / VISITS =================

CREATE TABLE IF NOT EXISTS visits (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    symptoms VARCHAR(1000),
    diagnosis VARCHAR(1000),
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN/LAB_REQUIRED/LAB_DONE/READY_TO_DISPENSE/CLOSED
    priority VARCHAR(20) DEFAULT 'NORMAL', -- NORMAL/URGENT
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visits_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_visits_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_visits_doctor FOREIGN KEY (doctor_id) REFERENCES staff(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_visits_appointment ON visits(appointment_id);
CREATE INDEX IF NOT EXISTS idx_visits_patient ON visits(patient_id);
CREATE INDEX IF NOT EXISTS idx_visits_doctor ON visits(doctor_id);
CREATE INDEX IF NOT EXISTS idx_visits_status ON visits(status);

-- ================= LAB MASTER + ORDERS =================

CREATE TABLE IF NOT EXISTS lab_tests_master (
    id BIGSERIAL PRIMARY KEY,
    test_name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL DEFAULT 0,
    normal_range_text VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS lab_orders (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    ordered_by_doctor_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING/IN_PROGRESS/COMPLETED
    ordered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),
    CONSTRAINT fk_lab_orders_visit FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_orders_doctor FOREIGN KEY (ordered_by_doctor_id) REFERENCES staff(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lab_order_items (
    id BIGSERIAL PRIMARY KEY,
    lab_order_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING/COMPLETED
    result_value VARCHAR(100),
    result_unit VARCHAR(50),
    reference_range VARCHAR(100),
    result_flag VARCHAR(20),
    result_notes VARCHAR(1000),
    CONSTRAINT fk_lab_items_order FOREIGN KEY (lab_order_id) REFERENCES lab_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_items_test FOREIGN KEY (test_id) REFERENCES lab_tests_master(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lab_reports (
    id BIGSERIAL PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    lab_order_id BIGINT,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lab_reports_visit FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_reports_order FOREIGN KEY (lab_order_id) REFERENCES lab_orders(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_lab_orders_visit ON lab_orders(visit_id);
CREATE INDEX IF NOT EXISTS idx_lab_orders_status ON lab_orders(status);
CREATE INDEX IF NOT EXISTS idx_lab_items_order ON lab_order_items(lab_order_id);
CREATE INDEX IF NOT EXISTS idx_lab_items_test ON lab_order_items(test_id);
CREATE INDEX IF NOT EXISTS idx_lab_reports_visit ON lab_reports(visit_id);

-- ================= DISPENSE (BILLING-READY) =================

CREATE TABLE IF NOT EXISTS dispense (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    dispensed_by BIGINT NOT NULL, -- staff id (pharmacist)
    dispensed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_dispense_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
    CONSTRAINT fk_dispense_staff FOREIGN KEY (dispensed_by) REFERENCES staff(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dispense_items (
    id BIGSERIAL PRIMARY KEY,
    dispense_id BIGINT NOT NULL,
    medicine_batch_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_dispense_items_dispense FOREIGN KEY (dispense_id) REFERENCES dispense(id) ON DELETE CASCADE,
    CONSTRAINT fk_dispense_items_batch FOREIGN KEY (medicine_batch_id) REFERENCES medicine_batches(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dispense_prescription ON dispense(prescription_id);
CREATE INDEX IF NOT EXISTS idx_dispense_items_dispense ON dispense_items(dispense_id);

-- ================= OPTIONAL: LINK INVOICES TO VISITS =================

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS visit_id BIGINT;

ALTER TABLE invoices
    ADD CONSTRAINT fk_invoices_visit
    FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_invoices_visit ON invoices(visit_id);

-- ================= STATUS FLOW: DISPENSE -> VISIT CLOSED =================

CREATE OR REPLACE FUNCTION update_visit_status_on_dispense()
RETURNS TRIGGER AS $$
BEGIN
    -- Close visit tied to the prescription's appointment
    UPDATE visits v
    SET status = 'CLOSED',
        updated_at = CURRENT_TIMESTAMP
    FROM prescriptions p
    WHERE p.id = NEW.prescription_id
      AND v.appointment_id = p.appointment_id
      AND v.status <> 'CLOSED';

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_visit_status_on_dispense ON dispense;

CREATE TRIGGER trg_update_visit_status_on_dispense
AFTER INSERT ON dispense
FOR EACH ROW
EXECUTE FUNCTION update_visit_status_on_dispense();

-- ================= IPD / BEDS =================

CREATE TABLE IF NOT EXISTS beds (
    id BIGSERIAL PRIMARY KEY,
    bed_number VARCHAR(50) NOT NULL,
    ward VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- GENERAL/ICU/PRIVATE
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE/OCCUPIED/MAINTENANCE
    daily_charge DECIMAL(10,2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS admissions (
    id BIGSERIAL PRIMARY KEY,
    bed_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    visit_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    admitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    discharged_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/DISCHARGED
    notes VARCHAR(1000),
    CONSTRAINT fk_admissions_bed FOREIGN KEY (bed_id) REFERENCES beds(id) ON DELETE CASCADE,
    CONSTRAINT fk_admissions_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_admissions_visit FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE CASCADE,
    CONSTRAINT fk_admissions_doctor FOREIGN KEY (doctor_id) REFERENCES staff(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_beds_status ON beds(status);
CREATE INDEX IF NOT EXISTS idx_beds_type ON beds(type);
CREATE INDEX IF NOT EXISTS idx_admissions_bed ON admissions(bed_id);
CREATE INDEX IF NOT EXISTS idx_admissions_patient ON admissions(patient_id);
CREATE INDEX IF NOT EXISTS idx_admissions_visit ON admissions(visit_id);
CREATE INDEX IF NOT EXISTS idx_admissions_status ON admissions(status);

-- ================= STATUS FLOW: LAB ITEMS -> ORDER IN_PROGRESS =================

CREATE OR REPLACE FUNCTION update_lab_order_status_on_item_progress()
RETURNS TRIGGER AS $$
BEGIN
    -- If any item is IN_PROGRESS, reflect it on the order
    IF NEW.status = 'IN_PROGRESS' THEN
        UPDATE lab_orders
        SET status = 'IN_PROGRESS'
        WHERE id = NEW.lab_order_id
          AND status <> 'COMPLETED';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_lab_order_status_on_item_progress ON lab_order_items;

CREATE TRIGGER trg_update_lab_order_status_on_item_progress
AFTER INSERT OR UPDATE ON lab_order_items
FOR EACH ROW
EXECUTE FUNCTION update_lab_order_status_on_item_progress();

-- ================= NOTIFICATIONS =================

CREATE TABLE IF NOT EXISTS notification_settings (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(50) NOT NULL, -- Doctor/Lab/Billing/Receptionist
    event_type VARCHAR(50) NOT NULL, -- APPOINTMENT_REMINDER/LAB_REPORT_READY/BILLING_REMINDER
    channel VARCHAR(20) NOT NULL, -- EMAIL/SMS
    enabled BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS notification_queue (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(255),
    subject VARCHAR(255),
    body TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING/SENT/FAILED
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    error_message VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_notification_settings_role ON notification_settings(role);
CREATE INDEX IF NOT EXISTS idx_notification_settings_event ON notification_settings(event_type);
CREATE INDEX IF NOT EXISTS idx_notification_queue_status ON notification_queue(status);
