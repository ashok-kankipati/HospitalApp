-- Generated: 2026-01-26 09:47:39
-- Insert sample users into the users table
-- NOTE: If you want to reset all data, uncomment the DELETE statements below:
-- DELETE FROM patient_medical_history WHERE 1=1;
-- DELETE FROM patient_chronic_conditions WHERE 1=1;
-- DELETE FROM patient_allergies WHERE 1=1;
-- DELETE FROM appointments WHERE 1=1;
-- DELETE FROM staff WHERE 1=1;
-- DELETE FROM patients WHERE 1=1;
-- DELETE FROM users WHERE 1=1;

-- Insert users only if they don't exist
INSERT INTO users (username, email, password, role, is_active, created_at) 
SELECT * FROM (VALUES
('admin', 'admin@hospital.com', 'admin123', 'Admin', true, NOW()),
('dr_smith', 'smith@hospital.com', 'doctor123', 'Doctor', true, NOW()),
('dr_johnson', 'johnson@hospital.com', 'doctor456', 'Doctor', true, NOW()),
('nurse_sarah', 'sarah@hospital.com', 'nurse123', 'Nurse', true, NOW()),
('nurse_mike', 'mike@hospital.com', 'nurse456', 'Nurse', true, NOW()),
('patient_john', 'john.patient@hospital.com', 'patient123', 'Patient', true, NOW()),
('patient_jane', 'jane.patient@hospital.com', 'patient456', 'Patient', true, NOW()),
('patient_david', 'david.patient@hospital.com', 'patient789', 'Patient', true, NOW()),
('receptionist_emma', 'emma@hospital.com', 'receptionist123', 'Receptionist', true, NOW()),
('lab_technician', 'lab@hospital.com', 'lab123', 'Lab Technician', true, NOW())
) AS t(username, email, password, role, is_active, created_at)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = t.username);

-- Insert sample patients into the patients table (only if not already present)
INSERT INTO patients (name, email, phone, date_of_birth, address, medical_history)
SELECT * FROM (VALUES
('John Doe', 'john.doe@email.com', '+1-800-123-4567', '1985-05-15', '123 Main Street, New York, NY 10001', 'Hypertension, Diabetes Type 2'),
('Jane Smith', 'jane.smith@email.com', '+1-800-234-5678', '1990-08-22', '456 Oak Avenue, Los Angeles, CA 90001', 'Asthma, Allergies'),
('David Johnson', 'david.johnson@email.com', '+1-800-345-6789', '1988-12-03', '789 Pine Road, Chicago, IL 60601', 'Heart condition, Cholesterol'),
('Sarah Williams', 'sarah.williams@email.com', '+1-800-456-7890', '1992-03-18', '321 Elm Street, Houston, TX 77001', 'Migraine, Anxiety'),
('Michael Brown', 'michael.brown@email.com', '+1-800-567-8901', '1987-07-25', '654 Maple Lane, Phoenix, AZ 85001', 'Arthritis, Back pain'),
('Emily Davis', 'emily.davis@email.com', '+1-800-678-9012', '1995-01-30', '987 Cedar Court, Philadelphia, PA 19101', 'No chronic conditions'),
('Robert Wilson', 'robert.wilson@email.com', '+1-800-789-0123', '1980-11-12', '147 Birch Boulevard, San Antonio, TX 78201', 'Hypertension, Sleep apnea'),
('Linda Martinez', 'linda.martinez@email.com', '+1-800-890-1234', '1993-06-08', '258 Spruce Lane, San Diego, CA 92101', 'Thyroid disorder'),
('James Taylor', 'james.taylor@email.com', '+1-800-901-2345', '1986-09-20', '369 Walnut Street, Dallas, TX 75201', 'Diabetes, Kidney disease'),
('Patricia Anderson', 'patricia.anderson@email.com', '+1-800-012-3456', '1991-04-14', '741 Chestnut Road, San Jose, CA 95101', 'Osteoporosis, Vitamin D deficiency')
) AS t(name, email, phone, date_of_birth, address, medical_history)
WHERE NOT EXISTS (SELECT 1 FROM patients WHERE email = t.email);

