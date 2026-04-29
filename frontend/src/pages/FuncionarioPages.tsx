import { saleRows } from '../data/mockData'
import { Button, Callout, Panel, SelectField, TextField } from '../components/ui'

export function FuncionarioSalePage() {
  return (
    <div className="sale-layout">
      <div className="sale-left">
        <div className="sale-search-row">
          <TextField placeholder="Código de barras ou pesquisa rápida..." className="grow" />
          <Button variant="secondary" className="scan-button">
            Scan ▷
          </Button>
        </div>

        <Panel className="sale-table-panel">
          <table className="mf-table compact sale">
            <thead>
              <tr>
                <th>PRODUTO</th>
                <th>QTD</th>
                <th>SUBTOTAL</th>
              </tr>
            </thead>
            <tbody>
              {saleRows.map((row) => (
                <tr key={row.product}>
                  <td>
                    <div className="sale-product">
                      <strong>{row.product}</strong>
                      <span className="muted">
                        {row.price} · {row.iva}
                      </span>
                    </div>
                  </td>
                  <td className="muted">{row.quantity}</td>
                  <td>
                    <div className="sale-subtotal">
                      <strong>{row.subtotal}</strong>
                      <button type="button" className="sale-remove">
                        ×
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
      </div>

      <div className="sale-right">
        <Panel title="MEIO DE PAGAMENTO">
          <div className="payment-stack">
            <button type="button" className="payment-button">
              Numerário
            </button>
            <button type="button" className="payment-button">
              Cartão
            </button>
            <button type="button" className="payment-button">
              MB Way
            </button>
          </div>
        </Panel>

        <Panel>
          <div className="summary-list sale-summary">
            <div className="summary-row">
              <span className="muted">Subtotal</span>
              <span className="muted">€4.19</span>
            </div>
            <div className="summary-row">
              <span className="muted">IVA discriminado</span>
              <span className="muted">€0.63</span>
            </div>
            <div className="summary-row total">
              <strong>Total</strong>
              <strong>€4.82</strong>
            </div>
          </div>

          <Button className="full-width mt-large">Finalizar venda</Button>
          <Button variant="secondary" className="full-width mt-compact">
            Cancelar
          </Button>
        </Panel>
      </div>
    </div>
  )
}

export function FuncionarioReturnPage() {
  return (
    <div className="narrow-page">
      <div className="mf-stack">
        <Panel title="Pesquisar venda original">
          <TextField placeholder="N.º da venda ou data..." />
          <div className="sale-reference">
            <strong>#V-2801 — 18/04 15:30</strong>
            <span className="muted">Sandwich queijo · Leite UHT · Água 0.5L · Total: €6.48 · Cartão</span>
          </div>
        </Panel>

        <Panel title="Artigo a devolver">
          <SelectField label="Produto" value="Sandwich queijo (€2.99)" options={['Sandwich queijo (€2.99)']} onChange={() => undefined} />
          <TextField label="Quantidade" type="number" defaultValue="1" className="small-field" />
          <Callout tone="warning" className="mt-compact">
            Stock será reposto e nota de crédito emitida. Registado no log de auditoria.
          </Callout>
          <Button className="full-width mt-large">Confirmar devolução</Button>
        </Panel>
      </div>
    </div>
  )
}
