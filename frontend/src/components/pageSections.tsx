import { useCallback, useEffect, useMemo, useState } from 'react'

import { Button, Callout, MetricCard, Panel, ProgressBar, SelectField, StatusBadge, TextField } from './ui'
import {
  apiDownload,
  apiRequest,
  type AlertaStockResponse,
  type CategoriaResponse,
  type LocalizacaoProdutoResponse,
  type LojaResponse,
  type RelatorioRentabilidadeResponse,
  type RelatorioStockResponse,
  type RelatorioVendasResponse,
  type StockResponse,
} from '../lib/api'
import { useAuth } from '../lib/auth'

const money = new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' })
const percent = new Intl.NumberFormat('pt-PT', { maximumFractionDigits: 1 })

type ReportType = 'STOCK' | 'VENDAS' | 'RENTABILIDADE'
type ReportData = RelatorioStockResponse | RelatorioVendasResponse | RelatorioRentabilidadeResponse

const reportTypeOptions = [
  { value: 'STOCK', label: 'Stock' },
  { value: 'VENDAS', label: 'Vendas' },
  { value: 'RENTABILIDADE', label: 'Rentabilidade' },
]

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

function buildReportQuery(filters: { lojaId: string; inicio: string; fim: string; categoriaId: string }) {
  const params = new URLSearchParams()
  if (filters.lojaId) params.set('lojaId', filters.lojaId)
  if (filters.inicio) params.set('inicio', filters.inicio)
  if (filters.fim) params.set('fim', filters.fim)
  if (filters.categoriaId) params.set('categoriaId', filters.categoriaId)
  const query = params.toString()
  return query ? `?${query}` : ''
}