-- Insert sample staff into the staff table (only if not already present)
INSERT INTO staff (name, position, department, email, phone, specialization, is_active, joining_date)
SELECT * FROM (VALUES
('Dr. Michael Smith', 'Doctor', 'Cardiology', 'dr.smith@hospital.com', '+1-800-111-2222', 'Cardiology', true, '2020-01-15'),
('Dr. Jennifer Johnson', 'Doctor', 'Neurology', 'dr.johnson@hospital.com', '+1-800-222-3333', 'Neurology', true, '2019-06-20'),
('Dr. Robert Wilson', 'Doctor', 'Orthopedics', 'dr.wilson@hospital.com', '+1-800-333-4444', 'Orthopedic Surgery', true, '2021-03-10'),
('Sarah Brown', 'Nurse', 'Cardiology', 'nurse.sarah@hospital.com', '+1-800-444-5555', 'Cardiac Nursing', true, '2022-02-01'),
('Mike Davis', 'Nurse', 'Emergency', 'nurse.mike@hospital.com', '+1-800-555-6666', 'Emergency Nursing', true, '2021-09-15'),
('Emma Wilson', 'Receptionist', 'Administration', 'emma.wilson@hospital.com', '+1-800-666-7777', 'Patient Services', true, '2022-05-20'),
('Tom Anderson', 'Lab Technician', 'Laboratory', 'tom.anderson@hospital.com', '+1-800-777-8888', 'Clinical Lab Science', true, '2020-11-10'),
('Lisa Martinez', 'Pharmacist', 'Pharmacy', 'lisa.martinez@hospital.com', '+1-800-888-9999', 'Pharmacy', true, '2021-04-05'),
('James Taylor', 'Radiologist', 'Radiology', 'james.taylor@hospital.com', '+1-800-999-0000', 'Medical Imaging', true, '2019-08-12'),
('Patricia Chen', 'Surgeon', 'Surgery', 'patricia.chen@hospital.com', '+1-800-000-1111', 'General Surgery', true, '2020-07-22')
) AS t(name, position, department, email, phone, specialization, is_active, joining_date)
WHERE NOT EXISTS (SELECT 1 FROM staff WHERE email = t.email);
-- Insert sample appointments into the appointments table (only if not already present)
INSERT INTO appointments (patient_id, staff_id, appointment_date, appointment_time, reason, status, notes)
SELECT * FROM (VALUES
(1, 1, '2026-02-10', '10:00 AM', 'Routine cardiac checkup', 'CONFIRMED', 'Patient stable, follow-up in 3 months'),
(2, 2, '2026-02-11', '02:30 PM', 'Migraine consultation', 'PENDING', 'Discuss new treatment options'),
(3, 3, '2026-02-12', '11:00 AM', 'Knee pain follow-up', 'CONFIRMED', 'Physical therapy continues'),
(4, 4, '2026-02-13', '03:00 PM', 'Post-surgery check', 'CONFIRMED', 'Healing progressing well'),
(5, 5, '2026-02-14', '09:30 AM', 'Emergency consultation', 'PENDING', 'Urgent pain management needed'),
(6, 1, '2026-02-15', '01:00 PM', 'Annual physical exam', 'PENDING', 'Complete health screening'),
(7, 2, '2026-02-16', '04:00 PM', 'Neurological assessment', 'CONFIRMED', 'Monitor sleep patterns'),
(8, 3, '2026-02-17', '10:30 AM', 'Back pain evaluation', 'PENDING', 'Consider imaging studies'),
(9, 4, '2026-02-18', '02:00 PM', 'Diabetes management', 'CONFIRMED', 'Adjust medication dosage'),
(10, 5, '2026-02-19', '11:30 AM', 'Lab work review', 'PENDING', 'Discuss test results')
) AS t(patient_id, staff_id, appointment_date, appointment_time, reason, status, notes)
WHERE NOT EXISTS (SELECT 1 FROM appointments WHERE patient_id = t.patient_id AND appointment_date = t.appointment_date AND appointment_time = t.appointment_time);
-- Insert test data for patient allergies (only if not already present)
INSERT INTO patient_allergies (patient_id, allergy_type, allergy_name, reaction, severity, notes, recorded_at)
SELECT p.id, t.allergy_type, t.allergy_name, t.reaction, t.severity, t.notes, t.recorded_at
FROM (
  VALUES
  ('john.doe@email.com', 'Medicine', 'Aspirin', 'Stomach upset, nausea', 'Moderate', 'Avoid all aspirin-based medications', '2025-11-15 10:30:00'),
  ('john.doe@email.com', 'Food', 'Shellfish', 'Severe itching and swelling', 'Severe', 'Anaphylaxis risk - carry epinephrine auto-injector', '2025-10-20 14:45:00'),
  ('jane.smith@email.com', 'Environmental', 'Pollen', 'Sneezing, nasal congestion', 'Mild', 'Seasonal allergy, worse in spring', '2025-12-01 09:15:00'),
  ('jane.smith@email.com', 'Food', 'Nuts', 'Throat swelling', 'Severe', 'Complete avoidance required', '2025-11-10 11:20:00'),
  ('david.johnson@email.com', 'Medicine', 'Penicillin', 'Rash, fever', 'Severe', 'Documented drug allergy - use alternatives', '2025-09-05 16:00:00'),
  ('sarah.williams@email.com', 'Food', 'Dairy', 'Lactose intolerance symptoms', 'Moderate', 'Can tolerate small amounts of aged cheese', '2025-12-10 13:30:00'),
  ('michael.brown@email.com', 'Environmental', 'Dust mites', 'Allergic rhinitis', 'Mild', 'Use air purifier at home', '2025-11-22 08:45:00'),
  ('emily.davis@email.com', 'Medicine', 'Sulfonamides', 'Rash', 'Mild', 'Watch for early signs', '2025-10-15 10:00:00'),
  ('robert.wilson@email.com', 'Food', 'Soy', 'Digestive issues', 'Moderate', 'Read labels carefully', '2025-12-03 15:20:00'),
  ('linda.martinez@email.com', 'Environmental', 'Latex', 'Contact dermatitis', 'Mild', 'Use non-latex gloves during procedures', '2025-11-28 11:50:00')
) AS t(patient_email, allergy_type, allergy_name, reaction, severity, notes, recorded_at)
JOIN patients p ON p.email = t.patient_email
WHERE NOT EXISTS (SELECT 1 FROM patient_allergies pa WHERE pa.patient_id = p.id AND pa.allergy_name = t.allergy_name);

