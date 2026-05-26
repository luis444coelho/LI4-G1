import { useEffect, useMemo, useState } from 'react'

import { Button, Callout, Panel, SelectField, TextField } from '../components/ui'
import { apiDownload, apiRequest, type DevolucaoResponse, type FaturaResponse, type MeioPagamentoResponse, type PageResponse, type ProdutoResponse, type VendaResponse } from '../lib/api'
import { useAuth } from '../lib/auth'
import { formatDateInput } from '../lib/date'
import { downloadBlob } from '../lib/download'

const money = new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' })

async function fetchInvoices(lojaId: string, cliente = '') {
  const params = new URLSearchParams({ lojaId, size: '50' })
  const termo = cliente.trim()
  if (termo) params.set('cliente', termo)
  const page = await apiRequest<PageResponse<FaturaResponse>>(`/vendas/faturas?${params.toString()}`)
  return page.content
}

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
  const [lastInvoice, setLastInvoice] = useState<FaturaResponse | null>(null)
  const [invoiceQuery, setInvoiceQuery] = useState('')
  const [invoiceSearch, setInvoiceSearch] = useState('')
  const [invoices, setInvoices] = useState<FaturaResponse[]>([])
  const [selectedInvoiceId, setSelectedInvoiceId] = useState('')
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

  useEffect(() => {
    if (!session?.lojaId) return
    const lojaId = session.lojaId
    const timeoutId = window.setTimeout(() => {
      void fetchInvoices(lojaId).then((rows) => {
        setInvoices(rows)
        setSelectedInvoiceId((current) => rows.some((invoice) => invoice.id === current) ? current : rows[0]?.id || '')
      }).catch(() => undefined)
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [session?.lojaId])

  async function loadInvoices(cliente = '') {
    if (!session?.lojaId) return
    const rows = await fetchInvoices(session.lojaId, cliente)
    setInvoices(rows)
    setSelectedInvoiceId((current) => rows.some((invoice) => invoice.id === current) ? current : rows[0]?.id || '')
  }

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
    setLastInvoice(null)
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
      setLastInvoice(fatura)
      setInvoiceQuery(fatura.numeroFatura)
      setSelectedInvoiceId(fatura.id)
      setInvoices((items) => [fatura, ...items.filter((item) => item.id !== fatura.id)])
      setMessage(`Venda finalizada com sucesso. Fatura ${fatura.numeroFatura} e recibo disponíveis.`)
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
    setLastInvoice(null)
    setPaymentType('')
    setMessage('Venda anulada.')
  }

  async function downloadFiscalDocument(type: 'FATURA' | 'RECIBO') {
    if (!lastInvoice) return
    setError(null)
    try {
      const response = await apiDownload(`/vendas/faturas/${lastInvoice.id}/documento?tipo=${type}`)
      downloadBlob(response.blob, response.filename)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : `Não foi possível emitir ${type.toLowerCase()}.`)
    }
  }

  async function lookupInvoice() {
    const queryValue = invoiceQuery.trim()
    if (!queryValue) return
    setError(null)
    setMessage(null)
    try {
      const invoice = queryValue.includes('/')
        ? await apiRequest<FaturaResponse>(`/vendas/faturas/numero?numeroFatura=${encodeURIComponent(queryValue)}`)
        : await apiRequest<FaturaResponse>(`/vendas/faturas/${queryValue}`)
      setLastInvoice(invoice)
      setInvoiceQuery(invoice.numeroFatura)
      setSelectedInvoiceId(invoice.id)
      setInvoices((items) => items.some((item) => item.id === invoice.id) ? items : [invoice, ...items])
      setMessage(`Fatura ${invoice.numeroFatura} obtida.`)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível obter a fatura.')
    }
  }

  function selectInvoice(invoiceId: string) {
    setSelectedInvoiceId(invoiceId)
    const invoice = invoices.find((item) => item.id === invoiceId)
    if (!invoice) return
    setLastInvoice(invoice)
    setInvoiceQuery(invoice.numeroFatura)
    setMessage(null)
    setError(null)
  }

  const invoiceOptions = useMemo(() => {
    if (invoices.length === 0) {
      return [{ value: '', label: 'Sem faturas encontradas' }]
    }
    return invoices.map((invoice) => {
      const cliente = invoice.nomeCliente || invoice.nifCliente || 'Consumidor final'
      return {
        value: invoice.id,
        label: `${cliente} - ${invoice.numeroFatura} - ${money.format(invoice.totalComIva)}`,
      }
    })
  }, [invoices])

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
        {lastInvoice ? (
          <Panel title="Documento fiscal" className="fiscal-panel">
            <div className="invoice-preview">
              <strong>{lastInvoice.numeroFatura}</strong>
              <span>{lastInvoice.tipo} · {money.format(lastInvoice.totalComIva)}</span>
              <span>{lastInvoice.nomeCliente || lastInvoice.nifCliente ? [lastInvoice.nomeCliente, lastInvoice.nifCliente].filter(Boolean).join(' · ') : 'Consumidor final'}</span>
            </div>
            <div className="mf-actions-row fiscal-actions">
              <Button variant="secondary" onClick={() => void downloadFiscalDocument('FATURA')}>Descarregar fatura</Button>
              <Button variant="secondary" onClick={() => void downloadFiscalDocument('RECIBO')}>Descarregar recibo</Button>
            </div>
          </Panel>
        ) : null}

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

        <Panel title="Consultar documento">
          <TextField label="Cliente/NIF" value={invoiceSearch} onChange={(event) => setInvoiceSearch(event.target.value)} />
          <Button variant="secondary" className="full-width mt-compact" onClick={() => void loadInvoices(invoiceSearch)} disabled={loading}>Pesquisar faturas</Button>
          <SelectField label="Faturas" value={selectedInvoiceId} options={invoiceOptions} onChange={(event) => selectInvoice(event.target.value)} />
          <TextField label="N.º fatura ou ID" value={invoiceQuery} onChange={(event) => setInvoiceQuery(event.target.value)} />
          <Button variant="secondary" className="full-width mt-compact" onClick={() => void lookupInvoice()} disabled={!invoiceQuery.trim() || loading}>Obter por número/ID</Button>
          {lastInvoice ? (
            <div className="mf-actions-row fiscal-actions">
              <Button variant="secondary" onClick={() => void downloadFiscalDocument('FATURA')}>Fatura</Button>
              <Button variant="secondary" onClick={() => void downloadFiscalDocument('RECIBO')}>Recibo</Button>
            </div>
          ) : null}
        </Panel>
      </div>
    </div>
  )
}

