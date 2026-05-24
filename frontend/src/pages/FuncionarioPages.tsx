import { useEffect, useMemo, useState } from 'react'

import { Button, Callout, Panel, SelectField, TextField } from '../components/ui'
import { apiRequest, type FaturaResponse, type MeioPagamentoResponse, type PageResponse, type ProdutoResponse, type VendaResponse } from '../lib/api'
import { useAuth } from '../lib/auth'

const money = new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' })

export function FuncionarioSalePage() {
  const { session } = useAuth()
  const [sale, setSale] = useState<VendaResponse | null>(null)
  const [products, setProducts] = useState<ProdutoResponse[]>([])
  const [payments, setPayments] = useState<MeioPagamentoResponse[]>([])
  const [query, setQuery] = useState('')
  const [selectedProductId, setSelectedProductId] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [paymentType, setPaymentType] = useState('')
  const [nifCliente, setNifCliente] = useState('')
  const [nomeCliente, setNomeCliente] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    Promise.all([
      apiRequest<PageResponse<ProdutoResponse>>(`/produtos?lojaId=${session?.lojaId}&size=100`),
      apiRequest<MeioPagamentoResponse[]>('/meios-pagamento'),
    ]).then(([productPage, paymentRows]) => {
      if (ignore) return
      setProducts(productPage.content)
      setPayments(paymentRows)
      setPaymentType('')
    }).catch(() => {
      if (!ignore) setError('Não foi possível carregar produtos/meios de pagamento.')
    })
    return () => {
      ignore = true
    }
  }, [session?.lojaId])

  const matchingProducts = useMemo(() => {
    const term = query.trim().toLowerCase()
    if (!term) return []
    return products
      .filter((product) => product.codigoBarras === query.trim() || product.nome.toLowerCase().includes(term))
      .slice(0, 8)
  }, [products, query])

  const selectedProduct = useMemo(
    () => products.find((product) => product.id === selectedProductId) ?? null,
    [products, selectedProductId],
  )

  const selectedSubtotal = selectedProduct ? selectedProduct.precoVenda * Math.max(1, quantity || 1) : 0

  async function ensureSale() {
    if (sale) return sale
    const created = await apiRequest<VendaResponse>('/vendas', {
      method: 'POST',
      body: JSON.stringify({ lojaId: session?.lojaId, utilizadorId: session?.utilizadorId }),
    })
    setSale(created)
    return created
  }

  async function addLine() {
    setLoading(true)
    setError(null)
    setMessage(null)
    try {
      const product = selectedProduct
      if (!product) throw new Error('Produto não encontrado.')
      const currentSale = await ensureSale()
      const updated = await apiRequest<VendaResponse>(`/vendas/${currentSale.id}/linhas`, {
        method: 'POST',
        body: JSON.stringify({ produtoId: product.id, quantidade: quantity }),
      })
      setSale(updated)
      setQuery('')
      setSelectedProductId('')
      setQuantity(1)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao adicionar produto.')
    } finally {
      setLoading(false)
    }
  }

  async function removeLine(lineId: string) {
    if (!sale) return
    const updated = await apiRequest<VendaResponse>(`/vendas/${sale.id}/linhas/${lineId}`, { method: 'DELETE' })
    setSale(updated)
  }

  async function finalizeSale() {
    if (!sale || !paymentType) return
    setLoading(true)
    setError(null)
    setMessage(null)
    try {
      const finalized = await apiRequest<VendaResponse>(`/vendas/${sale.id}/finalizar`, {
        method: 'POST',
        body: JSON.stringify({ meioPagamento: paymentType }),
      })
      const fatura = await apiRequest<FaturaResponse>(`/vendas/${finalized.id}/fatura`, {
        method: 'POST',
        body: JSON.stringify({
          nifCliente: nifCliente.trim() || null,
          nomeCliente: nomeCliente.trim() || null,
        }),
      })
      setSale(null)
      setNifCliente('')
      setNomeCliente('')
      setPaymentType('')
      setMessage(`Venda finalizada com sucesso. Fatura ${fatura.numeroFatura} emitida.`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao finalizar venda.')
    } finally {
      setLoading(false)
    }
  }

  async function cancelSale() {
    if (!sale) return
    await apiRequest<void>(`/vendas/${sale.id}/anular`, { method: 'POST' })
    setSale(null)
    setPaymentType('')
    setMessage('Venda anulada.')
  }

  return (
    <div className="sale-layout">
      <div className="sale-left">
        <div className="sale-search-row">
          <TextField
            placeholder="Código de barras ou pesquisa rápida..."
            className="grow"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value)
              setSelectedProductId('')
            }}
          />
          <TextField type="number" min={1} value={quantity} onChange={(event) => setQuantity(Math.max(1, Number(event.target.value) || 1))} className="small-field" />
          <Button variant="secondary" className="scan-button" onClick={addLine} disabled={loading || !selectedProduct}>
            Adicionar
          </Button>
        </div>
        {matchingProducts.length > 0 ? (
          <div className="product-search-results" role="listbox" aria-label="Artigos encontrados">
            {matchingProducts.map((product) => (
              <button
                key={product.id}
                type="button"
                className={`product-search-option ${selectedProductId === product.id ? 'is-selected' : ''}`}
                onClick={() => setSelectedProductId(product.id)}
              >
                <span>
                  <strong>{product.nome}</strong>
                  <small>{product.codigoBarras}</small>
                </span>
                <span>{money.format(product.precoVenda)}</span>
              </button>
            ))}
          </div>
        ) : query.trim() ? <p className="mf-empty-state compact">Sem artigos encontrados.</p> : null}
        {selectedProduct ? (
          <p className="mf-section-note">
            Artigo selecionado: {selectedProduct.nome} · {quantity} × {money.format(selectedProduct.precoVenda)} = {money.format(selectedSubtotal)}
          </p>
        ) : null}
        {error ? <Callout tone="warning">{error}</Callout> : null}
        {message ? <Callout tone="info">{message}</Callout> : null}

        <Panel className="sale-table-panel">
          {!sale?.linhas.length ? <p className="mf-empty-state">Sem artigos na venda.</p> : null}
          <table className="mf-table compact sale">
            <thead>
              <tr>
                <th>PRODUTO</th>
                <th>QTD</th>
                <th>SUBTOTAL</th>
              </tr>
            </thead>
            <tbody>
              {(sale?.linhas ?? []).filter((row) => !row.anulada).map((row) => (
                <tr key={row.id}>
                  <td>
                    <div className="sale-product">
                      <strong>{row.produto}</strong>
                      <span className="muted">{money.format(row.precoUnitario)}</span>
                    </div>
                  </td>
                  <td className="muted">{row.quantidade}</td>
                  <td>
                    <div className="sale-subtotal">
                      <strong>{money.format(row.totalLinha || row.precoUnitario * row.quantidade)}</strong>
                      <button type="button" className="sale-remove" onClick={() => removeLine(row.id)}>×</button>
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
            {payments.map((payment) => (
              <button
                key={payment.tipo}
                type="button"
                className={`payment-button ${paymentType === payment.tipo ? 'is-selected' : ''}`}
                aria-pressed={paymentType === payment.tipo}
                onClick={() => setPaymentType(payment.tipo)}
              >
                {payment.descricao || payment.tipo}
              </button>
            ))}
          </div>
        </Panel>

        <Panel>
          <div className="mf-fields-grid two">
            <TextField label="NIF cliente" value={nifCliente} onChange={(event) => setNifCliente(event.target.value)} />
            <TextField label="Nome cliente" value={nomeCliente} onChange={(event) => setNomeCliente(event.target.value)} />
          </div>
          <div className="summary-list sale-summary">
            <div className="summary-row"><span className="muted">Subtotal</span><span className="muted">{money.format(sale?.subtotal ?? 0)}</span></div>
            <div className="summary-row"><span className="muted">IVA discriminado</span><span className="muted">{money.format(sale?.iva ?? 0)}</span></div>
            <div className="summary-row total"><strong>Total</strong><strong>{money.format(sale?.total ?? 0)}</strong></div>
          </div>

          {!paymentType ? <p className="mf-section-note">Selecione um meio de pagamento para finalizar.</p> : null}
          <Button className="full-width mt-large" onClick={finalizeSale} disabled={!sale?.linhas.length || !paymentType || loading}>
            {loading ? 'A finalizar...' : 'Finalizar venda'}
          </Button>
          <Button variant="secondary" className="full-width mt-compact" onClick={cancelSale} disabled={!sale || loading}>Cancelar</Button>
        </Panel>
      </div>
    </div>
  )
}

export function FuncionarioReturnPage() {
  const [saleId, setSaleId] = useState('')
  const [sale, setSale] = useState<VendaResponse | null>(null)
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function findSale() {
    setError(null)
    setMessage(null)
    try {
      const response = await apiRequest<VendaResponse>(`/vendas/${saleId}`)
      setSale(response)
      setProductId(response.linhas.find((line) => !line.anulada)?.produtoId ?? '')
    } catch (caught) {
      setSale(null)
      setProductId('')
      setError(caught instanceof Error ? caught.message : 'Venda não encontrada.')
    }
  }

  async function returnProduct() {
    if (!sale || !productId) return
    setError(null)
    setMessage(null)
    try {
      const response = await apiRequest<VendaResponse>(`/vendas/${sale.id}/devolucao`, {
        method: 'POST',
        body: JSON.stringify({ produtoId: productId, quantidade: quantity }),
      })
      setSale(response)
      setMessage('Devolução registada e stock reposto.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível registar a devolução.')
    }
  }

  return (
    <div className="narrow-page">
      <div className="mf-stack">
        <Panel title="Pesquisar venda original">
          <TextField placeholder="ID da venda" value={saleId} onChange={(event) => setSaleId(event.target.value)} />
          <Button className="mt-compact" onClick={findSale} disabled={!saleId}>Pesquisar</Button>
          {sale ? <div className="sale-reference"><strong>{sale.id}</strong><span className="muted">Total: {money.format(sale.total)} · {sale.meioPagamento}</span></div> : null}
        </Panel>

        <Panel title="Artigo a devolver">
          <SelectField
            label="Produto"
            value={productId}
            options={[{ value: '', label: 'Selecionar produto' }, ...(sale?.linhas ?? []).filter((line) => !line.anulada).map((line) => ({ value: line.produtoId, label: line.produto }))]}
            onChange={(event) => setProductId(event.target.value)}
          />
          <TextField label="Quantidade" type="number" min={1} value={quantity} onChange={(event) => setQuantity(Number(event.target.value))} className="small-field" />
          <Callout tone="warning" className="mt-compact">Stock será reposto e operação registada no log de auditoria.</Callout>
          {error ? <Callout tone="warning">{error}</Callout> : null}
          {message ? <Callout tone="info">{message}</Callout> : null}
          <Button className="full-width mt-large" onClick={returnProduct} disabled={!sale || !productId}>Confirmar devolução</Button>
        </Panel>
      </div>
    </div>
  )
}
