/**
 * Global auth failure event system.
 * When the API returns 401/403, this event is fired so that
 * the AuthGuard can redirect to the onboarding/login flow.
 */

type AuthFailureListener = () => void

const listeners: Set<AuthFailureListener> = new Set()
let isFiring = false

export function onAuthFailure(listener: AuthFailureListener) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function fireAuthFailure() {
  // Prevent multiple rapid fires (e.g. parallel API calls all returning 401)
  if (isFiring) return
  isFiring = true

  listeners.forEach((listener) => {
    try {
      listener()
    } catch (e) {
      // ignore
    }
  })

  // Reset after a short delay to allow re-firing if needed later
  setTimeout(() => {
    isFiring = false
  }, 3000)
}
