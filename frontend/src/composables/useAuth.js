import { ref } from 'vue'
import { apiFetch, loginUser, logoutUser } from '../services/api'

const currentUser = ref(null)
const isLoggedIn = ref(false)
const loginError = ref('')
const loginLoading = ref(false)


export function useAuth() {
  async function restoreSession() {
    try {
      const response = await apiFetch('/auth/me');

      if (!response.ok) {
        clearSession();
        return false;
      }

      currentUser.value = await response.json();
      isLoggedIn.value = true;
      return true;
    } catch {
      clearSession();
      return false;
    }
  }

  async function login(credentials) {
    loginError.value = ''
    loginLoading.value = true

    try {
      const response = await loginUser(credentials)

      if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
          throw new Error('Invalid email or password.')
        }

        throw new Error(`Login failed (HTTP ${response.status}).`)
      }

      currentUser.value = await response.json()
      isLoggedIn.value = true

      return currentUser.value
    } catch (error) {
      currentUser.value = null
      isLoggedIn.value = false
      loginError.value = error.message || 'Login failed. Please try again.'
      throw error
    } finally {
      loginLoading.value = false
    }
  }

  async function logout() {
    let logoutError = ''

    try {
      await logoutUser()
    } catch (error) {
      logoutError = error.message || 'Logout could not be confirmed.'
    } finally {
      clearSession()
    }

    return logoutError
  }

  function clearSession() {
    currentUser.value = null
    isLoggedIn.value = false
  }

  return {
    currentUser,
    isLoggedIn,
    loginError,
    loginLoading,
    login,
    logout,
    restoreSession,
    clearSession
  }
}
