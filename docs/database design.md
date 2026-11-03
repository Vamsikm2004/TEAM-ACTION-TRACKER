# Team Action Tracker — Database Design

## 1. Users Table

The `users` table stores the users who can access and interact with the Team Action Tracker.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `user_id` | BIGINT | Primary Key, Auto Increment | Unique identifier for the user |
| `name` | VARCHAR(100) | NOT NULL | User's display name |
| `email` | VARCHAR(150) | NOT NULL, UNIQUE | User's email address |
| `password_hash` | VARCHAR(100) | NULL | BCrypt password hash used for authentication |
| `role` | VARCHAR(30) | NOT NULL | User role: MANAGER or TEAM_MEMBER |
| `active_status` | BIT(1) | NOT NULL | Indicates whether the user is active |
| `created_at` | DATETIME(6) | NOT NULL | Date and time when the user was created |
| `updated_at` | DATETIME(6) | NOT NULL | Date and time when the user was last updated |

### User Roles

The application supports two roles:

- `MANAGER`
- `TEAM_MEMBER`

The `user_id` is referenced by other tables whenever an action item, comment, or history record is associated with a user.

## 2. Action Items Table

The `action_items` table stores tasks or actions that need to be completed by team members.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `action_item_id` | BIGINT | Primary Key, Auto Increment | Unique identifier for the action item |
| `title` | VARCHAR(200) | NOT NULL | Short title of the action item |
| `description` | TEXT | NULL | Detailed description of the action item |
| `assignee_id` | BIGINT | NOT NULL, Foreign Key | User responsible for completing the action item |
| `priority` | VARCHAR(20) | NOT NULL | Priority of the action item |
| `status` | VARCHAR(30) | NOT NULL | Current status of the action item |
| `due_date` | DATE | NOT NULL | Date by which the action item should be completed |
| `created_by` | BIGINT | NOT NULL, Foreign Key | User who created the action item |
| `created_at` | DATETIME(6) | NOT NULL | Date and time when the action item was created |
| `updated_at` | DATETIME(6) | NOT NULL | Date and time when the action item was last updated |
| `completed_at` | DATETIME(6) | NULL | Date and time when the action item was completed |

### Priority Values

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

### Status Values

- `NOT_STARTED`
- `IN_PROGRESS`
- `BLOCKED`
- `COMPLETED`

### Relationships

- `assignee_id` references `users.user_id`.
- `created_by` references `users.user_id`.
- One user may be assigned multiple action items.
- One user may create multiple action items.

### Business Rules

- Every action item must have an assignee and a due date.
- The due date must not be earlier than the creation date.
- Only authorized users can create or reassign action items.
- When an action item changes to `COMPLETED`, `completed_at` must be populated.
- When a completed action item is reopened, `completed_at` must be cleared.

## 3. Action Comments Table

The `action_comments` table stores progress updates and comments added to action items by users.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `comment_id` | BIGINT | Primary Key, Auto Increment | Unique identifier for the comment |
| `action_item_id` | BIGINT | NOT NULL, Foreign Key | Action item associated with the comment |
| `user_id` | BIGINT | NOT NULL, Foreign Key | User who added the comment |
| `comment` | TEXT | NOT NULL | Content of the progress comment |
| `created_at` | DATETIME(6) | NOT NULL | Date and time when the comment was added |

### Relationships

- `action_item_id` references `action_items.action_item_id`.
- `user_id` references `users.user_id`.
- One action item can have multiple comments.
- One user can add comments to multiple action items.

### Business Rules

- Every comment must be associated with an existing action item.
- A comment must contain a non-empty message.
- The user adding a comment must be authenticated.
- Users may add comments only when permitted to access the associated action item.

## 4. Action History Table

The `action_history` table maintains an audit trail of important status changes made to action items.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `history_id` | BIGINT | Primary Key, Auto Increment | Unique identifier for the history record |
| `action_item_id` | BIGINT | NOT NULL, Foreign Key | Action item associated with the history entry |
| `previous_status` | VARCHAR(30) | NULL | Status before the change |
| `new_status` | VARCHAR(30) | NOT NULL | Status after the change |
| `comment` | TEXT | NULL | Optional explanation for the change |
| `updated_by` | BIGINT | NOT NULL, Foreign Key | User who made the status change |
| `updated_at` | DATETIME(6) | NOT NULL | Date and time of the status change |

### Relationships

- `action_item_id` references `action_items.action_item_id`.
- `updated_by` references `users.user_id`.
- One action item can have multiple history records.
- One user can create multiple history records.

### Business Rules

- A history record must be created whenever an action item's status changes.
- Each record must identify the previous status and the new status.
- Each record must identify the user who performed the change.
- History records should be retained and must not be modified during normal task updates.

## 5. Entity Relationships

The primary entity relationships are:

text
USERS
  |
  |---- 1 : Many ---- ACTION_ITEMS
  |                       |
  |                       |---- 1 : Many ---- ACTION_COMMENTS
  |                       |
  |                       |---- 1 : Many ---- ACTION_HISTORY
  |
  |---- Creates ACTION_ITEMS through created_by
  |
  |---- Assigned to ACTION_ITEMS through assignee_id
  |
  |---- Creates ACTION_COMMENTS through user_id
  |
  |---- Creates ACTION_HISTORY through updated_by

### Relationship Summary

| Relationship | Cardinality | Foreign Key |
|---|---|---|
| Users to assigned action items | One-to-Many | `action_items.assignee_id` |
| Users to created action items | One-to-Many | `action_items.created_by` |
| Action items to comments | One-to-Many | `action_comments.action_item_id` |
| Users to comments | One-to-Many | `action_comments.user_id` |
| Action items to history | One-to-Many | `action_history.action_item_id` |
| Users to history records | One-to-Many | `action_history.updated_by` |

## 6. Database Design Considerations

- Data integrity: Primary keys and foreign keys maintain relationships between records.
- Uniqueness: The unique constraint on `users.email` prevents duplicate email addresses.
- Authentication: Passwords are stored as BCrypt hashes rather than plain text.
- Auditability: Creation, update, completion, and status-history fields support tracking changes.
- Access control: User roles support manager and team-member permissions enforced by the application.
- Validation: Required fields and business rules help maintain consistent application data.
- Query performance: Indexes support frequently used lookups and filtering operations.
- Database migrations: Flyway manages schema creation and versioned database changes.

## 7. Database Implementation Notes

- The database uses MySQL with the InnoDB storage engine.
- The character set is `utf8mb4`.
- Timestamps use `DATETIME(6)` for microsecond precision.
- Foreign-key constraints maintain relationships between the four tables.
- Flyway migration `V1__create_initial_schema.sql` creates the initial database schema.
- Flyway migration `V2__add_action_item_status_index.sql` adds an index for action-item status queries.
- Hibernate schema validation is used to verify the database schema against the application entities.