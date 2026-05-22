import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

import { AUTH_STORAGE_KEY, apiRequest, type LoginResponse } from './api'
import { roles, type RoleId } from '../data/mockData'

export interface AuthSession extends LoginResponse {
  roleId: RoleId
}

interface AuthContextValue {
  session: AuthSession | null
  login: (username: string, password: string) => Promise<AuthSession>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => loadSession())

  const login = useCallback(async (username: string, password: string) => {
    const response = await apiRequest<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    })
    const nextSession = { ...response, roleId: perfilToRoleId(response.perfil) }
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession))
    setSession(nextSession)
    return nextSession
  }, [])

  const logout = useCallback(async () => {
    try {
      await apiRequest<void>('/auth/logout', { method: 'POST' })
    } finally {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      setSession(null)
    }
  }, [])

  const value = useMemo(() => ({ session, login, logout }), [session, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de AuthProvider')
  }
  return context
}

export function perfilToRoleId(perfil: string): RoleId {
  switch (perfil) {
    case 'GESTOR':
      return 'gestor'
    case 'GERENTE':
      return 'gerente'
    case 'FUNCIONARIO':
      return 'funcionario'
    case 'RESPONSAVEL_ARMAZEM':
      return 'armazem'
    default:
      return 'funcionario'
  }
}

export function roleDefaultPath(roleId: RoleId) {
  return roles[roleId].defaultPath
}

function loadSession() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw) as AuthSession
    if (!parsed.accessToken || !parsed.perfil) {
      return null
    }
    return { ...parsed, roleId: parsed.roleId ?? perfilToRoleId(parsed.perfil) }
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}
