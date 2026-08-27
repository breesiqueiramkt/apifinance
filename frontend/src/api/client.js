import axios from 'axios'

// Em dev: usa '/api', que o Vite reencaminha para o backend local (vite.config.js proxy).
// Em produção: defina VITE_API_BASE_URL com a URL pública do backend no Render,
// já que build estático na Vercel não tem proxy de servidor - a chamada vai direto
// pro Render, e por isso o backend precisa liberar a origem da Vercel no CORS_ALLOWED_ORIGINS.
const baseURL = import.meta.env.VITE_API_BASE_URL
  ? `${import.meta.env.VITE_API_BASE_URL}/api`
  : '/api'

const api = axios.create({
  baseURL
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('finance_app_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('finance_app_token')
      localStorage.removeItem('finance_app_user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
