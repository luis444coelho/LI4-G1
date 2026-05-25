export type RoleId = 'gestor' | 'gerente' | 'funcionario' | 'armazem'

export type IconName =
  | 'dashboard'
  | 'reports'
  | 'suppliers'
  | 'orders'
  | 'users'
  | 'sync'
  | 'stock'
  | 'cash'
  | 'adjustment'
  | 'staff'
  | 'sale'
  | 'return'
  | 'goods'
  | 'inventory'
  | 'chain'
  | 'store'
  | 'employee'
  | 'warehouse'
  | 'lock'
  | 'chevron'

export type StatusTone = 'success' | 'danger' | 'warning' | 'neutral' | 'info'

export interface MenuItem {
  label: string
  path: string
  icon: IconName
}

export interface RoleConfig {
  id: RoleId
  label: string
  authLabel: string
  selectionTitle: string
  selectionSubtitle: string
  sidebarSubtitle: string
  defaultPath: string
  accent: string
  accentSoft: string
  accentBorder: string
  accentMuted: string
  iconBg: string
  iconBorder: string
  profileIcon: IconName
  menu: MenuItem[]
}

export const roles: Record<RoleId, RoleConfig> = {
  gestor: {
    id: 'gestor',
    label: 'Gestor da Cadeia',
    authLabel: 'Gestor da Cadeia',
    selectionTitle: 'Gestor da\nCadeia',
    selectionSubtitle: 'Proprietário',
    sidebarSubtitle: 'Gestor da Cadeia',
    defaultPath: '/gestor/dashboard',
    accent: '#4a83f1',
    accentSoft: 'rgba(74, 131, 241, 0.16)',
    accentBorder: 'rgba(74, 131, 241, 0.65)',
    accentMuted: 'rgba(74, 131, 241, 0.22)',
    iconBg: 'rgba(58, 109, 208, 0.16)',
    iconBorder: 'rgba(74, 131, 241, 0.45)',
    profileIcon: 'chain',
    menu: [
      { label: 'Dashboard', path: '/gestor/dashboard', icon: 'dashboard' },
      { label: 'Relatórios', path: '/gestor/relatorios', icon: 'reports' },
      { label: 'Fornecedores', path: '/gestor/fornecedores', icon: 'suppliers' },
      { label: 'Encomendas', path: '/gestor/encomendas', icon: 'orders' },
      { label: 'Utilizadores', path: '/gestor/utilizadores', icon: 'users' },
      { label: 'Sincronização', path: '/gestor/sincronizacao', icon: 'sync' },
    ],
  },
  gerente: {
    id: 'gerente',
    label: 'Gerente de Loja',
    authLabel: 'Gerente de Loja',
    selectionTitle: 'Gerente de\nLoja',
    selectionSubtitle: 'Loja Norte',
    sidebarSubtitle: 'Gerente de Loja',
    defaultPath: '/gerente/stock',
    accent: '#18c5bd',
    accentSoft: 'rgba(24, 197, 189, 0.16)',
    accentBorder: 'rgba(24, 197, 189, 0.65)',
    accentMuted: 'rgba(24, 197, 189, 0.22)',
    iconBg: 'rgba(20, 168, 161, 0.16)',
    iconBorder: 'rgba(55, 229, 220, 0.45)',
    profileIcon: 'store',
    menu: [
      { label: 'Stock e alertas', path: '/gerente/stock', icon: 'stock' },
      { label: 'Fecho de caixa', path: '/gerente/fecho-caixa', icon: 'cash' },
      { label: 'Relatórios', path: '/gerente/relatorios', icon: 'reports' },
      { label: 'Ajuste inventário', path: '/gerente/ajuste-inventario', icon: 'adjustment' },
      { label: 'Encomendas', path: '/gerente/encomendas', icon: 'orders' },
      { label: 'Entrada mercadoria', path: '/gerente/entrada-mercadoria', icon: 'goods' },
      { label: 'Funcionários', path: '/gerente/funcionarios', icon: 'staff' },
    ],
  },
  funcionario: {
    id: 'funcionario',
    label: 'Funcionário',
    authLabel: 'Funcionário',
    selectionTitle: 'Funcionário',
    selectionSubtitle: 'Operador PDV',
    sidebarSubtitle: 'Funcionário',
    defaultPath: '/funcionario/venda',
    accent: '#ff4d59',
    accentSoft: 'rgba(255, 77, 89, 0.16)',
    accentBorder: 'rgba(255, 77, 89, 0.62)',
    accentMuted: 'rgba(255, 77, 89, 0.22)',
    iconBg: 'rgba(176, 63, 72, 0.18)',
    iconBorder: 'rgba(255, 111, 120, 0.45)',
    profileIcon: 'employee',
    menu: [
      { label: 'Registar venda', path: '/funcionario/venda', icon: 'sale' },
      { label: 'Processar devolução', path: '/funcionario/devolucao', icon: 'return' },
    ],
  },
  armazem: {
    id: 'armazem',
    label: 'Resp. de Armazém',
    authLabel: 'Resp. de Armazém',
    selectionTitle: 'Resp. de\nArmazém',
    selectionSubtitle: 'Armazém · Loja Norte',
    sidebarSubtitle: 'Resp. de Armazém',
    defaultPath: '/armazem/stock',
    accent: '#f3ab16',
    accentSoft: 'rgba(243, 171, 22, 0.16)',
    accentBorder: 'rgba(243, 171, 22, 0.62)',
    accentMuted: 'rgba(243, 171, 22, 0.22)',
    iconBg: 'rgba(130, 93, 18, 0.2)',
    iconBorder: 'rgba(243, 171, 22, 0.45)',
    profileIcon: 'warehouse',
    menu: [
      { label: 'Consultar stock', path: '/armazem/stock', icon: 'stock' },
      { label: 'Entrada mercadoria', path: '/armazem/entrada-mercadoria', icon: 'goods' },
      { label: 'Inventário físico', path: '/armazem/inventario', icon: 'inventory' },
    ],
  },
}

