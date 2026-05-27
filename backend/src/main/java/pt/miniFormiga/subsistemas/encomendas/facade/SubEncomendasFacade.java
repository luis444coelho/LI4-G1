package pt.miniFormiga.subsistemas.encomendas.facade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.subsistemas.auditoria.facade.ISubAuditoria;
import pt.miniFormiga.domain.CondicaoComercial;
import pt.miniFormiga.domain.Encomenda;
import pt.miniFormiga.domain.EntradaMercadoria;
import pt.miniFormiga.domain.EstadoEncomendaCodigo;
import pt.miniFormiga.domain.Fornecedor;
import pt.miniFormiga.domain.GuiaRemessa;
import pt.miniFormiga.domain.LinhaEncomenda;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.subsistemas.encomendas.repository.CondicaoComercialRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.EncomendaRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.EntradaMercadoriaRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.FornecedorRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.GuiaRemessaRepository;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;
import pt.miniFormiga.subsistemas.catalogo.repository.ProdutoRepository;
import pt.miniFormiga.subsistemas.utilizadores.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.stock.facade.ISubStock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.*;

@Service
@Transactional
public class SubEncomendasFacade implements ISubEncomendas {

    private static final String ESTADO_PENDENTE = "PENDENTE";
    private static final String ESTADO_RECEBIDA = "RECEBIDA";

    private final FornecedorRepository fornecedorRepository;
    private final CondicaoComercialRepository condicaoComercialRepository;
    private final EncomendaRepository encomendaRepository;
    private final GuiaRemessaRepository guiaRemessaRepository;
    private final EntradaMercadoriaRepository entradaMercadoriaRepository;
    private final LojaRepository lojaRepository;
    private final ProdutoRepository produtoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ISubStock stock;
    private final ISubAuditoria auditoria;

