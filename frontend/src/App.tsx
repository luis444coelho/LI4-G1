import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import { AppShell } from './components/AppShell'
import { roles, type RoleId } from './data/mockData'
import { AuthProvider, useAuth } from './lib/auth'
import { roleDefaultPath } from './lib/authRoutes'
import { AuthenticationPage, ProfileSelectionPage } from './pages/AuthPages'
import { ArmazemGoodsReceiptPage, ArmazemInventoryPage, ArmazemStockPage } from './pages/ArmazemPages'
import { FuncionarioReturnPage, FuncionarioSalePage } from './pages/FuncionarioPages'
import { GerenteAdjustmentPage, GerenteCashPage, GerenteEmployeesPage, GerenteReportsPage, GerenteStockPage } from './pages/GerentePages'
import { GestorDashboardPage, GestorOrdersPage, GestorReportsPage, GestorSuppliersPage, GestorSyncPage, GestorUsersPage } from './pages/GestorPages'

function ProtectedShell({ roleKey, title, component }: { roleKey: RoleId; title: string; component: JSX.Element }) {
  const { session } = useAuth()

  if (!session) {
    return <Navigate to="/" replace />
  }
  if (session.roleId !== roleKey) {
    return <Navigate to={roleDefaultPath(session.roleId)} replace />
  }

  return (
    <AppShell role={roles[roleKey]} title={title}>
      {component}
    </AppShell>
  )
}

function StartPage() {
  const { session } = useAuth()
  if (session) {
    return <Navigate to={roleDefaultPath(session.roleId)} replace />
  }
  return <ProfileSelectionPage />
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<StartPage />} />
          <Route path="/auth/:roleId" element={<AuthenticationPage />} />

          <Route path="/gestor/dashboard" element={<ProtectedShell roleKey="gestor" title="Dashboard" component={<GestorDashboardPage />} />} />
          <Route path="/gestor/relatorios" element={<ProtectedShell roleKey="gestor" title="Relatórios" component={<GestorReportsPage />} />} />
          <Route path="/gestor/fornecedores" element={<ProtectedShell roleKey="gestor" title="Fornecedores" component={<GestorSuppliersPage />} />} />
          <Route path="/gestor/encomendas" element={<ProtectedShell roleKey="gestor" title="Encomendas" component={<GestorOrdersPage />} />} />
          <Route path="/gestor/utilizadores" element={<ProtectedShell roleKey="gestor" title="Utilizadores" component={<GestorUsersPage />} />} />
          <Route path="/gestor/sincronizacao" element={<ProtectedShell roleKey="gestor" title="Sincronização" component={<GestorSyncPage />} />} />

          <Route path="/gerente/stock" element={<ProtectedShell roleKey="gerente" title="Stock e alertas" component={<GerenteStockPage />} />} />
          <Route path="/gerente/fecho-caixa" element={<ProtectedShell roleKey="gerente" title="Fecho de caixa" component={<GerenteCashPage />} />} />
          <Route path="/gerente/relatorios" element={<ProtectedShell roleKey="gerente" title="Relatórios" component={<GerenteReportsPage />} />} />
          <Route path="/gerente/ajuste-inventario" element={<ProtectedShell roleKey="gerente" title="Ajuste inventário" component={<GerenteAdjustmentPage />} />} />
          <Route path="/gerente/encomendas" element={<ProtectedShell roleKey="gerente" title="Encomendas" component={<GestorOrdersPage fixedStore />} />} />
          <Route path="/gerente/entrada-mercadoria" element={<ProtectedShell roleKey="gerente" title="Entrada mercadoria" component={<ArmazemGoodsReceiptPage />} />} />
          <Route path="/gerente/funcionarios" element={<ProtectedShell roleKey="gerente" title="Funcionários" component={<GerenteEmployeesPage />} />} />

          <Route path="/funcionario/venda" element={<ProtectedShell roleKey="funcionario" title="Registar venda" component={<FuncionarioSalePage />} />} />
          <Route path="/funcionario/devolucao" element={<ProtectedShell roleKey="funcionario" title="Processar devolução" component={<FuncionarioReturnPage />} />} />

          <Route path="/armazem/stock" element={<ProtectedShell roleKey="armazem" title="Consultar stock" component={<ArmazemStockPage />} />} />
          <Route path="/armazem/entrada-mercadoria" element={<ProtectedShell roleKey="armazem" title="Entrada mercadoria" component={<ArmazemGoodsReceiptPage />} />} />
          <Route path="/armazem/inventario" element={<ProtectedShell roleKey="armazem" title="Inventário físico" component={<ArmazemInventoryPage />} />} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
