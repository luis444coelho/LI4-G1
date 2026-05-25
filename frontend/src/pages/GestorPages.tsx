import { useCallback, useEffect, useMemo, useState } from 'react'

import { Button, Callout, InitialAvatar, MetricCard, Panel, SelectField, StatusBadge, TextField } from '../components/ui'
import { ReportsContent } from '../components/pageSections'
import { LOCAL_API_BASE_URL, apiRequest, type CondicaoComercialResponse, type ConflitoSincronizacaoResponse, type DashboardResponse, type EncomendaResponse, type FornecedorResponse, type LojaResponse, type PageResponse, type PerfilResponse, type RelatorioStockResponse, type SincronizacaoResponse, type SugestaoEncomendaResponse, type UtilizadorResponse } from '../lib/api'
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

function formatDateInput(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function firstDayOfCurrentMonth() {
  const today = new Date()
  return formatDateInput(new Date(today.getFullYear(), today.getMonth(), 1))
}

function buildDashboardQuery(inicio: string, fim: string) {
  const params = new URLSearchParams()
  if (inicio) params.set('inicio', inicio)
  if (fim) params.set('fim', fim)
  const query = params.toString()
  return query ? `?${query}` : ''
}

type DraftOrderLine = {
  produtoId: string
  produto: string
  fornecedorId: string
  fornecedor: string
  quantidade: number
  precoUnitario: number
  quantidadeAtual?: number
  origem: 'auto' | 'manual'
}

export function GestorDashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [stock, setStock] = useState<RelatorioStockResponse | null>(null)
  const [inicio, setInicio] = useState(firstDayOfCurrentMonth)
  const [fim, setFim] = useState(() => formatDateInput(new Date()))
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const carregarDashboard = useCallback((showLoading = false) => {
    if (showLoading) {
      setLoading(true)
    }
    const query = buildDashboardQuery(inicio, fim)
    return Promise.all([
      apiRequest<DashboardResponse>(`/dashboard${query}`),
      apiRequest<RelatorioStockResponse>('/relatorios/stock'),
    ])
      .then(([dashboardResponse, stockResponse]) => {
        setDashboard(dashboardResponse)
        setStock(stockResponse)
        setError(null)
      })
      .catch(() => {
        setError('Não foi possível carregar o dashboard.')
      })
      .finally(() => {
        if (showLoading) {
          setLoading(false)
        }
      })
  }, [fim, inicio])

  useEffect(() => {
    let ignore = false
    let primeiraCarga = true

    const carregar = () => {
      const query = buildDashboardQuery(inicio, fim)
      Promise.all([
        apiRequest<DashboardResponse>(`/dashboard${query}`),
        apiRequest<RelatorioStockResponse>('/relatorios/stock'),
      ])
        .then(([dashboardResponse, stockResponse]) => {
          if (!ignore) {
            setDashboard(dashboardResponse)
            setStock(stockResponse)
            setError(null)
          }
        })
        .catch(() => {
          if (!ignore) {
            setError('Não foi possível carregar o dashboard.')
          }
        })
        .finally(() => {
          if (!ignore && primeiraCarga) {
            setLoading(false)
            primeiraCarga = false
          }
        })
    }

    carregar()
    const intervalo = window.setInterval(carregar, 10000)

    return () => {
      ignore = true
      window.clearInterval(intervalo)
    }
  }, [fim, inicio])

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
      <Panel title="Filtros">
        <div className="mf-toolbar-space">
          <div className="mf-fields-grid two dashboard-period-fields">
            <TextField label="Início" type="date" value={inicio} onChange={(event) => setInicio(event.target.value)} />
            <TextField label="Fim" type="date" value={fim} onChange={(event) => setFim(event.target.value)} />
          </div>
          <Button onClick={() => void carregarDashboard(true)} disabled={loading}>Atualizar</Button>
        </div>
      </Panel>

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

export function GestorOrdersPage({ fixedStore = false }: { fixedStore?: boolean }) {
  const { session } = useAuth()
  const [orders, setOrders] = useState<EncomendaResponse[]>([])
  const [suggestions, setSuggestions] = useState<SugestaoEncomendaResponse[]>([])
  const [draftLines, setDraftLines] = useState<DraftOrderLine[]>([])
  const [conditions, setConditions] = useState<CondicaoComercialResponse[]>([])
  const [stores, setStores] = useState<LojaResponse[]>([])
  const [suppliers, setSuppliers] = useState<FornecedorResponse[]>([])
  const [storeId, setStoreId] = useState(session?.lojaId ?? '')
  const [targetStoreIds, setTargetStoreIds] = useState<string[]>(session?.lojaId ? [session.lojaId] : [])
  const [supplierId, setSupplierId] = useState('')
  const [manualProductId, setManualProductId] = useState('')
  const [manualQuantity, setManualQuantity] = useState(1)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const supplierOptions = useMemo(() => {
    return suppliers
      .filter((supplier) => supplier.ativo)
      .map((supplier) => ({ value: supplier.id, label: supplier.nome }))
  }, [suppliers])
  const storeOptions = stores.map((store) => ({ value: store.id, label: store.nome }))
  const selectedStoreName = stores.find((store) => store.id === storeId)?.nome ?? 'loja atribuída'
  const selectedSupplierName = supplierOptions.find((supplier) => supplier.value === supplierId)?.label ?? ''
  const selectedSuggestions = suggestions.filter((item) => item.fornecedorId === supplierId)
  const selectedDraftLines = draftLines.filter((line) => line.fornecedorId === supplierId)
  const activeSuggestionIds = new Set(selectedDraftLines.filter((line) => line.origem === 'auto').map((line) => line.produtoId))
  const removedSuggestions = selectedSuggestions.filter((item) => !activeSuggestionIds.has(item.produtoId))
  const productOptions = conditions.map((condition) => ({ value: condition.produtoId, label: condition.produto }))
  const selectedTargetStoreIds = fixedStore ? (session?.lojaId ? [session.lojaId] : []) : targetStoreIds
  const targetStoreNames = selectedTargetStoreIds
    .map((id) => stores.find((store) => store.id === id)?.nome)
    .filter(Boolean)
    .join(', ')
  const storeNameById = useMemo(() => new Map(stores.map((store) => [store.id, store.nome])), [stores])

  useEffect(() => {
    Promise.all([
      apiRequest<LojaResponse[]>('/utilizadores/lojas'),
      apiRequest<PageResponse<FornecedorResponse>>('/fornecedores?size=100'),
    ]).then(([storeRows, supplierPage]) => {
      setStores(storeRows)
      setSuppliers(supplierPage.content)
      setStoreId((current) => {
        const next = fixedStore ? session?.lojaId || current : current || storeRows[0]?.id || session?.lojaId || ''
        setTargetStoreIds((ids) => ids.length > 0 ? ids : next ? [next] : [])
        return next
      })
      setSupplierId((current) => current || supplierPage.content.find((supplier) => supplier.ativo)?.id || '')
    }).catch(() => undefined)
  }, [fixedStore, session?.lojaId])

  useEffect(() => {
    if (!storeId) return
    Promise.all([
      apiRequest<PageResponse<EncomendaResponse>>(`/encomendas?lojaId=${storeId}&size=20`),
      apiRequest<SugestaoEncomendaResponse[]>(`/encomendas/sugestoes?lojaId=${storeId}`),
    ]).then(([ordersPage, suggestionRows]) => {
      setOrders(ordersPage.content)
      setSuggestions(suggestionRows)
      setDraftLines(suggestionRows.map((item) => ({
        produtoId: item.produtoId,
        produto: item.produto,
        fornecedorId: item.fornecedorId,
        fornecedor: item.fornecedor,
        quantidade: item.quantidadeSugerida,
        precoUnitario: item.precoUnitario,
        quantidadeAtual: item.quantidadeAtual,
        origem: 'auto',
      })))
      setSupplierId((current) => current || suggestionRows[0]?.fornecedorId || '')
    }).catch(() => undefined)
  }, [storeId])

  useEffect(() => {
    if (!supplierId) {
      const timeoutId = window.setTimeout(() => {
        setConditions([])
        setManualProductId('')
      }, 0)
      return () => window.clearTimeout(timeoutId)
    }
    let ignore = false
    apiRequest<CondicaoComercialResponse[]>(`/fornecedores/${supplierId}/condicoes`)
      .then((rows) => {
        if (ignore) return
        setConditions(rows)
        setManualProductId((current) => rows.some((row) => row.produtoId === current) ? current : rows[0]?.produtoId || '')
      })
      .catch(() => {
        if (ignore) return
        setConditions([])
        setManualProductId('')
      })
    return () => {
      ignore = true
    }
  }, [supplierId])

  function toggleTargetStore(id: string) {
    setTargetStoreIds((ids) => {
      if (ids.includes(id)) {
        return ids.filter((item) => item !== id)
      }
      return [...ids, id]
    })
  }

  async function submitSuggestedOrder() {
    setError(null)
    setMessage(null)
    const selectedLines = selectedDraftLines
    if (!supplierId || selectedLines.length === 0 || selectedTargetStoreIds.length === 0) {
      setError('Adicione pelo menos um produto à encomenda.')
      return
    }
    try {
      const linhas = selectedLines.map((item) => ({
        produtoId: item.produtoId,
        quantidade: item.quantidade,
        precoUnitario: item.precoUnitario,
      }))
      const created = selectedTargetStoreIds.length > 1
        ? await apiRequest<EncomendaResponse[]>('/encomendas/consolidada', {
          method: 'POST',
          body: JSON.stringify({
            lojaIds: selectedTargetStoreIds,
            fornecedorId: supplierId,
            linhas,
          }),
        })
        : [await apiRequest<EncomendaResponse>('/encomendas', {
          method: 'POST',
          body: JSON.stringify({
            lojaId: selectedTargetStoreIds[0],
            fornecedorId: supplierId,
            linhas,
          }),
        })]
      setOrders((items) => [...created, ...items])
      setDraftLines((items) => items.filter((item) => item.fornecedorId !== supplierId))
      setMessage(created.length > 1 ? `Encomenda consolidada criada para ${created.length} lojas.` : 'Encomenda criada.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível criar encomenda.')
    }
  }

  function addManualLine() {
    const condition = conditions.find((item) => item.produtoId === manualProductId)
    const supplier = suppliers.find((item) => item.id === supplierId)
    if (!condition || !supplier || manualQuantity <= 0) return
    setDraftLines((items) => {
      const existing = items.find((item) => item.fornecedorId === supplierId && item.produtoId === condition.produtoId)
      if (existing) {
        return items.map((item) => item === existing
          ? { ...item, quantidade: item.quantidade + manualQuantity }
          : item)
      }
      return [...items, {
        produtoId: condition.produtoId,
        produto: condition.produto,
        fornecedorId: supplier.id,
        fornecedor: supplier.nome,
        quantidade: manualQuantity,
        precoUnitario: condition.precoUnitario,
        origem: 'manual',
      }]
    })
  }

  function removeDraftLine(line: DraftOrderLine) {
    setDraftLines((items) => items.filter((item) => !(item.fornecedorId === line.fornecedorId && item.produtoId === line.produtoId)))
  }

  function restoreSuggestion(item: SugestaoEncomendaResponse) {
    setDraftLines((items) => [...items, {
      produtoId: item.produtoId,
      produto: item.produto,
      fornecedorId: item.fornecedorId,
      fornecedor: item.fornecedor,
      quantidade: item.quantidadeSugerida,
      precoUnitario: item.precoUnitario,
      quantidadeAtual: item.quantidadeAtual,
      origem: 'auto',
    }])
  }

  return (
    <div className="mf-stack">
      <Callout tone={suggestions.length > 0 ? 'warning' : 'info'}>
        {suggestions.length} produtos abaixo do mínimo — sugestão automática de encomenda disponível.
      </Callout>

      <Panel title="Nova encomenda" className="order-main-panel">
        <div className="mf-fields-grid two">
          <SelectField label="Fornecedor" value={supplierId} options={supplierOptions} onChange={(event) => setSupplierId(event.target.value)} />
          {fixedStore ? (
            <TextField label="Loja destino" value={selectedStoreName} disabled />
          ) : (
            <SelectField label="Loja base / histórico" value={storeId} options={storeOptions} onChange={(event) => setStoreId(event.target.value)} />
          )}
        </div>
        {!fixedStore ? (
          <div className="target-store-grid">
            {stores.map((store) => (
              <label key={store.id} className="mf-checkbox-row target-store-option">
                <input
                  type="checkbox"
                  checked={targetStoreIds.includes(store.id)}
                  onChange={() => toggleTargetStore(store.id)}
                />
                {store.nome}
              </label>
            ))}
          </div>
        ) : null}
        <p className="mf-section-note mt-compact">
          Destino: {targetStoreNames || 'selecione pelo menos uma loja'}
        </p>
      </Panel>

      <Panel title="Adicionar produto">
        <div className="mf-fields-grid three">
          <SelectField label="Produto" value={manualProductId} options={productOptions} onChange={(event) => setManualProductId(event.target.value)} />
          <TextField label="Quantidade" type="number" min={1} value={manualQuantity} onChange={(event) => setManualQuantity(Math.max(1, Number(event.target.value) || 1))} />
          <div className="mf-field">
            <span className="mf-field-label">&nbsp;</span>
            <Button className="small" onClick={addManualLine} disabled={!manualProductId || manualQuantity <= 0}>Adicionar</Button>
          </div>
        </div>
      </Panel>

      <Panel title="Encomenda atual">
        <table className="mf-table">
          <thead>
            <tr>
              <th>PRODUTO</th>
              <th>FORNECEDOR</th>
              <th>STOCK</th>
              <th>QTD.</th>
              <th>PREÇO</th>
              <th>ORIGEM</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {selectedDraftLines.length === 0 ? (
              <tr>
                <td colSpan={7} className="muted">Sem produtos na encomenda para {selectedSupplierName || 'o fornecedor selecionado'}.</td>
              </tr>
            ) : null}
            {selectedDraftLines.map((item) => (
              <tr key={`${item.fornecedorId}-${item.produtoId}`}>
                <td>{item.produto}</td>
                <td>{item.fornecedor}</td>
                <td>{item.quantidadeAtual ?? '-'}</td>
                <td><TextField type="number" min={1} value={item.quantidade} onChange={(event) => setDraftLines((lines) => lines.map((line) => line === item ? { ...line, quantidade: Math.max(1, Number(event.target.value) || 1) } : line))} className="receipt-input" /></td>
                <td>{money(item.precoUnitario)}</td>
                <td className="muted">{item.origem === 'auto' ? 'sugestão automática' : 'manual'}</td>
                <td><Button className="small" onClick={() => removeDraftLine(item)}>Remover</Button></td>
              </tr>
            ))}
          </tbody>
        </table>

        {removedSuggestions.length > 0 ? (
          <div className="mf-actions-row mt-compact">
            {removedSuggestions.map((item) => (
              <Button className="small" key={`${item.fornecedorId}-${item.produtoId}`} onClick={() => restoreSuggestion(item)}>
                Adicionar sugestão automática novamente: {item.produto}
              </Button>
            ))}
          </div>
        ) : null}

        <Callout tone="info" className="mt-compact">
          Encomenda submetida após as 18h00 → processada a 21/04 (segunda-feira) às 08h00 (RD-06)
        </Callout>
        {error ? <Callout tone="warning" className="mt-compact">{error}</Callout> : null}
        {message ? <Callout tone="info" className="mt-compact">{message}</Callout> : null}

        <div className="mf-actions-row">
          <Button onClick={submitSuggestedOrder} disabled={!supplierId || selectedDraftLines.length === 0 || selectedTargetStoreIds.length === 0}>
            {selectedTargetStoreIds.length > 1 ? 'Submeter encomenda consolidada' : 'Submeter encomenda'}
          </Button>
        </div>
      </Panel>

      <Panel title="Histórico" className="order-history-panel">
        {orders.length === 0 ? <p className="mf-empty-state">Sem encomendas registadas.</p> : null}
        <table className="mf-table">
          <thead>
            <tr>
              <th>N.º</th>
              <th>LOJA</th>
              <th>FORNECEDOR</th>
              <th>PRODUTOS</th>
              <th>DATA</th>
              <th>ESTADO</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((row) => (
              <tr key={row.id}>
                <td className="muted">{row.numeroDocumento}</td>
                <td>{storeNameById.get(row.lojaId) ?? row.lojaId}</td>
                <td>{row.fornecedor}</td>
                <td>{row.linhas.map((line) => line.produto).join(', ') || 'Sem produtos'}</td>
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
    lojaNome: '',
  })
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const storeOptions = stores.map((store) => ({ value: store.id, label: store.nome }))
  const creatingStoreManager = draft.perfilId === 'GERENTE'

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
          perfilId: state.perfilId || profileRows[0]?.nome || '',
          lojaId: state.lojaId || storeRows[0]?.id || '',
        }))
      })
      .catch(() => setError('Não foi possível carregar utilizadores.'))
  }, [])

  async function createUser() {
    setError(null)
    setMessage(null)
    if (rows.some((row) => row.username.toLowerCase() === draft.username.trim().toLowerCase())) {
      setError('Username já existe.')
      return
    }
    try {
      const created = await apiRequest<UtilizadorResponse>('/utilizadores', {
        method: 'POST',
        body: JSON.stringify({
          username: draft.username,
          password: draft.password,
          nome: draft.nome,
          email: draft.email,
          perfil: draft.perfilId,
          lojaId: creatingStoreManager ? null : draft.lojaId,
          lojaNome: creatingStoreManager ? draft.lojaNome : null,
        }),
      })
      setRows((items) => [created, ...items])
      if (creatingStoreManager && created.lojaId && created.loja) {
        setStores((items) => items.some((store) => store.id === created.lojaId)
          ? items
          : [...items, { id: created.lojaId, nome: created.loja, morada: '', nif: '', ativa: true }])
      }
      setDraft((state) => ({ ...state, username: '', nome: '', email: '', lojaNome: '', password: 'MiniFormiga2026!' }))
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
          <SelectField label="Perfil" value={draft.perfilId} options={profiles.map((profile) => ({ value: profile.nome, label: profile.nome }))} onChange={(event) => setDraft((state) => ({ ...state, perfilId: event.target.value }))} />
          {creatingStoreManager ? (
            <TextField label="Nome da nova loja" value={draft.lojaNome} onChange={(event) => setDraft((state) => ({ ...state, lojaNome: event.target.value }))} />
          ) : (
            <SelectField label="Loja" value={draft.lojaId} options={storeOptions} onChange={(event) => setDraft((state) => ({ ...state, lojaId: event.target.value }))} />
          )}
          <TextField label="Password inicial" value={draft.password} onChange={(event) => setDraft((state) => ({ ...state, password: event.target.value }))} />
        </div>
        {error ? <Callout tone="warning" className="mt-compact">{error}</Callout> : null}
        {message ? <Callout tone="info" className="mt-compact">{message}</Callout> : null}
        <Button className="mt-compact" onClick={createUser} disabled={!draft.username || !draft.password || !draft.nome || !draft.perfilId || (creatingStoreManager ? !draft.lojaNome : !draft.lojaId)}>Criar utilizador</Button>
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
  const [stores, setStores] = useState<LojaResponse[]>([])
  const [selectedStoreId, setSelectedStoreId] = useState('')
  const [history, setHistory] = useState<SincronizacaoResponse[]>([])
  const [current, setCurrent] = useState<SincronizacaoResponse | null>(null)
  const [conflictRows, setConflictRows] = useState<ConflitoSincronizacaoResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [syncing, setSyncing] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    apiRequest<LojaResponse[]>('/utilizadores/lojas', { apiBaseUrl: LOCAL_API_BASE_URL })
      .then((rows) => {
        setStores(rows)
        setSelectedStoreId((currentStoreId) => currentStoreId || rows[0]?.id || '')
      })
      .catch(() => setError('Não foi possível carregar lojas locais para sincronização.'))
  }, [])

  const loadSyncData = useCallback(async () => {
    if (!selectedStoreId) return
    setLoading(true)
    setError(null)
    try {
      const [page, state, conflicts] = await Promise.all([
        apiRequest<PageResponse<SincronizacaoResponse>>(`/sincronizacao/historico?lojaId=${selectedStoreId}&size=20`),
        apiRequest<SincronizacaoResponse>(`/sincronizacao/estado?lojaId=${selectedStoreId}`).catch(() => null),
        apiRequest<ConflitoSincronizacaoResponse[]>(`/sincronizacao/conflitos?lojaId=${selectedStoreId}`).catch(() => []),
      ])
      setHistory(page.content)
      setCurrent(state)
      setConflictRows(conflicts)
    } catch (caught) {
      setHistory([])
      setConflictRows([])
      setError(caught instanceof Error ? caught.message : 'Não foi possível carregar sincronizações.')
    } finally {
      setLoading(false)
    }
  }, [selectedStoreId])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadSyncData()
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadSyncData])

  const completed = history.filter((item) => item.estado === 'CONCLUIDA').length
  const conflicts = conflictRows.reduce((sum, item) => sum + item.conflitosResolvidos, 0)
  const storeNameById = useMemo(() => new Map(stores.map((store) => [store.id, store.nome])), [stores])

  async function startManualSync() {
    if (!selectedStoreId) return
    setSyncing(true)
    setError(null)
    setMessage(null)
    try {
      const response = await apiRequest<SincronizacaoResponse>('/sincronizacao/iniciar', {
        method: 'POST',
        apiBaseUrl: LOCAL_API_BASE_URL,
        body: JSON.stringify({ lojaId: selectedStoreId }),
      })
      setCurrent(response)
      setMessage(`Sincronização manual iniciada: ${response.estado}.`)
      await loadSyncData()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível iniciar a sincronização manual.')
    } finally {
      setSyncing(false)
    }
  }

  return (
    <div className="mf-stack">
      <div className="mf-metrics-grid three">
        <MetricCard label="Sincronizações" value={String(history.length)} footnote="Histórico da loja" tone="neutral" />
        <MetricCard label="Estado atual" value={current?.estado ?? '-'} footnote={current?.proximaTentativa ? `Retry ${new Date(current.proximaTentativa).toLocaleString('pt-PT')}` : `${completed} concluídas`} tone={current?.estado === 'FALHADA' ? 'danger' : current?.estado === 'PENDENTE' ? 'neutral' : 'success'} />
        <MetricCard label="Conflitos" value={String(conflicts)} footnote="Resolvidos por last-write-wins" tone={conflicts > 0 ? 'danger' : 'success'} />
      </div>

      <Panel
        title="Sincronização"
      >
        <SelectField
          label="Loja local"
          value={selectedStoreId}
          options={stores.map((store) => ({ value: store.id, label: store.nome }))}
          onChange={(event) => setSelectedStoreId(event.target.value)}
        />
        <div className="mf-actions-row mt-compact">
          <Button onClick={startManualSync} disabled={!selectedStoreId || syncing}>
            {syncing ? 'A sincronizar...' : 'Sincronizar agora'}
          </Button>
        </div>
        {error ? <Callout tone="warning">{error}</Callout> : null}
        {message ? <Callout tone="info">{message}</Callout> : null}
        {!error ? <p className="mf-empty-state">Última sincronização recebida: {current?.inicio ? new Date(current.inicio).toLocaleString('pt-PT') : 'sem registo'}</p> : null}
      </Panel>

      <Panel title="Histórico de sincronizações">
        {loading ? <p className="mf-empty-state">A carregar sincronizações...</p> : null}
        {!loading && history.length === 0 ? <p className="mf-empty-state">Sem sincronizações registadas.</p> : null}
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
                <td>{storeNameById.get(row.lojaId) ?? 'Loja'}</td>
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

      <Panel title="Conflitos resolvidos">
        {conflictRows.length === 0 ? <p className="mf-empty-state">Sem conflitos registados.</p> : null}
        <table className="mf-table">
          <thead>
            <tr>
              <th>LOJA</th>
              <th>DATA/HORA</th>
              <th>ESTADO</th>
              <th>CONFLITOS</th>
            </tr>
          </thead>
          <tbody>
            {conflictRows.map((row) => (
              <tr key={`${row.sincronizacaoId}-${row.dataHora}`}>
                <td>{storeNameById.get(row.lojaId) ?? 'Loja'}</td>
                <td className="muted">{new Date(row.dataHora).toLocaleString('pt-PT')}</td>
                <td>
                  <StatusBadge tone={row.estado === 'CONCLUIDA' ? 'success' : 'warning'}>{row.estado}</StatusBadge>
                </td>
                <td className={row.conflitosResolvidos > 0 ? 'tone-danger' : 'muted'}>{row.conflitosResolvidos}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}
