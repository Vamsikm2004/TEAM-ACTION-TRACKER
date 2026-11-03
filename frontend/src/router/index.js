import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/useAuth'

const LoginView = () => import('../views/LoginView.vue')
const DashboardView = () => import('../views/DashboardView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { guestOnly: true }
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: { requiresAuth: true }
    },
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

let sessionRestored = false
let sessionRestorePromise

router.beforeEach(async (to) => {
  const auth = useAuth()

  if (!sessionRestored) {
    sessionRestorePromise ??= auth.restoreSession()
    await sessionRestorePromise
    sessionRestored = true
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn.value) {
    return { name: 'login' }
  }

  if (to.meta.guestOnly && auth.isLoggedIn.value) {
    return { name: 'dashboard' }
  }

  return true
})

export default router