function reportEndpoint(type: ReportType) {
  switch (type) {
    case 'VENDAS':
      return '/relatorios/vendas'
    case 'RENTABILIDADE':
      return '/relatorios/rentabilidade'
    case 'STOCK':
    default:
      return '/relatorios/stock'
  }
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

export function ReportsContent() {
  const [reportType, setReportType] = useState<ReportType>('STOCK')
  const [stores, setStores] = useState<LojaResponse[]>([])
  const [categories, setCategories] = useState<CategoriaResponse[]>([])
  const [lojaId, setLojaId] = useState('')
  const [categoriaId, setCategoriaId] = useState('')
  const [inicio, setInicio] = useState(firstDayOfCurrentMonth)
  const [fim, setFim] = useState(() => formatDateInput(new Date()))
  const [report, setReport] = useState<ReportData | null>(null)
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false

    Promise.all([
      apiRequest<LojaResponse[]>('/utilizadores/lojas'),
      apiRequest<CategoriaResponse[]>('/categorias'),
    ])
      .then(([storeRows, categoryRows]) => {
        if (ignore) return
        setStores(storeRows)
        setCategories(categoryRows)
      })
      .catch(() => {
        if (ignore) return
        setStores([])
        setCategories([])
      })

    return () => {
      ignore = true
    }
  }, [])

  const loadReport = useCallback(async () => {
    setLoading(true)
    setError(null)
    setMessage(null)

    try {
      const query = buildReportQuery({ lojaId, inicio, fim, categoriaId })
      const nextReport = await apiRequest<ReportData>(`${reportEndpoint(reportType)}${query}`)
      setReport(nextReport)
    } catch (caught) {
      setReport(null)
      setError(caught instanceof Error ? caught.message : 'Não foi possível carregar o relatório.')
    } finally {
      setLoading(false)
    }
  }, [categoriaId, fim, inicio, lojaId, reportType])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadReport()
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadReport])

  const summary = useMemo(() => {
    if (!report) return []
    if (reportType === 'VENDAS') {
      const sales = report as RelatorioVendasResponse
      return [
        { label: 'Total faturado', value: money.format(sales.totalComIva), footnote: `${sales.numeroVendas} vendas`, tone: sales.totalComIva > 0 ? 'success' as const : 'neutral' as const },
        { label: 'IVA', value: money.format(sales.totalIva), footnote: 'Valor discriminado', tone: 'neutral' as const },
        { label: 'Margem', value: money.format(sales.margem), footnote: 'Após custo estimado', tone: sales.margem >= 0 ? 'success' as const : 'danger' as const },
      ]
    }
    if (reportType === 'RENTABILIDADE') {
      const profitability = report as RelatorioRentabilidadeResponse
      return [
        { label: 'Receita sem IVA', value: money.format(profitability.receitaSemIva), footnote: 'Base contabilística', tone: 'neutral' as const },
        { label: 'Custo total', value: money.format(profitability.custoTotal), footnote: 'Preço de custo', tone: 'neutral' as const },
        { label: 'Margem total', value: money.format(profitability.margemTotal), footnote: `${percent.format(profitability.margemPercentagem)}%`, tone: profitability.margemTotal >= 0 ? 'success' as const : 'danger' as const },
      ]
    }
    const stock = report as RelatorioStockResponse
    return [
      { label: 'Produtos', value: String(stock.totalProdutos), footnote: `${stock.totalUnidades} unidades`, tone: 'neutral' as const },
      { label: 'Valor em stock', value: money.format(stock.valorStockPrecoCusto), footnote: 'Preço de custo', tone: 'neutral' as const },
      { label: 'Reposição', value: String(stock.produtosReposicao), footnote: `${stock.alertasAtivos} alertas ativos`, tone: stock.produtosReposicao > 0 ? 'danger' as const : 'success' as const },
    ]
  }, [report, reportType])

  async function exportReport(formato: 'CSV' | 'PDF') {
    setExporting(true)
    setError(null)
    setMessage(null)
    try {
      const response = await apiDownload('/relatorios/exportar', {
        method: 'POST',
        body: JSON.stringify({
          tipo: reportType,
          formato,
          lojaId: lojaId || null,
          inicio: inicio || null,
          fim: fim || null,
          categoriaId: categoriaId || null,
        }),
      })
      downloadBlob(response.blob, response.filename)
      setMessage(`Relatório ${formato} gerado com sucesso.`)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : `Não foi possível exportar ${formato}.`)
    } finally {
      setExporting(false)
    }
  }

  const renderReportTable = () => {
    if (!report) return null

    if (reportType === 'VENDAS') {
      const sales = report as RelatorioVendasResponse
      return (
        <>
          {sales.linhas.length === 0 ? <p className="mf-empty-state">Sem vendas no período selecionado.</p> : null}
          <table className="mf-table">
            <thead>
              <tr>
                <th>DATA</th>
                <th>LOJA</th>
                <th>PRODUTO</th>
                <th>QTD</th>
                <th>TOTAL</th>
                <th>MARGEM</th>
              </tr>
            </thead>
            <tbody>
              {sales.linhas.slice(0, 30).map((row) => (
                <tr key={`${row.vendaId}-${row.produtoId}-${row.dataHora}`}>
                  <td className="muted">{new Date(row.dataHora).toLocaleDateString('pt-PT')}</td>
                  <td>{row.loja}</td>
                  <td>{row.produto}</td>
                  <td className="muted">{row.quantidade}</td>
                  <td>{money.format(row.valorComIva)}</td>
                  <td>
                    <StatusBadge tone={row.margem >= 0 ? 'success' : 'danger'} compact>
                      {money.format(row.margem)}
                    </StatusBadge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )
    }

    if (reportType === 'RENTABILIDADE') {
      const profitability = report as RelatorioRentabilidadeResponse
      return (
        <>
          {profitability.produtos.length === 0 ? <p className="mf-empty-state">Sem rentabilidade para apresentar.</p> : null}
          <table className="mf-table">
            <thead>
              <tr>
                <th>PRODUTO</th>
                <th>CATEGORIA</th>
                <th>QTD</th>
                <th>RECEITA</th>
                <th>CUSTO</th>
                <th>MARGEM</th>
              </tr>
            </thead>
            <tbody>
              {profitability.produtos.slice(0, 30).map((row) => (
                <tr key={row.produtoId}>
                  <td>{row.produto}</td>
                  <td className="muted">{row.categoria}</td>
                  <td className="muted">{row.quantidadeVendida}</td>
                  <td>{money.format(row.receitaSemIva)}</td>
                  <td className="muted">{money.format(row.custo)}</td>
                  <td>
                    <StatusBadge tone={row.margem >= 0 ? 'success' : 'danger'} compact>
                      {money.format(row.margem)}
                    </StatusBadge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )
    }

    const stock = report as RelatorioStockResponse
    return (
      <>
        {stock.itens.length === 0 ? <p className="mf-empty-state">Sem dados para apresentar.</p> : null}
        <table className="mf-table">
          <thead>
            <tr>
              <th>PRODUTO</th>
              <th>CATEGORIA</th>
              <th>LOJA</th>
              <th>UNID.</th>
              <th>VALOR</th>
              <th>ESTADO</th>
            </tr>
          </thead>
          <tbody>
            {stock.itens.slice(0, 30).map((row) => (
              <tr key={`${row.lojaId}-${row.produtoId}`}>
                <td>{row.produto}</td>
                <td className="muted">{row.categoria}</td>
                <td className="muted">{row.loja}</td>
                <td className="muted">{row.quantidade}</td>
                <td>{money.format(row.valorPrecoCusto)}</td>
                <td>
                  <StatusBadge tone={row.precisaReposicao ? 'danger' : 'success'} compact>
                    {row.precisaReposicao ? 'Reposição' : 'OK'}
                  </StatusBadge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </>
    )
  }

  return (
    <div className="mf-stack">
      <Panel title="Filtros">
        <div className="mf-fields-grid three">
          <SelectField label="Relatório" value={reportType} options={reportTypeOptions} onChange={(event) => setReportType(event.target.value as ReportType)} />
          <SelectField
            label="Loja"
            value={lojaId}
            options={[{ value: '', label: 'Todas as lojas' }, ...stores.map((store) => ({ value: store.id, label: store.nome }))]}
            onChange={(event) => setLojaId(event.target.value)}
          />
          <SelectField
            label="Categoria"
            value={categoriaId}
            options={[{ value: '', label: 'Todas as categorias' }, ...categories.map((category) => ({ value: category.id, label: category.nome }))]}
            onChange={(event) => setCategoriaId(event.target.value)}
          />
          <TextField label="Início" type="date" value={inicio} onChange={(event) => setInicio(event.target.value)} />
          <TextField label="Fim" type="date" value={fim} onChange={(event) => setFim(event.target.value)} />
        </div>
        <div className="mf-actions-row">
          <Button onClick={loadReport} disabled={loading}>Gerar relatório</Button>
          <Button variant="secondary" onClick={() => void exportReport('CSV')} disabled={exporting}>Exportar CSV</Button>
          <Button variant="secondary" onClick={() => void exportReport('PDF')} disabled={exporting}>Exportar PDF</Button>
        </div>
        {message ? <Callout tone="info" className="mt-compact">{message}</Callout> : null}
      </Panel>

      {summary.length > 0 ? (
        <div className="mf-metrics-grid three">
          {summary.map((metric) => (
            <MetricCard
              key={metric.label}
              label={metric.label}
              value={metric.value}
              footnote={metric.footnote}
              tone={metric.tone}
            />
          ))}
        </div>
      ) : null}

      <Panel title={`Relatório de ${reportType.toLowerCase()}`}>
        {loading ? <p className="mf-empty-state">A carregar relatório...</p> : null}
        {error ? <Callout tone="warning">{error}</Callout> : null}
        {!loading && !error ? renderReportTable() : null}
      </Panel>
    </div>
  )
}

export function StockContent() {
  const { session } = useAuth()
  const lojaId = session?.lojaId
  const [rows, setRows] = useState<StockResponse[]>([])
  const [alerts, setAlerts] = useState<AlertaStockResponse[]>([])
  const [query, setQuery] = useState('')
  const [minimumDraft, setMinimumDraft] = useState<Record<string, number>>({})
  const [selectedProductId, setSelectedProductId] = useState('')
  const [corredor, setCorredor] = useState('')
  const [prateleira, setPrateleira] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  const loadStock = useCallback(async () => {
    if (!lojaId) return
    setLoading(true)
    setError(null)
    try {
      const response = await apiRequest<StockResponse[]>(`/stock?lojaId=${lojaId}`)
      setRows(response)
      setMinimumDraft(Object.fromEntries(response.map((row) => [row.produtoId, row.nivelMinimo ?? 0])))
      setSelectedProductId((current) => current || response[0]?.produtoId || '')
    } catch {
      setError('Não foi possível carregar o stock.')
    } finally {
      setLoading(false)
    }
  }, [lojaId])

  const loadAlerts = useCallback(async () => {
    if (!lojaId) return
    try {
      const response = await apiRequest<AlertaStockResponse[]>(`/stock/alertas?lojaId=${lojaId}`)
      setAlerts(response)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível carregar alertas de stock.')
    }
  }, [lojaId])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadStock()
      void loadAlerts()
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadAlerts, loadStock])

  useEffect(() => {
    if (!selectedProductId) return
    let ignore = false

    apiRequest<LocalizacaoProdutoResponse>(`/produtos/${selectedProductId}/localizacao`)
      .then((response) => {
        if (ignore) return
        setCorredor(response.corredor)
        setPrateleira(response.prateleira)
      })
      .catch(() => {
        if (ignore) return
        setCorredor('')
        setPrateleira('')
      })

    return () => {
      ignore = true
    }
  }, [selectedProductId])

  async function saveMinimum(product: StockResponse) {
    setError(null)
    setMessage(null)
    try {
      await apiRequest<void>(`/stock/${product.produtoId}/nivel-minimo`, {
        method: 'PUT',
        body: JSON.stringify({
          lojaId: product.lojaId,
          quantidade: minimumDraft[product.produtoId] ?? 0,
        }),
      })
      await loadStock()
      await loadAlerts()
      setMessage(`Nível mínimo atualizado para ${product.produto}.`)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível atualizar o nível mínimo.')
    }
  }

  async function updateAlert(alertId: string, action: 'lido' | 'resolver') {
    setError(null)
    setMessage(null)
    try {
      await apiRequest<AlertaStockResponse>(`/stock/alertas/${alertId}/${action}`, { method: 'PATCH' })
      await loadAlerts()
      await loadStock()
      setMessage(action === 'lido' ? 'Alerta marcado como lido.' : 'Alerta resolvido.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível atualizar o alerta.')
    }
  }

  async function saveLocation() {
    if (!selectedProductId) return
    setError(null)
    setMessage(null)
    try {
      await apiRequest<LocalizacaoProdutoResponse>(`/produtos/${selectedProductId}/localizacao`, {
        method: 'PUT',
        body: JSON.stringify({ corredor, prateleira }),
      })
      setMessage('Localização do produto atualizada.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível atualizar a localização.')
    }
  }

  const selectedProduct = rows.find((row) => row.produtoId === selectedProductId)
  const productOptions = rows.map((row) => ({ value: row.produtoId, label: row.produto }))

  const filteredRows = useMemo(() => {
    const term = query.trim().toLowerCase()
    if (!term) return rows
    return rows.filter((row) => row.produto.toLowerCase().includes(term))
  }, [query, rows])
  const alertCount = alerts.filter((row) => !row.resolvido).length

  return (
    <div className="mf-stack">
      <Callout tone={alertCount > 0 ? 'warning' : 'info'}>
        {alertCount} alertas ativos de stock.
      </Callout>

      <div className="mf-toolbar-grid">
        <TextField placeholder="Pesquisar produto..." value={query} onChange={(event) => setQuery(event.target.value)} />
        <Button variant="secondary" onClick={() => { void loadStock(); void loadAlerts() }}>Atualizar</Button>
      </div>

      {message ? <Callout tone="info">{message}</Callout> : null}

      <Panel title="Stock e níveis mínimos">
        {loading ? <p className="mf-empty-state">A carregar stock...</p> : null}
        {error ? <Callout tone="warning">{error}</Callout> : null}
        {!loading && !error && filteredRows.length === 0 ? <p className="mf-empty-state">Sem produtos para apresentar.</p> : null}
        <table className="mf-table stock">
          <thead>
            <tr>
              <th>PRODUTO</th>
              <th>STOCK</th>
              <th>MÍNIMO</th>
              <th>NÍVEL</th>
              <th>ESTADO</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {filteredRows.map((row) => {
              const minimum = row.nivelMinimo ?? Math.max(row.quantidade, 1)
              const draftMinimum = minimumDraft[row.produtoId] ?? row.nivelMinimo ?? 0
              const percentValue = minimum === 0 ? 100 : Math.min(100, Math.round((row.quantidade / minimum) * 100))
              return (
                <tr key={`${row.lojaId}-${row.produtoId}`}>
                  <td>{row.produto}</td>
                  <td>{row.quantidade}</td>
                  <td>
                    <TextField
                      type="number"
                      min={0}
                      value={draftMinimum}
                      onChange={(event) => setMinimumDraft((state) => ({ ...state, [row.produtoId]: Number(event.target.value) }))}
                      className="receipt-input"
                    />
                  </td>
                  <td>
                    <ProgressBar value={percentValue} tone={row.precisaReposicao ? 'danger' : 'success'} />
                  </td>
                  <td>
                    <StatusBadge tone={row.precisaReposicao ? 'danger' : 'success'}>{row.precisaReposicao ? 'Reposição' : 'OK'}</StatusBadge>
                  </td>
                  <td>
                    <Button variant="secondary" className="small" onClick={() => void saveMinimum(row)}>Guardar</Button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </Panel>

      <div className="mf-two-column">
        <Panel title="Alertas de stock">
          {alerts.length === 0 ? <p className="mf-empty-state">Sem alertas ativos.</p> : null}
          <div className="alert-list">
            {alerts.map((alert) => (
              <div key={alert.id} className="alert-row">
                <div className="alert-left">
                  <span className="alert-dot" />
                  <span>{alert.produto}</span>
                </div>
                <div className="alert-right">
                  <span className="muted">{alert.quantidadeNoMomento} un</span>
                  <Button variant="ghost" className="small" onClick={() => void updateAlert(alert.id, 'lido')} disabled={alert.lido}>Lido</Button>
                  <Button variant="secondary" className="small" onClick={() => void updateAlert(alert.id, 'resolver')}>Resolver</Button>
                </div>
              </div>
            ))}
          </div>
        </Panel>

        <Panel title="Localização do produto">
          <div className="mf-fields-grid two">
            <SelectField label="Produto" value={selectedProductId} options={productOptions} onChange={(event) => setSelectedProductId(event.target.value)} />
            <TextField label="Stock atual" value={selectedProduct?.quantidade ?? ''} readOnly />
            <TextField label="Corredor" value={corredor} onChange={(event) => setCorredor(event.target.value)} />
            <TextField label="Prateleira" value={prateleira} onChange={(event) => setPrateleira(event.target.value)} />
          </div>
          <Button className="mt-compact" onClick={() => void saveLocation()} disabled={!selectedProductId || !corredor || !prateleira}>Guardar localização</Button>
        </Panel>
      </div>
    </div>
  )
}
