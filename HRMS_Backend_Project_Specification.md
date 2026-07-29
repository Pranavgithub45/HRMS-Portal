# HRMS Portal Backend Project Specification (Updated)

> **Change Log (this revision):**
> - ❌ **Attendance Management** feature removed completely (entity, endpoints, workflows, dashboard items, role permissions).
> - ✅ **Leave Management** feature retained as-is.
> - ✅ New **Document Management** feature added — stores employee documents (e.g. a PDF or a scanned photo of an ID like Aadhar). Scope for now: one document per employee, designed so it can be extended to multiple documents later.

------------------------------------------------------------------------

## Project Overview

Develop a **Spring Boot Backend** for a Human Resource Management System
(HRMS).

The project focuses on employee management, leave approval workflow,
payroll management, department management, employee document
management, announcements, JWT authentication, and role-based access.

The backend will be tested using **Postman**. No frontend is required.

------------------------------------------------------------------------

# Tech Stack

-   Java 21
-   Spring Boot 3.x
-   Maven
-   Spring Web
-   Spring Data JPA
-   MySQL
-   Lombok
-   Validation
-   JWT Authentication (Without Spring Security)
-   BCrypt Password Hashing
-   MapStruct
-   Global Exception Handling

------------------------------------------------------------------------

# User Roles

## Employee

-   Login
-   View Profile
-   Update Profile
-   Apply Leave
-   View Leave Status
-   View Payroll
-   View Announcements
-   Upload/View Own Document

## Manager

Everything Employee can do, plus: - View Team Members - Approve Leave
Requests - Reject Leave Requests - View Team Leave History - View Team
Documents

## HR/Admin

Everything Manager can do, plus: - Manage Employees - Manage
Departments - Generate Payroll - Create Announcements - Manage
(Upload/Update/Delete) Documents For Any Employee - View Complete
Dashboard

------------------------------------------------------------------------

# Database Entities

## 1. Department

### Fields

Column               Type
  -------------------- ---------
department_id (PK)   BIGINT
department_name      VARCHAR
location             VARCHAR
description          TEXT

### Relationship

-   One Department → Many Employees

------------------------------------------------------------------------

## 2. Employee

### Fields

Column                         Type
  ------------------------------ -----------------------------
employee_id (PK)               BIGINT
name                           VARCHAR
email (Unique)                 VARCHAR
password                       VARCHAR
phone                          VARCHAR
designation                    VARCHAR
join_date                      DATE
role                           ENUM(EMPLOYEE, MANAGER, HR)
status                         ENUM(ACTIVE, INACTIVE)
department_id (FK)             BIGINT
manager_id (FK -\> Employee)   BIGINT (Nullable)

### Relationship

-   Many Employees belong to one Department.
-   One Manager manages many Employees (Self Reference).

------------------------------------------------------------------------

## 3. LeaveRequest

### Fields

Column                          Type
  ------------------------------- -----------------------------------
leave_id (PK)                   BIGINT
employee_id (FK)                BIGINT
leave_type                      VARCHAR
start_date                      DATE
end_date                        DATE
reason                          TEXT
status                          ENUM(PENDING, APPROVED, REJECTED)
applied_date                    DATE
approved_by (FK -\> Employee)   BIGINT
remarks                         TEXT

### Relationship

-   One Employee → Many Leave Requests

------------------------------------------------------------------------

## 4. Payroll

### Fields

Column             Type
  ------------------ ---------------
payroll_id (PK)    BIGINT
employee_id (FK)   BIGINT
month              INT
year               INT
base_salary        DECIMAL(10,2)
bonus              DECIMAL(10,2)
net_salary         DECIMAL(10,2)
generated_date     DATE

### Business Logic

Net Salary = Base Salary + Bonus

Rules: - One payroll per employee per month. - Only HR/Admin can
generate payroll. - Employees can only view their own payroll.

------------------------------------------------------------------------

## 5. Announcement

### Fields

Column                         Type
  ------------------------------ ---------
announcement_id (PK)           BIGINT
title                          VARCHAR
description                    TEXT
created_by (FK -\> Employee)   BIGINT
created_date                   DATE
expiry_date                    DATE

