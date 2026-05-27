import { useCallback, useEffect, useState } from 'react'

import { Button, Callout, InitialAvatar, Panel, SelectField, StatusBadge, TextField } from '../components/ui'
import { ReportsContent, StockContent } from '../features/shared/PageSections'
import { apiRequest, type AjusteInventarioResponse, type FechoCaixaResponse, type MotivoAjusteResponse, type PageResponse, type PerfilResponse, type StockResponse, type UtilizadorResponse, type VendaResponse } from '../lib/api'
import { useAuth } from '../lib/auth'
import { formatDateInput } from '../lib/date'

const money = new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' })
type PaymentTotalField = 'totalNumerario' | 'totalCartao' | 'totalMbway'
type SalesTotals = Record<PaymentTotalField | 'totalGeral', number>

const paymentTotalFields: Record<string, PaymentTotalField> = {
  NUMERARIO: 'totalNumerario',
  CARTAO: 'totalCartao',
  MBWAY: 'totalMbway',
}
const operationalEmployeeProfiles = ['FUNCIONARIO', 'ARMAZEM', 'RESPONSAVEL_ARMAZEM']

function isOperationalEmployee(profile: string) {
  return operationalEmployeeProfiles.includes(profile)
}

export function GerenteStockPage() {
  return <StockContent />
}

export function GerenteCashPage() {
  const { session } = useAuth()
  const lojaId = session?.lojaId
  const [closings, setClosings] = useState<FechoCaixaResponse[]>([])
  const [current, setCurrent] = useState<FechoCaixaResponse | null>(null)
  const [todaySales, setTodaySales] = useState<VendaResponse[]>([])
  const [closingNote, setClosingNote] = useState('')
  const [error, setError] = useState<string | null>(null)

  const loadCashData = useCallback(async () => {
    if (!lojaId) return
    const today = formatDateInput(new Date())
    const [closingPage, salesPage] = await Promise.all([
      apiRequest<PageResponse<FechoCaixaResponse>>(`/fechos-caixa?lojaId=${lojaId}&size=5`),
      apiRequest<PageResponse<VendaResponse>>(`/vendas?lojaId=${lojaId}&inicio=${today}&fim=${today}&porFechar=true&size=100`),
    ])
    setClosings(closingPage.content)
    const pendingClosing = closingPage.content.find((closing) => closing.data === today && !closing.confirmado) ?? null
    setCurrent(pendingClosing)
    setClosingNote((note) => (pendingClosing && note.trim() ? note : pendingClosing?.observacoesDiscrepancia ?? ''))
    setTodaySales(salesPage.content.filter((sale) => sale.meioPagamento && !sale.anulada))
  }, [lojaId])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      loadCashData().catch(() => setError('Não foi possível carregar fechos de caixa.'))
    }, 0)
    const interval = window.setInterval(() => {
      loadCashData().catch(() => setError('Não foi possível atualizar fechos de caixa.'))
    }, 5000)
    return () => {
      window.clearTimeout(timeoutId)
      window.clearInterval(interval)
    }
  }, [loadCashData])

  async function createClosing() {
    setError(null)
    try {
      const created = await apiRequest<FechoCaixaResponse>('/fechos-caixa', {
        method: 'POST',
        body: JSON.stringify({ lojaId, utilizadorId: session?.utilizadorId }),
      })
      setCurrent(created)
      setClosingNote(created.observacoesDiscrepancia ?? '')
      await loadCashData()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível criar o fecho de caixa.')
    }
  }

  async function confirmClosing() {
    if (!current) return
    setError(null)
    try {
      const confirmed = await apiRequest<FechoCaixaResponse>(`/fechos-caixa/${current.id}/confirmar`, {
        method: 'POST',
        body: JSON.stringify({ observacoesDiscrepancia: closingNote.trim() || null }),
      })
      setCurrent(null)
      setClosingNote('')
      setTodaySales([])
      setClosings((items) => [confirmed, ...items.filter((item) => item.id !== confirmed.id)])
      await loadCashData()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível confirmar o fecho de caixa.')
    }
  }

  const salesSummary = summarizeSales(todaySales)
  const summary = current ?? salesSummary
  const hasPendingClosing = Boolean(current && !current.confirmado)
  const today = formatDateInput(new Date())
  const hasClosedToday = closings.some((closing) => closing.data === today && closing.confirmado)

  return (
    <div className="mf-two-column closing">
      <Panel title={`Resumo do dia — ${summary?.data ?? new Date().toLocaleDateString('pt-PT')}`}>
        {error ? <Callout tone="warning">{error}</Callout> : null}
        <div className="summary-list">
          <div className="summary-row"><span className="muted">Numerário</span><strong>{money.format(summary?.totalNumerario ?? 0)}</strong></div>
          <div className="summary-row"><span className="muted">Cartão</span><strong>{money.format(summary?.totalCartao ?? 0)}</strong></div>
          <div className="summary-row"><span className="muted">MB Way</span><strong>{money.format(summary?.totalMbway ?? 0)}</strong></div>
          <div className="summary-row"><span className="muted">Total geral</span><strong className="summary-strong">{money.format(summary?.totalGeral ?? 0)}</strong></div>
        </div>

        {hasPendingClosing ? (
          <TextField
            label="Observação de discrepância"
            placeholder="Ex.: numerário contado difere do total esperado"
            value={closingNote}
            onChange={(event) => setClosingNote(event.target.value)}
            className="mt-large"
          />
        ) : null}

        <Button className="full-width mt-large" onClick={hasPendingClosing ? confirmClosing : createClosing} disabled={hasClosedToday && !hasPendingClosing}>
          {hasClosedToday && !hasPendingClosing ? 'Caixa fechada hoje' : hasPendingClosing ? 'Confirmar fecho de caixa' : 'Criar fecho de caixa'}
        </Button>
      </Panel>

      <Panel title="Vendas de hoje">
        <div className="closing-history">
          {todaySales.length === 0 ? <p className="mf-empty-state">Sem vendas finalizadas hoje.</p> : null}
          {todaySales.map((sale) => (
            <div key={sale.id} className="summary-row">
              <div className="closing-left">
                <span className="muted">{new Date(sale.dataHora).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' })}</span>
                <strong>{money.format(sale.total)}</strong>
              </div>
              <StatusBadge tone="success">{sale.meioPagamento}</StatusBadge>
            </div>
          ))}
        </div>
      </Panel>

      <Panel title="Histórico — 5 dias">
        <div className="closing-history">
          {closings.length === 0 ? <p className="mf-empty-state">Sem fechos registados.</p> : null}
          {closings.map((item) => (
            <div key={item.id} className="summary-row">
              <div className="closing-left">
                <span className="muted">{item.data}</span>
                <strong>{money.format(item.totalGeral)}</strong>
                {item.observacoesDiscrepancia ? <span className="muted faint">{item.observacoesDiscrepancia}</span> : null}
              </div>
              <StatusBadge tone={item.confirmado ? 'success' : 'warning'}>{item.confirmado ? 'Confirmado' : 'Pendente'}</StatusBadge>
            </div>
          ))}
        </div>
      </Panel>
    </div>
  )
}

