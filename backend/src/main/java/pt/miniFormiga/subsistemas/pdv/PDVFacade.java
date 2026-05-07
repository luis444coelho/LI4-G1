package pt.miniFormiga.subsistemas.pdv;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.*;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.FaturaNaoEmitidaException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.VendaNaoEncontradaException;
import pt.miniFormiga.repository.*;
import pt.miniFormiga.subsistemas.sincronizacao.ISubSincronizacao;
import pt.miniFormiga.subsistemas.stock.ISubStock;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.pdv.PdvDtos.*;

@Service
@Transactional
public class PDVFacade implements ISubPDV {

    private static final LocalTime ABERTURA_LOJA = LocalTime.of(7, 30);
    private static final LocalTime FECHO_LOJA = LocalTime.of(20, 0);

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TaxaIVARepository taxaIVARepository;
    private final FornecedorRepository fornecedorRepository;
    private final LojaRepository lojaRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final VendaRepository vendaRepository;
    private final FaturaRepository faturaRepository;
    private final FaturaSequenciaRepository faturaSequenciaRepository;
    private final FechoCaixaRepository fechoCaixaRepository;
    private final MeioPagamentoRepository meioPagamentoRepository;
    private final ISubStock stock;
    private final ISubSincronizacao sincronizacao;
    private final AuditoriaService auditoria;