-- Insert test data for chronic conditions (only if not already present)
INSERT INTO patient_chronic_conditions (patient_id, condition_name, diagnosed_date, status, notes, recorded_at)
SELECT p.id, t.condition_name, t.diagnosed_date, t.status, t.notes, t.recorded_at
FROM (
  VALUES
  ('john.doe@email.com', 'Type 2 Diabetes', '2018-03-15', 'ACTIVE', 'Well-controlled with metformin and lifestyle changes. HbA1c 6.8%', '2025-12-10 09:00:00'),
  ('john.doe@email.com', 'Hypertension', '2015-06-20', 'ACTIVE', 'Controlled with lisinopril 10mg daily. Last BP reading 128/82', '2025-12-10 09:10:00'),
  ('jane.smith@email.com', 'Asthma', '2010-01-10', 'ACTIVE', 'Mild persistent asthma. Uses inhaler as needed', '2025-12-08 14:30:00'),
  ('david.johnson@email.com', 'Coronary Artery Disease', '2019-11-25', 'ACTIVE', 'Previous MI in 2019. On dual antiplatelet therapy and statin', '2025-12-05 10:15:00'),
  ('david.johnson@email.com', 'Hyperlipidemia', '2018-07-30', 'ACTIVE', 'Triglycerides slightly elevated. LDL controlled on atorvastatin 40mg', '2025-12-05 10:20:00'),
  ('sarah.williams@email.com', 'Anxiety Disorder', '2012-05-14', 'ACTIVE', 'Generalized anxiety. On sertraline 100mg daily with therapy', '2025-12-02 13:45:00'),
  ('michael.brown@email.com', 'Osteoarthritis', '2020-02-18', 'ACTIVE', 'Bilateral knee involvement. Physical therapy ongoing', '2025-11-30 11:00:00'),
  ('emily.davis@email.com', 'Thyroid Disorder', '2016-09-22', 'ACTIVE', 'Hypothyroidism on levothyroxine 50mcg. TSH normal', '2025-12-01 08:30:00'),
  ('robert.wilson@email.com', 'Sleep Apnea', '2017-12-03', 'ACTIVE', 'Obstructive sleep apnea. Uses CPAP machine nightly', '2025-11-25 16:00:00'),
  ('linda.martinez@email.com', 'Osteoporosis', '2015-04-11', 'ACTIVE', 'Postmenopausal. On alendronate and calcium supplements', '2025-12-04 10:45:00')
) AS t(patient_email, condition_name, diagnosed_date, status, notes, recorded_at)
JOIN patients p ON p.email = t.patient_email
WHERE NOT EXISTS (SELECT 1 FROM patient_chronic_conditions pcc WHERE pcc.patient_id = p.id AND pcc.condition_name = t.condition_name);