export const roleList = Object.values(roles)

export function getRole(id: string | undefined): RoleConfig | undefined {
  if (!id || !(id in roles)) {
    return undefined
  }

  return roles[id as RoleId]
}

export const dashboardMetrics = [
  {
    label: 'Vendas hoje (total)',
    value: '€10.840',
    footnote: '↑ 8% vs. ontem',
    tone: 'success' as const,
  },
  {
    label: 'Margem média',
    value: '28%',
    footnote: 'Todas as lojas',
    tone: 'neutral' as const,
  },
  {
    label: 'Lojas ativas',
    value: '3 / 3',
    footnote: '↑ Todas operacionais',
    tone: 'success' as const,
  },
  {
    label: 'Alertas de stock',
    value: '4',
    footnote: '↓ Produtos abaixo mínimo',
    tone: 'danger' as const,
  },
]

export const salesByStore = [
  { store: 'Norte', amount: '€4280', height: 72, color: '#4777d8' },
  { store: 'Centro', amount: '€3620', height: 72, color: '#25a49d' },
  { store: 'Sul', amount: '€2940', height: 63, color: '#e49a07' },
]

export const performanceByStore = [
  { store: 'Loja Norte', sales: '€4280', margin: '28%' },
  { store: 'Loja Centro', sales: '€3620', margin: '31%' },
  { store: 'Loja Sul', sales: '€2940', margin: '25%' },
]

export const stockAlerts = [
  { product: 'Água 0.5L', store: 'Loja Norte', quantity: '8 un', minimum: 'mín. 20' },
  { product: 'Sumo laranja', store: 'Loja Norte', quantity: '6 un', minimum: 'mín. 15' },
  { product: 'Sandwich mista', store: 'Loja Norte', quantity: '4 un', minimum: 'mín. 10' },
  { product: 'Iogurte natural', store: 'Loja Centro', quantity: '5 un', minimum: 'mín. 20' },
]

export const reportRows = [
  { category: 'Bebidas', sales: '€4.280', units: '2.140', margin: '31%' },
  { category: 'Padaria', sales: '€2.960', units: '1.350', margin: '24%' },
  { category: 'Pronto consumo', sales: '€3.120', units: '890', margin: '38%' },
  { category: 'Higiene', sales: '€1.480', units: '340', margin: '42%' },
  { category: 'Outros', sales: '€980', units: '210', margin: '28%' },
]

