<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { login, loginError, loginLoading } = useAuth()

const loginEmail = ref('')
const loginPassword = ref('')

async function submitLogin() {
  try {
    await login({
      email: loginEmail.value.trim(),
      password: loginPassword.value
    })
    await router.push({ name: 'dashboard' })
  } catch {
    // Error is provided by useAuth().
  }
}
</script>

<template>
  <main class="dashboard">
    <section
      class="panel"
      style="max-width: 480px; margin: 8vh auto; padding: 32px;"
    >
      <p class="eyebrow">TEAM WORKSPACE</p>
      <h1>Team Action Tracker</h1>
      <p class="subtitle">Sign in to manage your team's action items.</p>

      <form
        @submit.prevent="submitLogin"
        style="display: grid; gap: 16px; margin-top: 28px;"
      >
        <label for="login-email">Email address</label>
        <input
          id="login-email"
          v-model="loginEmail"
          type="email" maxlength="150"
          autocomplete="username"
          required
          placeholder="you@example.com"
        />

        <label for="login-password">Password</label>
        <input
          id="login-password"
          v-model="loginPassword"
          type="password"
          autocomplete="current-password"
          required
          placeholder="Enter your password"
        />

        <p v-if="loginError" class="form-error" role="alert">
          {{ loginError }}
        </p>

        <button class="primary-btn" type="submit" :disabled="loginLoading">
          {{ loginLoading ? 'Signing in...' : 'Sign in' }}
        </button>
      </form>
    </section>
  </main>
</template>