-- Insert test data for medical history (only if not already present)
INSERT INTO patient_medical_history (patient_id, history_type, description, history_date, notes, recorded_at)
SELECT p.id, t.history_type, t.description, t.history_date, t.notes, t.recorded_at
FROM (
  VALUES
  ('john.doe@email.com', 'Hospitalization', 'Acute coronary syndrome with successful stent placement', '2022-08-15', 'Discharged after 5-day stay. Recovered well post-procedure', '2025-11-20 09:30:00'),
  ('john.doe@email.com', 'Surgery', 'Laparoscopic cholecystectomy for gallstones', '2019-03-22', 'Uncomplicated procedure, recovery normal', '2025-11-20 09:40:00'),
  ('jane.smith@email.com', 'Medication Event', 'Adverse reaction to antibiotic clarithromycin', '2024-05-10', 'Severe headache and dizziness, switched to alternative antibiotic', '2025-11-22 14:15:00'),
  ('david.johnson@email.com', 'Fracture', 'Right tibia fracture from motor vehicle accident', '2020-07-18', 'Required open reduction and internal fixation with plate and screws', '2025-11-18 10:50:00'),
  ('david.johnson@email.com', 'Hospitalization', 'Acute MI with emergency cardiac catheterization', '2019-10-22', '3 vessel disease, multiple stents placed', '2025-11-18 11:00:00'),
  ('sarah.williams@email.com', 'Vaccination', 'COVID-19 booster dose', '2025-10-15', 'No adverse reactions reported', '2025-11-15 08:20:00'),
  ('michael.brown@email.com', 'Physical Therapy', 'Started intensive knee rehabilitation program', '2021-03-15', 'Attending 2x weekly sessions for 8 weeks', '2025-11-25 13:45:00'),
  ('emily.davis@email.com', 'Diagnostic Test', 'Thyroid ultrasound and TSH level monitoring', '2024-01-20', 'Nodule found, benign on FNA. Annual follow-up recommended', '2025-11-28 11:30:00'),
  ('robert.wilson@email.com', 'Sleep Study', 'Polysomnography confirmed moderate obstructive sleep apnea', '2018-06-10', 'AHI score 22 events/hour. CPAP initiated', '2025-11-20 15:00:00'),
  ('linda.martinez@email.com', 'Laboratory Test', 'Bone density scan (DEXA scan) T-score -2.8', '2022-09-12', 'Osteoporosis confirmed. Bisphosphonate therapy started', '2025-12-01 09:15:00')
) AS t(patient_email, history_type, description, history_date, notes, recorded_at)
JOIN patients p ON p.email = t.patient_email
WHERE NOT EXISTS (SELECT 1 FROM patient_medical_history pmh WHERE pmh.patient_id = p.id AND pmh.history_type = t.history_type AND pmh.description = t.description);


-- Pharmacy sample data (medicines, batches, prescriptions, dispensing)
INSERT INTO medicines (name, description)
SELECT * FROM (VALUES
('Paracetamol 500mg', 'Pain reliever and fever reducer'),
('Amoxicillin 500mg', 'Antibiotic for bacterial infections'),
('Ibuprofen 400mg', 'NSAID pain relief'),
('Cetirizine 10mg', 'Antihistamine for allergies'),
('Metformin 500mg', 'Type 2 diabetes management')
) AS t(name, description)
WHERE NOT EXISTS (SELECT 1 FROM medicines m WHERE m.name = t.name);

