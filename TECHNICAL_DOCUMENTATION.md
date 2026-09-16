# Hospital Management System - Technical Documentation

## System Architecture

### Backend Stack
- **Framework**: Spring Boot (Java)
- **ORM**: JPA/Hibernate
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **API Style**: RESTful

### Frontend Stack
- **HTML5**: Semantic markup
- **CSS3**: Modern styling with flexbox/grid
- **JavaScript (ES6)**: Vanilla JS (no external dependencies)
- **Architecture**: Event-driven, callback-based

### Database Architecture
- **Type**: Relational (PostgreSQL)
- **Normalization**: 3NF
- **Constraints**: Foreign keys with cascade delete
- **Indexes**: Performance-optimized on foreign keys

---

## Database Schema Details

### patient_allergies Table

```sql
CREATE TABLE patient_allergies (
    allergy_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    allergy_type VARCHAR(255) NOT NULL,
    allergy_name VARCHAR(255) NOT NULL,
    reaction VARCHAR(255),
    severity VARCHAR(255),
    notes VARCHAR(255),
    recorded_at VARCHAR(255)
);
```

**Columns**:
- `allergy_id`: Unique identifier (auto-incrementing)
- `patient_id`: Foreign key to patients table
- `allergy_type`: Category (Food, Medicine, Environmental, Other)
- `allergy_name`: Specific allergen name
- `reaction`: Physical manifestation
- `severity`: Risk level (Mild, Moderate, Severe)
- `notes`: Clinical observations
- `recorded_at`: System timestamp (format: YYYY-MM-DD HH:MM:SS)

**Performance Index**:
```sql
CREATE INDEX idx_allergies_patient ON patient_allergies(patient_id);
```

### patient_chronic_conditions Table

```sql
CREATE TABLE patient_chronic_conditions (
    condition_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    condition_name VARCHAR(255) NOT NULL,
    diagnosed_date VARCHAR(255),
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(255),
    recorded_at VARCHAR(255)
);
```

**Columns**:
- `condition_id`: Unique identifier (auto-incrementing)
- `patient_id`: Foreign key to patients table
- `condition_name`: Disease/condition name
- `diagnosed_date`: Date of diagnosis (format: YYYY-MM-DD)
- `status`: Current state (ACTIVE, INACTIVE, UNDER_OBSERVATION)
- `notes`: Management details and observations
- `recorded_at`: System timestamp

**Performance Index**:
```sql
CREATE INDEX idx_chronic_patient ON patient_chronic_conditions(patient_id);
```

### patient_medical_history Table

```sql
CREATE TABLE patient_medical_history (
    history_id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    history_type VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    history_date VARCHAR(255),
    notes VARCHAR(255),
    recorded_at VARCHAR(255)
);
```

**Columns**:
- `history_id`: Unique identifier (auto-incrementing)
- `patient_id`: Foreign key to patients table
- `history_type`: Event type (Surgery, Hospitalization, etc.)
- `description`: Detailed event information
- `history_date`: Date of event (format: YYYY-MM-DD)
- `notes`: Additional clinical notes
- `recorded_at`: System timestamp

**Performance Index**:
```sql
CREATE INDEX idx_history_patient ON patient_medical_history(patient_id);
```

---

## Java Class Hierarchy

### Entity Classes

```
@Entity
@Table(name = "patient_allergies")
class PatientAllergy {
    @Id @GeneratedValue Long allergyId
    Long patientId
    String allergyType
    String allergyName
    String reaction
    String severity
    String notes
    String recordedAt
}

@Entity
@Table(name = "patient_chronic_conditions")
class PatientChronicCondition {
    @Id @GeneratedValue Long conditionId
    Long patientId
    String conditionName
    String diagnosedDate
    String status
    String notes
    String recordedAt
}

@Entity
@Table(name = "patient_medical_history")
class PatientMedicalHistory {
    @Id @GeneratedValue Long historyId
    Long patientId
    String historyType
    String description
    String historyDate
    String notes
    String recordedAt
}
```

### Repository Pattern

```
interface PatientAllergyRepository extends JpaRepository<PatientAllergy, Long> {
    List<PatientAllergy> findByPatientId(Long patientId)
}

interface PatientChronicConditionRepository extends JpaRepository<PatientChronicCondition, Long> {
    List<PatientChronicCondition> findByPatientId(Long patientId)
}

interface PatientMedicalHistoryRepository extends JpaRepository<PatientMedicalHistory, Long> {
    List<PatientMedicalHistory> findByPatientId(Long patientId)
}
```

### Service Layer

```
class PatientAllergyService {
    List<PatientAllergy> getAllergiesByPatientId(Long patientId)
    Optional<PatientAllergy> getAllergyById(Long allergyId)
    PatientAllergy createAllergy(PatientAllergy allergy)
    PatientAllergy updateAllergy(Long allergyId, PatientAllergy allergyDetails)
    boolean deleteAllergy(Long allergyId)
}

[Similar for PatientChronicConditionService and PatientMedicalHistoryService]
```

