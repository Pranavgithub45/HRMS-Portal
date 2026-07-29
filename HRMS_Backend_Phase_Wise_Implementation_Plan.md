# HRMS Backend — Phase-Wise Implementation Plan (v2)

> Based on the **updated spec**: Attendance Management removed; Leave Management retained; new **Document Management** feature added (stores one file per employee for now, e.g. Aadhar PDF/photo).

Each phase produces a working, Postman-testable increment before moving to the next.

**Dependency order:** Setup → DB/Entities → Auth → Core Masters (Employee/Department) → Leave → Payroll → Document → Announcements → Dashboard → Hardening/Docs.

---

## Phase 0 — Project Setup & Foundation

**Goal:** A running Spring Boot app connected to MySQL, with base config and folder structure in place.

### Tasks
- Initialize Spring Boot 3.x project (Spring Initializr) with: Spring Web, Spring Data JPA, MySQL Driver, Lombok, Validation.
- Add dependencies manually: JWT library (e.g. `jjwt`), BCrypt (via `spring-security-crypto` or a standalone bcrypt lib — Spring Security itself is not used), MapStruct + annotation processor.
- Set up package structure:
  ```
  com.hrms
   ├── config
   ├── controller
   ├── service
   ├── service.impl
   ├── repository
   ├── entity
   ├── dto
   ├── mapper
   ├── security (JWT filter/util — custom, no Spring Security)
   ├── exception
   └── util
  ```
- Configure `application.properties`/`.yml`: MySQL datasource, JPA (`ddl-auto=update` for dev), server port, JWT secret & expiry, and a local file-storage directory path (used later by the Document module — e.g. `app.file.upload-dir=./uploads`).
- Create the MySQL schema (empty), verify connection on startup.
- Set up global exception handler skeleton (`@ControllerAdvice`) and a standard `ApiResponse<T>` wrapper.

### Deliverable
App boots, connects to MySQL, and a `/health` endpoint returns 200 OK.

---

## Phase 1 — Database Design & Core Entities

**Goal:** All JPA entities and relationships from the updated spec are modeled and persisted correctly.

### Tasks
- Create entities with Lombok:
    - `Department`
    - `Employee` (self-reference `manager_id`; `@ManyToOne` to `Department`; `role`/`status` as `@Enumerated(EnumType.STRING)`)
    - `LeaveRequest` (`@ManyToOne` to `Employee` for both `employee_id` and `approved_by`)
    - `Payroll` (`@ManyToOne` to `Employee`, unique constraint on `employee_id + month + year`)
    - `Announcement` (`@ManyToOne` to `Employee` as `created_by`)
    - `Document` (`@ManyToOne` to `Employee` for `employee_id`; `@ManyToOne` to `Employee` for `uploaded_by`; fields: `document_type`, `document_name`, `file_url`, `file_format`, `uploaded_date`, `remarks`)
    - **No Attendance entity in this version.**
- Define enums: `Role` (EMPLOYEE, MANAGER, HR), `EmployeeStatus` (ACTIVE, INACTIVE), `LeaveStatus` (PENDING, APPROVED, REJECTED), `DocumentType` (AADHAR, PAN, RESUME, OFFER_LETTER, OTHER).
- Create repositories: `DepartmentRepository`, `EmployeeRepository`, `LeaveRequestRepository`, `PayrollRepository`, `AnnouncementRepository`, `DocumentRepository` (anticipate `findByEmployeeId`, `findByEmail`, `findByManagerId`, `findByEmployeeIdAndMonthAndYear`, `findByExpiryDateGreaterThanEqual`).
- Verify tables auto-generate correctly with correct FKs.
- Seed minimal data (1 department, 1 HR, 1 manager, 1 employee) for later phases.

### Deliverable
All 6 tables exist in MySQL with correct relationships; repositories compile; basic `findAll()` works via a temporary test controller.

---

## Phase 2 — Authentication & JWT (No Spring Security)

**Goal:** Employees can register/login, receive a JWT, and protected endpoints reject invalid/missing tokens.