INSERT INTO medicine_batches (medicine_id, batch_no, expiry_date, quantity, price, is_active)
SELECT m.id, t.batch_no, t.expiry_date, t.quantity, t.price, t.is_active
FROM (
  VALUES
  ('Paracetamol 500mg', 'PCM-2026-A1', DATE '2027-06-30', 150, 2.50, true),
  ('Paracetamol 500mg', 'PCM-2026-B2', DATE '2027-12-31', 100, 2.75, true),
  ('Amoxicillin 500mg', 'AMX-2026-A1', DATE '2026-11-30', 80, 8.25, true),
  ('Ibuprofen 400mg', 'IBU-2026-A1', DATE '2027-03-31', 120, 5.10, true),
  ('Cetirizine 10mg', 'CTZ-2026-A1', DATE '2027-01-31', 90, 4.40, true),
  ('Metformin 500mg', 'MET-2026-A1', DATE '2027-08-31', 110, 6.95, true)
) AS t(medicine_name, batch_no, expiry_date, quantity, price, is_active)
JOIN medicines m ON m.name = t.medicine_name
WHERE NOT EXISTS (
  SELECT 1 FROM medicine_batches b
  WHERE b.batch_no = t.batch_no
);

-- Sample prescriptions linked to existing appointments
INSERT INTO prescriptions (appointment_id, doctor_id, date, status)
SELECT t.appointment_id, t.doctor_id, t.date, t.status
FROM (
  VALUES
  (1, 1, NOW(), 'CREATED'),
  (2, 2, NOW(), 'CREATED'),
  (3, 3, NOW(), 'CREATED')
) AS t(appointment_id, doctor_id, date, status)
WHERE NOT EXISTS (
  SELECT 1 FROM prescriptions p
  WHERE p.appointment_id = t.appointment_id
);

-- Prescription items (map to batches)
INSERT INTO prescription_items (prescription_id, medicine_batch_id, quantity, instructions)
SELECT p.id, b.id, t.quantity, t.instructions
FROM (
  VALUES
  (1, 'Paracetamol 500mg', 'PCM-2026-A1', 10, '1 tablet every 6 hours after meals'),
  (1, 'Cetirizine 10mg', 'CTZ-2026-A1', 5, '1 tablet at night'),
  (2, 'Amoxicillin 500mg', 'AMX-2026-A1', 14, '1 capsule every 8 hours'),
  (3, 'Ibuprofen 400mg', 'IBU-2026-A1', 12, '1 tablet after meals as needed'),
  (3, 'Metformin 500mg', 'MET-2026-A1', 30, '1 tablet twice daily')
) AS t(appointment_id, medicine_name, batch_no, quantity, instructions)
JOIN prescriptions p ON p.appointment_id = t.appointment_id
JOIN medicines m ON m.name = t.medicine_name
JOIN medicine_batches b ON b.medicine_id = m.id AND b.batch_no = t.batch_no
WHERE NOT EXISTS (
  SELECT 1 FROM prescription_items pi
  WHERE pi.prescription_id = p.id AND pi.medicine_batch_id = b.id
);

-- Dispensed items (by pharmacist)
INSERT INTO dispensed_items (prescription_item_id, dispensed_by, dispensed_date, quantity)
SELECT pi.id,
       (SELECT s.id FROM staff s WHERE s.email = 'lisa.martinez@hospital.com' LIMIT 1),
       NOW(),
       t.quantity
FROM (
  VALUES
  (1, 'Paracetamol 500mg', 'PCM-2026-A1', 6),
  (1, 'Cetirizine 10mg', 'CTZ-2026-A1', 5),
  (2, 'Amoxicillin 500mg', 'AMX-2026-A1', 8)
) AS t(appointment_id, medicine_name, batch_no, quantity)
JOIN prescriptions p ON p.appointment_id = t.appointment_id
JOIN medicines m ON m.name = t.medicine_name
JOIN medicine_batches b ON b.medicine_id = m.id AND b.batch_no = t.batch_no
JOIN prescription_items pi ON pi.prescription_id = p.id AND pi.medicine_batch_id = b.id
WHERE NOT EXISTS (
  SELECT 1 FROM dispensed_items di
  WHERE di.prescription_item_id = pi.id
);

