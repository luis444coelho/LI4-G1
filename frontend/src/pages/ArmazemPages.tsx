import { inventoryRows, receiptRows } from '../data/mockData'
import { Button, Callout, Panel, SelectField, TextField } from '../components/ui'
import { StockContent } from '../components/pageSections'

export function ArmazemStockPage() {
  return <StockContent />
}

export function ArmazemGoodsReceiptPage() {
  return (
    <div className="mf-stack">
      <Panel title="Registar entrada de mercadoria" className="wide-form">
        <div className="mf-fields-grid two">
          <SelectField label="Encomenda associada" value="E-2026-041 — Distribuidora Norte" options={['E-2026-041 — Distribuidora Norte']} onChange={() => undefined} />
          <TextField label="N.º guia de remessa" defaultValue="GR-2026-0584" />
        </div>

        <div className="receipt-table">
          <table className="mf-table compact">
            <thead>
              <tr>
                <th>PRODUTO</th>
                <th>ENCOM.</th>
                <th>RECEBIDO</th>
                <th>DIF.</th>
              </tr>
            </thead>
            <tbody>
              {receiptRows.map((row) => (
                <tr key={row.product}>
                  <td>{row.product}</td>
                  <td className="muted">{row.ordered}</td>
                  <td>
                    <TextField type="number" defaultValue={String(row.received)} className="receipt-input" />
                  </td>
                  <td className={row.tone === 'danger' ? 'tone-danger' : 'muted'}>{row.difference}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <Callout tone="warning" className="mt-compact">
          Discrepância: Sumo laranja — 6 unidades em falta. Confirme a quantidade recebida.
        </Callout>
        <Button className="mt-large">Confirmar receção e atualizar stock</Button>
      </Panel>
    </div>
  )
}

export function ArmazemInventoryPage() {
  return (
    <div className="mf-stack">
      <div className="mf-toolbar-space">
        <p className="mf-section-note">Inventário físico — 20/04/2026</p>
        <Button>Fechar inventário</Button>
      </div>

      <Panel>
        <table className="mf-table compact inventory">
          <thead>
            <tr>
              <th>PRODUTO</th>
              <th>SISTEMA</th>
              <th>CONTADO</th>
              <th>DIFERENÇA</th>
            </tr>
          </thead>
          <tbody>
            {inventoryRows.map((row) => (
              <tr key={row.product}>
                <td>{row.product}</td>
                <td className="muted">{row.system}</td>
                <td>
                  <TextField type="number" defaultValue={String(row.counted)} className="receipt-input" />
                </td>
                <td>
                  {row.tone === 'neutral' ? (
                    <span className="muted">OK</span>
                  ) : (
                    <span className={row.tone === 'danger' ? 'tone-danger' : 'tone-warning'}>{row.difference}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}