export const supplierRows = [
  {
    name: 'Distribuidora Norte Lda.',
    nif: '512 345 678',
    categories: 'Bebidas, Lacticínios',
    term: 'D+1',
    tone: 'info' as StatusTone,
    status: 'Ativo',
    statusTone: 'success' as StatusTone,
  },
  {
    name: 'Panificadora Central',
    nif: '501 234 567',
    categories: 'Padaria, Pronto consumo',
    term: 'D+0',
    tone: 'info' as StatusTone,
    status: 'Ativo',
    statusTone: 'success' as StatusTone,
  },
  {
    name: 'HigieneMax Lda.',
    nif: '509 876 543',
    categories: 'Higiene, Limpeza',
    term: 'D+2',
    tone: 'info' as StatusTone,
    status: 'Ativo',
    statusTone: 'success' as StatusTone,
  },
  {
    name: 'SnacksPortugal S.A.',
    nif: '506 543 210',
    categories: 'Snacks, Doces',
    term: 'D+1',
    tone: 'info' as StatusTone,
    status: 'Inativo',
    statusTone: 'neutral' as StatusTone,
  },
]

export const orderHistory = [
  { number: 'E-2026-041', supplier: 'Distribuidora Norte', date: '18/04', status: 'Recebida', tone: 'success' as StatusTone },
  { number: 'E-2026-040', supplier: 'Panificadora Central', date: '17/04', status: 'Recebida', tone: 'success' as StatusTone },
  { number: 'E-2026-039', supplier: 'HigieneMax', date: '16/04', status: 'Enviada', tone: 'info' as StatusTone },
  { number: 'E-2026-038', supplier: 'Distribuidora Norte', date: '15/04', status: 'Pendente', tone: 'warning' as StatusTone },
]

export const usersRows = [
  { initials: 'AC', name: 'Ana Costa', role: 'Gerente', store: 'Loja Norte', status: 'Ativo', tone: 'success' as StatusTone },
  { initials: 'PN', name: 'Pedro Nunes', role: 'Funcionário', store: 'Loja Norte', status: 'Ativo', tone: 'success' as StatusTone },
  { initials: 'SM', name: 'Sofia Martins', role: 'Resp. Armazém', store: 'Loja Norte', status: 'Ativo', tone: 'success' as StatusTone },
  { initials: 'CS', name: 'Carlos Silva', role: 'Funcionário', store: 'Loja Centro', status: 'Ativo', tone: 'success' as StatusTone },
  { initials: 'MJ', name: 'Maria João', role: 'Gerente', store: 'Loja Centro', status: 'Inativo', tone: 'neutral' as StatusTone },
]

export const syncMetrics = [
  { label: 'Última sincronização', value: '19/04 · 20:02', footnote: 'Ontem automática', tone: 'neutral' as const },
  { label: 'Registos pendentes', value: '0', footnote: '↑ Totalmente sincronizado', tone: 'success' as const },
  { label: 'Conflitos detectados', value: '0', footnote: 'Resolução last-write-wins', tone: 'neutral' as const },
]

export const syncHistory = [
  { store: 'Loja Norte', datetime: '19/04 · 20:02', records: '248', status: 'Concluída', tone: 'success' as StatusTone },
  { store: 'Loja Centro', datetime: '19/04 · 20:05', records: '193', status: 'Concluída', tone: 'success' as StatusTone },
  { store: 'Loja Sul', datetime: '19/04 · 20:08', records: '176', status: 'Concluída', tone: 'success' as StatusTone },
  { store: 'Loja Norte', datetime: '18/04 · 20:02', records: '231', status: 'Concluída', tone: 'success' as StatusTone },
  { store: 'Loja Centro', datetime: '18/04 · 20:04', records: '210', status: 'Conflitos', tone: 'danger' as StatusTone },
]

export const stockRows = [
  { product: 'Água 0.5L', category: 'Bebidas', stock: 8, minimum: 20, levelPercent: 40, status: 'Crítico', tone: 'danger' as StatusTone },
  { product: 'Pão de forma', category: 'Padaria', stock: 14, minimum: 15, levelPercent: 90, status: 'Normal', tone: 'success' as StatusTone },
  { product: 'Coca-Cola 0.33L', category: 'Bebidas', stock: 42, minimum: 30, levelPercent: 100, status: 'Normal', tone: 'success' as StatusTone },
  { product: 'Sumo laranja', category: 'Bebidas', stock: 6, minimum: 15, levelPercent: 40, status: 'Crítico', tone: 'danger' as StatusTone },
  { product: 'Iogurte natural', category: 'Lacticínios', stock: 18, minimum: 20, levelPercent: 86, status: 'Normal', tone: 'success' as StatusTone },
  { product: 'Croissant', category: 'Padaria', stock: 9, minimum: 10, levelPercent: 85, status: 'Normal', tone: 'success' as StatusTone },
  { product: 'Desodorizante', category: 'Higiene', stock: 25, minimum: 8, levelPercent: 100, status: 'Normal', tone: 'success' as StatusTone },
  { product: 'Sandwich mista', category: 'Pronto consumo', stock: 4, minimum: 10, levelPercent: 40, status: 'Crítico', tone: 'danger' as StatusTone },
]