-- Stock transactions for dispensed items (OUT)
INSERT INTO stock_transactions (batch_id, transaction_type, quantity, transaction_date, reference)
SELECT b.id, 'OUT', t.quantity, NOW(), t.reference
FROM (
  VALUES
  ('PCM-2026-A1', 6, 'PRESC-APPT-1'),
  ('CTZ-2026-A1', 5, 'PRESC-APPT-1'),
  ('AMX-2026-A1', 8, 'PRESC-APPT-2')
) AS t(batch_no, quantity, reference)
JOIN medicine_batches b ON b.batch_no = t.batch_no
WHERE NOT EXISTS (
  SELECT 1 FROM stock_transactions st
  WHERE st.batch_id = b.id AND st.reference = t.reference
);

-- ================= BILLING & INVOICES SAMPLE DATA =================

-- Sample invoices (consultation + pharmacy + lab)
INSERT INTO invoices (invoice_number, patient_id, appointment_id, status, subtotal, tax, discount, total, amount_paid, balance_due, issued_at, due_date, notes)
SELECT * FROM (VALUES
('INV-2026-0001', 1, 1, 'PARTIAL', 180.00, 0.00, 0.00, 180.00, 100.00, 80.00, NOW(), DATE '2026-02-20', 'Consultation + pharmacy + lab'),
('INV-2026-0002', 2, 2, 'PAID', 150.00, 0.00, 0.00, 150.00, 150.00, 0.00, NOW(), DATE '2026-02-22', 'Consultation + lab')
) AS t(invoice_number, patient_id, appointment_id, status, subtotal, tax, discount, total, amount_paid, balance_due, issued_at, due_date, notes)
WHERE NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_number = t.invoice_number);

-- Invoice line items
INSERT INTO invoice_items (invoice_id, item_type, description, quantity, unit_price, line_total, reference_type, reference_id)
SELECT i.id, t.item_type, t.description, t.quantity, t.unit_price, t.line_total, t.reference_type, t.reference_id
FROM (
  VALUES
  ('INV-2026-0001', 'CONSULTATION', 'Cardiology Consultation', 1, 100.00, 100.00, 'APPOINTMENT', 1),
  ('INV-2026-0001', 'PHARMACY', 'Dispensed medicines', 1, 50.00, 50.00, 'PRESCRIPTION', 1),
  ('INV-2026-0001', 'LAB', 'ECG + Blood Panel', 1, 30.00, 30.00, 'LAB_ORDER', NULL),
  ('INV-2026-0002', 'CONSULTATION', 'Neurology Consultation', 1, 100.00, 100.00, 'APPOINTMENT', 2),
  ('INV-2026-0002', 'LAB', 'MRI Review + Labs', 1, 50.00, 50.00, 'LAB_ORDER', NULL)
) AS t(invoice_number, item_type, description, quantity, unit_price, line_total, reference_type, reference_id)
JOIN invoices i ON i.invoice_number = t.invoice_number
WHERE NOT EXISTS (
  SELECT 1 FROM invoice_items ii
  WHERE ii.invoice_id = i.id AND ii.description = t.description AND ii.line_total = t.line_total
);

-- Invoice payments (supports partials)
INSERT INTO invoice_payments (invoice_id, amount, payment_method, payment_status, paid_at, reference)
SELECT i.id, t.amount, t.payment_method, t.payment_status, t.paid_at, t.reference
FROM (
  VALUES
  ('INV-2026-0001', 100.00, 'Cash', 'PAID', NOW(), 'PAY-INV-0001-1'),
  ('INV-2026-0002', 150.00, 'Card', 'PAID', NOW(), 'PAY-INV-0002-1')
) AS t(invoice_number, amount, payment_method, payment_status, paid_at, reference)
JOIN invoices i ON i.invoice_number = t.invoice_number
WHERE NOT EXISTS (
  SELECT 1 FROM invoice_payments ip
  WHERE ip.invoice_id = i.id AND ip.reference = t.reference
);

-- Invoice documents (PDFs)
INSERT INTO invoice_documents (invoice_id, file_name, file_url, created_at)
SELECT i.id, t.file_name, t.file_url, t.created_at
FROM (
  VALUES
  ('INV-2026-0001', 'INV-2026-0001.pdf', '/invoices/INV-2026-0001.pdf', NOW()),
  ('INV-2026-0002', 'INV-2026-0002.pdf', '/invoices/INV-2026-0002.pdf', NOW())
) AS t(invoice_number, file_name, file_url, created_at)
JOIN invoices i ON i.invoice_number = t.invoice_number
WHERE NOT EXISTS (
  SELECT 1 FROM invoice_documents d
  WHERE d.invoice_id = i.id AND d.file_name = t.file_name
);

