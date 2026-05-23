import { useEffect, useState } from 'react'

import { Button, Callout, Panel, SelectField, TextField } from '../components/ui'
import { StockContent } from '../components/pageSections'
import { apiRequest, type EncomendaResponse, type EntradaMercadoriaResponse, type InventarioFisicoResponse, type LinhaInventarioResponse, type PageResponse, type StockResponse } from '../lib/api'
import { useAuth } from '../lib/auth'

export function ArmazemStockPage() {
  return <StockContent />
}

export function ArmazemGoodsReceiptPage() {
  const { session } = useAuth()
  const [orders, setOrders] = useState<EncomendaResponse[]>([])
  const [orderId, setOrderId] = useState('')
  const [guide, setGuide] = useState('')
  const [received, setReceived] = useState<Record<string, number>>({})
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    apiRequest<PageResponse<EncomendaResponse>>(`/encomendas?lojaId=${session?.lojaId}&size=20`)
      .then((page) => {
        setOrders(page.content)
        setOrderId(page.content[0]?.id ?? '')
      })
      .catch(() => setError('Não foi possível carregar encomendas.'))
  }, [session?.lojaId])

  const order = orders.find((item) => item.id === orderId)

  async function submitReceipt() {
    if (!order) return
    setError(null)
    const response = await apiRequest<EntradaMercadoriaResponse[]>('/entradas-mercadoria', {
      method: 'POST',
      body: JSON.stringify({
        encomendaId: order.id,
        lojaId: session?.lojaId,
        responsavelId: session?.utilizadorId,
        guiaNumero: guide || `GR-${order.id.slice(0, 8)}`,
        dataEmissao: new Date().toISOString().slice(0, 10),
        linhas: order.linhas.map((line) => ({
          produtoId: line.produtoId,
          quantidadeEncomendada: line.quantidade,
          quantidadeRecebida: received[line.produtoId] ?? line.quantidade,
          observacoes: '',
        })),
      }),
    })
    setMessage(`${response.length} linhas de mercadoria registadas.`)
  }

  return (
    <div className="mf-stack">
      <Panel title="Registar entrada de mercadoria" className="wide-form">
        <div className="mf-fields-grid two">
          <SelectField label="Encomenda associada" value={orderId} options={orders.map((item) => item.id)} onChange={(event) => setOrderId(event.target.value)} />
          <TextField label="N.º guia de remessa" value={guide} onChange={(event) => setGuide(event.target.value)} />
        </div>

        <div className="receipt-table">
          {!order ? <p className="mf-empty-state">Sem encomenda selecionada.</p> : null}
          <table className="mf-table compact">
            <thead><tr><th>PRODUTO</th><th>ENCOM.</th><th>RECEBIDO</th><th>DIF.</th></tr></thead>
            <tbody>
              {(order?.linhas ?? []).map((row) => {
                const value = received[row.produtoId] ?? row.quantidade
                const diff = value - row.quantidade
                return (
                  <tr key={row.id}>
                    <td>{row.produto}</td>
                    <td className="muted">{row.quantidade}</td>
                    <td><TextField type="number" value={value} onChange={(event) => setReceived((state) => ({ ...state, [row.produtoId]: Number(event.target.value) }))} className="receipt-input" /></td>
                    <td className={diff < 0 ? 'tone-danger' : 'muted'}>{diff}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        {error ? <Callout tone="warning" className="mt-compact">{error}</Callout> : null}
        {message ? <Callout tone="info" className="mt-compact">{message}</Callout> : null}
        <Button className="mt-large" onClick={submitReceipt} disabled={!order}>Confirmar receção e atualizar stock</Button>
      </Panel>
    </div>
  )
}

export function ArmazemInventoryPage() {
  const { session } = useAuth()
  const [stockRows, setStockRows] = useState<StockResponse[]>([])
  const [inventory, setInventory] = useState<InventarioFisicoResponse | null>(null)
  const [lines, setLines] = useState<LinhaInventarioResponse[]>([])
  const [counted, setCounted] = useState<Record<string, number>>({})

  useEffect(() => {
    Promise.all([
      apiRequest<StockResponse[]>(`/stock?lojaId=${session?.lojaId}`),
      apiRequest<PageResponse<InventarioFisicoResponse>>(`/inventarios?lojaId=${session?.lojaId}&size=1`),
    ]).then(([stock, page]) => {
      setStockRows(stock)
      setInventory(page.content[0] ?? null)
    }).catch(() => undefined)
  }, [session?.lojaId])

  async function startInventory() {
    const created = await apiRequest<InventarioFisicoResponse>('/inventarios', {
      method: 'POST',
      body: JSON.stringify({ lojaId: session?.lojaId, utilizadorId: session?.utilizadorId }),
    })
    setInventory(created)
  }

  async function registerCount(product: StockResponse) {
    if (!inventory) return
    const line = await apiRequest<LinhaInventarioResponse>(`/inventarios/${inventory.id}/linhas`, {
      method: 'POST',
      body: JSON.stringify({ produtoId: product.produtoId, quantidade: counted[product.produtoId] ?? product.quantidade }),
    })
    setLines((items) => [line, ...items.filter((item) => item.produtoId !== line.produtoId)])
  }

  async function closeInventory() {
    if (!inventory) return
    await apiRequest<void>(`/inventarios/${inventory.id}/fechar`, { method: 'POST' })
    setInventory({ ...inventory, fechado: true })
  }

  return (
    <div className="mf-stack">
      <div className="mf-toolbar-space">
        <p className="mf-section-note">Inventário físico {inventory ? inventory.id.slice(0, 8) : 'por iniciar'}</p>
        {inventory ? <Button onClick={closeInventory} disabled={inventory.fechado}>Fechar inventário</Button> : <Button onClick={startInventory}>Iniciar inventário</Button>}
      </div>

      <Panel>
        <table className="mf-table compact inventory">
          <thead><tr><th>PRODUTO</th><th>SISTEMA</th><th>CONTADO</th><th>DIFERENÇA</th><th></th></tr></thead>
          <tbody>
            {stockRows.map((row) => {
              const countedValue = counted[row.produtoId] ?? row.quantidade
              const line = lines.find((item) => item.produtoId === row.produtoId)
              return (
                <tr key={row.produtoId}>
                  <td>{row.produto}</td>
                  <td className="muted">{row.quantidade}</td>
                  <td><TextField type="number" value={countedValue} onChange={(event) => setCounted((state) => ({ ...state, [row.produtoId]: Number(event.target.value) }))} className="receipt-input" /></td>
                  <td>{line ? line.discrepancia : countedValue - row.quantidade}</td>
                  <td><Button variant="secondary" className="small" disabled={!inventory || inventory.fechado} onClick={() => registerCount(row)}>Registar</Button></td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </Panel>
    </div>
  )
}
