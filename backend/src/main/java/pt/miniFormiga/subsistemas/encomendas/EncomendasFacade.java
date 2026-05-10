package pt.miniFormiga.subsistemas.encomendas;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.CondicaoComercial;
import pt.miniFormiga.domain.Encomenda;
import pt.miniFormiga.domain.EntradaMercadoria;
import pt.miniFormiga.domain.EstadoEncomenda;
import pt.miniFormiga.domain.Fornecedor;
import pt.miniFormiga.domain.GuiaRemessa;
import pt.miniFormiga.domain.LinhaEncomenda;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.repository.CondicaoComercialRepository;
import pt.miniFormiga.repository.EncomendaRepository;
import pt.miniFormiga.repository.EntradaMercadoriaRepository;
import pt.miniFormiga.repository.EstadoEncomendaRepository;
import pt.miniFormiga.repository.FornecedorRepository;
import pt.miniFormiga.repository.GuiaRemessaRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.ProdutoRepository;
import pt.miniFormiga.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.stock.ISubStock;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.encomendas.EncomendasDtos.*;

@Service
@Transactional
public class EncomendasFacade implements ISubEncomendas {

    private static final String ESTADO_PENDENTE = "PENDENTE";
    private static final String ESTADO_RECEBIDA = "RECEBIDA";

    private final FornecedorRepository fornecedorRepository;
    private final CondicaoComercialRepository condicaoComercialRepository;
    private final EncomendaRepository encomendaRepository;
    private final EstadoEncomendaRepository estadoEncomendaRepository;
    private final GuiaRemessaRepository guiaRemessaRepository;
    private final EntradaMercadoriaRepository entradaMercadoriaRepository;
    private final LojaRepository lojaRepository;
    private final ProdutoRepository produtoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ISubStock stock;
    private final AuditoriaService auditoria;

    public EncomendasFacade(FornecedorRepository fornecedorRepository,
                            CondicaoComercialRepository condicaoComercialRepository,
                            EncomendaRepository encomendaRepository,
                            EstadoEncomendaRepository estadoEncomendaRepository,
                            GuiaRemessaRepository guiaRemessaRepository,
                            EntradaMercadoriaRepository entradaMercadoriaRepository,
                            LojaRepository lojaRepository,
                            ProdutoRepository produtoRepository,
                            UtilizadorRepository utilizadorRepository,
                            ISubStock stock,
                            AuditoriaService auditoria) {
        this.fornecedorRepository = fornecedorRepository;
        this.condicaoComercialRepository = condicaoComercialRepository;
        this.encomendaRepository = encomendaRepository;
        this.estadoEncomendaRepository = estadoEncomendaRepository;
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
        return encomendaRepository.findByLojaId(lojaId, pageable).map(EncomendaResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public EncomendaResponse obterEncomenda(UUID id) {
        return EncomendaResponse.from(obterEncomendaEntidade(id));
    }

    @Override
    public EncomendaResponse criarEncomenda(CriarEncomendaRequest request) {
        Loja loja = lojaRepository.findById(request.lojaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja", request.lojaId()));
        Fornecedor fornecedor = obterFornecedorEntidade(request.fornecedorId());
        if (!fornecedor.isAtivo()) {
            throw new BusinessException("FORNECEDOR_INATIVO", "Fornecedor inativo");
        }

        EstadoEncomenda pendente = obterOuCriarEstado(ESTADO_PENDENTE, "Pendente");
        Encomenda encomenda = new Encomenda(loja, fornecedor, pendente);
        for (CriarLinhaEncomendaRequest linhaRequest : request.linhas()) {
            Produto produto = produtoRepository.findById(linhaRequest.produtoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", linhaRequest.produtoId()));
            new LinhaEncomenda(encomenda, produto, linhaRequest.quantidade(), linhaRequest.precoUnitario());
        }
        encomenda.submeter();
        Encomenda guardada = encomendaRepository.save(encomenda);
        auditoria.registar(TipoOperacao.ENCOMENDA_CRIADA, null, "ENCOMENDA", "Encomenda criada");
        return EncomendaResponse.from(guardada);
    }

    @Override
    public EncomendaResponse atualizarEstado(UUID id, AtualizarEstadoEncomendaRequest request) {
        Encomenda encomenda = obterEncomendaEntidade(id);
        EstadoEncomenda estado = estadoEncomendaRepository.findByCodigo(request.estadoCodigo())
                .orElseThrow(() -> new BusinessException("ESTADO_ENCOMENDA_INVALIDO", "Estado de encomenda invalido"));
        encomenda.alterarEstado(estado);
        auditoria.registar(TipoOperacao.ENCOMENDA_ESTADO_ATUALIZADO, null, "ENCOMENDA", "Estado de encomenda atualizado");
        return EncomendaResponse.from(encomenda);
    }

    @Override
    public EntradaMercadoriaResponse registarEntradaMercadoria(RegistarEntradaMercadoriaRequest request) {
        Encomenda encomenda = obterEncomendaEntidade(request.encomendaId());
        Loja loja = lojaRepository.findById(request.lojaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja", request.lojaId()));
        if (!encomenda.getLoja().getId().equals(loja.getId())) {
            throw new BusinessException("ENCOMENDA_LOJA_INVALIDA", "Encomenda nao pertence a loja indicada");
        }
        Utilizador responsavel = utilizadorRepository.findById(request.responsavelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador", request.responsavelId()));
        Produto produto = produtoRepository.findById(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", request.produtoId()));

        GuiaRemessa guia = guiaRemessaRepository.findByNumero(request.guiaNumero())
                .orElseGet(() -> guiaRemessaRepository.save(new GuiaRemessa(
                        encomenda.getFornecedor(),
                        encomenda,
                        request.guiaNumero(),
                        request.dataEmissao(),
                        request.dataRecepcao()
                )));
        EntradaMercadoria entrada = new EntradaMercadoria(
                guia,
                loja,
                responsavel,
                request.quantidadeRecebida(),
                request.quantidadeEncomendada(),
                request.observacoes()
        );
        stock.atualizarStock(produto.getId(), loja.getId(), request.quantidadeRecebida());
        encomenda.alterarEstado(obterOuCriarEstado(ESTADO_RECEBIDA, "Recebida"));
        EntradaMercadoria guardada = entradaMercadoriaRepository.save(entrada);
        auditoria.registar(TipoOperacao.ENTRADA_MERCADORIA_REGISTADA,
                responsavel.getId(),
                "ENTRADA_MERCADORIA",
                "Entrada de mercadoria registada");
        return EntradaMercadoriaResponse.from(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntradaMercadoriaResponse> listarEntradasMercadoria(UUID lojaId, Pageable pageable) {
        return entradaMercadoriaRepository.findByLojaId(lojaId, pageable).map(EntradaMercadoriaResponse::from);
    }

    private Fornecedor obterFornecedorEntidade(UUID id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", id));
    }

    private Encomenda obterEncomendaEntidade(UUID id) {
        return encomendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encomenda", id));
    }

    private EstadoEncomenda obterOuCriarEstado(String codigo, String descricao) {
        return estadoEncomendaRepository.findByCodigo(codigo)
                .orElseGet(() -> estadoEncomendaRepository.save(new EstadoEncomenda(codigo, descricao)));
    }
}
