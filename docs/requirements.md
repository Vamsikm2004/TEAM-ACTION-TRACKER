# Team Action Tracker — Requirements Document

## 1. Project Overview

The Team Action Tracker is a web-based application designed to help project teams create, assign, update, and monitor action items arising from meetings.

The application supports two user roles: Manager and Team Member. Managers can create and manage action items, while Team Members can view their assigned tasks and update their progress.

The application consists of a Vue.js frontend, a Java Spring Boot REST backend, and a MySQL relational database.

## 2. Functional Requirements

### 2.1 User Management

- The system shall support two roles: Manager and Team Member.
- Team Members shall be able to view action items assigned to them.
- Team Members shall be able to update the progress of their assigned action items.
- Managers shall be able to create action items.
- Managers shall be able to assign action items to Team Members.
- Managers shall be able to edit action items.
- Managers shall be able to close and reopen action items.
- Predefined test users may be used for authentication during this bounded exercise.

### 2.2 Action-Item Management

- The system shall allow Managers to create action items and assign them to Team Members.
- Each action item shall include a title, description, assignee, priority, status, and due date.
- Users shall be able to add progress comments to action items.
- Authorized users shall be able to edit the fields permitted by their role.
- Managers shall be able to close or reopen action items.
- The system shall maintain an update history for action items.

The supported statuses are:

- `NOT_STARTED`
- `IN_PROGRESS`
- `BLOCKED`
- `COMPLETED`

### 2.3 Dashboard

The dashboard shall display:

- Total action items.
- Open action items.
- Completed action items.
- Overdue action items.
- Action items grouped by status.
- Action items assigned to the signed-in user.

### 2.4 Search and Filtering

The system shall support filtering by:

- Assignee.
- Status.
- Priority.
- Due-date range.

The system shall also support sorting and pagination.

### 2.5 Validation and Business Rules

- Title, assignee, priority, and due date shall be mandatory.
- The due date shall not be earlier than the creation date.
- Only authorized users shall be allowed to create or reassign action items.
- A completion date shall be recorded when an action item is completed.
- Every status change shall create a history record.
- Invalid requests shall return meaningful validation messages.

## 3. Non-Functional Requirements

### 3.1 Security

- The application shall restrict functionality based on the user's role.
- Only authorized users shall be allowed to create or reassign action items.
- Users shall only perform actions permitted for their role.
- Sensitive configuration, including database credentials, shall not be committed to source control.

### 3.2 Performance

- The application should provide responsive user interactions under normal usage.
- API requests should return within a reasonable time under the expected development workload.
- Database queries should retrieve only the data required for the requested operation.

### 3.3 Reliability

- The application shall handle invalid requests without crashing.
- Meaningful error responses shall be returned when an operation cannot be completed.
- Database operations shall maintain data consistency.

### 3.4 Maintainability

- The backend shall follow a layered architecture consisting of controllers, services, and data-access components.
- Frontend components shall be organized according to their responsibilities.
- The codebase shall follow consistent naming and coding conventions.
- Configuration shall be separated from application logic where appropriate.

### 3.5 Testability

- Backend business logic shall be covered by unit tests where applicable.
- REST APIs shall have integration test coverage for important workflows.
- Important frontend components shall be testable.
- Tests shall be executable using documented project commands.

### 3.6 Usability

- The user interface shall provide clear navigation and understandable labels.
- Validation errors shall be presented clearly.
- Task status, priority, due date, and assignee shall be visible to users.

### 3.7 Logging and Monitoring

- The backend shall log important application events and errors.
- Logs shall provide sufficient information to investigate failures without exposing sensitive information.
- Application health shall be verifiable through a health-check endpoint.

## 4. Assumptions

- The application will support two roles: Manager and Team Member.
- Predefined test users may be used for authentication during this bounded exercise.
- MySQL will be used as the relational database.
- Vue.js will be used for the frontend.
- Java Spring Boot will be used for the backend REST API.
- REST APIs will provide communication between the frontend and backend.
- The application will initially run in a local development environment.
- Team Members can view and update action items assigned to them.
- Managers have the required permissions to create, assign, edit, close, and reopen action items.
- Action-item history will be retained for status changes.
- Physical deletion of action items is outside the mandatory scope unless explicitly required.
- Enterprise single sign-on and production-grade authentication are outside the bounded exercise.

