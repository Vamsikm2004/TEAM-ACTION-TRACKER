\# Team Action Tracker — Backend



A Spring Boot REST API for managing users, team action items, assignments, status updates, comments, and action history.



\## Technology Stack



\- Java 21

\- Spring Boot 4.1.1

\- Spring Security

\- Spring Data JPA

\- MySQL

\- Flyway Database Migrations

\- Bean Validation

\- Springdoc OpenAPI / Swagger UI

\- Maven



\## Project Structure



text

src/

├── main/

│   ├── java/com/teamactiontracker/backend/

│   │   ├── config/

│   │   ├── controller/

│   │   ├── dto/

│   │   ├── enums/

│   │   ├── exception/

│   │   ├── model/

│   │   ├── repository/

│   │   ├── service/

│   │   └── validation/

│   └── resources/

│       ├── db/migration/

│       ├── application.properties

│       └── application-prod.properties

└── test/

&#x20;   └── java/com/teamactiontracker/backend/



\## Prerequisites



\- Java 21

\- MySQL Server

\- Maven Wrapper (included)

\- A configured MySQL database



\## Database Setup



Create the database:



sql

CREATE DATABASE team\_action\_tracker;



Flyway manages the database schema through versioned SQL migrations.



Current migrations:



\- `V1\_\_create\_initial\_schema.sql` — initial database schema.

\- `V2\_\_add\_action\_item\_status\_index.sql` — action-item status index.



Do not modify migrations that have already been applied. Create a new versioned migration for subsequent schema changes.



\## Environment Configuration



The application supports the following environment variables:



| Variable | Local default / purpose |

|---|---|

| `DB\_URL` | `jdbc:mysql://localhost:3306/team\_action\_tracker` |

| `DB\_USERNAME` | `root` |

| `DB\_PASSWORD` | Database password; required |

| `APP\_FRONTEND\_URL` | Configure the allowed frontend origin if supported by the deployment configuration |



Never commit actual database passwords or production secrets.



\## Local Configuration



The default `application.properties` configuration uses:



\- Hibernate schema validation.

\- Flyway-managed database migrations.

\- SQL statement logging disabled.

\- Environment-based database credentials.



\## Production Configuration



Activate the production profile with:



bash

java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod



The production configuration:



\- Keeps Hibernate schema validation enabled.

\- Disables Open Session in View.

\- Disables Swagger UI and OpenAPI endpoints.

\- Disables Flyway baseline-on-migrate.



Configure database credentials and other deployment settings securely in the production environment.



\## Run the Application



On Windows PowerShell:



powershell

$env:DB\_URL="jdbc:mysql://localhost:3306/team\_action\_tracker"

$env:DB\_USERNAME="root"

$env:DB\_PASSWORD="your-local-database-password"



.\\mvnw.cmd spring-boot:run



The API normally runs at:



text

http://localhost:8080



\## API Documentation



During local development, Swagger UI is available at:



text

http://localhost:8080/swagger-ui/index.html



OpenAPI specification:



text

http://localhost:8080/v3/api-docs



These endpoints are disabled in the production profile.



\## Security



The backend uses Spring Security with session-based authentication.



Security features include:



\- Authenticated API access.

\- Role-based authorization for managers and team members.

\- CSRF protection for state-changing requests.

\- Session-based login and logout.

\- Backend-side ownership and assignment checks.

\- Centralized authentication and authorization error handling.

\- Password hashing using BCrypt.



Frontend route protection is not a substitute for backend authorization.



\## Application Features



\- User management.

\- Role-based access.

\- Action-item creation and assignment.

\- Action-item updates.

\- Status updates.

\- Action history.

\- Action comments.

\- Input and business validation.

\- Centralized exception handling.



\## Testing



Run backend tests:



powershell

.\\mvnw.cmd clean test



Package the application:



powershell

.\\mvnw.cmd clean package



\## Important Development Guidelines



\- Use Flyway migrations for database schema changes.

\- Keep Hibernate `ddl-auto` set to `validate`.

\- Keep credentials outside source control.

\- Enforce authorization in backend services and security configuration.

\- Use appropriate HTTP status codes for validation, authentication, authorization, and missing resources.

\- Add regression tests when changing business logic or security behavior.



\## Known Configuration Considerations



\- Production database connection settings must be supplied through the deployment environment.

\- The production frontend origin must be configured correctly for credentialed CORS requests.

\- Swagger and OpenAPI are intentionally disabled in production.

\- Deployment-specific secrets and URLs must not be committed.



\## Related Documentation



See the frontend README for frontend installation, environment configuration, and development instructions.

