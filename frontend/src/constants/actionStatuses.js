export const ACTION_STATUSES = [
  { value: 'NOT_STARTED', label: 'Not started' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'BLOCKED', label: 'Blocked' },
  { value: 'COMPLETED', label: 'Completed' }
]

export const ACTION_STATUS_VALUES = ACTION_STATUSES.map(
  status => status.value
)
