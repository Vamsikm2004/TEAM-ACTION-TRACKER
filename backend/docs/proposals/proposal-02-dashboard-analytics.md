\# Proposal 02: Dashboard Analytics and Team Performance



Status: Proposed — Awaiting Review



\## 1. Problem Statement



Managers need better visibility into team workload, action-item progress, overdue tasks, and blocked work.



\## 2. Evidence



The application already maintains:

\- Action-item status.

\- Priority.

\- Assignee information.

\- Due dates.

\- Status history.



The dashboard displays basic action-item counts, but there is an opportunity to provide more useful consolidated analytics.



\## 3. Business Impact



\- Improves workload visibility.

\- Helps managers identify bottlenecks.

\- Supports better task allocation.

\- Makes progress reviews more effective.



\## 4. Proposed Solution



Enhance the dashboard with:

\- Total action-item count.

\- Completed, pending, in-progress and blocked counts.

\- Overdue task metrics.

\- Workload distribution by team member.

\- Priority-wise action-item distribution.



Use existing backend data and aggregation logic wherever possible.



\## 5. Dependencies



\- Existing action-item and user repositories.

\- Backend aggregation endpoints or service methods.

\- Frontend dashboard components.

\- Consistent status and priority definitions.



\## 6. Risks



\- Incorrect aggregation may produce misleading metrics.

\- Large datasets may affect dashboard performance.

\- Metrics must respect user authorization.

\- Additional queries may increase database load.



\## 7. Expected Outcome



A more informative dashboard that helps managers understand team performance and take timely action.



\## 8. Approval



Approval status: Awaiting manager review.



Implementation status: Not started.

