<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { apiFetch } from '../services/api'
import { useAuth } from '../composables/useAuth'
import { useRouter } from 'vue-router'
import { ACTION_STATUSES } from '../constants/actionStatuses'

const STATUS = Object.freeze(
  Object.fromEntries(ACTION_STATUSES.map(status => [status.value, status.value]))
)


const router = useRouter()

// Form defaults and UI state used by the template and action handlers.
function createEmptyUser() {
  return {
    name: '',
    email: '',
    password: '',
    role: 'TEAM_MEMBER'
  }
}

function createEmptyAction() {
  return {
    title: '',
    description: '',
    assigneeId: '',
    priority: 'MEDIUM',
    dueDate: ''
  }
}

const newAction = ref(createEmptyAction())
const updatingActionId = ref(null)
const statusUpdateError = ref('')
const statusUpdateSuccess = ref('')

const selectedHistoryAction = ref(null)
const actionHistory = ref([])
const historyError = ref('')
const historyLoading = ref(false)

const selectedCommentAction = ref(null)
const actionComments = ref([])
const newCommentText = ref('')
const commentsError = ref('')
const commentsSuccess = ref('')
const commentsLoading = ref(false)
const commentsSaving = ref(false)


const {
  currentUser,
  logout: endSession,
  clearSession
} = useAuth()

function handleSessionExpired() {
  clearSession()
  actions.value = []
  users.value = []
  void router.replace({ name: 'login' })
}


const actions = ref([])
const users = ref([])
const usersLoading = ref(false)
const userListError = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const formError = ref('')
const formSuccess = ref('')
const showCreateForm = ref(false)
const editingActionId = ref(null)

const showCreateUserForm = ref(false)
const creatingUser = ref(false)
const userError = ref('')
const userSuccess = ref('')

const newUser = ref(createEmptyUser())

const search = ref('')
const statusFilter = ref('ALL')
const priorityFilter = ref('ALL')
const assigneeFilter = ref('ALL')
const dateFrom = ref('')
const dateTo = ref('')
const sortBy = ref('dueDate')
const sortDirection = ref('asc')
const currentPage = ref(1)
const pageSize = ref(10)

const openActions = computed(() =>
  actions.value.filter(a => a.status !== STATUS.COMPLETED).length
)

const myActions = computed(() => {
  const userId = Number(currentUser.value?.userId)
  if (!Number.isFinite(userId)) return 0

  return actions.value.filter(
    a => Number(a.assignee?.userId) === userId
  ).length
})
const total = computed(() => actions.value.length)

const completed = computed(() =>
  actions.value.filter(a => a.status === STATUS.COMPLETED).length
)

const inProgress = computed(() =>
  actions.value.filter(a => a.status === STATUS.IN_PROGRESS).length
)

const pending = computed(() =>
  actions.value.filter(a => a.status === STATUS.NOT_STARTED).length
)

function isOverdue(action) {
  if (!action.dueDate || action.status === STATUS.COMPLETED) {
    return false
  }

  const now = new Date()
  const today = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0')
  ].join('-')

  return action.dueDate < today
}

const overdue = computed(() =>
  actions.value.filter(isOverdue).length
)

const blocked = computed(() =>
  actions.value.filter(a => a.status === STATUS.BLOCKED).length
)

const filteredActions = computed(() => {
  const query = search.value.trim().toLowerCase()

  const result = actions.value.filter(action => {
    const matchesSearch = !query || [
      action.title,
      action.description,
      action.assignee?.name
    ].some(value =>
      String(value || '').toLowerCase().includes(query)
    )

    const matchesStatus =
      statusFilter.value === 'ALL' ||
      action.status === statusFilter.value

    const matchesPriority =
      priorityFilter.value === 'ALL' ||
      action.priority === priorityFilter.value

    const matchesAssignee =
      assigneeFilter.value === 'ALL' ||
      String(action.assignee?.userId ?? '') === assigneeFilter.value

    const matchesStartDate =
      !dateFrom.value ||
      Boolean(action.dueDate && action.dueDate >= dateFrom.value)

    const matchesEndDate =
      !dateTo.value ||
      Boolean(action.dueDate && action.dueDate <= dateTo.value)

    return matchesSearch &&
      matchesStatus &&
      matchesPriority &&
      matchesAssignee &&
      matchesStartDate &&
      matchesEndDate
  })

  const priorityRank = {
    LOW: 1,
    MEDIUM: 2,
    HIGH: 3,
    CRITICAL: 4
  }

  result.sort((a, b) => {
    let comparison = 0

    if (sortBy.value === 'title') {
      comparison = String(a.title || '').localeCompare(
        String(b.title || '')
      )
    } else if (sortBy.value === 'priority') {
      comparison =
        (priorityRank[a.priority] || 0) -
        (priorityRank[b.priority] || 0)
    } else {
      comparison = String(a.dueDate || '9999-12-31').localeCompare(
        String(b.dueDate || '9999-12-31')
      )
    }

    return sortDirection.value === 'asc'
      ? comparison
      : -comparison
  })

  return result
})

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredActions.value.length / pageSize.value))
)

