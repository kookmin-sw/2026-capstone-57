const API_BASE = process.env.NEXT_PUBLIC_API_URL || "/backend"

function getToken(): string | null {
  if (typeof window === "undefined") return null
  return localStorage.getItem("token")
}

export function setToken(token: string) {
  localStorage.setItem("token", token)
}

export function clearToken() {
  localStorage.removeItem("token")
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
    const error = await res.text().catch(() => "Unknown error")
    throw new Error(`API Error ${res.status}: ${error}`)
  }

  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return undefined as T
  }

  return res.json()
}
