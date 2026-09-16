# Hospital Management System - Medical Records Feature Guide

## Quick Start Guide

### Accessing Patient Medical Records

1. **Navigate to Dashboard**: Login to your hospital dashboard
2. **Go to Patients Section**: Click on "Patients" in the sidebar
3. **Click "View"**: Click the View button for any patient
4. **Access Medical Records**: You'll be redirected to the Patient Details page with 3 tabs

---

## Feature Walkthrough

### Patient Details Page Structure

```
┌─────────────────────────────────────────────┐
│         PATIENT DETAILS HEADER              │
│  Name: John Doe                             │
│  Email | Phone | DOB | Address              │
└─────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────┐
│  TAB 1: MEDICAL HISTORY  | TAB 2: ALLERGIES │
│  TAB 3: CHRONIC CONDITIONS                  │
├─────────────────────────────────────────────┤
│  [+ Add History]                            │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ Surgery                       [Edit] │   │
│  │ Type: Appendectomy           [Delete]│  │
│  │ Date: 2023-05-20                    │   │
│  │ Description: Successful procedure   │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ Hospitalization          [Edit]     │   │
│  │ Type: Acute Illness     [Delete]    │   │
│  │ Date: 2022-08-15                    │   │
│  │ Description: 5-day stay, recovered  │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

---

## Tab 1: Medical History

### What It Shows
- Significant medical events
- Surgeries and procedures
- Hospitalizations
- Medications and reactions
- Vaccinations
- Laboratory tests

### How to Use

#### View Records
- Records display in reverse chronological order (newest first)
- Each record shows:
  - History Type (Surgery, Hospitalization, etc.)
  - Description (detailed information)
  - Date (when the event occurred)
  - Notes (additional observations)
  - Recorded At (when entered into system)

#### Add New Medical History
```
1. Click "+ Add History" button
2. Fill in the form:
   - History Type: Choose type of event
   - Description: Detailed account (Required)
   - Date: When did this occur?
   - Notes: Additional information
3. Click "Save"
4. Record appears in the list immediately
```

#### Edit Medical History
```
1. Find the record you want to edit
2. Click "Edit" button
3. Modal opens with pre-filled information
4. Make changes
5. Click "Save"
6. Changes appear in real-time
```

#### Delete Medical History
```
1. Find the record to delete
2. Click "Delete" button
3. Confirm deletion in popup
4. Record is removed immediately
```

---

## Tab 2: Allergies

### What It Shows
- **Allergy Type**: Food, Medicine, Environmental, or Other
- **Allergy Name**: Specific allergen
- **Reaction**: Physical symptoms experienced
- **Severity**: Mild, Moderate, or Severe
- **Notes**: Special handling instructions

### How to Use

#### View Allergies
- Color-coded severity badges:
  - 🟢 **Mild** (Green) - Minor reactions
  - 🟡 **Moderate** (Yellow) - Significant symptoms
  - 🔴 **Severe** (Red) - Serious reactions/anaphylaxis

#### Add New Allergy
```
1. Click "+ Add Allergy" button
2. Fill in the form:
   - Allergy Type: Select from dropdown
   - Allergy Name: Specific allergen (Required)
   - Reaction: What happens when exposed?
   - Severity: Select from dropdown
   - Notes: Special instructions or precautions
3. Click "Save"
```

**Example: Peanut Allergy**
```
Type: Food
Name: Peanuts
Reaction: Severe throat swelling, difficulty breathing
Severity: Severe
Notes: Anaphylaxis risk - Always carry EpiPen
```

#### Edit Allergy
```
1. Find the allergy record
2. Click "Edit"
3. Modify any field
4. Click "Save"
```

#### Delete Allergy
```
1. Click "Delete" button
2. Confirm deletion
3. Record is removed
```

---

## Tab 3: Chronic Conditions

### What It Shows
- Ongoing health conditions
- Status tracking (ACTIVE, INACTIVE, UNDER_OBSERVATION)
- Diagnosis date
- Management notes

### Status Types
- **ACTIVE**: Currently being treated/monitored
- **INACTIVE**: No longer an active concern
- **UNDER_OBSERVATION**: Being closely monitored

### How to Use

#### View Conditions
- Status shown with color badges:
  - 🟢 **ACTIVE** (Green) - Currently managed
  - ⚫ **INACTIVE** (Gray) - No longer active
  - 🟡 **UNDER_OBSERVATION** (Yellow) - Close monitoring

#### Add New Condition
```
1. Click "+ Add Condition" button
2. Fill in the form:
   - Condition Name: Name of disease/condition (Required)
   - Diagnosed Date: When diagnosed
   - Status: Select from dropdown (Required)
   - Notes: Treatment plan or observations