watch([filteredActions, pageSize], () => {
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value
  }
})

const paginatedActions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value

  return filteredActions.value.slice(
    start,
    start + pageSize.value
  )
})

const firstVisibleItem = computed(() =>
  filteredActions.value.length === 0
    ? 0
    : (currentPage.value - 1) * pageSize.value + 1
)

const lastVisibleItem = computed(() =>
  Math.min(
    currentPage.value * pageSize.value,
    filteredActions.value.length
  )
)

function resetPage() {
  currentPage.value = 1
}

function changeSort(field) {
  if (sortBy.value === field) {
    sortDirection.value =
      sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = field
    sortDirection.value = 'asc'
  }

  resetPage()
}

async function loadActions() {
  loading.value = true
  error.value = ''

  try {
    const response = await apiFetch('/action-items')

    if (response.status === 401) {
      handleSessionExpired()
      actions.value = []
      users.value = []
      throw new Error('Your session has expired. Please log in again.')
    }

    if (response.status === 403) {
      throw new Error('You are not authorized to view action items.')
    }

    if (!response.ok) {
      throw new Error(`Request failed: ${response.status}`)
    }

    const data = await response.json()
    actions.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message ||
      'Could not load action items. Check that the backend is running.'
  } finally {
    loading.value = false
  }
}

async function updateActionStatus(action, newStatus) {
  updatingActionId.value = action.actionItemId
  statusUpdateError.value = ''
  statusUpdateSuccess.value = ''

  try {
    const response = await apiFetch(
      `/action-items/${action.actionItemId}/status`,
      {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          status: newStatus,
          comment: `Status changed to ${newStatus}`
        })
      }
    )

    if (!response.ok) {
      if (response.status === 401) {
        handleSessionExpired()
        actions.value = []
        users.value = []
        throw new Error('Your session has expired. Please log in again.')
      }

      if (response.status === 403) {
        throw new Error('You are not authorized to update this action item.')
      }

      const errorData = await response.json().catch(() => null)

      throw new Error(
        errorData?.message ||
        `Status update failed (HTTP ${response.status}).`
      )
    }

    const updatedAction = await response.json()

    actions.value = actions.value.map(item =>
      item.actionItemId === updatedAction.actionItemId
        ? updatedAction
        : item
    )

    statusUpdateSuccess.value =
      `Status updated to ${newStatus.replaceAll('_', ' ')}.`
  } catch (e) {
    statusUpdateError.value =
      e.message || 'Could not update the action item.'
  } finally {
    updatingActionId.value = null
  }
}

async function loadActionHistory(action) {
  selectedHistoryAction.value = action;
  actionHistory.value = [];
  historyError.value = '';
  historyLoading.value = true;

  try {
    const response = await apiFetch(
      `/action-items/${action.actionItemId}/history`
    );

    if (response.status === 401) {
      handleSessionExpired()
      actions.value = []
      users.value = []
      throw new Error('Your session has expired. Please log in again.')
    }

    if (response.status === 403) {
      throw new Error('You are not authorized to view this action history.')
    }

    if (!response.ok) {
      throw new Error(`Could not load action history (HTTP ${response.status}).`);
    }

    const data = await response.json();
    actionHistory.value = Array.isArray(data)
      ? data
      : Array.isArray(data.value) ? data.value : [];
  } catch (e) {
    historyError.value = e.message || 'Could not load action history.';
  } finally {
    historyLoading.value = false;
  }
}
function closeActionHistory() {
  selectedHistoryAction.value = null
  actionHistory.value = []
  historyError.value = ''
}