### Tasks
- `PasswordUtil` — BCrypt encode/verify.
- `JwtUtil` — generate token (embed `employeeId`, `email`, `role`), validate, extract claims, handle expiry.
- Custom `JwtAuthFilter` (plain Servlet `Filter` or `OncePerRequestFilter`, registered manually):
    - Reads `Authorization: Bearer <token>`.
    - Validates, extracts role/employeeId, stores in request attributes/thread-local for controllers/services.
    - Skips `/api/auth/**`.
- Simple role-check mechanism: custom `@RequireRole(Role.HR)` annotation + interceptor, or a manual helper called at the top of each service method — pick one, apply consistently.
- Endpoints:
    - `POST /api/auth/register`
    - `POST /api/auth/login`
- DTOs: `LoginRequest`, `LoginResponse`, `RegisterRequest`.
- Exception handling for 401 (invalid/expired token, bad credentials) and 403 (wrong role).

### Deliverable
Postman: register → login → get token → call a protected dummy endpoint with/without token, confirm 200 vs 401.

---

## Phase 3 — Employee & Department Management (Core Masters)

**Goal:** Full CRUD for Departments and Employees with role-based access — the foundation everything else depends on.

### Tasks
- **Department** (HR-only writes; all roles read):
    - `POST /api/departments`, `GET /api/departments`, `GET /api/departments/{id}`, `PUT /api/departments/{id}`, `DELETE /api/departments/{id}`
- **Employee**:
    - `POST /api/employees` — HR creates employee (department, manager, role).
    - `GET /api/employees` — HR: all; Manager: team only; Employee: self only.
    - `GET /api/employees/{id}` — self, own manager, or HR.
    - `PUT /api/employees/{id}` — HR full update; `PUT /api/employees/me` — self-update (phone etc.).
    - `PUT /api/employees/{id}/status` — HR activate/deactivate.
    - `GET /api/employees/me` — own profile.
    - `GET /api/employees/team` — Manager: direct reports.
- DTOs + MapStruct: `EmployeeRequestDto`, `EmployeeResponseDto` (never expose password), `DepartmentDto`.
- Validation: `@NotBlank`, `@Email`, unique email check.
- Role enforcement per the matrix above.

### Deliverable
Postman collection covering employee/department CRUD across all three roles, confirming correct 403s.

---

## Phase 4 — Leave Management

**Goal:** Full leave application → approval/rejection workflow (unchanged from original spec).

### Tasks
- Endpoints:
    - `POST /api/leaves` — Employee applies (status = PENDING, `applied_date` = today).
    - `GET /api/leaves/me` — own leave history.
    - `GET /api/leaves/team` — Manager: team's leave requests.
    - `GET /api/leaves/team/pending` — Manager: pending queue.
    - `PUT /api/leaves/{id}/approve` — Manager/HR approves, sets `approved_by`.
    - `PUT /api/leaves/{id}/reject` — Manager/HR rejects with `remarks`.
    - `GET /api/leaves` — HR: org-wide view.
- Business rules:
    - Validate `start_date <= end_date`; no overlapping PENDING/APPROVED leave for same employee.
    - Only the employee's own manager (or HR) can approve/reject.
- DTOs: `LeaveRequestDto`, `LeaveResponseDto`, `LeaveActionDto`.

### Deliverable
Postman flow: employee applies → manager sees it pending → approves/rejects → employee sees updated status.

---

## Phase 5 — Payroll Management

**Goal:** HR generates monthly payroll; employees view only their own.

### Tasks
- Endpoints:
    - `POST /api/payroll` — HR generates for an employee/month/year (`net_salary` computed server-side, never trusted from client).
    - `POST /api/payroll/bulk` — optional: generate for all active employees/department for a month.
    - `GET /api/payroll/me` — own payroll history.
    - `GET /api/payroll/{employeeId}` — HR view for a specific employee.
    - `GET /api/payroll` — HR: all records, filterable by month/year/department.
- Business rules:
    - One payroll per employee per month (unique check).
    - `net_salary = base_salary + bonus`, recomputed in service layer.
- DTOs: `PayrollGenerateDto`, `PayrollResponseDto`.

