export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
export const AUTH_STORAGE_KEY = 'mini-formiga.auth'

interface StoredSession {
  tokenType: string
  accessToken: string
}

export interface LoginResponse {
  tokenType: string
  accessToken: string
  utilizadorId: string
  nome: string
  perfil: string
  lojaId: string
  permissoes: string[]
}

export interface VendasPorLojaResponse {
  lojaId: string
  loja: string
  total: number
  iva: number
  margem: number
  numeroVendas: number
}

export interface DashboardResponse {
  totalVendas: number
  totalIva: number
  margem: number
  numeroVendas: number
  numeroLojasComVendas: number
  totalLojas: number
  alertasAtivos: number
  ticketMedio: number
  vendasPorLoja: VendasPorLojaResponse[]
}

export interface StockItemResponse {
  produtoId: string
  produto: string
  categoria: string
  lojaId: string
  loja: string
  quantidade: number
  nivelMinimo: number | null
  precisaReposicao: boolean
  valorPrecoCusto: number
  dataUltimaAtualizacao: string
}

export interface RelatorioStockResponse {
  totalProdutos: number
  totalUnidades: number
  valorStockPrecoCusto: number
  produtosReposicao: number
  alertasAtivos: number
  itens: StockItemResponse[]
}

export class ApiError extends Error {
  status: number
  code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = getStoredToken()

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })
  const payload = await parseResponse(response)

  if (!response.ok) {
    const message = payload && typeof payload === 'object' && 'message' in payload
      ? String(payload.message)
      : 'Pedido rejeitado pelo servidor'
    const code = payload && typeof payload === 'object' && 'code' in payload
      ? String(payload.code)
      : undefined
    throw new ApiError(response.status, message, code)
  }

  return payload as T
}

function getStoredToken() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) {
      return null
    }
    const session = JSON.parse(raw) as Partial<StoredSession>
    return session.accessToken ?? null
  } catch {
    return null
  }
}

async function parseResponse(response: Response) {
  if (response.status === 204) {
    return undefined
  }
  const text = await response.text()
  if (!text) {
    return undefined
  }
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    return JSON.parse(text) as unknown
  }
  return text
}