export function FuncionarioReturnPage() {
  const { session } = useAuth()
  const [recentSales, setRecentSales] = useState<VendaResponse[]>([])
  const [returns, setReturns] = useState<DevolucaoResponse[]>([])
  const [saleSearch, setSaleSearch] = useState('')
  const [saleId, setSaleId] = useState('')
  const [sale, setSale] = useState<VendaResponse | null>(null)
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function loadReturnData() {
    if (!session?.lojaId) return
    setError(null)
    try {
      const today = new Date()
      const start = new Date(today)
      start.setDate(start.getDate() - 30)
      const [salesPage, returnRows] = await Promise.all([
        apiRequest<PageResponse<VendaResponse>>(`/vendas?lojaId=${session.lojaId}&inicio=${formatDateInput(start)}&fim=${formatDateInput(today)}&size=50`),
        apiRequest<DevolucaoResponse[]>(`/vendas/devolucoes?lojaId=${session.lojaId}`),
      ])
      const finalizedSales = salesPage.content.filter((row) => !row.anulada && row.meioPagamento)
      setRecentSales(finalizedSales)
      setReturns(returnRows)
      if (!sale && finalizedSales.length > 0) {
        selectSale(finalizedSales[0])
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível carregar vendas/devoluções.')
    }
  }

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void loadReturnData()
    }, 0)
    return () => window.clearTimeout(timeoutId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session?.lojaId])

  const filteredSales = useMemo(() => {
    const term = saleSearch.trim().toLowerCase()
    if (!term) return recentSales
    return recentSales.filter((row) => {
      const products = row.linhas.map((line) => line.produto).join(' ').toLowerCase()
      return row.id.toLowerCase().includes(term)
        || new Date(row.dataHora).toLocaleString('pt-PT').toLowerCase().includes(term)
        || products.includes(term)
        || String(row.total).includes(term)
    })
  }, [recentSales, saleSearch])

  const selectedLine = sale?.linhas.find((line) => line.produtoId === productId && !line.anulada) ?? null
  const creditValue = selectedLine ? selectedLine.precoUnitario * quantity : 0

  function selectSale(row: VendaResponse) {
    setSale(row)
    setSaleId(row.id)
    setProductId(row.linhas.find((line) => !line.anulada)?.produtoId ?? '')
    setQuantity(1)
    setMessage(null)
  }

  async function findSale() {
    setError(null)
    setMessage(null)
    const existing = recentSales.find((row) => row.id === saleId)
    if (existing) {
      selectSale(existing)
      return
    }
    try {
      const response = await apiRequest<VendaResponse>(`/vendas/${saleId}`)
      selectSale(response)
      setRecentSales((items) => items.some((item) => item.id === response.id) ? items : [response, ...items])
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
      await loadReturnData()
      setMessage(`Devolução registada. Valor creditado: ${money.format(creditValue)}. Stock atualizado.`)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível registar a devolução.')
    }
  }

  return (
    <div className="return-layout">
      <Panel title="Vendas recentes">
        <TextField placeholder="Pesquisar por produto, data, total ou ID..." value={saleSearch} onChange={(event) => setSaleSearch(event.target.value)} />
        <div className="return-sales-list">
          {filteredSales.length === 0 ? <p className="mf-empty-state">Sem vendas finalizadas para devolução.</p> : null}
          {filteredSales.map((row) => (
            <button
              key={row.id}
              type="button"
              className={`return-sale-option ${sale?.id === row.id ? 'is-selected' : ''}`}
              onClick={() => selectSale(row)}
            >
              <span>
                <strong>{new Date(row.dataHora).toLocaleString('pt-PT')}</strong>
                <small>{row.linhas.filter((line) => !line.anulada).map((line) => `${line.produto} (${line.quantidade})`).join(', ')}</small>
              </span>
              <span>{money.format(row.total)}</span>
            </button>
          ))}
        </div>
        <div className="mf-fields-grid two mt-compact">
          <TextField label="ID da venda" value={saleId} onChange={(event) => setSaleId(event.target.value)} />
          <div className="mf-field">
            <span className="mf-field-label">&nbsp;</span>
            <Button variant="secondary" onClick={findSale} disabled={!saleId}>Obter por ID</Button>
          </div>
        </div>
      </Panel>

      <div className="mf-stack">
        <Panel title="Artigo a devolver">
          {sale ? <div className="sale-reference"><strong>{new Date(sale.dataHora).toLocaleString('pt-PT')}</strong><span className="muted">Total: {money.format(sale.total)} · {sale.meioPagamento} · {sale.id}</span></div> : <p className="mf-empty-state">Selecione uma venda finalizada.</p>}
          <SelectField
            label="Produto"
            value={productId}
            options={[{ value: '', label: 'Selecionar produto' }, ...(sale?.linhas ?? []).filter((line) => !line.anulada).map((line) => ({ value: line.produtoId, label: `${line.produto} - vendido ${line.quantidade} - ${money.format(line.precoUnitario)}` }))]}
            onChange={(event) => setProductId(event.target.value)}
          />
          <div className="mf-fields-grid two mt-compact">
            <TextField label="Quantidade" type="number" min={1} max={selectedLine?.quantidade ?? undefined} value={quantity} onChange={(event) => setQuantity(Math.max(1, Number(event.target.value) || 1))} />
            <TextField label="Valor creditado" value={money.format(creditValue)} disabled />
          </div>
          <Callout tone="warning" className="mt-compact">A confirmação credita o valor, repõe stock e regista a devolução no histórico.</Callout>
          {error ? <Callout tone="warning">{error}</Callout> : null}
          {message ? <Callout tone="info">{message}</Callout> : null}
          <Button className="full-width mt-large" onClick={returnProduct} disabled={!sale || !productId || quantity <= 0}>Confirmar devolução</Button>
        </Panel>

        <Panel title="Histórico de devoluções">
          <table className="mf-table compact">
            <thead><tr><th>DOC.</th><th>PRODUTO</th><th>QTD.</th><th>CRÉDITO</th><th>DATA</th></tr></thead>
            <tbody>
              {returns.length === 0 ? <tr><td colSpan={5} className="muted">Sem devoluções registadas.</td></tr> : null}
              {returns.map((row) => (
                <tr key={row.id}>
                  <td>{row.numeroDocumento}</td>
                  <td>{row.produto}</td>
                  <td>{row.quantidade}</td>
                  <td>{money.format(row.valorCreditado)}</td>
                  <td className="muted">{new Date(row.dataHora).toLocaleString('pt-PT')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>
      </div>
    </div>
  )
}