### Deliverable
Postman flow: HR generates payroll, duplicate for same month rejected, employee sees only their own record.

---

## Phase 6 — Document Management *(NEW — replaces the old Attendance phase)*

**Goal:** Employees/HR can upload, view, and manage an employee's document (e.g. Aadhar PDF/photo); Managers can view their team's documents read-only.

### Tasks
- Decide storage approach for the file itself (DB stores only metadata + reference):
    - Simplest for dev/demo: store uploaded files on local disk under the configured `app.file.upload-dir`, and save the relative path in `file_url`.
    - Note for later: this can be swapped for cloud storage (S3-compatible, etc.) without changing the API contract — only the storage-service implementation changes.
- File upload handling: use `MultipartFile` in the controller; validate file type (PDF, JPG, PNG only) and a reasonable max size (e.g. 5MB); generate a safe stored filename (e.g. `employeeId_timestamp.ext`) to avoid collisions.
- Endpoints:
    - `POST /api/documents` — Employee uploads/replaces their own document (multipart form: file + `document_type`, optional `remarks`). If a document already exists for that employee, this replaces it (delete old file, insert/update record) since scope is one document per employee for now.
    - `POST /api/documents/{employeeId}` — HR uploads/replaces a document on behalf of any employee.
    - `GET /api/documents/me` — Employee views their own document metadata + download link.
    - `GET /api/documents/{employeeId}` — HR: any employee's document; Manager: only if `employeeId` is in their team.
    - `GET /api/documents/team` — Manager: list of team members' document status/metadata.
    - `GET /api/documents` — HR: all documents (or a document-status report across all employees).
    - `GET /api/documents/download/{documentId}` — stream/download the actual file (role-checked same as view: self, own manager, or HR).
    - `DELETE /api/documents/{id}` — HR only.
- Business rules:
    - Employee can only upload/view/replace **their own** document.
    - Manager: read-only access to team members' documents.
    - HR: full access (upload/update/delete/view) to any employee's document.
    - Reject unsupported file types/oversized files with a clear 400 error.
    - Store `uploaded_by` (self vs HR) for audit purposes.
- DTOs: `DocumentUploadDto` (metadata fields; file handled separately as multipart), `DocumentResponseDto` (includes `document_type`, `document_name`, `file_format`, `uploaded_date`, `uploaded_by`, download URL — never expose raw server file paths, only an API download endpoint).

### Deliverable
Postman flow: employee uploads a PDF/photo as their document → views it → re-uploads to replace it; HR uploads a document on behalf of another employee and deletes one; manager confirms they can view (not upload) a team member's document; a non-team, non-HR user is correctly blocked (403) from another employee's document.

---

## Phase 7 — Announcement Management

**Goal:** HR publishes announcements visible to everyone until expiry.

### Tasks
- Endpoints:
    - `POST /api/announcements` — HR creates (title, description, expiry_date).
    - `PUT /api/announcements/{id}` — HR updates.
    - `DELETE /api/announcements/{id}` — HR deletes.
    - `GET /api/announcements` — all roles: only non-expired (`findByExpiryDateGreaterThanEqual`).
- DTOs: `AnnouncementRequestDto`, `AnnouncementResponseDto`.

### Deliverable
Postman flow: HR creates one expired and one active announcement; confirm only the active one shows for an Employee-role token.

---

## Phase 8 — Dashboard APIs

**Goal:** Aggregate, role-specific dashboard endpoints built on top of the modules already implemented (Leave, Payroll, Document, Announcements — no Attendance).