// Load comments for one action item from the secured backend API.
async function loadActionComments(action) {
  selectedCommentAction.value = action;
  actionComments.value = [];
  newCommentText.value = '';
  commentsError.value = '';
  commentsSuccess.value = '';
  commentsLoading.value = true;

  try {
    const response = await apiFetch(
      `/comments?actionItemId=${encodeURIComponent(action.actionItemId)}`
    );

    if (response.status === 401) {
      handleSessionExpired()
      actions.value = []
      users.value = []
      throw new Error('Your session has expired. Please log in again.')
    }

    if (response.status === 403) {
      throw new Error('You are not authorized to view comments.')
    }

    if (!response.ok) {
      throw new Error(`Could not load comments (HTTP ${response.status}).`);
    }

    const data = await response.json();
    actionComments.value = Array.isArray(data)
      ? data
      : Array.isArray(data.value) ? data.value : [];
  } catch (e) {
    commentsError.value = e.message || 'Could not load comments.';
  } finally {
    commentsLoading.value = false;
  }
}
// The backend derives the commenter from the authenticated session.
// Never send a user ID from the frontend as proof of identity.
async function addActionComment() {
  const text = newCommentText.value.trim();

  if (!selectedCommentAction.value || !text) {
    commentsError.value = 'Enter a comment before submitting.';
    return;
  }

  if (text.length > 5000) {
    commentsError.value = 'Comment must be 5000 characters or fewer.';
    return;
  }

  commentsSaving.value = true;
  commentsError.value = '';
  commentsSuccess.value = '';

  try {
    const response = await apiFetch('/comments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        actionItemId: selectedCommentAction.value.actionItemId,
        comment: text
      })
    });

    if (response.status === 401) {
      handleSessionExpired()
      actions.value = []
      users.value = []
      throw new Error('Your session has expired. Please log in again.')
    }

    if (response.status === 403) {
      throw new Error('You are not authorized to add comments.');
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(
        errorData?.message || `Could not add comment (HTTP ${response.status}).`
      );
    }

    const savedResponse = await response.json();
    const savedComment = savedResponse?.value ?? savedResponse;
    actionComments.value.push(savedComment);

    actionComments.value.sort(
      (a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0)
    );

    newCommentText.value = '';
    commentsSuccess.value = 'Comment added successfully.';
  } catch (e) {
    commentsError.value = e.message || 'Could not add comment.';
  } finally {
    commentsSaving.value = false;
  }
}
function closeActionComments() {
  selectedCommentAction.value = null
  actionComments.value = []
  newCommentText.value = ''
  commentsError.value = ''
  commentsSuccess.value = ''
}

async function loadUsers() {
  if (currentUser.value?.role !== 'MANAGER') return;

  usersLoading.value = true;
  userListError.value = '';

  try {
    const response = await apiFetch('/users');

    if (response.status === 401) {
      handleSessionExpired()
      actions.value = []
      users.value = []
      throw new Error('Your session has expired. Please log in again.')
    }

    if (response.status === 403) {
      throw new Error('Only a Manager can load the assignee list.')
    }

    if (!response.ok) {
      throw new Error(`Could not load users (HTTP ${response.status}).`);
    }

    const data = await response.json();
    users.value = Array.isArray(data) ? data : [];

    const selectedId = Number(newAction.value.assigneeId);
    const selectedUserExists = users.value.some(
      user => Number(user.userId) === selectedId
    );

    if (!selectedUserExists && users.value.length > 0) {
      newAction.value.assigneeId = Number(users.value[0].userId);
    }
  } catch (e) {
    users.value = [];
    userListError.value = e.message || 'Could not load users for assignment.';
  } finally {
    usersLoading.value = false;
  }
}
async function createAction() {
  if (currentUser.value?.role !== 'MANAGER') return

  editingActionId.value = null
  formError.value = ''
  formSuccess.value = ''
  error.value = ''
  newAction.value = createEmptyAction()

  await loadUsers()
  showCreateForm.value = true
}

async function openEditAction(action) {
  if (currentUser.value?.role !== 'MANAGER') return

  formError.value = ''
  formSuccess.value = ''
  userListError.value = ''
  editingActionId.value = action.actionItemId

  await loadUsers()

  newAction.value = {
    title: action.title || '',
    description: action.description || '',
    assigneeId: Number(action.assignee?.userId || ''),
    priority: action.priority || 'MEDIUM',
    dueDate: action.dueDate || ''
  }

  showCreateForm.value = true
}
function closeCreateForm() {
  if (saving.value) return
  showCreateForm.value = false
  editingActionId.value = null
  formError.value = ''
}

function openCreateUserForm() {
  if (currentUser.value?.role !== 'MANAGER') return

  userError.value = ''
  userSuccess.value = ''
  newUser.value = createEmptyUser()
  showCreateUserForm.value = true
}

function closeCreateUserForm() {
  if (creatingUser.value) return
  showCreateUserForm.value = false
  userError.value = ''
}