function summarizeSales(sales: VendaResponse[]): FechoCaixaResponse {
  const totals = sales.reduce<SalesTotals>((accumulator, sale) => {
    const field = sale.meioPagamento ? paymentTotalFields[sale.meioPagamento] : undefined
    if (field) {
      accumulator[field] += sale.total
    }
    accumulator.totalGeral += sale.total
    return accumulator
  }, {
    totalNumerario: 0,
    totalCartao: 0,
    totalMbway: 0,
    totalGeral: 0,
  })

  return {
    id: 'preview',
    lojaId: '',
    gerenteId: '',
    data: formatDateInput(new Date()),
    confirmado: false,
    ...totals,
  }
}

export function GerenteReportsPage() {
  return <ReportsContent />
}

export function GerenteAdjustmentPage() {
  const { session } = useAuth()
  const [stock, setStock] = useState<StockResponse[]>([])
  const [motives, setMotives] = useState<MotivoAjusteResponse[]>([])
  const [history, setHistory] = useState<AjusteInventarioResponse[]>([])
  const [produtoId, setProdutoId] = useState('')
  const [motivo, setMotivo] = useState('QUEBRA')
  const [quantidade, setQuantidade] = useState(-1)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const productOptions = stock.map((item) => ({
    value: item.produtoId,
    label: `${item.produto} (${item.quantidade} un.)`,
  }))
  const motiveOptions = motives.map((item) => ({
    value: item.codigo,
    label: item.descricao || item.codigo,
  }))

  useEffect(() => {
    Promise.all([
      apiRequest<StockResponse[]>(`/stock?lojaId=${session?.lojaId}`),
      apiRequest<MotivoAjusteResponse[]>('/stock/motivos-ajuste'),
      apiRequest<PageResponse<AjusteInventarioResponse>>(`/stock/ajustes?lojaId=${session?.lojaId}&size=10`),
    ]).then(([stockRows, motiveRows, historyPage]) => {
      setStock(stockRows)
      setProdutoId(stockRows[0]?.produtoId ?? '')
      setMotives(motiveRows)
      setMotivo(motiveRows[0]?.codigo ?? 'QUEBRA')
      setHistory(historyPage.content)
    }).catch(() => setError('Não foi possível carregar dados de inventário.'))
  }, [session?.lojaId])

  async function submitAdjustment() {
    setError(null)
    setMessage(null)
    try {
      const adjustment = await apiRequest<AjusteInventarioResponse>('/stock/ajustes', {
        method: 'POST',
        body: JSON.stringify({ produtoId, lojaId: session?.lojaId, quantidade, motivo, utilizadorId: session?.utilizadorId }),
      })
      setHistory((items) => [adjustment, ...items])
      setStock((items) => items.map((item) => (
        item.produtoId === produtoId
          ? { ...item, quantidade: item.quantidade + quantidade }
          : item
      )))
      setMessage('Ajuste registado com sucesso.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível registar o ajuste.')
    }
  }

  return (
    <div className="mf-stack">
      <Panel title="Registar ajuste manual" className="narrow-form">
        <SelectField label="Produto" value={produtoId} options={productOptions} onChange={(event) => setProdutoId(event.target.value)} />
        <div className="mf-fields-grid two">
          <TextField label="Quantidade (+ ou −)" type="number" value={quantidade} onChange={(event) => setQuantidade(Number(event.target.value))} />
          <SelectField label="Motivo" value={motivo} options={motiveOptions} onChange={(event) => setMotivo(event.target.value)} />
        </div>
        <Callout tone="info" className="mt-compact">Operação registada no histórico de auditoria.</Callout>
        {error ? <Callout tone="warning">{error}</Callout> : null}
        {message ? <Callout tone="info">{message}</Callout> : null}
        <Button className="full-width mt-large" onClick={submitAdjustment} disabled={!produtoId}>Confirmar ajuste</Button>
      </Panel>

      <Panel title="Últimos ajustes" className="narrow-form">
        <div className="adjustment-list">
          {history.length === 0 ? <p className="mf-empty-state">Sem ajustes registados.</p> : null}
          {history.map((item) => (
            <div key={item.id} className="adjustment-row">
              <span>{item.produto}</span>
              <div className="adjustment-meta">
                <span className="muted">{item.motivo}</span>
                <span className={item.quantidade < 0 ? 'tone-danger' : 'tone-success'}>{item.quantidade}</span>
                <span className="muted faint">{new Date(item.dataHora).toLocaleString('pt-PT')}</span>
              </div>
            </div>
          ))}
        </div>
      </Panel>
    </div>
  )
}

