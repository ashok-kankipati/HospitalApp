# HospitalApp - Spring Boot Maven Project

Hospital Management System built with Spring Boot and Maven.

## Project Structure

```
HospitalApp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/hospital/app/
│   │   │       ├── HospitalAppApplication.java (Main application class)
│   │   │       ├── controller/
│   │   │       │   ├── HospitalController.java
│   │   │       │   └── PatientController.java
│   │   │       ├── model/
│   │   │       │   └── Patient.java
│   │   │       ├── repository/
│   │   │       │   └── PatientRepository.java
│   │   │       └── service/
│   │   │           └── PatientService.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/hospital/app/
│           └── HospitalAppApplicationTests.java
└── pom.xml
```

## Technologies Used

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **MySQL 8.0**
- **Lombok**
- **Maven**

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL Server

## Setup Instructions

### 1. Database Setup

```sql
CREATE DATABASE IF NOT EXISTS hospitaldb;
USE hospitaldb;
```

### 2. Configuration

Update `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hospitaldb
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Health Check
- `GET /api/health` - Check application health
- `GET /api/` - Welcome message

### Patient Management
- `POST /api/patients` - Add a new patient
- `GET /api/patients` - Get all patients
- `GET /api/patients/{id}` - Get patient by ID
- `PUT /api/patients/{id}` - Update patient
- `DELETE /api/patients/{id}` - Delete patient
- `GET /api/patients/search/email/{email}` - Search by email
- `GET /api/patients/search/name/{name}` - Search by name

## Sample Patient JSON

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "555-1234",
  "dateOfBirth": "1990-01-15",
  "address": "123 Main St, City",
  "medicalHistory": "No known allergies"
}
```

## Maven Goals

- `mvn clean` - Clean the project
- `mvn compile` - Compile the project
- `mvn test` - Run tests
- `mvn package` - Package the application
- `mvn spring-boot:run` - Run the application
- `mvn clean install` - Clean and build the project

## Author

Created for Hospital Management System
