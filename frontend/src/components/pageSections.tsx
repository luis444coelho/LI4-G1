import { useEffect, useMemo, useState } from 'react'

import { Button, Callout, Panel, ProgressBar, SelectField, StatusBadge, TextField } from './ui'
import { apiRequest, type RelatorioStockResponse, type StockResponse } from '../lib/api'
import { useAuth } from '../lib/auth'

const money = new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' })

export function ReportsContent() {
  const [report, setReport] = useState<RelatorioStockResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    apiRequest<RelatorioStockResponse>('/relatorios/stock')
      .then((response) => {
        if (!ignore) setReport(response)
      })
      .catch(() => {
        if (!ignore) setError('Não foi possível carregar o relatório.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  return (
    <div className="mf-stack">
      <Panel title="Filtros">
        <div className="mf-fields-grid three">
          <SelectField label="Loja" value="Todas as lojas" options={['Todas as lojas']} onChange={() => undefined} />
          <SelectField label="Período" value="Abril 2026" options={['Abril 2026']} onChange={() => undefined} />
          <SelectField label="Categoria" value="Todas" options={['Todas']} onChange={() => undefined} />
        </div>
        <div className="mf-actions-row">
          <Button>Gerar relatório</Button>
          <Button variant="secondary">Exportar CSV</Button>
          <Button variant="secondary">Exportar PDF</Button>
        </div>
      </Panel>

      <Panel title="Stock por categoria">
        {loading ? <p className="mf-empty-state">A carregar relatório...</p> : null}
        {error ? <Callout tone="warning">{error}</Callout> : null}
        {!loading && !error && !report?.itens.length ? <p className="mf-empty-state">Sem dados para apresentar.</p> : null}
        <table className="mf-table">
          <thead>
            <tr>
              <th>PRODUTO</th>
              <th>CATEGORIA</th>
              <th>UNID.</th>
              <th>VALOR</th>
            </tr>
          </thead>
          <tbody>
            {(report?.itens ?? []).slice(0, 20).map((row) => (
              <tr key={`${row.lojaId}-${row.produtoId}`}>
                <td>{row.produto}</td>
                <td>{row.categoria}</td>
                <td className="muted">{row.quantidade}</td>
                <td>
                  <StatusBadge tone="success" compact>
                    {money.format(row.valorPrecoCusto)}
                  </StatusBadge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}

export function StockContent() {
  const { session } = useAuth()
  const [rows, setRows] = useState<StockResponse[]>([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    apiRequest<StockResponse[]>(`/stock?lojaId=${session?.lojaId}`)
      .then((response) => {
        if (!ignore) setRows(response)
      })
      .catch(() => {
        if (!ignore) setError('Não foi possível carregar o stock.')
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [session?.lojaId])

  const filteredRows = useMemo(() => {
    const term = query.trim().toLowerCase()
    if (!term) return rows
    return rows.filter((row) => row.produto.toLowerCase().includes(term))
  }, [query, rows])
  const alertCount = rows.filter((row) => row.precisaReposicao).length

  return (
    <div className="mf-stack">
      <Callout tone={alertCount > 0 ? 'warning' : 'info'}>
        {alertCount} produtos abaixo do nível mínimo — alertas emitidos (RF-05)
      </Callout>

      <div className="mf-toolbar-grid">
        <TextField placeholder="Pesquisar produto..." value={query} onChange={(event) => setQuery(event.target.value)} />
        <SelectField value="Todas as categorias" options={['Todas as categorias']} onChange={() => undefined} />
      </div>

      <Panel>
        {loading ? <p className="mf-empty-state">A carregar stock...</p> : null}
        {error ? <Callout tone="warning">{error}</Callout> : null}
        {!loading && !error && filteredRows.length === 0 ? <p className="mf-empty-state">Sem produtos para apresentar.</p> : null}
        <table className="mf-table stock">
          <thead>
            <tr>
              <th>PRODUTO</th>
              <th>CAT.</th>
              <th>STOCK</th>
              <th>MÍN.</th>
              <th>NÍVEL</th>
              <th>ESTADO</th>
            </tr>
          </thead>
          <tbody>
            {filteredRows.map((row) => {
              const minimum = row.nivelMinimo ?? Math.max(row.quantidade, 1)
              const percent = minimum === 0 ? 100 : Math.min(100, Math.round((row.quantidade / minimum) * 100))
              return (
              <tr key={`${row.lojaId}-${row.produtoId}`}>
                <td>{row.produto}</td>
                <td className="muted">-</td>
                <td>{row.quantidade}</td>
                <td className="muted">{row.nivelMinimo ?? '-'}</td>
                <td>
                  <ProgressBar value={percent} tone={row.precisaReposicao ? 'danger' : 'success'} />
                </td>
                <td>
                  <StatusBadge tone={row.precisaReposicao ? 'danger' : 'success'}>{row.precisaReposicao ? 'Reposição' : 'OK'}</StatusBadge>
                </td>
              </tr>
            )})}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}