export function GerenteEmployeesPage() {
  const { session } = useAuth()
  const [rows, setRows] = useState<UtilizadorResponse[]>([])
  const [profiles, setProfiles] = useState<PerfilResponse[]>([])
  const [editingId, setEditingId] = useState<string | null>(null)
  const [draft, setDraft] = useState({
    username: '',
    password: 'MiniFormiga2026!',
    nome: '',
    email: '',
    perfilId: '',
    ativo: true,
  })
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([
      apiRequest<PageResponse<UtilizadorResponse>>(`/utilizadores?lojaId=${session?.lojaId}&size=50`),
      apiRequest<PerfilResponse[]>('/utilizadores/perfis'),
    ])
      .then(([page, profileRows]) => {
        const operationalProfiles = profileRows.filter((profile) => isOperationalEmployee(profile.nome))
        setRows(page.content.filter((row) => isOperationalEmployee(row.perfilId || row.perfil)))
        setProfiles(operationalProfiles)
        setDraft((state) => ({ ...state, perfilId: state.perfilId || operationalProfiles.find((profile) => profile.nome === 'FUNCIONARIO')?.nome || operationalProfiles[0]?.nome || '' }))
      })
      .catch(() => setError('Não foi possível carregar funcionários.'))
  }, [session?.lojaId])

  function resetDraft() {
    setEditingId(null)
    setDraft((state) => ({
      username: '',
      password: 'MiniFormiga2026!',
      nome: '',
      email: '',
      perfilId: profiles.find((profile) => profile.nome === 'FUNCIONARIO')?.nome || profiles[0]?.nome || state.perfilId,
      ativo: true,
    }))
  }

  async function submitEmployee() {
    setError(null)
    setMessage(null)
    try {
      if (editingId) {
        const updated = await apiRequest<UtilizadorResponse>(`/utilizadores/${editingId}`, {
          method: 'PUT',
          body: JSON.stringify({
            nome: draft.nome,
            email: draft.email,
            perfil: draft.perfilId,
            lojaId: session?.lojaId,
            ativo: draft.ativo,
            password: draft.password.trim() ? draft.password : null,
          }),
        })
        setRows((items) => isOperationalEmployee(updated.perfilId || updated.perfil)
          ? items.map((item) => (item.id === updated.id ? updated : item))
          : items.filter((item) => item.id !== updated.id))
        setMessage('Funcionário atualizado.')
      } else {
        const created = await apiRequest<UtilizadorResponse>('/utilizadores', {
          method: 'POST',
          body: JSON.stringify({
            username: draft.username,
            password: draft.password,
            nome: draft.nome,
            email: draft.email,
            perfil: draft.perfilId,
            lojaId: session?.lojaId,
          }),
        })
        setRows((items) => isOperationalEmployee(created.perfilId || created.perfil) ? [created, ...items] : items)
        setMessage('Funcionário criado.')
      }
      resetDraft()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Não foi possível guardar funcionário.')
    }
  }

  function editEmployee(employee: UtilizadorResponse) {
    setEditingId(employee.id)
    setDraft({
      username: employee.username,
      password: '',
      nome: employee.nome,
      email: employee.email,
      perfilId: employee.perfilId || profiles.find((profile) => profile.nome === employee.perfil)?.nome || profiles[0]?.nome || '',
      ativo: employee.ativo,
    })
  }

  return (
    <div className="mf-stack">
      <div className="mf-toolbar-space"><p className="mf-section-note">{rows.length} funcionários registados</p><Button onClick={resetDraft}>Novo funcionário</Button></div>
      <Panel title={editingId ? 'Editar funcionário' : 'Novo funcionário'}>
        <div className="mf-fields-grid three">
          <TextField label="Username" value={draft.username} disabled={Boolean(editingId)} onChange={(event) => setDraft((state) => ({ ...state, username: event.target.value }))} />
          <TextField label="Nome" value={draft.nome} onChange={(event) => setDraft((state) => ({ ...state, nome: event.target.value }))} />
          <TextField label="Email" value={draft.email} onChange={(event) => setDraft((state) => ({ ...state, email: event.target.value }))} />
          <SelectField label="Perfil" value={draft.perfilId} options={profiles.map((profile) => ({ value: profile.nome, label: profile.nome }))} onChange={(event) => setDraft((state) => ({ ...state, perfilId: event.target.value }))} />
          <SelectField label="Estado" value={draft.ativo ? 'ativo' : 'inativo'} options={[{ value: 'ativo', label: 'Ativo' }, { value: 'inativo', label: 'Inativo' }]} onChange={(event) => setDraft((state) => ({ ...state, ativo: event.target.value === 'ativo' }))} />
          <TextField label={editingId ? 'Nova password' : 'Password inicial'} value={draft.password} onChange={(event) => setDraft((state) => ({ ...state, password: event.target.value }))} />
        </div>
        {error ? <Callout tone="warning" className="mt-compact">{error}</Callout> : null}
        {message ? <Callout tone="info" className="mt-compact">{message}</Callout> : null}
        <div className="mf-actions-row">
          <Button onClick={submitEmployee} disabled={!draft.nome || !draft.email || !draft.perfilId || (!editingId && (!draft.username || !draft.password))}>{editingId ? 'Guardar alterações' : 'Criar funcionário'}</Button>
          {editingId ? <Button variant="secondary" onClick={resetDraft}>Cancelar</Button> : null}
        </div>
      </Panel>
      <div className="employee-stack">
        {rows.length === 0 ? <Panel><p className="mf-empty-state">Sem funcionários para apresentar.</p></Panel> : null}
        {rows.map((row) => (
          <Panel key={row.id}>
            <div className="employee-row">
              <div className="employee-left">
                <InitialAvatar initials={row.nome.slice(0, 2).toUpperCase()} />
                <div className="employee-copy"><strong>{row.nome}</strong><span className="muted">{row.email}</span></div>
              </div>
              <div className="employee-actions"><StatusBadge tone={row.ativo ? 'success' : 'danger'}>{row.ativo ? 'Ativo' : 'Inativo'}</StatusBadge><Button variant="secondary" className="small" onClick={() => editEmployee(row)}>Editar</Button></div>
            </div>
          </Panel>
        ))}
      </div>
    </div>
  )
}