### Tasks
- `GET /api/dashboard/employee` → profile summary, own leave requests, latest payroll, **document status** (uploaded / not uploaded), latest active announcements.
- `GET /api/dashboard/manager` → team member count, pending/approved/rejected leave counts, **team document status** (who has/hasn't uploaded), latest announcements.
- `GET /api/dashboard/hr` → total employees, total departments, pending/approved leave counts, payroll generated this month, department-wise employee count, employees joined this month, **employees missing required documents**, latest announcements.
- Implement each as a service method composing existing repository queries — reuse Phase 3–7 services/repositories rather than duplicating logic.
- Consider read-only projection DTOs (`EmployeeDashboardDto`, `ManagerDashboardDto`, `HrDashboardDto`).

### Deliverable
Postman calls to all three dashboard endpoints with tokens of each role, confirming correct scoping and accurate counts (including document-status figures) against seeded data.

---

## Phase 9 — Cross-Cutting Hardening

**Goal:** Make the API production-respectable: consistent errors, validation, and clean mapping everywhere.

### Tasks
- Finish **Global Exception Handling** (`@ControllerAdvice`): map `MethodArgumentNotValidException`, `EntityNotFoundException`, custom exceptions (`ResourceNotFoundException`, `UnauthorizedActionException`, `DuplicateResourceException`, `InvalidFileTypeException`, `FileTooLargeException`) to consistent JSON error responses with correct HTTP status codes.
- Audit every endpoint against the role matrix (Employee/Manager/HR) — write a checklist and verify each one, paying special attention to the Document module's self/team/all scoping.
- Ensure **MapStruct** mappers are used consistently across all modules.
- Add `@Valid` + Bean Validation annotations on all request DTOs; verify meaningful 400 error messages.
- Review file-upload handling: max size, allowed MIME types, safe filename generation, and confirm the download endpoint never exposes a raw filesystem path.
- Add basic logging (request/response or at least service-level) for traceability.
- Review DB constraints: unique keys (email, payroll month/employee), not-null, FK cascade behavior on delete (e.g. what happens to an Employee's LeaveRequests/Documents if the employee is deleted — likely restrict, not cascade).

### Deliverable
QA pass: run through the full role matrix in Postman, including document upload/view/delete edge cases, confirming no endpoint leaks data across roles or accepts invalid input silently.

---

## Phase 10 — Testing, Documentation & Handover

**Goal:** The project is easy for anyone (including future-you) to run, test, and demo.

### Tasks
- Build a complete **Postman Collection** organized by module (folders: Auth, Employee, Department, Leave, Payroll, Document, Announcement, Dashboard), with an environment for `base_url` and `token` (auto-attach JWT after login via a pre-request/test script). Document upload requests should be set up as `form-data` with a sample test file attached.
- Write a `README.md`: setup instructions (MySQL config, `application.properties` template including the file-upload directory, how to run), tech stack, module list, note on where uploaded files are stored locally, and how to import/run the Postman collection.
- Prepare a short seed-data script (SQL or `CommandLineRunner`) so a fresh clone can be demoed immediately.
- Optional: springdoc-openapi/Swagger UI for a browsable API reference alongside Postman.

### Deliverable
A clone-and-run project: fresh DB + seed data + Postman collection reproduces the entire workflow end to end (register → login → CRUD masters → apply/approve leave → generate payroll → upload/view document → post announcement → view dashboards).

---

## Suggested Timeline (Solo Developer, Rough Guide)

| Phase | Focus | Relative Effort |
|---|---|---|
| 0 | Project Setup | Small |
| 1 | Entities & DB | Small–Medium |
| 2 | Auth & JWT | Medium |
| 3 | Employee/Department | Medium–Large |
| 4 | Leave Management | Medium |
| 5 | Payroll | Medium |
| 6 | Document Management | Medium (file handling adds some complexity) |
| 7 | Announcements | Small |
| 8 | Dashboard | Medium |
| 9 | Hardening | Medium |
| 10 | Testing & Docs | Small–Medium |

**Critical path:** Phases 0–3 must be done in order (everything depends on Auth + Employee). Phases 4, 5, 6, 7 are largely independent of each other once Phase 3 is done, and can be reordered or parallelized. Phase 8 (Dashboard) depends on 4–7 being complete since it aggregates their data.

**What changed vs. the original plan:** the old Phase 5 (Attendance Management) is removed entirely; Payroll and Document management shift up to fill that slot, with Document Management (Phase 6) replacing Attendance as the "operational tracking" module — file upload/storage is the main new technical concern to plan for, everything else (role-scoped CRUD, DTOs, validation) follows the same pattern as the other modules.