### Controller Endpoints

```
@RestController
@RequestMapping("/api/patients")
class PatientController {
    
    // Allergy Operations
    GET    /{patientId}/allergies -> List<PatientAllergy>
    POST   /{patientId}/allergies -> PatientAllergy
    PUT    /{patientId}/allergies/{allergyId} -> PatientAllergy
    DELETE /{patientId}/allergies/{allergyId} -> void
    
    // Chronic Condition Operations
    GET    /{patientId}/chronic-conditions -> List<PatientChronicCondition>
    POST   /{patientId}/chronic-conditions -> PatientChronicCondition
    PUT    /{patientId}/chronic-conditions/{conditionId} -> PatientChronicCondition
    DELETE /{patientId}/chronic-conditions/{conditionId} -> void
    
    // Medical History Operations
    GET    /{patientId}/medical-history -> List<PatientMedicalHistory>
    POST   /{patientId}/medical-history -> PatientMedicalHistory
    PUT    /{patientId}/medical-history/{historyId} -> PatientMedicalHistory
    DELETE /{patientId}/medical-history/{historyId} -> void
}
```

---

## API Specifications

### Request/Response Format

**All requests use JSON with Content-Type: application/json**

### Response Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | OK | Successful GET/PUT |
| 201 | Created | Successful POST |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Invalid JSON |
| 404 | Not Found | Patient/record doesn't exist |
| 500 | Server Error | Unexpected error |

### Example Request: Create Allergy

```http
POST /api/patients/1/allergies HTTP/1.1
Content-Type: application/json

{
  "allergyType": "Food",
  "allergyName": "Peanuts",
  "reaction": "Throat swelling",
  "severity": "Severe",
  "notes": "Anaphylaxis risk"
}
```

### Example Response: 201 Created

```json
{
  "allergyId": 1,
  "patientId": 1,
  "allergyType": "Food",
  "allergyName": "Peanuts",
  "reaction": "Throat swelling",
  "severity": "Severe",
  "notes": "Anaphylaxis risk",
  "recordedAt": "2026-01-25 10:30:45"
}
```

---

## Frontend Architecture

### Page Structure

```
patient-details.html
├── Header Section
│   ├── Navigation
│   └── Patient Info Display
├── Tab Container
│   ├── Medical History Tab
│   ├── Allergies Tab
│   └── Chronic Conditions Tab
├── Modals (3 types)
│   ├── History Form Modal
│   ├── Allergy Form Modal
│   └── Condition Form Modal
└── Scripts
    └── patient-details.js
```

### Data Flow Architecture

```
User Interaction
    ↓
Event Listener (JavaScript)
    ↓
API Call (Fetch)
    ↓
Server Processing
    ↓
Database Operation
    ↓
Response JSON
    ↓
DOM Manipulation
    ↓
Visual Update
```

### State Management

**Global Variables**:
```javascript
let currentPatientId = null;
let currentEditingHistoryId = null;
let currentEditingAllergyId = null;
let currentEditingConditionId = null;
```

These track:
- Which patient we're viewing
- Which record is being edited (for distinguishing POST vs PUT)

---

## CSS Architecture

### Design System

**Color Palette**:
```css
Primary: #3498db (Blue)      /* Actions, highlights */
Secondary: #95a5a6 (Gray)    /* Neutral, inactive */
Success: #27ae60 (Green)     /* Positive actions */
Danger: #e74c3c (Red)        /* Destructive actions */
Light: #ecf0f1 (Light Gray)  /* Backgrounds */
Text: #333 (Dark Gray)       /* Main text */
```

**Typography**:
```css
Font Family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif
Base Font Size: 14px
Headings: 16px - 28px (H3 to H1)
Line Height: ~1.5
```

**Spacing System** (8px base):
```css
Small:  8px
Medium: 16px
Large:  24px
XLarge: 32px
```

**Responsive Breakpoints**:
```css
Mobile:  < 480px
Tablet:  480px - 768px
Desktop: > 768px
```

---

## JavaScript Architecture

### Module Organization

**Initialization Phase**:
1. DOM Ready event listener
2. Authentication check
3. URL parameter parsing
4. Event listener binding
5. Initial data loading

**Data Loading Phase**:
- Parallel fetch requests for patient data
- Record list population
- Error handling with user feedback

**Interaction Phase**:
- Event delegation on buttons
- Modal management
- Form validation
- API communication
- DOM updates

**Error Handling**:
```javascript
fetch(url)
    .then(response => {
        if (!response.ok) throw new Error();
        return response.json();
    })
    .then(data => {/* success */})
    .catch(error => {/* error handling */});
```

