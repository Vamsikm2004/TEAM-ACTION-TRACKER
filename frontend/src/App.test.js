import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { defineComponent } from 'vue'
import LoginView from './views/LoginView.vue'

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  loginError: { value: '' },
  loginLoading: { value: false }
}))

vi.mock('./composables/useAuth', () => ({
  useAuth: () => mocks
}))

const DashboardStub = defineComponent({
  template: '<div>Dashboard</div>'
})

async function mountLogin() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'login', component: LoginView },
      { path: '/dashboard', name: 'dashboard', component: DashboardStub }
    ]
  })

  await router.push('/login')
  await router.isReady()

  const wrapper = mount(LoginView, {
    global: { plugins: [router] }
  })

  return { wrapper, router }
}

describe('Team Action Tracker login', () => {
  it('renders the login screen', async () => {
    const { wrapper } = await mountLogin()

    expect(wrapper.text()).toContain('Team Action Tracker')
    expect(wrapper.text()).toContain('Sign in')
    expect(wrapper.find('input[type="email"]').exists()).toBe(true)
    expect(wrapper.find('input[type="password"]').exists()).toBe(true)
  })

  it('requires email and password', async () => {
    const { wrapper } = await mountLogin()

    expect(wrapper.find('input[type="email"]').attributes('required')).toBeDefined()
    expect(wrapper.find('input[type="password"]').attributes('required')).toBeDefined()
  })
})