    public PDVFacade(ProdutoRepository produtoRepository,
                     CategoriaRepository categoriaRepository,
                     TaxaIVARepository taxaIVARepository,
                     FornecedorRepository fornecedorRepository,
                     LojaRepository lojaRepository,
                     UtilizadorRepository utilizadorRepository,
                     VendaRepository vendaRepository,
                     FaturaRepository faturaRepository,
                     FaturaSequenciaRepository faturaSequenciaRepository,
                     FechoCaixaRepository fechoCaixaRepository,
                     MeioPagamentoRepository meioPagamentoRepository,
                     ISubStock stock,
                     ISubSincronizacao sincronizacao,
                     AuditoriaService auditoria) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.taxaIVARepository = taxaIVARepository;
        this.fornecedorRepository = fornecedorRepository;
        this.lojaRepository = lojaRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.vendaRepository = vendaRepository;
        this.faturaRepository = faturaRepository;
        this.faturaSequenciaRepository = faturaSequenciaRepository;
        this.fechoCaixaRepository = fechoCaixaRepository;
        this.meioPagamentoRepository = meioPagamentoRepository;
        this.stock = stock;
        this.sincronizacao = sincronizacao;
        this.auditoria = auditoria;
    }

    @Override
    public ProdutoDTO criarProduto(CriarProdutoRequest request) {
        TaxaIVA taxaIVA = obterTaxaIvaObrigatoria(request.taxaIvaId());
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", request.categoriaId()));
        Fornecedor fornecedor = request.fornecedorPrincipalId() == null ? null : fornecedorRepository.findById(request.fornecedorPrincipalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", request.fornecedorPrincipalId()));
        Produto produto = new Produto(request.codigoBarras(), request.nome(), request.precoVenda(), request.precoCusto(), taxaIVA, categoria);
        produto.atualizar(null, request.descricao(), null, null, null, null, fornecedor, null);
        return ProdutoDTO.from(produtoRepository.save(produto));
    }

    @Override
    public ProdutoDTO atualizarProduto(UUID id, AtualizarProdutoRequest request) {
        Produto produto = produtoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
        Categoria categoria = request.categoriaId() == null ? null : categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", request.categoriaId()));
        TaxaIVA taxaIVA = request.taxaIvaId() == null ? produto.getTaxaIVA() : obterTaxaIvaObrigatoria(request.taxaIvaId());
        Fornecedor fornecedor = request.fornecedorPrincipalId() == null ? null : fornecedorRepository.findById(request.fornecedorPrincipalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", request.fornecedorPrincipalId()));
        produto.atualizar(request.nome(), request.descricao(), request.precoVenda(), request.precoCusto(), categoria, taxaIVA, fornecedor, request.ativo());
        return ProdutoDTO.from(produto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProdutoDTO obterProdutoPorId(UUID id) {
        return ProdutoDTO.from(produtoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public ProdutoDTO obterProdutoPorCodigoBarras(String codigoBarras) {
        return ProdutoDTO.from(produtoRepository.findByCodigoBarras(codigoBarras)
                .or(() -> produtoRepository.findByCodigo(codigoBarras))
                .orElseThrow(() -> new BusinessException("PRODUTO_NAO_ENCONTRADO", "Produto nao encontrado")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProdutoDTO> listarProdutos(UUID lojaId, Pageable pageable) {
        return produtoRepository.findAll(pageable).map(ProdutoDTO::from);
    }

    @Override
    public Venda registarVenda(UUID lojaId, UUID utilizadorId) {
        Loja loja = lojaRepository.findById(lojaId).orElseThrow(() -> new RecursoNaoEncontradoException("Loja", lojaId));
        Utilizador operador = utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador", utilizadorId));
        Venda venda = vendaRepository.save(new Venda(loja, operador));
        registarVendaForaHorarioSeNecessario(venda);
        return venda;
    }

    @Override
    public LinhaVenda adicionarLinhaVenda(UUID vendaId, UUID produtoId, Integer quantidade) {
        Venda venda = obterVendaEntidade(vendaId);
        if (venda.isAnulada() || venda.getMeioPagamento() != null) {
            throw new BusinessException("VENDA_NAO_ABERTA", "A venda deve estar aberta");
        }
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
        if (!produto.isAtivo()) {
            throw new BusinessException("PRODUTO_INATIVO", "Produto inativo");
        }
        stock.consultarStock(venda.getLoja().getId()).stream()
                .filter(item -> item.produtoId().equals(produto.getId()))
                .findFirst()
                .filter(item -> item.quantidade() >= quantidade)
                .orElseThrow(() -> new pt.miniFormiga.exception.StockInsuficienteException(produto.getId(), 0, quantidade));
        LinhaVenda linhaVenda = new LinhaVenda(venda, produto, quantidade, produto.getPrecoVenda());
        venda.calcularTotais();
        return linhaVenda;
    }

    @Override
    public void anularLinhaVenda(UUID vendaId, UUID linhaId) {
        Venda venda = obterVendaEntidade(vendaId);
        venda.anularLinha(linhaId);
    }

    @Override
    public Venda finalizarVenda(UUID vendaId, UUID meioPagamentoId) {
        Venda venda = obterVendaEntidade(vendaId);
        MeioPagamento meioPagamento = meioPagamentoRepository.findById(meioPagamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("MeioPagamento", meioPagamentoId));
        for (LinhaVenda linha : venda.getLinhas()) {
            if (!linha.isAnulada()) {
                stock.atualizarStock(linha.getProduto().getId(), venda.getLoja().getId(), -linha.getQuantidade());
            }
        }
        venda.finalizar(meioPagamento);
        auditoria.registar(TipoOperacao.VENDA_FINALIZADA, venda.getUtilizador().getId(), "VENDA", "Venda finalizada");
        registarVendaForaHorarioSeNecessario(venda);
        return venda;
    }

    @Override
    public void anularVenda(UUID vendaId) {
        Venda venda = obterVendaEntidade(vendaId);
        venda.anular();
        auditoria.registar(TipoOperacao.VENDA_ANULADA, venda.getUtilizador().getId(), "VENDA", "Venda anulada");
    }

    @Override
    @Transactional(readOnly = true)
    public VendaDTO obterVenda(UUID vendaId) {
        return VendaDTO.from(obterVendaEntidade(vendaId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendaDTO> listarVendas(UUID lojaId, LocalDate inicio, LocalDate fim, Pageable pageable) {
        LocalDate dataInicio = inicio == null ? LocalDate.now() : inicio;
        LocalDate dataFim = fim == null ? dataInicio : fim;
        return vendaRepository.findByLojaIdAndDataHoraBetween(lojaId, dataInicio.atStartOfDay(), dataFim.plusDays(1).atStartOfDay(), pageable)
                .map(VendaDTO::from);
    }

    @Override
    public Fatura emitirFatura(UUID vendaId, String nifCliente, String nomeCliente) {
        Venda venda = obterVendaEntidade(vendaId);
        if (venda.isAnulada() || venda.getMeioPagamento() == null) {
            throw new FaturaNaoEmitidaException("Venda deve estar finalizada");
        }
        if (faturaRepository.existsByVendaId(vendaId)) {
            throw new FaturaNaoEmitidaException("Venda ja tem fatura emitida");
        }
        venda.calcularTotais();
        BigDecimal total = venda.getTotalComIVA();
        String tipo = Fatura.decidirTipo(total, nifCliente);
        if ("COMPLETA".equals(tipo) && (nifCliente == null || nifCliente.isBlank())) {
            throw new BusinessException("NIF_OBRIGATORIO", "NIF obrigatorio para fatura completa");
        }
        String serie = "A/" + LocalDate.now().getYear();
        int numero = proximoNumeroFatura(serie);
        Fatura fatura = new Fatura(venda, String.format("%05d", numero), serie, tipo, nifCliente, nomeCliente);
        fatura.emitir();
        Fatura guardada = faturaRepository.save(fatura);
        auditoria.registar(TipoOperacao.FATURA_EMITIDA, venda.getUtilizador().getId(), "FATURA", "Fatura emitida");
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public FaturaDTO obterFatura(UUID faturaId) {
        return FaturaDTO.from(faturaRepository.findById(faturaId).orElseThrow(() -> new RecursoNaoEncontradoException("Fatura", faturaId)));
    }

    public VendaDTO processarDevolucao(UUID vendaId, ProcessarDevolucaoRequest request) {
        Venda original = obterVendaEntidade(vendaId);
        if (original.isAnulada() || original.getMeioPagamento() == null) {
            throw new BusinessException("VENDA_NAO_FINALIZADA", "A venda original deve estar finalizada");
        }
        LinhaVenda linhaOriginal = original.getLinhas().stream()
                .filter(linha -> linha.getProduto().getId().equals(request.produtoId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PRODUTO_NAO_EXISTE_NA_VENDA", "Produto inexistente na venda original"));
        if (linhaOriginal.getQuantidade() < request.quantidade()) {
            throw new BusinessException("QUANTIDADE_DEVOLUCAO_INVALIDA", "Quantidade de devolucao superior a venda original");
        }
        stock.atualizarStock(request.produtoId(), original.getLoja().getId(), request.quantidade());
        auditoria.registar(TipoOperacao.DEVOLUCAO_REGISTADA, original.getUtilizador().getId(), "VENDA", "Devolucao registada");
        return VendaDTO.from(original);
    }

    @Override
    public FechoCaixa registarFechoCaixa(UUID lojaId, UUID utilizadorId) {
        Loja loja = lojaRepository.findById(lojaId).orElseThrow(() -> new RecursoNaoEncontradoException("Loja", lojaId));
        Utilizador gerente = utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador", utilizadorId));
        LocalDate data = LocalDate.now();
        var vendas = vendaRepository.findByLojaIdAndAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(lojaId, data.atStartOfDay(), data.plusDays(1).atStartOfDay());
        FechoCaixa fecho = new FechoCaixa(loja, gerente, data, vendas);
        fecho.calcularTotais();
        FechoCaixa guardado = fechoCaixaRepository.save(fecho);
        auditoria.registar(TipoOperacao.FECHO_CAIXA_INICIADO, utilizadorId, "FECHO_CAIXA", "Fecho de caixa iniciado");
        return guardado;
    }

    @Override
    public FechoCaixa confirmarFechoCaixa(UUID fechoCaixaId, String observacoesDiscrepancia) {
        FechoCaixa fecho = fechoCaixaRepository.findById(fechoCaixaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("FechoCaixa", fechoCaixaId));
        fecho.setObservacaoDiscrepancia(observacoesDiscrepancia);
        fecho.confirmar();
        auditoria.registar(TipoOperacao.FECHO_CAIXA_CONFIRMADO,
                fecho.getResponsavel() == null ? null : fecho.getResponsavel().getId(),
                "FECHO_CAIXA",
                "Fecho de caixa confirmado");
        fecho.getLoja().iniciarSincronizacao();
        sincronizacao.agendarSincronizacao(fecho.getLoja().getId());
        return fecho;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venda> getVendasPorLoja(UUID lojaId, LocalDate inicio, LocalDate fim) {
        LocalDate dataInicio = inicio == null ? LocalDate.now() : inicio;
        LocalDate dataFim = fim == null ? dataInicio : fim;
        return vendaRepository.findByLojaIdAndDataHoraBetween(lojaId, dataInicio.atStartOfDay(), dataFim.plusDays(1).atStartOfDay(), Pageable.unpaged()).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FechoCaixa> getFechoCaixaByLoja(UUID lojaId) {
        return fechoCaixaRepository.findByLojaId(lojaId, Pageable.unpaged()).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FechoCaixaDTO> listarFechosCaixa(UUID lojaId, Pageable pageable) {
        return fechoCaixaRepository.findByLojaId(lojaId, pageable).map(FechoCaixaDTO::from);
    }

    @Override
    @Transactional(readOnly = true)
    public FechoCaixaDTO obterFechoCaixa(UUID fechoCaixaId) {
        return FechoCaixaDTO.from(fechoCaixaRepository.findById(fechoCaixaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("FechoCaixa", fechoCaixaId)));
    }

    private TaxaIVA obterTaxaIvaObrigatoria(UUID taxaIvaId) {
        if (taxaIvaId == null) {
            throw new BusinessException("TAXA_IVA_OBRIGATORIA", "Taxa IVA obrigatoria");
        }
        return taxaIVARepository.findById(taxaIvaId).orElseThrow(() -> new RecursoNaoEncontradoException("TaxaIVA", taxaIvaId));
    }

    private Venda obterVendaEntidade(UUID vendaId) {
        return vendaRepository.findById(vendaId).orElseThrow(() -> new VendaNaoEncontradaException(vendaId));
    }

    private int proximoNumeroFatura(String serie) {
        FaturaSequencia sequencia = faturaSequenciaRepository.findBySerie(serie)
                .orElseGet(() -> faturaSequenciaRepository.saveAndFlush(new FaturaSequencia(serie)));
        return sequencia.proximoNumero();
    }

    private void registarVendaForaHorarioSeNecessario(Venda venda) {
        if (!dentroHorarioLoja(venda.getDataHora())) {
            auditoria.registar(TipoOperacao.VENDA_FORA_HORARIO, venda.getUtilizador().getId(), "VENDA", "Venda fora do horario normal");
        }
    }

    private boolean dentroHorarioLoja(LocalDateTime momento) {
        DayOfWeek dia = momento.getDayOfWeek();
        LocalTime hora = momento.toLocalTime();
        return dia != DayOfWeek.SUNDAY && !hora.isBefore(ABERTURA_LOJA) && !hora.isAfter(FECHO_LOJA);
    }
}
