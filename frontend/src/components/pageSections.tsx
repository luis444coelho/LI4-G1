import { reportRows, stockRows } from '../data/mockData'
import { Button, Callout, Panel, ProgressBar, SelectField, StatusBadge, TextField } from './ui'

export function ReportsContent() {
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

      <Panel title="Vendas por categoria — Abril 2026">
        <table className="mf-table">
          <thead>
            <tr>
              <th>CATEGORIA</th>
              <th>VENDAS</th>
              <th>UNID.</th>
              <th>MARGEM</th>
            </tr>
          </thead>
          <tbody>
            {reportRows.map((row) => (
              <tr key={row.category}>
                <td>{row.category}</td>
                <td>{row.sales}</td>
                <td className="muted">{row.units}</td>
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
  )
}

export function StockContent() {
  return (
    <div className="mf-stack">
      <Callout tone="warning">4 produtos abaixo do nível mínimo — alertas emitidos (RF-05)</Callout>

      <div className="mf-toolbar-grid">
        <TextField placeholder="Pesquisar produto..." />
        <SelectField value="Todas as categorias" options={['Todas as categorias']} onChange={() => undefined} />
      </div>

      <Panel>
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
            {stockRows.map((row) => (
              <tr key={row.product}>
                <td>{row.product}</td>
                <td className="muted">{row.category}</td>
                <td>{row.stock}</td>
                <td className="muted">{row.minimum}</td>
                <td>
                  <ProgressBar value={row.levelPercent} tone={row.tone === 'danger' ? 'danger' : 'success'} />
                </td>
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
