# Team Action Tracker

A full-stack application for managing team action items, assigning responsibilities, tracking priorities and statuses, and maintaining comments and activity history.

## Technology Stack

- Backend: Java 21, Spring Boot, Maven, Spring Data JPA, Spring Security
- Frontend: Vue 3, JavaScript, Vite, Vue Router
- Database: MySQL 8
- Migrations: Flyway
- Testing: JUnit, Vitest, Vue Test Utils, Playwright
- Version control: Git and GitHub

## Project Structure

text
Team Action Tracker/
├── backend/                 # Spring Boot REST API
├── frontend/                # Vue application
├── backups/                 # Database backups
├── database/                # Database scripts and migration notes
├── docs/                    # Requirements, design, API and progress
└── README.md

## Prerequisites

- Java 21
- MySQL 8
- Node.js and npm
- Git

Verify the installations:

powershell
java -version
node --version
npm --version
git --version

## Database Setup

1. Start MySQL.
2. Create the database if it does not already exist:

sql
CREATE DATABASE IF NOT EXISTS team_action_tracker;

3. Set the `DB_PASSWORD` environment variable in the terminal used to run the backend:

powershell
$env:DB_PASSWORD = Read-Host "Enter your MySQL password"

4. Check `backend/src/main/resources/application.properties` for the JDBC URL and username.
5. Flyway migrations are located in `backend/src/main/resources/db/migration/`.

Do not drop or recreate a database containing data you need to preserve. Keep database backups secure.

## Run the Backend

Open PowerShell:

powershell
cd "C:\Team Action Tracker\Backend"
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_PASSWORD = Read-Host "Enter your MySQL password"
.\mvnw.cmd spring-boot:run

Adjust `JAVA_HOME` if Java is installed elsewhere. The password is entered interactively and should never be committed to source control.

The backend normally runs at `http://localhost:8080`.

Health endpoint:

`http://localhost:8080/api/health`

Keep the backend terminal open while using the frontend.

## Run the Frontend

Open a separate PowerShell terminal:

powershell
cd "C:\Team Action Tracker\frontend"
npm install
npm run dev

Open the local URL printed by Vite, normally `http://localhost:5173`.

The frontend API base URL is configured through environment variables:

- Development: `.env.development`
- Production example: `.env.production.example`
- Variable: `VITE_API_BASE_URL`
- Default local API URL: `http://localhost:8080/api`

Ensure the backend is running and the configured API URL matches your environment. Do not commit local `.env` files containing private configuration.

## Application Features

The current frontend implementation includes:

- Session-based login and CSRF handling
- Login and dashboard routes
- Dashboard counts for action statuses and overdue items
- Action-item search, filtering, sorting and pagination
- Status updates
- Action history viewing
- Comments viewing and adding
- Manager-only user and action-item creation

Access depends on the authenticated user's role and backend authorization rules.

## API Endpoints

The API is served under `/api`. Authentication, authorization, and CSRF protection may apply.

| Method | Endpoint | Permission | Purpose |
|---|---|---|---|
| GET | `/api/health` | Public | Backend health check |
| GET | `/api/auth/csrf` | Public | Obtain a CSRF token |
| POST | `/api/auth/login` | Public | Sign in |
| GET | `/api/auth/me` | Authenticated | Retrieve the current authenticated user |
| POST | `/api/auth/logout` | Public | Sign out |
| GET | `/api/users` | Manager | Retrieve active users |
| POST | `/api/users` | Manager | Create a user |
| GET | `/api/action-items` | Manager / Team Member | Retrieve permitted action items |
| GET | `/api/action-items/{id}` | Manager / Assigned Team Member | Retrieve one permitted action item |
| POST | `/api/action-items` | Manager | Create an action item |
| PUT | `/api/action-items/{id}` | Manager | Update an action item |
| PATCH | `/api/action-items/{id}/status` | Manager / Assigned Team Member | Update status |
| GET | `/api/action-items/{id}/history` | Manager / Assigned Team Member | Retrieve permitted action history |
| GET | `/api/comments?actionItemId={id}` | Manager / Assigned Team Member | Retrieve permitted action comments |
| POST | `/api/comments` | Manager / Assigned Team Member | Add a permitted action-item comment |

Confirm the actual controller mappings if an endpoint returns `404 Not Found`.

Swagger UI, if enabled, is normally available at:

`http://localhost:8080/swagger-ui/index.html`

## Testing and Production Build

The results below are historical results from runs performed before the latest Week 4 changes. Run the commands again to establish fresh results for the current code.

### Backend tests

Run from the backend directory:

powershell
cd "C:\Team Action Tracker\Backend"
.\mvnw.cmd test

Previously recorded verification results:

- Tests: 39
- Failures: 0
- Errors: 0

### Frontend unit tests

Run from the frontend directory:

powershell
cd "C:\Team Action Tracker\frontend"
npm test -- --pool=forks --maxWorkers=1

