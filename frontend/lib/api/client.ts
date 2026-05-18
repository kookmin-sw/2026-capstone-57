import { fireAuthFailure } from "@/lib/auth-event"

const API_BASE = "/backend"

function getToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("token")
}

export function setToken(token: string) {
  localStorage.setItem("token", token)
}

export function clearToken() {
  localStorage.removeItem("token")
  localStorage.removeItem("userId")
  localStorage.removeItem("refreshToken")
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  }
  if (token) {
    headers["Authorization"] = `Bearer ${token}`
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (!res.ok) {
    // 401/403 → 인증 실패: 토큰 제거 후 온보딩으로 이동
    if (res.status === 401 || res.status === 403) {
      clearToken()
      fireAuthFailure()
    }

    const error = await res.text().catch(() => "Unknown error")
    throw new Error(`API Error ${res.status}: ${error}`)
  }

  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return undefined as T
  }

  return res.json()
}
