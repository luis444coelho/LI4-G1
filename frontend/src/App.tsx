import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

import { AppShell } from './components/AppShell'
import { roles } from './data/mockData'
import { AuthenticationPage, ProfileSelectionPage } from './pages/AuthPages'
import { ArmazemGoodsReceiptPage, ArmazemInventoryPage, ArmazemStockPage } from './pages/ArmazemPages'
import { FuncionarioReturnPage, FuncionarioSalePage } from './pages/FuncionarioPages'
import { GerenteAdjustmentPage, GerenteCashPage, GerenteEmployeesPage, GerenteReportsPage, GerenteStockPage } from './pages/GerentePages'
import { GestorDashboardPage, GestorOrdersPage, GestorReportsPage, GestorSuppliersPage, GestorSyncPage, GestorUsersPage } from './pages/GestorPages'

function withShell(roleKey: keyof typeof roles, title: string, component: JSX.Element) {
  return (
    <AppShell role={roles[roleKey]} title={title}>
      {component}
    </AppShell>
  )
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ProfileSelectionPage />} />
        <Route path="/auth/:roleId" element={<AuthenticationPage />} />

        <Route path="/gestor/dashboard" element={withShell('gestor', 'Dashboard', <GestorDashboardPage />)} />
        <Route path="/gestor/relatorios" element={withShell('gestor', 'Relatórios', <GestorReportsPage />)} />
        <Route path="/gestor/fornecedores" element={withShell('gestor', 'Fornecedores', <GestorSuppliersPage />)} />
        <Route path="/gestor/encomendas" element={withShell('gestor', 'Encomendas', <GestorOrdersPage />)} />
        <Route path="/gestor/utilizadores" element={withShell('gestor', 'Utilizadores', <GestorUsersPage />)} />
        <Route path="/gestor/sincronizacao" element={withShell('gestor', 'Sincronização', <GestorSyncPage />)} />

        <Route path="/gerente/stock" element={withShell('gerente', 'Stock e alertas', <GerenteStockPage />)} />
        <Route path="/gerente/fecho-caixa" element={withShell('gerente', 'Fecho de caixa', <GerenteCashPage />)} />
        <Route path="/gerente/relatorios" element={withShell('gerente', 'Relatórios', <GerenteReportsPage />)} />
        <Route path="/gerente/ajuste-inventario" element={withShell('gerente', 'Ajuste inventário', <GerenteAdjustmentPage />)} />
        <Route path="/gerente/funcionarios" element={withShell('gerente', 'Funcionários', <GerenteEmployeesPage />)} />

        <Route path="/funcionario/venda" element={withShell('funcionario', 'Registar venda', <FuncionarioSalePage />)} />
        <Route path="/funcionario/devolucao" element={withShell('funcionario', 'Processar devolução', <FuncionarioReturnPage />)} />

        <Route path="/armazem/stock" element={withShell('armazem', 'Consultar stock', <ArmazemStockPage />)} />
        <Route path="/armazem/entrada-mercadoria" element={withShell('armazem', 'Entrada mercadoria', <ArmazemGoodsReceiptPage />)} />
        <Route path="/armazem/inventario" element={withShell('armazem', 'Inventário físico', <ArmazemInventoryPage />)} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