3. Click "Save"
```

**Example: Diabetes Type 2**
```
Name: Type 2 Diabetes
Diagnosed: 2018-03-15
Status: ACTIVE
Notes: Well-controlled with metformin. HbA1c 6.8%
```

#### Edit Condition
```
1. Click "Edit" on the condition card
2. Update fields (e.g., change status to INACTIVE)
3. Click "Save"
```

#### Delete Condition
```
1. Click "Delete"
2. Confirm deletion
3. Record is removed
```

---

## Test Data Available

### Sample Patients with Medical Records

#### Patient 1: John Doe (ID: 1)
**Allergies:**
- Aspirin (Medicine) - Moderate
- Shellfish (Food) - Severe

**Chronic Conditions:**
- Type 2 Diabetes (ACTIVE)
- Hypertension (ACTIVE)

**Medical History:**
- Coronary event (2022)
- Cholecystectomy (2019)

#### Patient 2: Jane Smith (ID: 2)
**Allergies:**
- Pollen (Environmental) - Mild
- Nuts (Food) - Severe

**Chronic Conditions:**
- Asthma (ACTIVE)

**Medical History:**
- Adverse medication reaction (2024)

#### Patient 3: David Johnson (ID: 3)
**Allergies:**
- Penicillin (Medicine) - Severe

**Chronic Conditions:**
- Coronary Artery Disease (ACTIVE)
- Hyperlipidemia (ACTIVE)

**Medical History:**
- MI with stent placement (2019)
- Tibia fracture from accident (2020)

*...and 5 more patients with complete medical records*

---

## API Integration

The patient-details.html page communicates with the backend using these endpoints:

### Allergies API
```
GET    /api/patients/{patientId}/allergies
POST   /api/patients/{patientId}/allergies
PUT    /api/patients/{patientId}/allergies/{allergyId}
DELETE /api/patients/{patientId}/allergies/{allergyId}
```

### Chronic Conditions API
```
GET    /api/patients/{patientId}/chronic-conditions
POST   /api/patients/{patientId}/chronic-conditions
PUT    /api/patients/{patientId}/chronic-conditions/{conditionId}
DELETE /api/patients/{patientId}/chronic-conditions/{conditionId}
```

### Medical History API
```
GET    /api/patients/{patientId}/medical-history
POST   /api/patients/{patientId}/medical-history
PUT    /api/patients/{patientId}/medical-history/{historyId}
DELETE /api/patients/{patientId}/medical-history/{historyId}
```

---

## Tips & Best Practices

### 1. **Severity Coding for Allergies**
- Use **Severe** for any life-threatening reactions
- Use **Moderate** for reactions requiring medical attention
- Use **Mild** for minor irritations

### 2. **Status Management for Conditions**
- Keep status as **ACTIVE** for current management
- Change to **INACTIVE** when condition is resolved
- Use **UNDER_OBSERVATION** during critical periods

### 3. **Medical History Documentation**
- Include specific dates when possible
- Note surgical techniques/procedures used
- Record outcomes and complications
- Document follow-up requirements

### 4. **Data Accuracy**
- Double-check spellings of drug names
- Verify dates before saving
- Include complete reaction descriptions
- Add relevant notes for clinical reference

### 5. **Patient Safety**
- Review allergies before appointment
- Communicate severe allergies to all staff
- Update records immediately if new information emerges
- Keep notes current for continuity of care

---

## Troubleshooting

### Records Not Showing
- **Solution**: Refresh the page or check browser console for errors
- **Verify**: Patient ID in URL matches the patient

### Unable to Add Records
- **Check**: All required fields are filled (marked with *)
- **Verify**: Browser allows JavaScript
- **Try**: Clearing browser cache and reloading

### Modal Not Closing
- **Click**: Outside the modal or press Escape key
- **Try**: Clicking the X button in modal corner

### Data Not Saving
- **Check**: Browser console for error messages
- **Verify**: Backend API is running on port 8080
- **Ensure**: Form validation passes (required fields filled)

### Deleted Records Reappear
- Hard refresh browser: Ctrl+F5 (Windows) or Cmd+Shift+R (Mac)

---

## Feature Limitations & Notes

1. **Timestamps**: Recorded automatically in system time
2. **Bulk Operations**: Currently limited to single record operations
3. **Exports**: No built-in PDF export yet
4. **Search**: Use patient records directly (no search filter)
5. **History Viewing**: Shows most recent first

---

## Future Enhancements

- 📄 PDF report generation
- 🔍 Advanced search and filtering
- 📊 Condition timeline visualization
- 📱 Mobile app integration
- 🔔 Allergy alerts for staff
- 📥 Bulk import functionality
- 🔐 Audit trail and access logs

---

## Support & Questions

For additional support or to report issues:
1. Check this guide for common problems
2. Review the implementation summary document
3. Contact system administrator
4. Check application logs for errors

---

## Quick Reference

| Feature | Action | Shortcut |
|---------|--------|----------|
| Add Record | Click +Add button | - |
| Edit Record | Click Edit button | - |
| Delete Record | Click Delete, then Confirm | - |
| Switch Tab | Click tab button | - |
| Close Modal | Click X or outside modal | Esc |
| Go Back | Click Back button | - |

---

**Version**: 1.0  
**Last Updated**: January 25, 2026  
**System**: Hospital Management System v1.0