-- ================= VISITS SAMPLE DATA =================
INSERT INTO visits (appointment_id, patient_id, doctor_id, symptoms, diagnosis, status, priority)
SELECT a.id, a.patient_id, a.staff_id, t.symptoms, t.diagnosis, t.status, t.priority
FROM (
  VALUES
  (1, 'Chest pain, fatigue', 'Possible angina', 'LAB_REQUIRED', 'NORMAL'),
  (2, 'Severe migraine', 'Migraine with aura', 'OPEN', 'NORMAL'),
  (3, 'Knee pain', 'Possible meniscus injury', 'LAB_REQUIRED', 'URGENT')
) AS t(appointment_id, symptoms, diagnosis, status, priority)
JOIN appointments a ON a.id = t.appointment_id
WHERE NOT EXISTS (
  SELECT 1 FROM visits v WHERE v.appointment_id = a.id
);

-- ================= LAB TESTS MASTER =================
INSERT INTO lab_tests_master (test_name, price, normal_range_text, is_active)
SELECT * FROM (VALUES
('Complete Blood Count (CBC)', 25.00, 'WBC 4.0-11.0, Hb 12-16 g/dL', true),
('Thyroid Profile (T3/T4/TSH)', 40.00, 'TSH 0.4-4.0 mIU/L', true),
('Lipid Profile', 35.00, 'Total Cholesterol < 200 mg/dL', true),
('Blood Glucose (Fasting)', 15.00, '70-100 mg/dL', true),
('ESR', 12.00, '0-20 mm/hr', true)
) AS t(test_name, price, normal_range_text, is_active)
WHERE NOT EXISTS (SELECT 1 FROM lab_tests_master m WHERE m.test_name = t.test_name);

-- ================= LAB ORDERS + ITEMS =================
INSERT INTO lab_orders (visit_id, ordered_by_doctor_id, status, notes)
SELECT v.id, v.doctor_id, t.status, t.notes
FROM (
  VALUES
  (1, 'COMPLETED', 'Chest pain workup'),
  (3, 'IN_PROGRESS', 'Knee inflammation assessment')
) AS t(appointment_id, status, notes)
JOIN visits v ON v.appointment_id = t.appointment_id
WHERE NOT EXISTS (SELECT 1 FROM lab_orders lo WHERE lo.visit_id = v.id);

INSERT INTO lab_order_items (lab_order_id, test_id, status, result_value, result_unit, reference_range, result_flag, result_notes)
SELECT lo.id, lt.id, t.status, t.result_value, t.result_unit, t.reference_range, t.result_flag, t.result_notes
FROM (
  VALUES
  (1, 'Complete Blood Count (CBC)', 'COMPLETED', '13.8', 'g/dL', '12.0 - 16.0', 'NORMAL', 'Hemoglobin within range'),
  (1, 'Lipid Profile', 'COMPLETED', '182', 'mg/dL', '< 200', 'NORMAL', 'Total cholesterol within range'),
  (3, 'ESR', 'PENDING', NULL, NULL, '0 - 20', NULL, NULL),
  (3, 'Blood Glucose (Fasting)', 'PENDING', NULL, NULL, '70 - 100', NULL, NULL)
) AS t(appointment_id, test_name, status, result_value, result_unit, reference_range, result_flag, result_notes)
JOIN visits v ON v.appointment_id = t.appointment_id
JOIN lab_orders lo ON lo.visit_id = v.id
JOIN lab_tests_master lt ON lt.test_name = t.test_name
WHERE NOT EXISTS (
  SELECT 1 FROM lab_order_items i
  WHERE i.lab_order_id = lo.id AND i.test_id = lt.id
);

-- ================= LAB REPORT UPLOADS =================
INSERT INTO lab_reports (visit_id, lab_order_id, file_name, file_url, mime_type)
SELECT v.id, lo.id, t.file_name, t.file_url, t.mime_type
FROM (
  VALUES
  (1, 'cbc_report_001.pdf', '/lab/reports/cbc_report_001.pdf', 'application/pdf'),
  (1, 'lipid_report_001.pdf', '/lab/reports/lipid_report_001.pdf', 'application/pdf')
) AS t(appointment_id, file_name, file_url, mime_type)
JOIN visits v ON v.appointment_id = t.appointment_id
JOIN lab_orders lo ON lo.visit_id = v.id
WHERE NOT EXISTS (
  SELECT 1 FROM lab_reports r
  WHERE r.visit_id = v.id AND r.file_name = t.file_name
);

