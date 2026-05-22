import { useEffect, useMemo, useState } from 'react'

import { orderHistory, supplierRows, syncHistory, syncMetrics, usersRows } from '../data/mockData'
import { Button, Callout, InitialAvatar, MetricCard, Panel, SelectField, StatusBadge, TextField } from '../components/ui'
import { ReportsContent } from '../components/pageSections'
import { apiRequest, type DashboardResponse, type RelatorioStockResponse } from '../lib/api'

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
    setLoading(true)
    setError(null)

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
  return (
    <div className="mf-stack">
      <div className="mf-toolbar-space">
        <TextField placeholder="Pesquisar fornecedor..." />
        <Button>Novo fornecedor</Button>
      </div>

      <Panel>
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
            {supplierRows.map((supplier) => (
              <tr key={supplier.name}>
                <td>{supplier.name}</td>
                <td className="muted">{supplier.nif}</td>
                <td className="muted">{supplier.categories}</td>
                <td>
                  <StatusBadge tone={supplier.tone} compact>
                    {supplier.term}
                  </StatusBadge>
                </td>
                <td>
                  <StatusBadge tone={supplier.statusTone}>{supplier.status}</StatusBadge>
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
  return (
    <div className="mf-stack">
      <Callout tone="warning">2 produtos abaixo do mínimo — sugestão automática de encomenda disponível.</Callout>

      <Panel title="Nova encomenda consolidada">
        <div className="mf-fields-grid two">
          <SelectField label="Fornecedor" value="Distribuidora Norte Lda." options={['Distribuidora Norte Lda.']} onChange={() => undefined} />
          <SelectField label="Loja destino" value="Todas as lojas" options={['Todas as lojas']} onChange={() => undefined} />
        </div>

        <Callout tone="info" className="mt-compact">
          Encomenda submetida após as 18h00 → processada a 21/04 (segunda-feira) às 08h00 (RD-06)
        </Callout>

        <div className="mf-actions-row">
          <Button>Submeter encomenda</Button>
        </div>
      </Panel>

      <Panel title="Histórico">
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
            {orderHistory.map((row) => (
              <tr key={row.number}>
                <td className="muted">{row.number}</td>
                <td>{row.supplier}</td>
                <td className="muted">{row.date}</td>
                <td>
                  <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
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
  return (
    <div className="mf-stack">
      <div className="mf-toolbar-space">
        <p className="mf-section-note">5 utilizadores registados</p>
        <Button>Novo utilizador</Button>
      </div>

      <Panel>
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
            {usersRows.map((row) => (
              <tr key={row.name}>
                <td>
                  <div className="table-user">
                    <InitialAvatar initials={row.initials} />
                    <span>{row.name}</span>
                  </div>
                </td>
                <td className="muted">{row.role}</td>
                <td className="muted">{row.store}</td>
                <td>
                  <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
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
  return (
    <div className="mf-stack">
      <div className="mf-metrics-grid three">
        {syncMetrics.map((metric) => (
          <MetricCard
            key={metric.label}
            label={metric.label}
            value={metric.value}
            footnote={metric.footnote}
            tone={metric.tone}
          />
        ))}
      </div>

      <Panel title="Histórico de sincronizações">
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
            {syncHistory.map((row) => (
              <tr key={`${row.store}-${row.datetime}`}>
                <td>{row.store}</td>
                <td className="muted">{row.datetime}</td>
                <td className="muted">{row.records}</td>
                <td>
                  <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}