    public SubEncomendasFacade(FornecedorRepository fornecedorRepository,
                            CondicaoComercialRepository condicaoComercialRepository,
                            EncomendaRepository encomendaRepository,
                            GuiaRemessaRepository guiaRemessaRepository,
                            EntradaMercadoriaRepository entradaMercadoriaRepository,
                            LojaRepository lojaRepository,
                            ProdutoRepository produtoRepository,
                            UtilizadorRepository utilizadorRepository,
                            ISubStock stock,
                            ISubAuditoria auditoria) {
        this.fornecedorRepository = fornecedorRepository;
        this.condicaoComercialRepository = condicaoComercialRepository;
        this.encomendaRepository = encomendaRepository;
        this.guiaRemessaRepository = guiaRemessaRepository;
        this.entradaMercadoriaRepository = entradaMercadoriaRepository;
        this.lojaRepository = lojaRepository;
        this.produtoRepository = produtoRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.stock = stock;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FornecedorResponse> listarFornecedores(Pageable pageable) {
        return fornecedorRepository.findAll(pageable).map(FornecedorResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public FornecedorResponse obterFornecedor(UUID id) {
        return FornecedorResponse.from(obterFornecedorEntidade(id));
    }

    @Override
    public FornecedorResponse criarFornecedor(CriarFornecedorRequest request) {
        fornecedorRepository.findByNif(request.nif()).ifPresent(fornecedor -> {
            throw new BusinessException("FORNECEDOR_NIF_DUPLICADO", "Ja existe fornecedor com esse NIF");
        });
        Fornecedor fornecedor = new Fornecedor(
                request.nome(),
                request.nif(),
                request.morada(),
                request.telefone(),
                request.email(),
                request.horarioInicioArmazem(),
                request.horarioFimArmazem()
        );
        Fornecedor guardado = fornecedorRepository.save(fornecedor);
        auditoria.registar(TipoOperacao.FORNECEDOR_CRIADO, null, "FORNECEDOR", "Fornecedor criado");
        return FornecedorResponse.from(guardado);
    }

    @Override
    public FornecedorResponse atualizarFornecedor(UUID id, AtualizarFornecedorRequest request) {
        Fornecedor fornecedor = obterFornecedorEntidade(id);
        fornecedorRepository.findByNif(request.nif())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new BusinessException("FORNECEDOR_NIF_DUPLICADO", "Ja existe fornecedor com esse NIF");
                });
        fornecedor.atualizarDados(
                request.nome(),
                request.nif(),
                request.morada(),
                request.telefone(),
                request.email(),
                request.horarioInicioArmazem(),
                request.horarioFimArmazem()
        );
        auditoria.registar(TipoOperacao.FORNECEDOR_ATUALIZADO, null, "FORNECEDOR", "Fornecedor atualizado");
        return FornecedorResponse.from(fornecedor);
    }

    @Override
    public void desativarFornecedor(UUID id) {
        Fornecedor fornecedor = obterFornecedorEntidade(id);
        fornecedor.desativar();
        auditoria.registar(TipoOperacao.FORNECEDOR_DESATIVADO, null, "FORNECEDOR", "Fornecedor desativado");
    }

    @Override
    public CondicaoComercialResponse definirCondicaoComercial(UUID fornecedorId, CondicaoComercialRequest request) {
        Fornecedor fornecedor = obterFornecedorEntidade(fornecedorId);
        Produto produto = produtoRepository.findById(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", request.produtoId()));

        CondicaoComercial condicao = condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedorId, request.produtoId())
                .orElseGet(() -> new CondicaoComercial(
                        fornecedor,
                        produto,
                        request.precoUnitario(),
                        request.prazoEntregaDias(),
                        request.quantidadeMinima(),
                        request.dataVigencia() == null ? LocalDate.now() : request.dataVigencia()
                ));
        condicao.atualizar(
                request.precoUnitario(),
                request.prazoEntregaDias(),
                request.quantidadeMinima(),
                request.dataVigencia() == null ? LocalDate.now() : request.dataVigencia()
        );
        return CondicaoComercialResponse.from(condicaoComercialRepository.save(condicao));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CondicaoComercialResponse> listarCondicoesComerciais(UUID fornecedorId) {
        if (!fornecedorRepository.existsById(fornecedorId)) {
            throw new RecursoNaoEncontradoException("Fornecedor", fornecedorId);
        }
        return condicaoComercialRepository.findByFornecedorId(fornecedorId).stream()
                .map(CondicaoComercialResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EncomendaResponse> listarEncomendas(UUID lojaId, Pageable pageable) {
        return encomendaRepository.findByLojaId(lojaId, pageable).map(this::toEncomendaResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EncomendaResponse obterEncomenda(UUID id) {
        return toEncomendaResponse(obterEncomendaEntidade(id));
    }

    @Override
    public EncomendaResponse criarEncomenda(CriarEncomendaRequest request) {
        if (request.linhas() == null || request.linhas().isEmpty()) {
            throw new BusinessException("ENCOMENDA_SEM_LINHAS", "Encomenda deve ter pelo menos uma linha");
        }
        Loja loja = lojaRepository.findById(request.lojaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja", request.lojaId()));
        Fornecedor fornecedor = obterFornecedorEntidade(request.fornecedorId());
        if (!fornecedor.isAtivo()) {
            throw new BusinessException("FORNECEDOR_INATIVO", "Fornecedor inativo");
        }

        EstadoEncomendaCodigo pendente = estado(ESTADO_PENDENTE);
        Encomenda encomenda = new Encomenda(loja, fornecedor, pendente);
        for (CriarLinhaEncomendaRequest linhaRequest : request.linhas()) {
            Produto produto = produtoRepository.findById(linhaRequest.produtoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", linhaRequest.produtoId()));
            CondicaoComercial condicao = obterCondicaoFornecedorProduto(fornecedor.getId(), produto.getId());
            validarLinhaEncomenda(linhaRequest, condicao);
            new LinhaEncomenda(encomenda, produto, linhaRequest.quantidade(), linhaRequest.precoUnitario());
        }
        encomenda.submeter(request.dataHoraSubmissao());
        Encomenda guardada = encomendaRepository.save(encomenda);
        auditoria.registar(TipoOperacao.ENCOMENDA_CRIADA, null, "ENCOMENDA", "Encomenda criada");
        return toEncomendaResponse(guardada);
    }

    @Override
    public List<EncomendaResponse> criarEncomendaConsolidada(CriarEncomendaConsolidadaRequest request) {
        LinkedHashSet<UUID> lojasUnicas = new LinkedHashSet<>(request.lojaIds());
        if (lojasUnicas.size() < 2) {
            throw new BusinessException("ENCOMENDA_CONSOLIDADA_LOJAS_INSUFICIENTES",
                    "Encomenda consolidada deve abranger pelo menos duas lojas distintas");
        }
        return lojasUnicas.stream()
                .map(lojaId -> criarEncomenda(new CriarEncomendaRequest(
                        lojaId,
                        request.fornecedorId(),
                        request.linhas(),
                        request.dataHoraSubmissao()
                )))
                .toList();
    }

    @Override
    public EncomendaResponse atualizarEstado(UUID id, AtualizarEstadoEncomendaRequest request) {
        Encomenda encomenda = obterEncomendaEntidade(id);
        EstadoEncomendaCodigo estado = estado(request.estadoCodigo());
        encomenda.alterarEstado(estado);
        auditoria.registar(TipoOperacao.ENCOMENDA_ESTADO_ATUALIZADO, null, "ENCOMENDA", "Estado de encomenda atualizado");
        return toEncomendaResponse(encomenda);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SugestaoEncomendaResponse> sugerirEncomendas(UUID lojaId, UUID fornecedorId) {
        stock.getAlertasAtivos(lojaId);
        return stock.consultarStock(lojaId).stream()
                .filter(ISubStock.StockDTO::precisaReposicao)
                .flatMap(item -> condicoesParaSugestao(item.produtoId(), fornecedorId).stream()
                        .map(condicao -> {
                            Integer nivelMinimoConfigurado = item.nivelMinimo();
                            int nivelMinimo = nivelMinimoConfigurado == null
                                    ? 0
                                    : nivelMinimoConfigurado;
                            int quantidadeSugerida = Math.max(condicao.getQuantidadeMinima(),
                                    Math.max(1, nivelMinimo - item.quantidade() + condicao.getQuantidadeMinima()));
                            return new SugestaoEncomendaResponse(
                                    condicao.getFornecedor().getId(),
                                    condicao.getFornecedor().getNome(),
                                    condicao.getProduto().getId(),
                                    condicao.getProduto().getNome(),
                                    lojaId,
                                    item.quantidade(),
                                    nivelMinimoConfigurado,
                                    quantidadeSugerida,
                                    condicao.getPrecoUnitario()
                            );
                        }))
                .sorted(Comparator.comparing(SugestaoEncomendaResponse::fornecedor)
                        .thenComparing(SugestaoEncomendaResponse::produto))
                .toList();
    }

    @Override
    public List<EntradaMercadoriaResponse> registarEntradaMercadoria(RegistarEntradaMercadoriaRequest request) {
        Encomenda encomenda = obterEncomendaEntidade(request.encomendaId());
        Loja loja = lojaRepository.findById(request.lojaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja", request.lojaId()));
        if (!encomenda.getLoja().getId().equals(loja.getId())) {
            throw new BusinessException("ENCOMENDA_LOJA_INVALIDA", "Encomenda nao pertence a loja indicada");
        }
        Utilizador responsavel = utilizadorRepository.findById(request.responsavelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador", request.responsavelId()));

        GuiaRemessa guia = guiaRemessaRepository.findByNumero(request.guiaNumero())
                .orElseGet(() -> guiaRemessaRepository.save(new GuiaRemessa(
                        encomenda.getFornecedor(),
                        encomenda,
                        request.guiaNumero(),
                        request.dataEmissao(),
                        request.dataRecepcao()
                )));
        List<EntradaMercadoria> guardadas = new ArrayList<>();
        for (RegistarEntradaMercadoriaLinhaRequest linha : linhasEntrada(request)) {
            Produto produto = produtoRepository.findById(linha.produtoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", linha.produtoId()));
            validarProdutoEncomendado(encomenda, produto.getId());
            EntradaMercadoria entrada = new EntradaMercadoria(
                    guia,
                    loja,
                    responsavel,
                    produto,
                    linha.quantidadeRecebida(),
                    linha.quantidadeEncomendada(),
                    linha.observacoes()
            );
            stock.atualizarStock(produto.getId(), loja.getId(), linha.quantidadeRecebida());
            guardadas.add(entradaMercadoriaRepository.save(entrada));
        }
        atualizarEstadoRececao(encomenda, guardadas);
        auditoria.registar(TipoOperacao.ENTRADA_MERCADORIA_REGISTADA,
                responsavel.getId(),
                "ENTRADA_MERCADORIA",
                "Entrada de mercadoria registada");
        return guardadas.stream().map(EntradaMercadoriaResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntradaMercadoriaResponse> listarEntradasMercadoria(UUID lojaId, Pageable pageable) {
        return entradaMercadoriaRepository.findByLojaId(lojaId, pageable).map(EntradaMercadoriaResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ProximaGuiaRemessaResponse obterProximaGuiaRemessa(UUID lojaId) {
        if (!lojaRepository.existsById(lojaId)) {
            throw new RecursoNaoEncontradoException("Loja", lojaId);
        }
        int ano = LocalDate.now().getYear();
        String prefixo = "GR/" + ano + "/";
        int ultimo = guiaRemessaRepository.findNumerosPorLojaEPrefixo(lojaId, prefixo + "%").stream()
                .map(numero -> numero.substring(prefixo.length()))
                .filter(sufixo -> sufixo.chars().allMatch(Character::isDigit))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return new ProximaGuiaRemessaResponse(prefixo + String.format("%05d", ultimo + 1));
    }

    private EncomendaResponse toEncomendaResponse(Encomenda encomenda) {
        return EncomendaResponse.from(encomenda, numeroDocumentoEncomenda(encomenda));
    }

    private String numeroDocumentoEncomenda(Encomenda encomenda) {
        LocalDateTime data = encomenda.getDataSubmissao();
        int ano = data == null ? LocalDate.now().getYear() : data.getYear();
        LocalDateTime inicio = LocalDate.of(ano, 1, 1).atStartOfDay();
        LocalDateTime fim = LocalDate.of(ano + 1, 1, 1).atStartOfDay();
        List<Encomenda> encomendasDoAno = Optional.ofNullable(encomendaRepository
                        .findByLojaIdAndDataSubmissaoBetweenOrderByDataSubmissaoAsc(encomenda.getLoja().getId(), inicio, fim))
                .orElse(List.of());
        int indice = -1;
        for (int i = 0; i < encomendasDoAno.size(); i++) {
            if (encomendasDoAno.get(i).getId().equals(encomenda.getId())) {
                indice = i;
                break;
            }
        }
        int sequencia = indice >= 0 ? indice + 1 : encomendasDoAno.size() + 1;
        return "ENC/" + ano + "/" + String.format("%05d", sequencia);
    }

    private Fornecedor obterFornecedorEntidade(UUID id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", id));
    }

    private Encomenda obterEncomendaEntidade(UUID id) {
        return encomendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encomenda", id));
    }

    private EstadoEncomendaCodigo estado(String codigo) {
        try {
            return EstadoEncomendaCodigo.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("ESTADO_ENCOMENDA_INVALIDO", "Estado de encomenda invalido");
        }
    }

    private CondicaoComercial obterCondicaoFornecedorProduto(UUID fornecedorId, UUID produtoId) {
        return condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedorId, produtoId)
                .orElseThrow(() -> new BusinessException("PRODUTO_NAO_FORNECIDO",
                        "Produto nao tem condicao comercial definida para o fornecedor"));
    }

    private void validarLinhaEncomenda(CriarLinhaEncomendaRequest linhaRequest, CondicaoComercial condicao) {
        if (linhaRequest.quantidade() <= 0) {
            throw new BusinessException("QUANTIDADE_ENCOMENDA_INVALIDA", "Quantidade encomendada deve ser positiva");
        }
        if (linhaRequest.precoUnitario() == null || linhaRequest.precoUnitario().signum() < 0) {
            throw new BusinessException("PRECO_ENCOMENDA_INVALIDO", "Preco unitario nao pode ser negativo");
        }
        if (linhaRequest.quantidade() < condicao.getQuantidadeMinima()) {
            throw new BusinessException("QUANTIDADE_ABAIXO_MINIMO", "Quantidade inferior ao minimo comercial do fornecedor");
        }
    }

    private List<CondicaoComercial> condicoesParaSugestao(UUID produtoId, UUID fornecedorId) {
        if (fornecedorId != null) {
            return condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedorId, produtoId)
                    .stream()
                    .filter(condicao -> condicao.getFornecedor().isAtivo())
                    .toList();
        }
        return condicaoComercialRepository.findAll().stream()
                .filter(condicao -> condicao.getProduto().getId().equals(produtoId))
                .filter(condicao -> condicao.getFornecedor().isAtivo())
                .toList();
    }

    private List<RegistarEntradaMercadoriaLinhaRequest> linhasEntrada(RegistarEntradaMercadoriaRequest request) {
        if (request.linhas() == null || request.linhas().isEmpty()) {
            throw new BusinessException("ENTRADA_SEM_LINHAS", "Entrada de mercadoria deve indicar pelo menos uma linha");
        }
        return request.linhas();
    }

    private void validarProdutoEncomendado(Encomenda encomenda, UUID produtoId) {
        boolean existe = encomenda.getLinhas().stream()
                .anyMatch(linha -> linha.getProduto().getId().equals(produtoId));
        if (!existe) {
            throw new BusinessException("PRODUTO_FORA_DA_ENCOMENDA", "Produto nao pertence a encomenda indicada");
        }
    }

    private void atualizarEstadoRececao(Encomenda encomenda, List<EntradaMercadoria> novasEntradas) {
        List<EntradaMercadoria> todas = new ArrayList<>(entradaMercadoriaRepository.findByGuiaRemessaEncomendaId(encomenda.getId()));
        novasEntradas.forEach(entrada -> {
            if (!todas.contains(entrada)) {
                todas.add(entrada);
            }
        });
        Map<UUID, Integer> recebidoPorProduto = todas.stream()
                .filter(entrada -> entrada.getProduto() != null)
                .collect(Collectors.groupingBy(
                        entrada -> entrada.getProduto().getId(),
                        Collectors.summingInt(EntradaMercadoria::getQuantidadeRecebida)
                ));
        boolean todasLinhasRecebidas = encomenda.getLinhas().stream()
                .allMatch(linha -> recebidoPorProduto.getOrDefault(linha.getProduto().getId(), 0) >= linha.getQuantidade());
        if (todasLinhasRecebidas) {
            encomenda.alterarEstado(estado(ESTADO_RECEBIDA));
        }
    }
}
