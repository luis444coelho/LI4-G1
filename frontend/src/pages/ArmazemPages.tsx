import { useCallback, useEffect, useState } from 'react'

import { Button, Callout, Panel, SelectField, TextField } from '../components/ui'
import { StockContent } from '../components/pageSections'
import { apiRequest, type EncomendaResponse, type EntradaMercadoriaResponse, type InventarioFisicoResponse, type LinhaInventarioResponse, type PageResponse, type ProximaGuiaRemessaResponse, type StockResponse } from '../lib/api'
import { useAuth } from '../lib/auth'

export function ArmazemStockPage() {
  return <StockContent />
}

export function ArmazemGoodsReceiptPage() {
  const { session } = useAuth()
  const lojaId = session?.lojaId
  const utilizadorId = session?.utilizadorId
  const [orders, setOrders] = useState<EncomendaResponse[]>([])
  const [orderId, setOrderId] = useState('')
  const [guide, setGuide] = useState('')
  const [received, setReceived] = useState<Record<string, number>>({})
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const loadReceiptData = useCallback(async (preserveSelection = true) => {
    if (!lojaId) return
    try {
      const [page, nextGuide] = await Promise.all([
        apiRequest<PageResponse<EncomendaResponse>>(`/encomendas?lojaId=${lojaId}&size=20`),
        apiRequest<ProximaGuiaRemessaResponse>(`/entradas-mercadoria/proxima-guia?lojaId=${lojaId}`),
      ])
      setOrders(page.content)
      setOrderId((current) => preserveSelection ? current || page.content[0]?.id || '' : page.content.find((item) => item.estado !== 'RECEBIDA')?.id ?? '')
      setGuide(nextGuide.numero)
    } catch {
      setError('Não foi possível carregar encomendas.')
    }
  }, [lojaId])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadReceiptData()
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadReceiptData])

  useEffect(() => {
    if (orderId && orders.some((item) => item.id === orderId)) return
    const timeoutId = window.setTimeout(() => {
      setOrderId(orders[0]?.id ?? '')
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [orderId, orders])

  const order = orders.find((item) => item.id === orderId)
  const pendingOrders = orders.filter((item) => item.estado !== 'RECEBIDA')
  const orderOptions = orders.map((item) => ({
    value: item.id,
    label: `${item.numeroDocumento} - ${item.fornecedor} - ${new Date(item.dataSubmissao).toLocaleDateString('pt-PT')}`,
  }))

  async function submitReceipt() {
    if (!order) return
    setError(null)
    setMessage(null)
    if (!guide.trim()) {
      setError('Não foi possível calcular a próxima guia de remessa.')
      return
    }
    try {
      const response = await apiRequest<EntradaMercadoriaResponse[]>('/entradas-mercadoria', {
        method: 'POST',
        body: JSON.stringify({
          encomendaId: order.id,
          lojaId,
          responsavelId: utilizadorId,
          guiaNumero: guide,
          dataEmissao: new Date().toISOString().slice(0, 10),
          linhas: order.linhas.map((line) => ({
            produtoId: line.produtoId,
            quantidadeEncomendada: line.quantidade,
            quantidadeRecebida: received[line.produtoId] ?? line.quantidade,
            observacoes: '',
          })),
        }),
      })
      setReceived({})
      setOrderId('')
      setGuide('')
      await loadReceiptData(false)
      setMessage(`${response.length} linhas de mercadoria registadas.`)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível registar a entrada de mercadoria.')
    }
  }

  return (
    <div className="mf-stack">
      <div className="mf-fields-grid two">
        <Panel title="Registar entrada de mercadoria" className="wide-form">
          <div className="mf-stack">
            <div className="mf-fields-grid two">
              <SelectField label="Encomenda associada" value={orderId} options={orderOptions} onChange={(event) => setOrderId(event.target.value)} />
              <TextField label="N.º guia de remessa" value={guide} disabled />
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
          </div>

          {error ? <Callout tone="warning" className="mt-compact">{error}</Callout> : null}
          {message ? <Callout tone="info" className="mt-compact">{message}</Callout> : null}
          <Button className="mt-large" onClick={submitReceipt} disabled={!order}>Confirmar receção e atualizar stock</Button>
        </Panel>

        <Panel title="Encomendas pendentes">
          {pendingOrders.length === 0 ? <p className="mf-empty-state">Sem encomendas pendentes.</p> : null}
          <table className="mf-table compact">
            <thead><tr><th>N.º</th><th>CHEGADA</th><th>ITENS</th><th></th></tr></thead>
            <tbody>
              {pendingOrders.map((item) => (
                <tr key={item.id}>
                  <td>{item.numeroDocumento}</td>
                  <td className="muted">{new Date(item.dataProcessamento).toLocaleString('pt-PT')}</td>
                  <td>{item.linhas.map((line) => `${line.produto} (${line.quantidade})`).join(', ')}</td>
                  <td><Button className="small" onClick={() => setOrderId(item.id)}>Selecionar</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
      </div>
    </div>
  )
}

export function ArmazemInventoryPage() {
  const { session } = useAuth()
  const lojaId = session?.lojaId
  const utilizadorId = session?.utilizadorId
  const [stockRows, setStockRows] = useState<StockResponse[]>([])
  const [inventory, setInventory] = useState<InventarioFisicoResponse | null>(null)
  const [lines, setLines] = useState<LinhaInventarioResponse[]>([])
  const [counted, setCounted] = useState<Record<string, number>>({})
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const loadInventoryData = useCallback(async () => {
    if (!lojaId) return
    setError(null)
    try {
      const [stock, page] = await Promise.all([
        apiRequest<StockResponse[]>(`/stock?lojaId=${lojaId}`),
        apiRequest<PageResponse<InventarioFisicoResponse>>(`/inventarios?lojaId=${lojaId}&size=20`),
      ])
      const currentInventory = page.content.find((item) => !item.fechado) ?? null
      setStockRows(stock)
      setInventory(currentInventory)

      if (currentInventory) {
        const discrepancyRows = await apiRequest<LinhaInventarioResponse[]>(`/inventarios/${currentInventory.id}/discrepancias`)
        setLines(discrepancyRows)
        setCounted(Object.fromEntries(discrepancyRows.map((line) => [line.produtoId, line.quantidadeContada])))
      } else {
        setLines([])
        setCounted({})
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível carregar o inventário.')
    }
  }, [lojaId])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadInventoryData()
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [loadInventoryData])

  async function startInventory() {
    setError(null)
    setMessage(null)
    try {
      const created = await apiRequest<InventarioFisicoResponse>('/inventarios', {
        method: 'POST',
        body: JSON.stringify({ lojaId, utilizadorId }),
      })
      setInventory(created)
      const discrepancyRows = await apiRequest<LinhaInventarioResponse[]>(`/inventarios/${created.id}/discrepancias`)
      setLines(discrepancyRows)
      setCounted(Object.fromEntries(discrepancyRows.map((line) => [line.produtoId, line.quantidadeContada])))
      setMessage('Inventário físico iniciado.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível iniciar o inventário.')
    }
  }

  async function registerCount(product: StockResponse) {
    if (!inventory) return
    setError(null)
    setMessage(null)
    try {
      const line = await apiRequest<LinhaInventarioResponse>(`/inventarios/${inventory.id}/linhas`, {
        method: 'POST',
        body: JSON.stringify({ produtoId: product.produtoId, quantidade: counted[product.produtoId] ?? product.quantidade }),
      })
      setLines((items) => [line, ...items.filter((item) => item.produtoId !== line.produtoId)])
      setCounted((items) => ({ ...items, [line.produtoId]: line.quantidadeContada }))
      setMessage(`Contagem registada para ${product.produto}.`)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível registar a contagem.')
    }
  }

  async function closeInventory() {
    if (!inventory) return
    setError(null)
    setMessage(null)
    try {
      for (const row of stockRows) {
        await apiRequest<LinhaInventarioResponse>(`/inventarios/${inventory.id}/linhas`, {
          method: 'POST',
          body: JSON.stringify({ produtoId: row.produtoId, quantidade: counted[row.produtoId] ?? row.quantidade }),
        })
      }
      await apiRequest<void>(`/inventarios/${inventory.id}/fechar`, { method: 'POST' })
      await loadInventoryData()
      setMessage('Inventário fechado e stock atualizado.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível fechar o inventário.')
    }
  }

  return (
    <div className="mf-stack">
      <div className="mf-toolbar-space">
        <p className="mf-section-note">{inventory ? 'Inventário físico em curso' : 'Inventário físico por iniciar'}</p>
        {inventory ? <Button onClick={closeInventory} disabled={inventory.fechado}>Fechar inventário</Button> : <Button onClick={startInventory}>Iniciar inventário</Button>}
      </div>
      {error ? <Callout tone="warning">{error}</Callout> : null}
      {message ? <Callout tone="info">{message}</Callout> : null}

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
