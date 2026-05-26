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