async function createUser() {
  userError.value = '';
  userSuccess.value = '';

  const name = newUser.value.name.trim();
  const email = newUser.value.email.trim();
  const password = newUser.value.password;
  const role = newUser.value.role;

  if (!name || !email || !password || !role) {
    userError.value = 'Please fill in all required fields.';
    return;
  }

  if (name.length > 100) {
    userError.value = 'Name must not exceed 100 characters.';
    return;
  }

  if (email.length > 150) {
    userError.value = 'Email must not exceed 150 characters.';
    return;
  }

  if (password.length < 12 || new TextEncoder().encode(password).length > 72) {
    userError.value = 'Password must contain at least 12 characters and no more than 72 UTF-8 bytes.';
    return;
  }

  if (!['MANAGER', 'TEAM_MEMBER'].includes(role)) {
    userError.value = 'Please select a valid role.';
    return;
  }

  creatingUser.value = true;

  try {
    const response = await apiFetch('/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password, role })
    });

    if (response.status === 401) {
      handleSessionExpired()
      actions.value = []
      users.value = []
      throw new Error('Your session has expired. Please log in again.')
    }

    if (response.status === 403) {
      throw new Error('Only a Manager can create users.')
    }

    if (response.status === 409) {
      throw new Error('An account with this email already exists.');
    }

    if (response.status === 400) {
      const message = await response.text();
      throw new Error(message || 'Please check the submitted details.');
    }

    if (!response.ok) {
      throw new Error(`Could not create account (HTTP ${response.status}).`);
    }

    const result = await response.json();
    userSuccess.value = `Account created for ${result.name} (${result.role}).`;

    newUser.value = createEmptyUser();
    showCreateUserForm.value = false;
    await loadUsers();
  } catch (e) {
    userError.value = e.message || 'Could not create the account.';
  } finally {
    creatingUser.value = false;
  }
}
async function saveAction() {
  if (currentUser.value?.role !== 'MANAGER') {
    formError.value = 'Only Managers can create action items.';
    return;
  }

  formError.value = '';
  formSuccess.value = '';

  const title = newAction.value.title.trim();
  const dueDate = newAction.value.dueDate;
  const today = new Date();

  const localToday = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, '0'),
    String(today.getDate()).padStart(2, '0')
  ].join('-');

  if (!title) {
    formError.value = 'Title is required.';
    return;
  }

  if (title.length > 200) {
    formError.value = 'Title must not exceed 200 characters.';
    return;
  }

  if (!dueDate) {
    formError.value = 'Due date is required.';
    return;
  }

  if (dueDate < localToday) {
    formError.value = 'Due date cannot be in the past.';
    return;
  }

  saving.value = true;

  try {
    const payload = {
      title,
      description: newAction.value.description.trim() || null,
      assigneeId: Number(newAction.value.assigneeId),
      priority: newAction.value.priority,
      status: editingActionId.value
        ? actions.value.find(a => a.actionItemId === editingActionId.value)?.status || STATUS.NOT_STARTED
        : STATUS.NOT_STARTED,
      dueDate
    };

    const isEditing = Boolean(editingActionId.value);
    const endpoint = isEditing
      ? `/action-items/${editingActionId.value}`
      : '/action-items';

    const response = await apiFetch(endpoint, {
      method: isEditing ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (response.status === 401) {
      handleSessionExpired()
      actions.value = []
      users.value = []
      throw new Error('Your session has expired. Please log in again.')
    }

    if (response.status === 403) {
      throw new Error('Only a Manager can create or update action items.')
    }

    if (!response.ok) {
      const message = await response.text();
      throw new Error(message || `Unable to save task (HTTP ${response.status}).`);
    }

    showCreateForm.value = false;
    formSuccess.value = isEditing
      ? 'Action item updated successfully.'
      : 'Action item created successfully.';

    editingActionId.value = null;
    await loadActions();
  } catch (e) {
    formError.value = e.message || 'Could not save action item.';
  } finally {
    saving.value = false;
  }
}

async function logout() {
  const logoutError = await endSession();

  if (logoutError) {
    error.value = logoutError;
  }

    actions.value = [];
  users.value = [];
  actionHistory.value = [];
  selectedHistoryAction.value = null;
  actionComments.value = [];
  selectedCommentAction.value = null;
  newCommentText.value = '';
  showCreateForm.value = false;
  showCreateUserForm.value = false;
  editingActionId.value = null;
  await router.replace({ name: 'login' });
}
function formatStatus(status) {
  return (status || 'UNKNOWN').replaceAll('_', ' ')
}

onMounted(async () => {

  await loadActions()

  if (currentUser.value?.role === 'MANAGER') {
    await loadUsers()
  }
})
</script>

<template>
  <!-- DASHBOARD -->
  <main class="dashboard">
    <header class="topbar">
      <div>
        <p class="eyebrow">TEAM WORKSPACE</p>
        <h1>Team Action Tracker</h1>
        <p class="subtitle">Track responsibilities. Keep work moving.</p>
        <p class="subtitle">
          Signed in as <strong>{{ currentUser?.name }}</strong>
          | {{ currentUser?.role }}
        </p>
      </div>

      <div class="topbar-actions">
        <button
          class="refresh-btn"
          type="button"
          @click="loadActions"
          :disabled="loading"
        >
          <span class="refresh-icon" aria-hidden="true">R</span>
          <span>{{ loading ? 'Refreshing...' : 'Refresh' }}</span>
        </button>

        <button
          v-if="currentUser?.role === 'MANAGER'"
          class="refresh-btn"
          type="button"
          @click="openCreateUserForm"
        >
          Create User
        </button>

        <button
          v-if="currentUser?.role === 'MANAGER'"
          class="primary-btn"
          type="button"
          @click="createAction"
        >
          <span aria-hidden="true">+</span>
          <span>Create Action</span>
        </button>
      
        <button
          class="refresh-btn"
          type="button"
          @click="logout"
        >
          Logout
        </button>
      </div>
    </header>

    <p v-if="formSuccess" class="success-banner" role="status">
      {{ formSuccess }}
      <button type="button" @click="formSuccess = ''" aria-label="Dismiss">
        X
      </button>
    </p>

    <p v-if="userSuccess" class="success-banner" role="status">
      {{ userSuccess }}
      <button type="button" @click="userSuccess = ''" aria-label="Dismiss">
        X
      </button>
    </p>

    <p v-if="statusUpdateSuccess" class="success-banner" role="status">
      {{ statusUpdateSuccess }}
      <button
        type="button"
        @click="statusUpdateSuccess = ''"
        aria-label="Dismiss"
      >X</button>
    </p>

    <p v-if="statusUpdateError" class="message error" role="alert">
      {{ statusUpdateError }}
      <button
        type="button"
        @click="statusUpdateError = ''"
        aria-label="Dismiss"
      >X</button>
    </p>

    <section class="summary">
  <article class="summary-card">
    <p>Total actions</p>
    <h2>{{ total }}</h2>
  </article>
  <article class="summary-card">
    <p>Open actions</p>
    <h2>{{ openActions }}</h2>
  </article>

  <article class="summary-card">
    <p>My actions</p>
    <h2>{{ myActions }}</h2>
  </article>


  <article class="summary-card">
    <p>Not started</p>
    <h2>{{ pending }}</h2>
  </article>

  <article class="summary-card">
    <p>In progress</p>
    <h2>{{ inProgress }}</h2>
  </article>

  <article class="summary-card">
    <p>Completed</p>
    <h2>{{ completed }}</h2>
  </article>

  <article class="summary-card">
    <p>Blocked</p>
    <h2>{{ blocked }}</h2>
  </article>

  <article class="summary-card">
    <p>Overdue</p>
    <h2>{{ overdue }}</h2>
  </article>
</section>

    <section class="panel">
      <div class="panel-heading">
        <div>
          <h2>Action items</h2>
          <p>Search and filter tasks assigned to your team.</p>
        </div>
        <span class="count">{{ filteredActions.length }} items</span>
      </div>

      <div class="filters">
  <input
    v-model="search"
    type="search"
    placeholder="Search title, description, or assignee..."
    aria-label="Search action items"
    @input="resetPage"
  />

  <select
    v-model="statusFilter"
    aria-label="Filter by status"
    @change="resetPage"
  >
    <option value="ALL">All statuses</option>
                  <option v-for="status in ACTION_STATUSES" :key="status.value" :value="status.value">{{ status.label }}</option>
  </select>

  <select
    v-model="priorityFilter"
    aria-label="Filter by priority"
    @change="resetPage"
  >
    <option value="ALL">All priorities</option>
    <option value="CRITICAL">Critical</option>
    <option value="HIGH">High</option>
    <option value="MEDIUM">Medium</option>
    <option value="LOW">Low</option>
  </select>

  <select
    v-if="currentUser?.role === 'MANAGER'"
    v-model="assigneeFilter"
    aria-label="Filter by assignee"
    @change="resetPage"
  >
    <option value="ALL">All assignees</option>
    <option
      v-for="user in users"
      :key="user.userId"
      :value="String(user.userId)"
    >
      {{ user.name }}
    </option>
  </select>

  <label>
    Due from
    <input
      v-model="dateFrom"
      type="date"
      aria-label="Due date from"
      @change="resetPage"
    />
  </label>

  <label>
    Due to
    <input
      v-model="dateTo"
      type="date"
      aria-label="Due date to"
      :min="dateFrom || undefined"
      @change="resetPage"
    />
  </label>

  <select
    v-model="sortBy"
    aria-label="Sort action items by"
    @change="resetPage"
  >
    <option value="dueDate">Sort by due date</option>
    <option value="title">Sort by title</option>
    <option value="priority">Sort by priority</option>
  </select>

  <button
    type="button"
    class="refresh-btn"
    @click="changeSort(sortBy)"
  >
    {{ sortDirection === 'asc' ? 'Ascending (A to Z)' : 'Descending (Z to A)' }}
  </button>

  <button
    type="button"
    class="refresh-btn"
    @click="
      search = '';
      statusFilter = 'ALL';
      priorityFilter = 'ALL';
      assigneeFilter = 'ALL';
      dateFrom = '';
      dateTo = '';
      sortBy = 'dueDate';
      sortDirection = 'asc';
      currentPage = 1
    "
  >
    Clear filters
  </button>
</div>

      <p v-if="loading" class="message">Loading action items...</p>

      <p v-else-if="error" class="message error">
        {{ error }}
        <button class="refresh-btn" type="button" @click="loadActions">
          Try again
        </button>
      </p>

      <p v-else-if="filteredActions.length === 0" class="message">
        No matching action items found.
      </p>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Action item</th>
              <th>Assignee</th>
              <th>Priority</th>
              <th>Status</th>
              <th>Due date</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="action in paginatedActions"
              :key="action.actionItemId"
            >
              <td class="task-cell">
                <strong>{{ action.title }}</strong>
                <small>{{ action.description || 'No description' }}</small>
              </td>

              <td>{{ action.assignee?.name || 'Unassigned' }}</td>

              <td>
                <span
                  class="priority"
                  :class="(action.priority || '').toLowerCase()"
                >
                  {{ action.priority || "N/A" }}
                </span>
              </td>

              <td>
                <span
                  class="status"
                  :class="(action.status || '').toLowerCase()"
                >
                  {{ formatStatus(action.status) }}
                </span>
              </td>

              <td>{{ action.dueDate || 'Not set' }}</td>

              <td class="action-controls">
                <select
                  :value="action.status"
                  :disabled="updatingActionId === action.actionItemId ||
                    (currentUser?.role !== 'MANAGER' &&
                     Number(action.assignee?.userId) !== Number(currentUser?.userId))"
                  :aria-label="`Update status for ${action.title}`"
                  @change="updateActionStatus(action, $event.target.value)"
                >
                  <option v-for="status in ACTION_STATUSES" :key="status.value" :value="status.value">{{ status.label }}</option>
                </select>

                <small v-if="updatingActionId === action.actionItemId">
                  Saving...
                </small>

                <button
                  v-if="currentUser?.role === 'MANAGER'"
                  type="button"
                  class="refresh-btn"
                  @click="openEditAction(action)"
                >
                  Edit
                </button>
                <button
                  type="button"
                  class="refresh-btn"
                  @click="loadActionHistory(action)"
                >
                  History
                </button>

                <button
                  type="button"
                  class="refresh-btn"
                  @click="loadActionComments(action)"
                >
                  Comments
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div
        v-if="!loading && !error && filteredActions.length > 0"
        class="pagination"
      >
        <span>
          Showing {{ firstVisibleItem }} - {{ lastVisibleItem }}
          of {{ filteredActions.length }} items
        </span>

        <label>
          Rows per page
          <select v-model.number="pageSize" @change="currentPage = 1">
            <option :value="5">5</option>
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>

        <button
          type="button"
          class="refresh-btn"
          :disabled="currentPage <= 1"
          @click="currentPage--"
        >
          Previous
        </button>

        <span>Page {{ currentPage }} of {{ totalPages }}</span>

        <button
          type="button"
          class="refresh-btn"
          :disabled="currentPage >= totalPages"
          @click="currentPage++"
        >
          Next
        </button>
      </div>
    </section>

    <footer>Team Action Tracker - Project dashboard</footer>

    <!-- ACTION HISTORY MODAL -->
    <div
      v-if="selectedHistoryAction"
      class="modal-overlay"
      @click.self="closeActionHistory"
    >
      <section
        class="create-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="history-title"
      >
        <div class="modal-heading">
          <div>
            <p class="eyebrow">AUDIT TRAIL</p>
            <h2 id="history-title">Action History</h2>
            <p class="subtitle">{{ selectedHistoryAction.title }}</p>
          </div>

          <button
            class="close-btn"
            type="button"
            @click="closeActionHistory"
            aria-label="Close history"
          >X</button>
        </div>

        <p v-if="historyLoading" class="message">
          Loading action history...
        </p>

        <p v-else-if="historyError" class="form-error" role="alert">
          {{ historyError }}
        </p>

        <p v-else-if="actionHistory.length === 0" class="message">
          No history entries yet.
        </p>

        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Previous status</th>
                <th>New status</th>
                <th>Updated by</th>
                <th>Comment</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="entry in actionHistory" :key="entry.historyId">
                <td>
                  {{ entry.updatedAt
                    ? new Date(entry.updatedAt).toLocaleString()
                    : 'Not available' }}
                </td>
                <td>
                  {{ entry.previousStatus
                    ? formatStatus(entry.previousStatus)
                    : 'Not available' }}
                </td>
                <td>{{ formatStatus(entry.newStatus) }}</td>
                <td>
                  {{ entry.updatedBy?.name ||
                     entry.updatedBy?.email ||
                     'Unknown' }}
                </td>
                <td>{{ entry.comment || 'No comment' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="modal-actions">
          <button
            class="refresh-btn"
            type="button"
            @click="closeActionHistory"
          >
            Close
          </button>
        </div>
      </section>
    </div>

    <!-- ACTION COMMENTS MODAL -->
    <div
      v-if="selectedCommentAction"
      class="modal-overlay"
      @click.self="closeActionComments"
    >
      <section
        class="create-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="comments-title"
      >
        <div class="modal-heading">
          <div>
            <p class="eyebrow">DISCUSSION</p>
            <h2 id="comments-title">Action Comments</h2>
            <p class="subtitle">{{ selectedCommentAction.title }}</p>
          </div>

          <button
            class="close-btn"
            type="button"
            @click="closeActionComments"
            aria-label="Close comments"
          >X</button>
        </div>

        <p v-if="commentsLoading" class="message">
          Loading comments...
        </p>

        <p v-if="commentsError" class="form-error" role="alert">
          {{ commentsError }}
        </p>

        <p v-if="commentsSuccess" class="form-success" role="status">
          {{ commentsSuccess }}
        </p>

        <div
          v-if="!commentsLoading &&
                actionComments.length === 0 &&
                !commentsError"
          class="message"
        >
          No comments yet. Start the discussion below.
        </div>

        <div v-if="actionComments.length > 0" class="comment-list">
          <article
            v-for="(entry, index) in actionComments"
            :key="entry.commentId || index"
            class="comment-entry"
          >
            <div class="comment-meta">
              <strong>
                {{ entry.user?.name || entry.user?.email || 'User' }}
              </strong>
              <small>
                {{ entry.createdAt
                  ? new Date(entry.createdAt).toLocaleString()
                  : '' }}
              </small>
            </div>

            <p>{{ entry.comment }}</p>
          </article>
        </div>

        <form class="comment-form" @submit.prevent="addActionComment">
          <label for="new-action-comment">Add a comment</label>

          <textarea
            id="new-action-comment"
            v-model="newCommentText"
            rows="3"
            maxlength="5000"
            placeholder="Write an update or ask a question..."
            required
          ></textarea>

          <div class="modal-actions">
            <button
              class="refresh-btn"
              type="button"
              @click="closeActionComments"
            >
              Close
            </button>

            <button
              class="primary-btn"
              type="submit"
              :disabled="commentsSaving || !newCommentText.trim()"
            >
              {{ commentsSaving ? 'Adding...' : 'Add comment' }}
            </button>
          </div>
        </form>
      </section>
    </div>

    <!-- CREATE ACTION MODAL -->
    <div
      v-if="showCreateForm"
      class="modal-overlay"
      @click.self="closeCreateForm"
    >
      <section
        class="create-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-title"
      >
        <div class="modal-heading">
          <div>
            <p class="eyebrow">{{ editingActionId ? 'EDIT TASK' : 'NEW TASK' }}</p>
            <h2 id="create-title">{{ editingActionId ? 'Edit Action' : 'Create Action' }}</h2>
            <p class="subtitle">Fill in the details below.</p>
          </div>

          <button
            class="close-btn"
            type="button"
            @click="closeCreateForm"
            :disabled="saving"
            aria-label="Close"
          >
            X
          </button>
        </div>

        <form @submit.prevent="saveAction">
          <label for="action-title">Title *</label>
          <input
            id="action-title"
            v-model="newAction.title"
            maxlength="200"
            required
            placeholder="Enter action title"
          />

          <label for="action-description">Description</label>
          <textarea
            id="action-description"
            v-model="newAction.description"
            rows="3"
            maxlength="2000"
            placeholder="Describe the action"
          ></textarea>

          <label for="action-assignee">Assignee *</label>
          <select
            id="action-assignee"
            v-model.number="newAction.assigneeId"
            :disabled="usersLoading || users.length === 0"
            required
          >
            <option v-if="usersLoading" disabled value="">
              Loading users...
            </option>

            <option
              v-for="user in users"
              :key="user.userId"
              :value="Number(user.userId)"
            >
              {{ user.name }} - {{ user.role.replaceAll('_', ' ') }}
            </option>
          </select>

          <small v-if="userListError" class="form-error" role="alert">
            {{ userListError }}
          </small>

          <small v-else-if="!usersLoading && users.length === 0">
            No active users available. Refresh the page or check the backend.
          </small>

          <label for="action-priority">Priority *</label>
          <select
            id="action-priority"
            v-model="newAction.priority"
            required
          >
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>

          <label for="action-due-date">Due date *</label>
          <input
            id="action-due-date"
            v-model="newAction.dueDate"
            type="date"
            :min="new Date().getFullYear() + '-' +
                 String(new Date().getMonth() + 1).padStart(2, '0') + '-' +
                 String(new Date().getDate()).padStart(2, '0')"
            required
          />

          <p v-if="formError" class="form-error" role="alert">
            {{ formError }}
          </p>

          <div class="modal-actions">
            <button
              type="button"
              class="refresh-btn"
              @click="closeCreateForm"
              :disabled="saving"
            >
              Cancel
            </button>

            <button
              class="primary-btn"
              type="submit"
              :disabled="saving"
            >
              {{ saving ? 'Saving...' : (editingActionId ? 'Update Action' : 'Save Action') }}
            </button>
          </div>
        </form>
      </section>
    </div>

    <!-- CREATE USER MODAL -->
    <div
      v-if="showCreateUserForm"
      class="modal-overlay"
      @click.self="closeCreateUserForm"
    >
      <section
        class="create-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-user-title"
      >
        <div class="modal-heading">
          <div>
            <p class="eyebrow">MANAGER ACCESS</p>
            <h2 id="create-user-title">Create User</h2>
            <p class="subtitle">
              Create an account for a Manager or Team Member.
            </p>
          </div>

          <button
            class="close-btn"
            type="button"
            @click="closeCreateUserForm"
            :disabled="creatingUser"
            aria-label="Close"
          >
            X
          </button>
        </div>

        <form @submit.prevent="createUser">
          <label for="new-user-name">Full name *</label>
          <input
            id="new-user-name"
            v-model="newUser.name"
            maxlength="100"
            autocomplete="name"
            required
            placeholder="Enter full name"
          />

          <label for="new-user-email">Email address *</label>
          <input
            id="new-user-email"
            v-model="newUser.email"
            type="email"
            maxlength="150"
            autocomplete="email"
            required
            placeholder="person@example.com"
          />

          <label for="new-user-password">Temporary password *</label>
          <input
            id="new-user-password"
            v-model="newUser.password"
            type="password"
            minlength="12"
            maxlength="72"
            autocomplete="new-password"
            required
            placeholder="At least 12 characters"
          />

          <small>Use a unique password of at least 12 characters.</small>

          <label for="new-user-role">Account role *</label>
          <select
            id="new-user-role"
            v-model="newUser.role"
            required
          >
            <option value="TEAM_MEMBER">Team Member</option>
            <option value="MANAGER">Manager</option>
          </select>

          <p v-if="userError" class="form-error" role="alert">
            {{ userError }}
          </p>

          <div class="modal-actions">
            <button
              type="button"
              class="refresh-btn"
              @click="closeCreateUserForm"
              :disabled="creatingUser"
            >
              Cancel
            </button>

            <button
              class="primary-btn"
              type="submit"
              :disabled="creatingUser"
            >
              {{ creatingUser ? 'Creating...' : 'Create Account' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </main>
</template>