-- ================= DISPENSE SAMPLE =================
INSERT INTO dispense (prescription_id, dispensed_by, total_amount)
SELECT p.id,
       s.id,
       25.50
FROM prescriptions p
JOIN LATERAL (
    SELECT id FROM staff WHERE position = 'Pharmacist'
    UNION ALL
    SELECT id FROM staff
    ORDER BY id
    LIMIT 1
) s ON true
WHERE p.appointmentid = 1
  AND NOT EXISTS (SELECT 1 FROM dispense d WHERE d.prescription_id = p.id);

INSERT INTO dispense_items (dispense_id, medicine_batch_id, quantity, unit_price, line_total)
SELECT d.id, b.id, t.quantity, t.unit_price, (t.quantity * t.unit_price)
FROM (
  VALUES
  (1, 'PCM-2026-A1', 6, 2.50),
  (1, 'CTZ-2026-A1', 5, 4.40)
) AS t(appointment_id, batch_no, quantity, unit_price)
JOIN prescriptions p ON p.appointmentid = t.appointment_id
JOIN dispense d ON d.prescription_id = p.id
JOIN medicine_batches b ON b.batch_no = t.batch_no
WHERE NOT EXISTS (
  SELECT 1 FROM dispense_items di
  WHERE di.dispense_id = d.id AND di.medicine_batch_id = b.id
);

-- ================= LINK EXISTING INVOICES TO VISITS =================
UPDATE invoices i
SET visit_id = v.id
FROM visits v
WHERE i.appointment_id = v.appointment_id
  AND i.visit_id IS NULL;

-- ================= IPD / BEDS SAMPLE DATA =================
INSERT INTO beds (bed_number, ward, type, status, daily_charge)
SELECT * FROM (VALUES
('B101', 'Ward A', 'GENERAL', 'AVAILABLE', 1200.00),
('B102', 'Ward A', 'GENERAL', 'AVAILABLE', 1200.00),
('ICU-01', 'ICU', 'ICU', 'AVAILABLE', 5000.00),
('P201', 'Private', 'PRIVATE', 'AVAILABLE', 2500.00)
) AS t(bed_number, ward, type, status, daily_charge)
WHERE NOT EXISTS (SELECT 1 FROM beds b WHERE b.bed_number = t.bed_number);

INSERT INTO admissions (bed_id, patient_id, visit_id, doctor_id, admitted_at, status, notes)
SELECT b.id, v.patient_id, v.id, v.doctor_id, NOW(), 'ACTIVE', 'Admitted for observation'
FROM beds b
JOIN visits v ON v.id = 1
WHERE b.bed_number = 'B101'
  AND NOT EXISTS (SELECT 1 FROM admissions a WHERE a.bed_id = b.id AND a.status = 'ACTIVE');

-- ================= NOTIFICATION SETTINGS SAMPLE =================
INSERT INTO notification_settings (role, event_type, channel, enabled)
SELECT * FROM (VALUES
('Doctor', 'APPOINTMENT_REMINDER', 'EMAIL', true),
('Doctor', 'LAB_REPORT_READY', 'EMAIL', true),
('Lab', 'LAB_REPORT_READY', 'EMAIL', true),
('Billing', 'BILLING_REMINDER', 'EMAIL', true),
('Receptionist', 'APPOINTMENT_REMINDER', 'EMAIL', true),
('Doctor', 'APPOINTMENT_REMINDER', 'SMS', false),
('Lab', 'LAB_REPORT_READY', 'SMS', false),
('Billing', 'BILLING_REMINDER', 'SMS', false),
('Receptionist', 'APPOINTMENT_REMINDER', 'SMS', false)
) AS t(role, event_type, channel, enabled)
WHERE NOT EXISTS (
  SELECT 1 FROM notification_settings ns
  WHERE ns.role = t.role AND ns.event_type = t.event_type AND ns.channel = t.channel
);
