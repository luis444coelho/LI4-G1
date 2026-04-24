import { dashboardMetrics, orderHistory, performanceByStore, salesByStore, stockAlerts, supplierRows, syncHistory, syncMetrics, usersRows } from '../data/mockData'
import { Button, Callout, InitialAvatar, MetricCard, Panel, SelectField, StatusBadge, TextField } from '../components/ui'
import { ReportsContent } from '../components/pageSections'

export function GestorDashboardPage() {
  return (
    <div className="mf-stack">
      <div className="mf-metrics-grid">
        {dashboardMetrics.map((metric) => (
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
        <Panel title="Vendas por loja — hoje" className="is-tall">
          <div className="sales-bars">
            {salesByStore.map((item) => (
              <div key={item.store} className="sales-bar-column">
                <span className="sales-bar-value">{item.amount}</span>
                <div className="sales-bar" style={{ height: `${item.height}px`, background: item.color }} />
                <span className="sales-bar-label">{item.store}</span>
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
              {performanceByStore.map((row) => (
                <tr key={row.store}>
                  <td>{row.store}</td>
                  <td>{row.sales}</td>
                  <td>
                    <StatusBadge tone="success" compact>
                      {row.margin}
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
            4 alertas
          </StatusBadge>
        }
      >
        <div className="alert-list">
          {stockAlerts.map((alert) => (
            <div key={`${alert.product}-${alert.store}`} className="alert-row">
              <div className="alert-left">
                <span className="alert-dot" />
                <span>{alert.product}</span>
              </div>
              <div className="alert-right">
                <span className="muted">{alert.store}</span>
                <span className="tone-danger">{alert.quantity}</span>
                <span className="muted faint">{alert.minimum}</span>
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
