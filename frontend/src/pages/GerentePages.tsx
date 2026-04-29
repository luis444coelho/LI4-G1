import { adjustmentHistory, cashClosingHistory, cashClosingSummary, employeeRows } from '../data/mockData'
import { Button, Callout, InitialAvatar, Panel, SelectField, StatusBadge, TextField } from '../components/ui'
import { ReportsContent, StockContent } from '../components/pageSections'

export function GerenteStockPage() {
  return <StockContent />
}

export function GerenteCashPage() {
  return (
    <div className="mf-two-column closing">
      <Panel title="Resumo do dia — 20/04/2026">
        <div className="summary-list">
          {cashClosingSummary.map((item) => (
            <div key={item.label} className="summary-row">
              <span className="muted">{item.label}</span>
              <strong className={item.strong ? 'summary-strong' : ''}>{item.value}</strong>
            </div>
          ))}
        </div>

        <Button className="full-width mt-large">Confirmar fecho de caixa</Button>
      </Panel>

      <Panel title="Histórico — 5 dias">
        <div className="closing-history">
          {cashClosingHistory.map((item) => (
            <div key={item.date} className="summary-row">
              <div className="closing-left">
                <span className="muted">{item.date}</span>
                <strong>{item.value}</strong>
              </div>
              <StatusBadge tone="success">{item.status}</StatusBadge>
            </div>
          ))}
        </div>
      </Panel>
    </div>
  )
}

export function GerenteReportsPage() {
  return <ReportsContent />
}

export function GerenteAdjustmentPage() {
  return (
    <div className="mf-stack">
      <Panel title="Registar ajuste manual" className="narrow-form">
        <SelectField label="Produto" value="Água 0.5L (stock: 8 un)" options={['Água 0.5L (stock: 8 un)']} onChange={() => undefined} />
        <div className="mf-fields-grid two">
          <TextField label="Quantidade (+ ou −)" type="number" defaultValue="-3" />
          <SelectField label="Motivo" value="Quebra" options={['Quebra']} onChange={() => undefined} />
        </div>
        <Callout tone="info" className="mt-compact">
          Operação registada no log de auditoria com identificação e hora (RNF-05)
        </Callout>
        <Button className="full-width mt-large">Confirmar ajuste</Button>
      </Panel>

      <Panel title="Últimos ajustes" className="narrow-form">
        <div className="adjustment-list">
          {adjustmentHistory.map((item) => (
            <div key={`${item.product}-${item.date}`} className="adjustment-row">
              <span>{item.product}</span>
              <div className="adjustment-meta">
                <span className="muted">{item.reason}</span>
                <span className={item.tone === 'danger' ? 'tone-danger' : 'tone-success'}>{item.value}</span>
                <span className="muted faint">{item.date}</span>
              </div>
            </div>
          ))}
        </div>
      </Panel>
    </div>
  )
}

export function GerenteEmployeesPage() {
  return (
    <div className="mf-stack">
      <div className="mf-toolbar-space">
        <p className="mf-section-note">Loja Norte · 3 funcionários</p>
        <Button>Novo funcionário</Button>
      </div>

      <div className="employee-stack">
        {employeeRows.map((row) => (
          <Panel key={row.name}>
            <div className="employee-row">
              <div className="employee-left">
                <InitialAvatar initials={row.initials} />
                <div className="employee-copy">
                  <strong>{row.name}</strong>
                  <span className="muted">{row.meta}</span>
                </div>
              </div>
              <div className="employee-actions">
                <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                <Button variant="secondary" className="small">
                  Editar
                </Button>
              </div>
            </div>
          </Panel>
        ))}
      </div>
    </div>
  )
}
