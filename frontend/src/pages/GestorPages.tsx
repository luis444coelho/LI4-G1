import { useEffect, useMemo, useState } from 'react'

import { Button, Callout, InitialAvatar, MetricCard, Panel, SelectField, StatusBadge, TextField } from '../components/ui'
import { ReportsContent } from '../components/pageSections'
import { apiRequest, type DashboardResponse, type EncomendaResponse, type FornecedorResponse, type LojaResponse, type PageResponse, type PerfilResponse, type RelatorioStockResponse, type SincronizacaoResponse, type SugestaoEncomendaResponse, type UtilizadorResponse } from '../lib/api'
import { useAuth } from '../lib/auth'

const currencyFormatter = new Intl.NumberFormat('pt-PT', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 2,
})

const numberFormatter = new Intl.NumberFormat('pt-PT')

function money(value: number) {
  return currencyFormatter.format(value)
}

export function GestorDashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [stock, setStock] = useState<RelatorioStockResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false

    Promise.all([
      apiRequest<DashboardResponse>('/dashboard'),
      apiRequest<RelatorioStockResponse>('/relatorios/stock'),
    ])
      .then(([dashboardResponse, stockResponse]) => {
        if (!ignore) {
          setDashboard(dashboardResponse)
          setStock(stockResponse)
        }
      })
      .catch(() => {
        if (!ignore) {
          setError('Não foi possível carregar o dashboard.')
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false)
        }
      })

    return () => {
      ignore = true
    }
  }, [])

  const metrics = useMemo(() => {
    if (!dashboard) {
      return []
    }
    return [
      {
        label: 'Vendas no período',
        value: money(dashboard.totalVendas),
        footnote: `${numberFormatter.format(dashboard.numeroVendas)} vendas`,
        tone: dashboard.totalVendas > 0 ? 'success' as const : 'neutral' as const,
      },
      {
        label: 'Margem',
        value: money(dashboard.margem),
        footnote: `Ticket médio ${money(dashboard.ticketMedio)}`,
        tone: 'neutral' as const,
      },
      {
        label: 'Lojas com vendas',
        value: `${dashboard.numeroLojasComVendas} / ${dashboard.totalLojas}`,
        footnote: 'Todas as lojas disponíveis',
        tone: 'success' as const,
      },
      {
        label: 'Alertas de stock',
        value: String(dashboard.alertasAtivos),
        footnote: `${stock?.produtosReposicao ?? 0} produtos em reposição`,
        tone: dashboard.alertasAtivos > 0 ? 'danger' as const : 'success' as const,
      },
    ]
  }, [dashboard, stock])

  const maxSales = Math.max(1, ...(dashboard?.vendasPorLoja.map((item) => item.total) ?? [1]))
  const stockAlerts = (stock?.itens ?? []).filter((item) => item.precisaReposicao).slice(0, 5)

  if (loading) {
    return (
      <Panel>
        <p className="mf-empty-state">A carregar dados...</p>
      </Panel>
    )
  }

  if (error || !dashboard) {
    return (
      <Callout tone="warning">
        {error ?? 'Dashboard indisponível.'}
      </Callout>
    )
  }

  return (
    <div className="mf-stack">
      <div className="mf-metrics-grid">
        {metrics.map((metric) => (
          <MetricCard
            key={metric.label}
            label={metric.label}
            value={metric.value}
            footnote={metric.footnote}
            tone={metric.tone}
          />
        ))}
      </div>

      <div className="mf-two-column">
        <Panel title="Vendas por loja" className="is-tall">
          <div className="sales-bars">
            {dashboard.vendasPorLoja.length === 0 ? (
              <p className="mf-empty-state">Sem vendas no período.</p>
            ) : dashboard.vendasPorLoja.map((item) => (
              <div key={item.lojaId} className="sales-bar-column">
                <span className="sales-bar-value">{money(item.total)}</span>
                <div className="sales-bar" style={{ height: `${24 + (item.total / maxSales) * 54}px`, background: '#4777d8' }} />
                <span className="sales-bar-label">{item.loja}</span>
              </div>
            ))}
          </div>
        </Panel>

        <Panel title="Desempenho por loja" className="is-tall">
          <table className="mf-table compact">
            <thead>
              <tr>
                <th>LOJA</th>
                <th>VENDAS</th>
                <th>MARGEM</th>
              </tr>
            </thead>
            <tbody>
              {dashboard.vendasPorLoja.map((row) => (
                <tr key={row.lojaId}>
                  <td>{row.loja}</td>
                  <td>{money(row.total)}</td>
                  <td>
                    <StatusBadge tone="success" compact>
                      {money(row.margem)}
                    </StatusBadge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
      </div>

      <Panel
        title="Alertas de stock"
        action={
          <StatusBadge tone="danger" compact>
            {dashboard.alertasAtivos} alertas
          </StatusBadge>
        }
      >
        <div className="alert-list">
          {stockAlerts.length === 0 ? (
            <p className="mf-empty-state">Sem alertas ativos.</p>
          ) : stockAlerts.map((alert) => (
            <div key={`${alert.produtoId}-${alert.lojaId}`} className="alert-row">
              <div className="alert-left">
                <span className="alert-dot" />
                <span>{alert.produto}</span>
              </div>
              <div className="alert-right">
                <span className="muted">{alert.loja}</span>
                <span className="tone-danger">{alert.quantidade} un</span>
                <span className="muted faint">mín. {alert.nivelMinimo ?? '-'}</span>
              </div>
            </div>
          ))}
        </div>
      </Panel>
    </div>
  )
}

export function GestorReportsPage() {
  return <ReportsContent />
}

export function GestorSuppliersPage() {
  const [rows, setRows] = useState<FornecedorResponse[]>([])
  const [query, setQuery] = useState('')
  const [draft, setDraft] = useState({
    nome: '',
    nif: '',
    morada: '',
    telefone: '',
    email: '',
    horarioInicioArmazem: '08:00',
    horarioFimArmazem: '18:00',
  })
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    apiRequest<PageResponse<FornecedorResponse>>('/fornecedores?size=50')
      .then((page) => setRows(page.content))
      .catch(() => setError('Não foi possível carregar fornecedores.'))
  }, [])

  const filtered = rows.filter((row) => row.nome.toLowerCase().includes(query.toLowerCase()))

  async function createSupplier() {
    setError(null)
    setMessage(null)
    try {
      const created = await apiRequest<FornecedorResponse>('/fornecedores', {
        method: 'POST',
        body: JSON.stringify(draft),
      })
      setRows((items) => [created, ...items])
      setDraft({ nome: '', nif: '', morada: '', telefone: '', email: '', horarioInicioArmazem: '08:00', horarioFimArmazem: '18:00' })
      setMessage('Fornecedor criado com sucesso.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível criar fornecedor.')
    }
  }

  return (
    <div className="mf-stack">
      <Panel title="Novo fornecedor">
        <div className="mf-fields-grid three">
          <TextField label="Nome" value={draft.nome} onChange={(event) => setDraft((state) => ({ ...state, nome: event.target.value }))} />
          <TextField label="NIF" value={draft.nif} onChange={(event) => setDraft((state) => ({ ...state, nif: event.target.value }))} />
          <TextField label="Email" value={draft.email} onChange={(event) => setDraft((state) => ({ ...state, email: event.target.value }))} />
          <TextField label="Morada" value={draft.morada} onChange={(event) => setDraft((state) => ({ ...state, morada: event.target.value }))} />
          <TextField label="Telefone" value={draft.telefone} onChange={(event) => setDraft((state) => ({ ...state, telefone: event.target.value }))} />
          <TextField label="Horário" value={`${draft.horarioInicioArmazem}-${draft.horarioFimArmazem}`} onChange={() => undefined} />
        </div>
        {message ? <Callout tone="info" className="mt-compact">{message}</Callout> : null}
        <Button className="mt-compact" onClick={createSupplier} disabled={!draft.nome || !draft.nif || !draft.email}>Criar fornecedor</Button>
      </Panel>

      <div className="mf-toolbar-space">
        <TextField placeholder="Pesquisar fornecedor..." value={query} onChange={(event) => setQuery(event.target.value)} />
      </div>

      <Panel>
        {error ? <Callout tone="warning">{error}</Callout> : null}
        {!error && filtered.length === 0 ? <p className="mf-empty-state">Sem fornecedores registados.</p> : null}
        <table className="mf-table">
          <thead>
            <tr>
              <th>NOME</th>
              <th>NIF</th>
              <th>CATEGORIAS</th>
              <th>PRAZO</th>
              <th>ESTADO</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((supplier) => (
              <tr key={supplier.id}>
                <td>{supplier.nome}</td>
                <td className="muted">{supplier.nif}</td>
                <td className="muted">{supplier.email}</td>
                <td>
                  <StatusBadge tone="neutral" compact>
                    {supplier.horarioInicioArmazem} - {supplier.horarioFimArmazem}
                  </StatusBadge>
                </td>
                <td>
                  <StatusBadge tone={supplier.ativo ? 'success' : 'danger'}>{supplier.ativo ? 'Ativo' : 'Inativo'}</StatusBadge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}

export function GestorOrdersPage() {
  const { session } = useAuth()
  const [orders, setOrders] = useState<EncomendaResponse[]>([])
  const [suggestions, setSuggestions] = useState<SugestaoEncomendaResponse[]>([])
  const [stores, setStores] = useState<LojaResponse[]>([])
  const [storeId, setStoreId] = useState(session?.lojaId ?? '')
  const [supplierId, setSupplierId] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    apiRequest<LojaResponse[]>('/utilizadores/lojas').then((response) => {
      setStores(response)
      setStoreId((current) => current || response[0]?.id || session?.lojaId || '')
    }).catch(() => undefined)
  }, [session?.lojaId])

  useEffect(() => {
    if (!storeId) return
    Promise.all([
      apiRequest<PageResponse<EncomendaResponse>>(`/encomendas?lojaId=${storeId}&size=20`),
      apiRequest<SugestaoEncomendaResponse[]>(`/encomendas/sugestoes?lojaId=${storeId}`),
    ]).then(([ordersPage, suggestionRows]) => {
      setOrders(ordersPage.content)
      setSuggestions(suggestionRows)
      setSupplierId((current) => current || suggestionRows[0]?.fornecedorId || '')
    }).catch(() => undefined)
  }, [storeId])

  async function submitSuggestedOrder() {
    setError(null)
    setMessage(null)
    const selectedLines = suggestions.filter((item) => item.fornecedorId === supplierId)
    if (!storeId || !supplierId || selectedLines.length === 0) {
      setError('Não existem sugestões válidas para submeter.')
      return
    }
    try {
      const created = await apiRequest<EncomendaResponse>('/encomendas', {
        method: 'POST',
        body: JSON.stringify({
          lojaId: storeId,
          fornecedorId: supplierId,
          linhas: selectedLines.map((item) => ({
            produtoId: item.produtoId,
            quantidade: item.quantidadeSugerida,
            precoUnitario: item.precoUnitario,
          })),
        }),
      })
      setOrders((items) => [created, ...items])
      setMessage('Encomenda criada a partir das sugestões de stock.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível criar encomenda.')
    }
  }

  return (
    <div className="mf-stack">
      <Callout tone={suggestions.length > 0 ? 'warning' : 'info'}>
        {suggestions.length} produtos abaixo do mínimo — sugestão automática de encomenda disponível.
      </Callout>

      <Panel title="Nova encomenda consolidada">
        <div className="mf-fields-grid two">
          <SelectField label="Fornecedor" value={supplierId} options={suggestions.map((item) => item.fornecedorId)} onChange={(event) => setSupplierId(event.target.value)} />
          <SelectField label="Loja destino" value={storeId} options={stores.map((store) => store.id)} onChange={(event) => setStoreId(event.target.value)} />
        </div>

        <Callout tone="info" className="mt-compact">
          Encomenda submetida após as 18h00 → processada a 21/04 (segunda-feira) às 08h00 (RD-06)
        </Callout>
        {error ? <Callout tone="warning" className="mt-compact">{error}</Callout> : null}
        {message ? <Callout tone="info" className="mt-compact">{message}</Callout> : null}

        <div className="mf-actions-row">
          <Button onClick={submitSuggestedOrder} disabled={!supplierId || suggestions.length === 0}>Submeter encomenda</Button>
        </div>
      </Panel>

      <Panel title="Histórico">
        {orders.length === 0 ? <p className="mf-empty-state">Sem encomendas registadas.</p> : null}
        <table className="mf-table">
          <thead>
            <tr>
              <th>N.º</th>
              <th>FORNECEDOR</th>
              <th>DATA</th>
              <th>ESTADO</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((row) => (
              <tr key={row.id}>
                <td className="muted">{row.id.slice(0, 8)}</td>
                <td>{row.fornecedor}</td>
                <td className="muted">{new Date(row.dataSubmissao).toLocaleString('pt-PT')}</td>
                <td>
                  <StatusBadge tone={row.estado === 'RECEBIDA' ? 'success' : 'warning'}>{row.estado}</StatusBadge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}

export function GestorUsersPage() {
  const [rows, setRows] = useState<UtilizadorResponse[]>([])
  const [profiles, setProfiles] = useState<PerfilResponse[]>([])
  const [stores, setStores] = useState<LojaResponse[]>([])
  const [draft, setDraft] = useState({
    username: '',
    password: 'MiniFormiga2026!',
    nome: '',
    email: '',
    perfilId: '',
    lojaId: '',
  })
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([
      apiRequest<PageResponse<UtilizadorResponse>>('/utilizadores?size=50'),
      apiRequest<PerfilResponse[]>('/utilizadores/perfis'),
      apiRequest<LojaResponse[]>('/utilizadores/lojas'),
    ])
      .then(([page, profileRows, storeRows]) => {
        setRows(page.content)
        setProfiles(profileRows)
        setStores(storeRows)
        setDraft((state) => ({
          ...state,
          perfilId: state.perfilId || profileRows[0]?.id || '',
          lojaId: state.lojaId || storeRows[0]?.id || '',
        }))
      })
      .catch(() => setError('Não foi possível carregar utilizadores.'))
  }, [])

  async function createUser() {
    setError(null)
    setMessage(null)
    try {
      const created = await apiRequest<UtilizadorResponse>('/utilizadores', {
        method: 'POST',
        body: JSON.stringify(draft),
      })
      setRows((items) => [created, ...items])
      setDraft((state) => ({ ...state, username: '', nome: '', email: '', password: 'MiniFormiga2026!' }))
      setMessage('Utilizador criado com sucesso.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível criar utilizador.')
    }
  }

  return (
    <div className="mf-stack">
      <Panel title="Novo utilizador">
        <div className="mf-fields-grid three">
          <TextField label="Username" value={draft.username} onChange={(event) => setDraft((state) => ({ ...state, username: event.target.value }))} />
          <TextField label="Nome" value={draft.nome} onChange={(event) => setDraft((state) => ({ ...state, nome: event.target.value }))} />
          <TextField label="Email" value={draft.email} onChange={(event) => setDraft((state) => ({ ...state, email: event.target.value }))} />
          <SelectField label="Perfil" value={draft.perfilId} options={profiles.map((profile) => profile.id)} onChange={(event) => setDraft((state) => ({ ...state, perfilId: event.target.value }))} />
          <SelectField label="Loja" value={draft.lojaId} options={stores.map((store) => store.id)} onChange={(event) => setDraft((state) => ({ ...state, lojaId: event.target.value }))} />
          <TextField label="Password inicial" value={draft.password} onChange={(event) => setDraft((state) => ({ ...state, password: event.target.value }))} />
        </div>
        {error ? <Callout tone="warning" className="mt-compact">{error}</Callout> : null}
        {message ? <Callout tone="info" className="mt-compact">{message}</Callout> : null}
        <Button className="mt-compact" onClick={createUser} disabled={!draft.username || !draft.nome || !draft.perfilId || !draft.lojaId}>Criar utilizador</Button>
      </Panel>

      <div className="mf-toolbar-space">
        <p className="mf-section-note">{rows.length} utilizadores registados</p>
      </div>

      <Panel>
        {rows.length === 0 ? <p className="mf-empty-state">Sem utilizadores para apresentar.</p> : null}
        <table className="mf-table users">
          <thead>
            <tr>
              <th>UTILIZADOR</th>
              <th>PERFIL</th>
              <th>LOJA</th>
              <th>ESTADO</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>
                  <div className="table-user">
                    <InitialAvatar initials={row.nome.slice(0, 2).toUpperCase()} />
                    <span>{row.nome}</span>
                  </div>
                </td>
                <td className="muted">{row.perfil}</td>
                <td className="muted">{row.loja}</td>
                <td>
                  <StatusBadge tone={row.ativo ? 'success' : 'danger'}>{row.ativo ? 'Ativo' : 'Inativo'}</StatusBadge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}

export function GestorSyncPage() {
  const { session } = useAuth()
  const [history, setHistory] = useState<SincronizacaoResponse[]>([])

  useEffect(() => {
    apiRequest<PageResponse<SincronizacaoResponse>>(`/sincronizacao/historico?lojaId=${session?.lojaId}&size=20`)
      .then((page) => setHistory(page.content))
      .catch(() => setHistory([]))
  }, [session?.lojaId])

  const completed = history.filter((item) => item.estado === 'CONCLUIDA').length
  const conflicts = history.reduce((sum, item) => sum + item.conflitosResolvidos, 0)

  return (
    <div className="mf-stack">
      <div className="mf-metrics-grid three">
        <MetricCard label="Sincronizações" value={String(history.length)} footnote="Histórico da loja" tone="neutral" />
        <MetricCard label="Concluídas" value={String(completed)} footnote="Sem conflitos registados" tone="success" />
        <MetricCard label="Conflitos" value={String(conflicts)} footnote="Resolvidos por last-write-wins" tone={conflicts > 0 ? 'danger' : 'success'} />
      </div>

      <Panel title="Histórico de sincronizações">
        {history.length === 0 ? <p className="mf-empty-state">Sem sincronizações registadas.</p> : null}
        <table className="mf-table">
          <thead>
            <tr>
              <th>LOJA</th>
              <th>DATA/HORA</th>
              <th>REGISTOS</th>
              <th>ESTADO</th>
            </tr>
          </thead>
          <tbody>
            {history.map((row) => (
              <tr key={row.id}>
                <td>{row.lojaId}</td>
                <td className="muted">{new Date(row.inicio).toLocaleString('pt-PT')}</td>
                <td className="muted">{row.quantidadeRegistos}</td>
                <td>
                  <StatusBadge tone={row.estado === 'CONCLUIDA' ? 'success' : row.estado === 'PENDENTE' ? 'warning' : 'danger'}>{row.estado}</StatusBadge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}