export const cashClosingSummary = [
  { label: 'Numerário', value: '€842,50' },
  { label: 'Cartão', value: '€1.420,00' },
  { label: 'MB Way', value: '€387,00' },
  { label: 'Total vendas', value: '€2.649,50', strong: true },
  { label: 'N.º transações', value: '78' },
  { label: 'Ticket médio', value: '€33,97' },
]

export const cashClosingHistory = [
  { date: '19/04', value: '€2.480', status: 'Fechado' },
  { date: '18/04', value: '€3.120', status: 'Fechado' },
  { date: '17/04', value: '€2.890', status: 'Fechado' },
  { date: '16/04', value: '€3.340', status: 'Fechado' },
  { date: '15/04', value: '€2.210', status: 'Fechado' },
]

export const adjustmentHistory = [
  { product: 'Pão de forma', reason: 'Quebra', value: '-2', tone: 'danger' as StatusTone, date: '19/04' },
  { product: 'Sumo laranja', reason: 'Validade', value: '-4', tone: 'danger' as StatusTone, date: '18/04' },
  { product: 'Água 0.5L', reason: 'Correção', value: '+5', tone: 'success' as StatusTone, date: '17/04' },
]

export const employeeRows = [
  { initials: 'PN', name: 'Pedro Nunes', meta: 'Funcionário · @pedro.nunes', status: 'Ativo', tone: 'success' as StatusTone },
  { initials: 'JA', name: 'Joana Alves', meta: 'Funcionário · @joana.alves', status: 'Ativo', tone: 'success' as StatusTone },
  { initials: 'RC', name: 'Rui Costa', meta: 'Funcionário · @rui.costa', status: 'Inativo', tone: 'neutral' as StatusTone },
]

export const saleRows = [
  { product: 'Água 0.5L', price: '€0.50', iva: 'IVA 23%', quantity: '×2', subtotal: '€1.00' },
  { product: 'Pão de forma', price: '€1.99', iva: 'IVA 6%', quantity: '×1', subtotal: '€1.99' },
  { product: 'Sumo laranja 0.33L', price: '€1.20', iva: 'IVA 23%', quantity: '×1', subtotal: '€1.20' },
]

export const receiptRows = [
  { product: 'Água 0.5L', ordered: 48, received: 48, difference: '–', tone: 'neutral' as StatusTone },
  { product: 'Coca-Cola 0.33L', ordered: 24, received: 24, difference: '–', tone: 'neutral' as StatusTone },
  { product: 'Sumo laranja', ordered: 36, received: 30, difference: '-6', tone: 'danger' as StatusTone },
  { product: 'Iogurte natural', ordered: 24, received: 24, difference: '–', tone: 'neutral' as StatusTone },
]

export const inventoryRows = [
  { product: 'Água 0.5L', system: 8, counted: 8, difference: 'OK', tone: 'neutral' as StatusTone },
  { product: 'Pão de forma', system: 14, counted: 12, difference: '-2', tone: 'danger' as StatusTone },
  { product: 'Coca-Cola 0.33L', system: 42, counted: 42, difference: 'OK', tone: 'neutral' as StatusTone },
  { product: 'Sumo laranja', system: 6, counted: 8, difference: '+2', tone: 'warning' as StatusTone },
  { product: 'Iogurte natural', system: 18, counted: 20, difference: '+2', tone: 'warning' as StatusTone },
  { product: 'Croissant', system: 9, counted: 9, difference: 'OK', tone: 'neutral' as StatusTone },
  { product: 'Desodorizante', system: 25, counted: 25, difference: 'OK', tone: 'neutral' as StatusTone },
  { product: 'Sandwich mista', system: 4, counted: 4, difference: 'OK', tone: 'neutral' as StatusTone },
]
