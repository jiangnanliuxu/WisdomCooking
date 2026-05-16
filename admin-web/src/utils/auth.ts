const ADMIN_TOKEN_KEY = 'cook_admin_token'
const ADMIN_EXPIRES_KEY = 'cook_admin_expires'

export function getAdminToken(): string {
  return localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}

export function setAdminToken(token: string): void {
  localStorage.setItem(ADMIN_TOKEN_KEY, token)
}

export function removeAdminToken(): void {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(ADMIN_EXPIRES_KEY)
}

export function setAdminExpires(expiresIn: number): void {
  localStorage.setItem(ADMIN_EXPIRES_KEY, String(expiresIn))
}
