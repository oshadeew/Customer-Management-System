# Customer Management System

A full-stack Customer Management System built with **Spring Boot 2.7.x** (backend) and **React JS** (frontend).

---

## Tech Stack

| Layer     | Technology                                                   |
|-----------|--------------------------------------------------------------|
| Backend   | Java 8, Spring Boot 2.7.18, Maven, Spring Data JPA, Lombok  |
| Database  | MariaDB 10.x                                                 |
| Excel     | Apache POI 5.2.3 (SAX streaming)                            |
| Mapping   | ModelMapper 3.1.1                                            |
| Frontend  | React 18, Bootstrap 5, Axios, react-datepicker 4.x          |
| Routing   | React Router DOM 6.x                                         |
| Tests     | JUnit 5, Mockito, MockMvc                                    |

---

## Prerequisites

- **Java 8** (JDK 8+)
- **Maven 3.6+**
- **Node.js 16+** and npm
- **MariaDB 10.x** running locally

---

## Step 1 – Database Setup

Open a MariaDB client and run:

```bash
# Create the database and tables
mysql -u root -p < backend/src/main/resources/schema.sql

# Seed initial data (countries, cities, sample customers)
mysql -u root -p < backend/src/main/resources/seed.sql
```

Or copy-paste the contents of `schema.sql` and `seed.sql` into your preferred DB client.

---

## Step 2 – Configure application.properties

Open `backend/src/main/resources/application.properties` and update the credentials to match your local MariaDB:

```properties
spring.datasource.username=root          # default MariaDB username
spring.datasource.password=              # leave empty if no password, or enter yours
```

> **Note:** The file already uses `root` as the username — this is the standard default.
> If your MariaDB root account has a password set, enter it after `spring.datasource.password=`.
> If it has **no password** (common in fresh installs), leave the value blank.

---

## Step 3 – Run the Backend

```bash
cd backend
mvn spring-boot:run
```


---

## Step 4 – Run the Frontend

```bash
cd frontend
npm install
npm start
```

The React app will open at **http://localhost:3000**.

---

## Step 5 – Run JUnit Tests

```bash
cd backend
mvn test
```

This runs:
- `CustomerServiceTest` – create, update, findById with Mockito
- `BulkUploadServiceTest` – batch parsing, upsert logic
- `CustomerControllerTest` – all endpoints via MockMvc

---

## Project Structure

```
customer-management/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/customer/
│       │   ├── CustomerManagementApplication.java
│       │   ├── controller/        # Thin REST layer
│       │   ├── service/           # Business logic
│       │   ├── repository/        # Spring Data JPA
│       │   ├── entity/            # JPA entities
│       │   ├── dto/               # Request & Response DTOs
│       │   └── exception/         # Custom exceptions + GlobalExceptionHandler
│       ├── main/resources/
│       │   ├── application.properties
│       │   ├── schema.sql
│       │   └── seed.sql
│       └── test/java/com/customer/
│           ├── controller/CustomerControllerTest.java
│           └── service/
│               ├── CustomerServiceTest.java
│               └── BulkUploadServiceTest.java
└── frontend/
|    ├── package.json
|   └── src/
|        ├── App.js                  # Routing
|       ├── index.js
|       ├── index.css
|       ├── services/
|       │   └── customerService.js  # All Axios API calls
|       └── pages/
|           ├── CustomerListPage.js
|           ├── CustomerFormPage.js
|           ├── CustomerDetailPage.js
|           └── BulkUploadPage.js
|___ excel files
```

---

## API Endpoints

| Method | Endpoint                          | Description                                |
|--------|-----------------------------------|--------------------------------------------|
| POST   | `/api/customers`                  | Create a new customer                      |
| PUT    | `/api/customers/{id}`             | Update an existing customer                |
| GET    | `/api/customers/{id}`             | Get customer with all details              |
| GET    | `/api/customers?page=0&size=10`   | Paginated customer list                    |
| GET    | `/api/customers/search?name=xyz`  | Search customers by name                   |
| POST   | `/api/customers/bulk-upload`      | Upload .xlsx for bulk create/update        |
| GET    | `/api/countries`                  | Get all countries                          |
| GET    | `/api/cities/{countryId}`         | Get cities by country                      |

---

## Bulk Upload Excel Format

The Excel file must be `.xlsx` format with the **first row as a header** (automatically skipped).

| Column A | Column B | Column C |
|----------|----------|----------|
| Name | Date of Birth (yyyy-MM-dd) | NIC Number |
| Alice Fernando | 1990-05-15 | 901234567V |
| Bob Perera | 1985-11-20 | 851234567V |

**Upsert logic:**
- If the NIC **does not exist** → insert new customer
- If the NIC **already exists** → update name and date of birth only

**Processing:**
- Streamed using Apache POI SAX (XSSFReader) — memory-efficient for large files
- Processed in batches of 500 rows per transaction
- A bad batch does not roll back successful batches
- Returns a summary: `{ totalRows, successCount, updatedCount, failedCount, errors[] }`

---

## Performance Notes

- Customer detail loads use `@EntityGraph` to fetch phones, addresses (+ city + country), and family members in a single optimized query — no N+1 problem.
- Bulk upload uses the SAX streaming API to avoid loading 1M+ rows into memory.
- Batch NIC lookups use `findByNicNumberIn()` (one query per batch) instead of one query per row.
- `saveAll()` is used for batch inserts/updates.

---

## Frontend Pages

| Route | Page | Description |
|-------|------|-------------|
| `/` | CustomerListPage | Paginated table with View/Edit actions |
| `/customers/new` | CustomerFormPage | Create new customer |
| `/customers/:id` | CustomerDetailPage | Read-only detail view |
| `/customers/:id/edit` | CustomerFormPage | Edit existing customer |
| `/bulk-upload` | BulkUploadPage | Upload Excel file |
