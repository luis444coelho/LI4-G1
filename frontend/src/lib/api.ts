export const LOCAL_API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
export const CENTRAL_API_BASE_URL = import.meta.env.VITE_CENTRAL_API_BASE_URL ?? 'http://localhost:8081/api/v1'
export const AUTH_STORAGE_KEY = 'mini-formiga.auth'

interface StoredSession {
  tokenType: string
  accessToken: string
  apiBaseUrl?: string
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

export interface PeriodoResponse {
  inicio: string
  fim: string
}

export interface LinhaVendaRelatorioResponse {
  vendaId: string
  dataHora: string
  lojaId: string
  loja: string
  produtoId: string
  produto: string
  categoria: string
  quantidade: number
  valorSemIva: number
  iva: number
  valorComIva: number
  margem: number
}

export interface VendasPorDiaResponse {
  data: string
  total: number
  iva: number
  margem: number
  numeroVendas: number
}

export interface RelatorioVendasResponse {
  periodo: PeriodoResponse
  lojaId?: string
  categoriaId?: string
  produtoId?: string
  turno?: string
  totalSemIva: number
  totalIva: number
  totalComIva: number
  margem: number
  numeroVendas: number
  vendasPorLoja: VendasPorLojaResponse[]
  vendasPorDia: VendasPorDiaResponse[]
  linhas: LinhaVendaRelatorioResponse[]
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

export interface RentabilidadeProdutoResponse {
  produtoId: string
  produto: string
  categoria: string
  quantidadeVendida: number
  receitaSemIva: number
  custo: number
  margem: number
  margemPercentagem: number
}

export interface RentabilidadeCategoriaResponse {
  categoria: string
  quantidadeVendida: number
  receitaSemIva: number
  custo: number
  margem: number
  margemPercentagem: number
}

export interface RelatorioRentabilidadeResponse {
  periodo: PeriodoResponse
  lojaId?: string
  categoriaId?: string
  produtoId?: string
  turno?: string
  receitaSemIva: number
  custoTotal: number
  margemTotal: number
  margemPercentagem: number
  produtos: RentabilidadeProdutoResponse[]
  categorias: RentabilidadeCategoriaResponse[]
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ProdutoResponse {
  id: string
  codigoBarras: string
  nome: string
  descricao?: string
  precoVenda: number
  precoCusto: number
  margem: number
  categoria: string
  taxaIva: number
  ativo: boolean
}

export interface CategoriaResponse {
  id: string
  nome: string
  descricao?: string
}

export interface LinhaVendaResponse {
  id: string
  produtoId: string
  produto: string
  quantidade: number
  precoUnitario: number
  totalLinha: number
  anulada: boolean
}

export interface VendaResponse {
  id: string
  lojaId: string
  operadorId: string
  dataHora: string
  anulada: boolean
  meioPagamento: string | null
  subtotal: number
  iva: number
  total: number
  linhas: LinhaVendaResponse[]
}

export interface FaturaResponse {
  id: string
  vendaId: string
  numeroFatura: string
  serie: string
  tipo: string
  nifCliente?: string
  nomeCliente?: string
  dataEmissao: string
  totalSemIva: number
  totalIva: number
  totalComIva: number
}

export interface MeioPagamentoResponse {
  id: string
  tipo: string
  descricao: string
}

export interface StockResponse {
  produtoId: string
  produto: string
  lojaId: string
  quantidade: number
  nivelMinimo: number | null
  precisaReposicao: boolean
}

export interface AlertaStockResponse {
  id: string
  produtoId: string
  produto: string
  lojaId: string
  dataHora: string
  quantidadeNoMomento: number
  lido: boolean
  resolvido: boolean
  dataResolucao?: string
  destinatarios: string[]
}

export interface AjusteInventarioResponse {
  id: string
  produtoId: string
  produto: string
  lojaId: string
  quantidade: number
  motivo: string
  utilizadorId: string
  dataHora: string
}

export interface MotivoAjusteResponse {
  id: string
  codigo: string
  descricao: string
}

export interface FechoCaixaResponse {
  id: string
  lojaId: string
  gerenteId: string
  data: string
  totalNumerario: number
  totalCartao: number
  totalMbway: number
  totalGeral: number
  observacoesDiscrepancia?: string
  confirmado: boolean
}

export interface UtilizadorResponse {
  id: string
  username: string
  nome: string
  email: string
  perfil: string
  perfilId?: string
  loja: string
  lojaId: string
  ativo: boolean
}

export interface PerfilResponse {
  id: string
  nome: string
  permissoes: string[]
}

export interface LojaResponse {
  id: string
  nome: string
  morada: string
  nif: string
  ativa: boolean
}

export interface FornecedorResponse {
  id: string
  nome: string
  nif: string
  morada: string
  telefone: string
  email: string
  ativo: boolean
  horarioInicioArmazem: string
  horarioFimArmazem: string
}

export interface CondicaoComercialResponse {
  id: string
  fornecedorId: string
  produtoId: string
  produto: string
  precoUnitario: number
  prazoEntregaDias: number
  quantidadeMinima: number
  dataVigencia: string
}

export interface EncomendaResponse {
  id: string
  numeroDocumento: string
  lojaId: string
  fornecedorId: string
  fornecedor: string
  estado: string
  dataSubmissao: string
  dataProcessamento: string
  totalEstimado: number
  linhas: Array<{ id: string; produtoId: string; produto: string; quantidade: number; precoUnitario: number; totalLinha: number }>
}

export interface ProximaGuiaRemessaResponse {
  numero: string
}

export interface SugestaoEncomendaResponse {
  fornecedorId: string
  fornecedor: string
  produtoId: string
  produto: string
  lojaId: string
  quantidadeAtual: number
  nivelMinimo: number | null
  quantidadeSugerida: number
  precoUnitario: number
}

export interface EntradaMercadoriaResponse {
  id: string
  encomendaId: string
  lojaId: string
  responsavelId: string
  produtoId: string
  produto: string
  guiaNumero: string
  dataHora: string
  quantidadeRecebida: number
  quantidadeEncomendada: number
  discrepancia: number
  observacoes?: string
}

export interface InventarioFisicoResponse {
  id: string
  lojaId: string
  utilizadorId: string
  dataInicio: string
  dataFecho?: string
  fechado: boolean
  totalDiscrepancias: number
}

export interface LinhaInventarioResponse {
  id: string
  produtoId: string
  produto: string
  quantidadeContada: number
  quantidadeSistema: number
  discrepancia: number
}

export interface LocalizacaoProdutoResponse {
  produtoId: string
  corredor: string
  prateleira: string
}

export interface SincronizacaoResponse {
  id: string
  lojaId: string
  estado: string
  inicio: string
  fim?: string
  proximaTentativa?: string
  quantidadeRegistos: number
  conflitosResolvidos: number
  mensagemErro?: string
}

export interface ConflitoSincronizacaoResponse {
  sincronizacaoId: string
  lojaId: string
  dataHora: string
  estado: string
  conflitosResolvidos: number
  conflitosJson?: string
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

interface ApiRequestOptions extends RequestInit {
  apiBaseUrl?: string
}

export interface ApiDownloadResponse {
  blob: Blob
  filename: string
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = getStoredToken()
  const { apiBaseUrl, ...requestOptions } = options
  const isLoginRequest = path === '/auth/login'

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }
  if (token && !isLoginRequest) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${apiBaseUrl ?? getStoredApiBaseUrl()}${path}`, {
    ...requestOptions,
    headers,
  })
  const payload = await parseResponse(response)

  if (!response.ok) {
    const message = payload && typeof payload === 'object' && 'message' in payload
      ? String(payload.message)
      : typeof payload === 'string' && payload.trim()
        ? payload
      : `Pedido rejeitado pelo servidor (${response.status})`
    const code = payload && typeof payload === 'object' && 'code' in payload
      ? String(payload.code)
      : undefined
    throw new ApiError(response.status, message, code)
  }

  return payload as T
}

export async function apiDownload(path: string, options: ApiRequestOptions = {}): Promise<ApiDownloadResponse> {
  const headers = new Headers(options.headers)
  const token = getStoredToken()
  const { apiBaseUrl, ...requestOptions } = options

  if (!headers.has('Accept')) {
    headers.set('Accept', '*/*')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${apiBaseUrl ?? getStoredApiBaseUrl()}${path}`, {
    ...requestOptions,
    headers,
  })

  if (!response.ok) {
    const text = await response.text()
    let message = 'Pedido rejeitado pelo servidor'
    try {
      const payload = JSON.parse(text) as { message?: string }
      message = payload.message ?? message
    } catch {
      message = text || message
    }
    throw new ApiError(response.status, message)
  }

  const disposition = response.headers.get('content-disposition') ?? ''
  const filenameMatch = /filename="?(?<filename>[^";]+)"?/i.exec(disposition)
  return {
    blob: await response.blob(),
    filename: filenameMatch?.groups?.filename ?? 'mini-formiga-relatorio',
  }
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

function getStoredApiBaseUrl() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) {
      return LOCAL_API_BASE_URL
    }
    const session = JSON.parse(raw) as Partial<StoredSession>
    return session.apiBaseUrl ?? LOCAL_API_BASE_URL
  } catch {
    return LOCAL_API_BASE_URL
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