---

## Performance Considerations

### Database Optimization

**Indexes**:
- Foreign key indexing on patient_id for O(log n) lookups
- Prevents full table scans

**Query Patterns**:
- Fetch only records for specific patient
- Use pagination for large datasets (future enhancement)

### Frontend Optimization

**Asset Loading**:
- Single CSS file: patient-details.css
- Single JS file: patient-details.js
- Minimal external dependencies

**DOM Manipulation**:
- Use innerHTML for batch updates
- Avoid jQuery or jQuery-like overhead
- Native Fetch API instead of AJAX libraries

**Browser Caching**:
- Static assets cached by browser
- API responses not cached (real-time data)

---

## Security Considerations

### Current Implementation

**Authentication**:
- Relies on existing system authentication
- User session stored in localStorage

**Data Protection**:
- Foreign key constraints prevent orphaned records
- Cascade delete maintains referential integrity

### Recommendations (Future)

1. **HTTPS**: Encrypt data in transit
2. **CSRF Protection**: Implement token validation
3. **Input Validation**: Server-side validation of all inputs
4. **SQL Injection Prevention**: Use parameterized queries (JPA handles this)
5. **XSS Prevention**: Escape HTML in user input
6. **Access Control**: Verify user permissions before operations
7. **Audit Logging**: Track who accessed/modified records
8. **Data Encryption**: Encrypt sensitive medical information at rest

---

## Testing Scenarios

### Manual Testing Checklist

```
[ ] Create new allergy record
[ ] Read allergy records
[ ] Update allergy details
[ ] Delete allergy record
[ ] Create new condition record
[ ] Update condition status
[ ] Delete condition record
[ ] Create new history record
[ ] Edit history description
[ ] Delete history record
[ ] Tab switching functionality
[ ] Modal open/close
[ ] Form validation
[ ] Error handling on network failure
[ ] Multiple patient records
[ ] Large record sets (performance)
```

### Sample Test Data Provided

10 patients with realistic medical records:
- Various allergy types and severities
- Multiple chronic conditions per patient
- Complete medical history with dates
- Ready for immediate testing

---

## Integration Points

### With Existing System

1. **Patient Dashboard**: Click "View" → navigates to patient-details.html
2. **Patient ID**: Passed via URL parameter (?id=X)
3. **API Base URL**: Uses /api/patients endpoints
4. **Authentication**: Inherits session from main dashboard

### External Systems (Future)

1. **EHR Systems**: Export medical records via FHIR standard
2. **Pharmacy Systems**: Allergy alerts at medication time
3. **Lab Systems**: Link test results to medical history
4. **Billing Systems**: Track procedures for invoicing

---

## Deployment Considerations

### Development Environment
```
Java Version: 11+
Maven: 3.6+
Spring Boot: 2.7+ or 3.x
Database: PostgreSQL 10+
Browser: Modern (Chrome 90+, Firefox 88+, Safari 14+)
```

### Build & Deploy
```bash
# Compile
mvn clean compile

# Test
mvn test

# Build
mvn clean package

# Run
java -jar target/HospitalApp-1.0.0.jar
```

### File Structure Post-Deployment
```
Hospital App Root/
├── src/main/java/com/hospital/app/
│   ├── controller/PatientController.java
│   ├── service/*Service.java
│   ├── repository/*Repository.java
│   ├── model/*.java
│   └── dto/*DTO.java
├── src/main/resources/
│   ├── schema.sql (with 3 new tables)
│   ├── data.sql (with test data)
│   └── static/
│       ├── patient-details.html
│       ├── css/patient-details.css
│       └── js/patient-details.js
└── pom.xml (no new dependencies required)
```

---

## Maintenance Notes

### Regular Tasks

1. **Backup**: Daily database backups (existing process)
2. **Monitoring**: Monitor API response times
3. **Updates**: Keep Spring Boot and dependencies current
4. **Cleanup**: Archive old records annually

### Troubleshooting

**Slow Queries**:
- Check indexes exist on patient_id columns
- Run ANALYZE on PostgreSQL tables
- Monitor query execution plans

**Memory Issues**:
- Implement pagination for large datasets
- Use database cursors for export operations
- Monitor heap size and adjust if needed

**Data Inconsistency**:
- Verify foreign key constraints are enabled
- Run referential integrity checks
- Review cascade delete operations

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-25 | Initial release with medical records feature |

---

## Related Documentation

- `FEATURE_GUIDE.md` - User-facing feature guide
- `IMPLEMENTATION_SUMMARY.md` - High-level overview
- `README.md` - General project documentation
- `pom.xml` - Maven dependencies

---

**Document Version**: 1.0  
**Last Updated**: January 25, 2026  
**Maintainer**: Hospital IT Department  
**Status**: Production Ready