### Business Logic

-   Only HR/Admin can create, update or delete announcements.
-   All users can view active announcements.
-   Expired announcements are not returned.

------------------------------------------------------------------------

## 6. Document *(NEW — replaces Attendance)*

### Fields

Column                Type
  ---------------------- ---------------------------------------
document_id (PK)       BIGINT
employee_id (FK)       BIGINT
document_type          VARCHAR *(e.g. AADHAR, PAN, RESUME, OFFER_LETTER, OTHER)*
document_name          VARCHAR *(display name / original file name)*
file_url               VARCHAR *(path or storage URL of the uploaded PDF/image)*
file_format            VARCHAR *(e.g. PDF, JPG, PNG)*
uploaded_date          DATE
uploaded_by (FK -\> Employee)   BIGINT *(who uploaded it — self or HR)*
remarks                TEXT *(optional notes)*

### Relationship

-   One Employee → Many Documents (schema supports multiple documents
    per employee going forward; **current scope: one document per
    employee** — e.g. a single Aadhar card PDF/photo).

### Business Logic

-   An employee can upload/view/replace **their own** document.
-   HR/Admin can upload, update, delete, or view **any** employee's
    document.
-   Manager can **view** (read-only) documents of their team members,
    but cannot upload/edit/delete on their behalf.
-   File itself (PDF/image) is stored on disk/cloud storage; the
    database stores only metadata + `file_url` reference.
-   Accepted formats for now: PDF, JPG, PNG (validate on upload).
-   For now, one document record represents one file per employee
    (e.g. Aadhar); re-uploading updates/replaces the existing record
    rather than creating a duplicate. This can later be extended to
    support multiple document types per employee without a schema
    change (the `document_type` field already allows it).

------------------------------------------------------------------------

# Entity Relationship Mapping

    Department (1)
          |
          |------< Employee (Many)
                        |
                        | Self Reference
                        | manager_id
                        |
              ----------------------------
              |            |            |
              |            |            |
         LeaveRequest    Payroll     Document
              |
     approved_by (Employee)

    Employee (HR/Admin)
            |
            |------< Announcement

------------------------------------------------------------------------

# Core Workflows

## Authentication

-   Register/Login
-   BCrypt password hashing
-   JWT generation
-   Role-based authorization

## Leave Workflow

1.  Employee applies for leave.
2.  Status = PENDING.
3.  Manager approves/rejects.
4.  Employee views updated status.

## Payroll Workflow

1.  HR generates payroll.
2.  Net Salary = Base Salary + Bonus.
3.  Employee views payroll.

## Announcement Workflow

1.  HR creates announcement.
2.  Announcement is visible to all users.
3.  Announcement expires automatically after expiry date.

## Document Workflow *(NEW)*

1.  Employee (or HR on their behalf) uploads a document (PDF/image),
    e.g. Aadhar card.
2.  Document metadata + file reference is stored against the
    employee.
3.  Employee can view/replace their own document at any time.
4.  Manager can view documents of team members (read-only).
5.  HR can view/manage documents of any employee.

------------------------------------------------------------------------

# Dashboard

## Employee Dashboard

-   My Profile
-   My Leave Requests
-   My Payroll
-   My Document Status *(e.g. Uploaded / Not Uploaded)*
-   Latest Announcements

## Manager Dashboard

-   Team Members
-   Pending Leave Requests
-   Approved Leaves
-   Rejected Leaves
-   Team Document Status *(who has/hasn't uploaded required documents)*
-   Latest Announcements

## HR/Admin Dashboard

-   Total Employees
-   Total Departments
-   Pending Leave Requests
-   Approved Leave Requests
-   Payroll Generated This Month
-   Department-wise Employee Count
-   Employees Joined This Month
-   Employees Missing Required Documents
-   Latest Announcements

------------------------------------------------------------------------

# Suggested REST Modules

-   Authentication
-   Employee Management
-   Department Management
-   Leave Management
-   Payroll Management
-   Announcement Management
-   Document Management *(NEW)*
-   Dashboard

> Note: **Attendance Management** module has been removed from this
> revision.