## 5. Dependencies

### 5.1 Development Environment

- Java 21 or a compatible supported Java version.
- Node.js and npm for frontend development.
- Git for source control.
- Visual Studio Code or another supported IDE.

### 5.2 Backend Dependencies

- Spring Boot for backend application development.
- Spring Web for REST API development.
- Spring Data JPA for database access.
- MySQL JDBC Driver for database connectivity.
- Jakarta Bean Validation for request validation.
- JUnit and Spring Boot testing support for backend tests.

### 5.3 Frontend Dependencies

- Vue.js for the user interface.
- Vite for frontend development and build tooling.
- The browser Fetch API or Axios for REST API communication.

### 5.4 Database

- MySQL Server for relational data storage.
- MySQL Workbench or another SQL client for database administration and verification.

### 5.5 Supporting Tools

- Postman or an equivalent REST API client for API testing.
- draw.io or an equivalent diagramming tool for architecture, ER, and user-flow diagrams.
- GitHub or an approved remote Git repository for source-code hosting.

## 6. Out of Scope

The following items are outside the mandatory scope:

- Enterprise Single Sign-On (SSO) integration.
- Integration with corporate identity providers.
- Mobile application development.
- Email, SMS, or push notifications.
- Real-time collaboration or WebSocket-based updates.
- Integration with external project-management systems.
- Advanced reporting and business intelligence dashboards.
- File and document attachments to action items.
- Production cloud infrastructure and enterprise deployment.
- Automated deployment pipelines unless specifically required.
- Multi-organization or multi-tenant support.
- Advanced user administration beyond the required roles.
- Permanent deletion and recovery workflows for action items unless required later.

## 7. Clarification Questions

The following questions require confirmation before the related functionality is finalized.

| ID | Clarification Question | Area Affected |
|---|---|---|
| Q-01 | Can a Team Member edit the title and description of an assigned action item, or can they only update its status and add comments? | Authorization |
| Q-02 | Can a Team Member change an action item from Blocked back to In Progress? | Status workflow |
| Q-03 | Can only Managers reopen a completed action item? | Authorization / Status workflow |
| Q-04 | Should action items be permanently deleted, or should they only be closed and reopened? | Data management |
| Q-05 | Can comments be edited or deleted after submission? | Comments |
| Q-06 | Which authentication mechanism is expected: predefined test users, basic authentication, JWT, or another approach? | Authentication |
| Q-07 | Is pagination required in the backend API, frontend UI, or both? | API / UI |
| Q-08 | What should happen when an action item's due date has passed but the item remains open? | Business rules |
| Q-09 | Can Managers view and modify all action items, including those assigned to other Managers? | Authorization |
| Q-10 | Are there specific coding standards, repository conventions, or branching requirements to follow? | Development process |

### 7.1 Decision Tracking

Record the decisions received from the reviewer or manager in the table below.

| Question ID | Decision | Date | Confirmed By |
|---|---|---|---|
| Q-01 | Pending confirmation | — | — |
| Q-02 | Pending confirmation | — | — |
| Q-03 | Pending confirmation | — | — |
| Q-04 | Pending confirmation | — | — |
| Q-05 | Pending confirmation | — | — |
| Q-06 | Pending confirmation | — | — |
| Q-07 | Pending confirmation | — | — |
| Q-08 | Pending confirmation | — | — |
| Q-09 | Pending confirmation | — | — |
| Q-10 | Pending confirmation | — | — |

## 8. Requirements Review and Acceptance

Before considering the requirements finalized:

- Confirm the role permissions and action-item status workflow.
- Confirm the expected authentication mechanism.
- Confirm the required dashboard, filtering, sorting, and pagination behavior.
- Review the requirements with the reviewer or manager.
- Record confirmed decisions and any changes to scope.
- Verify that implementation and testing evidence align with the agreed requirements.