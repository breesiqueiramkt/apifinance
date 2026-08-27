import { createContext, useContext, useState, useCallback } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

// Não existe cadastro neste app: o único usuário (compartilhado por você e
// sua esposa) é criado no backend na primeira subida da aplicação. A única
// ação de autenticação disponível aqui é login.
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('finance_app_user')
    return raw ? JSON.parse(raw) : null
  })

  const persist = (token, userData) => {
    localStorage.setItem('finance_app_token', token)
    localStorage.setItem('finance_app_user', JSON.stringify(userData))
    setUser(userData)
  }

  const login = useCallback(async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password })
    persist(data.token, { id: data.userId, name: data.name, email: data.email })
    return data
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('finance_app_token')
    localStorage.removeItem('finance_app_user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth precisa estar dentro de um AuthProvider')
  return ctx
}
