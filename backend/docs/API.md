\# Team Action Tracker — API Reference



Base URL: `http://localhost:8080`



All API routes below use the `/api` prefix.



\## 1. Authentication



| Method | Endpoint | Permission | Description |

|---|---|---|---|

| GET | `/api/auth/csrf` | Public | Obtain CSRF token |

| POST | `/api/auth/login` | Public | Authenticate user |

| GET | `/api/auth/me` | Authenticated | Retrieve current authenticated user |

| POST | `/api/auth/logout` | Public | Invalidate current session |



Authentication uses server-managed sessions and CSRF protection.



For state-changing requests, obtain the CSRF token and send it using the header name returned by the CSRF endpoint. Maintain the session cookie across requests.



\## 2. Health



| Method | Endpoint | Permission | Description |

|---|---|---|---|

| GET | `/api/health` | Public | Check application health |



\## 3. Users



| Method | Endpoint | Permission | Description |

|---|---|---|---|

| POST | `/api/users` | Manager | Create user |

| GET | `/api/users` | Manager | Retrieve active users |



\### Create user



`POST /api/users`



Example request:



json

{

&#x20; "name": "Sample User",

&#x20; "email": "sample@example.com",

&#x20; "password": "ExamplePassword123",

&#x20; "role": "TEAM\_MEMBER"

}



Validation:

\- Name: required, maximum 100 characters.

\- Email: required, valid email, maximum 150 characters.

\- Password: required, 12–72 characters.

\- Role: MANAGER or TEAM\_MEMBER.



Responses:

\- `201 Created` — user created.

\- `400 Bad Request` — invalid request.

\- `401 Unauthorized` — authentication required.

\- `403 Forbidden` — insufficient permissions.

\- `409 Conflict` — email already exists.



\### Get active users



`GET /api/users`



Returns active users.



Responses: `200`, `401`, `403`.



\## 4. Action Items



| Method | Endpoint | Permission | Description |

|---|---|---|---|

| GET | `/api/action-items` | Manager / Team Member | List accessible action items |

| GET | `/api/action-items/{id}` | Manager / Assigned Team Member | Retrieve action item |

| POST | `/api/action-items` | Manager | Create action item |

| PUT | `/api/action-items/{id}` | Manager | Update action item |

| PATCH | `/api/action-items/{id}/status` | Manager / Assigned Team Member | Update action-item status |

| GET | `/api/action-items/{id}/history` | Manager / Assigned Team Member | Retrieve status history |



Team members can access action items assigned to them, subject to backend authorization.



\### Create action item



`POST /api/action-items`



Example request:



json

{

&#x20; "title": "Prepare project status report",

&#x20; "description": "Prepare the weekly project status report.",

&#x20; "assigneeId": 2,

&#x20; "priority": "HIGH",

&#x20; "status": "NOT\_STARTED",

&#x20; "dueDate": "2026-12-15"

}



Validation:

\- Title: required, maximum 200 characters.

\- Description: optional, maximum 2,000 characters.

\- Assignee: required.

\- Priority: LOW, MEDIUM, HIGH, or CRITICAL.

\- Status: optional; allowed values are NOT\_STARTED, IN\_PROGRESS, BLOCKED, COMPLETED.

\- Due date: required and cannot be in the past.



\### Update action-item status



`PATCH /api/action-items/{id}/status`



Example request:



json

{

&#x20; "status": "IN\_PROGRESS",

&#x20; "comment": "Work has started."

}



Status update comments are optional and limited to 2,000 characters.



A status change creates an action-history entry.



\## 5. Comments



| Method | Endpoint | Permission | Description |

|---|---|---|---|

| GET | `/api/comments?actionItemId=1` | Manager / Assigned Team Member | Retrieve comments |

| POST | `/api/comments` | Manager / Assigned Team Member | Add comment |



\### Add comment



`POST /api/comments`



Example request:



json

{

&#x20; "actionItemId": 1,

&#x20; "comment": "The initial review is complete."

}



Validation:

\- Action-item ID: required and positive.

\- Comment: required, nonblank, maximum 5,000 characters.

\- User identity is derived from the authenticated session.



Responses:

\- `201 Created` — comment created.

\- `400 Bad Request` — invalid request.

\- `401 Unauthorized` — authentication required.

\- `403 Forbidden` — insufficient permissions.

\- `404 Not Found` — action item not found.



\## 6. Common HTTP Status Codes



| Code | Meaning |

|---|---|

| 200 | Successful request |

| 201 | Resource created |

| 204 | Request successful, no response body |

| 400 | Invalid request or validation failure |

| 401 | Authentication required or invalid credentials |

| 403 | Insufficient permissions |

| 404 | Resource not found |

| 405 | HTTP method not supported |

| 409 | Duplicate resource conflict |

| 500 | Unexpected server error |



\## 7. API Documentation Interface



During local development:



\- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

\- OpenAPI JSON: `http://localhost:8080/v3/api-docs`



Swagger and OpenAPI endpoints are disabled in the production profile.



\## 8. Security Notes



\- Authentication uses server-managed sessions.

\- CSRF protection applies to state-changing requests.

\- Backend authorization is enforced independently of frontend routing.

\- Passwords are hashed using BCrypt.

\- Sensitive credentials must not be included in API examples or committed to source control.



Note: Verify exact response schemas and status codes against the controllers and service implementations before treating this reference as a final contract.