The single-worker forks configuration was previously used after the default Vitest worker pool encountered a startup timeout.

**Previously recorded verification results:**

- Tests: 2 passed
- Login screen rendering
- Required-field validation

### End-to-end tests

Ensure the backend is running before executing the tests.

Install Chromium if needed:

powershell
cd "C:\Team Action Tracker\frontend"
npx playwright install chromium

Run the Playwright tests:

powershell
npm run test:e2e

**Previously recorded verification results:**

- Tests: 2 passed
- Login screen displays required fields.
- Invalid login stays on the login screen.

These tests cover the listed login scenarios. They do not verify the complete action-item workflow.

### Production build

Run from the frontend directory:

powershell
cd "C:\Team Action Tracker\frontend"
npm run build

The production build was successful during a previous verification. Vite generates the output in `frontend/dist/`.

## End-to-End Acceptance Checklist

Before declaring the application fully accepted, validate the following workflow using appropriate test accounts:

- [ ] Log in with valid credentials.
- [ ] Verify dashboard counts.
- [ ] Create an action item and assign it to a user.
- [ ] Search for the action item.
- [ ] Test available filters, sorting, and pagination.
- [ ] Update the action item's status.
- [ ] Add a comment and view action history.
- [ ] Refresh the page and verify that changes persist.
- [ ] Confirm Manager and Team Member restrictions.
- [ ] Verify validation messages for invalid input.
- [ ] Check loading, empty, and error states.
- [ ] Verify logout and protected-route behavior.

Compare the displayed data with the database where practical. Record the results of these checks before declaring full acceptance.

## Documentation

Requirements, high-level design, database design, API specifications, diagrams, proposals, and progress notes are maintained in `docs/` and the backend documentation folders.

Keep these documents aligned with the current implementation and verified behavior.

## Security Notes

- Keep passwords, tokens, and other secrets out of source control.
- Do not commit `.env` files containing credentials.
- Do not expose database credentials in frontend code.
- Use authorized test accounts and non-production data during validation.
- Enforce authorization on the backend, not only in the frontend.
- Preserve CSRF protection for applicable session-authenticated requests.
- Protect database backups because they may contain sensitive information.
- Use appropriately restricted database accounts in production.
- Review database migration and Hibernate schema-validation settings before production deployment.
- Do not commit `node_modules`, generated build output, or generated test reports unless explicitly required.

## Known Limitations

- The previously recorded automated end-to-end tests cover login-screen rendering and invalid-login behavior.
- The complete workflow involving action creation, assignment, status updates, comments, history, and persistence still requires acceptance testing.
- Role restrictions and all search, filtering, sorting, and pagination scenarios require functional validation beyond the previously recorded tests.
- The frontend unit-test suite previously contained only two tests, so broader component coverage remains desirable.
- Swagger/OpenAPI availability depends on the backend configuration.
- The application uses local development configuration and requires appropriate security and deployment review before production use.
- The two improvement proposals are awaiting review and approval; neither should be treated as approved or implemented until formally selected.

## Troubleshooting

### MySQL access denied

If the backend reports `Access denied for user 'root'@'localhost'`, verify that the password entered for `DB_PASSWORD` is correct.

Test the credentials using the MySQL client:

powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p

If the password is accepted, enter the same password when setting `DB_PASSWORD` in the backend terminal.

### Backend cannot connect to MySQL

Check that:

- The MySQL service is running.
- The `team_action_tracker` database exists.
- The JDBC URL and username are correct.
- `DB_PASSWORD` is set in the backend terminal.
- The configured MySQL port is correct.

### Frontend cannot reach the backend

Check that:

- The backend is running on port `8080`.
- `VITE_API_BASE_URL` points to the correct API base URL.
- The browser developer console and Network panel show no unexpected errors.
- Authentication cookies and CSRF tokens are handled correctly.

A `401 Unauthorized` or `403 Forbidden` response may indicate an authentication, authorization, or CSRF issue rather than a connectivity failure.

### Vitest worker startup timeout

If the default frontend test command times out while starting a worker, run:

powershell
npm test -- --pool=forks --maxWorkers=1

This configuration passed during a previous verification.

### Port already in use

If a port is occupied, identify the process using it before stopping anything. Avoid terminating unrelated processes.

## Project Status

| Verification | Status |
|---|---|
| Backend tests | Historical result: 39 tests; 0 failures; 0 errors |
| Frontend unit tests | Historical result: 2 passed |
| Playwright end-to-end tests | Historical result: 2 passed |
| Frontend production build | Historical result: successful |
| Fresh Week 4 regression tests | Pending |
| Full workflow acceptance | Pending confirmation |
| Improvement proposal approval | Pending |
| Final handover | Pending |

The project should be considered fully accepted only after the approved improvement, fresh regression testing, functional and role-based acceptance checks, and handover activities have been completed and documented.
