package pt.miniFormiga.subsistemas.encomendas.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import pt.miniFormiga.subsistemas.auditoria.ISubAuditoria;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.CondicaoComercial;
import pt.miniFormiga.domain.Encomenda;
import pt.miniFormiga.domain.EntradaMercadoria;
import pt.miniFormiga.domain.EstadoEncomendaCodigo;
import pt.miniFormiga.domain.Fornecedor;
import pt.miniFormiga.domain.GuiaRemessa;
import pt.miniFormiga.domain.LinhaEncomenda;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.subsistemas.encomendas.repository.CondicaoComercialRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.EncomendaRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.EntradaMercadoriaRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.FornecedorRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.GuiaRemessaRepository;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;
import pt.miniFormiga.subsistemas.catalogo.repository.ProdutoRepository;
import pt.miniFormiga.subsistemas.utilizadores.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.stock.facade.ISubStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.*;

@ExtendWith(MockitoExtension.class)
class SubEncomendasFacadeTest {

    @Mock FornecedorRepository fornecedorRepository;
    @Mock CondicaoComercialRepository condicaoComercialRepository;
    @Mock EncomendaRepository encomendaRepository;
    @Mock GuiaRemessaRepository guiaRemessaRepository;
    @Mock EntradaMercadoriaRepository entradaMercadoriaRepository;
    @Mock LojaRepository lojaRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock UtilizadorRepository utilizadorRepository;
    @Mock ISubStock stock;
    @Mock ISubAuditoria auditoria;

    SubEncomendasFacade facade;
    Loja loja;
    Fornecedor fornecedor;
    Produto produto;
    Utilizador responsavel;

    @BeforeEach
    void setUp() {
        facade = new SubEncomendasFacade(
                fornecedorRepository,
                condicaoComercialRepository,
                encomendaRepository,
                guiaRemessaRepository,
                entradaMercadoriaRepository,
                lojaRepository,
                produtoRepository,
                utilizadorRepository,
                stock,
                auditoria
        );
        loja = new Loja("Loja Braga", "Rua Central", "123456789");
        fornecedor = new Fornecedor("Fornecedor Norte", "987654321", "Rua Norte", "229000000",
                "norte@mini-formiga.pt", LocalTime.of(8, 0), LocalTime.of(18, 0));
        produto = new Produto("5600000000110", "Agua", new BigDecimal("1.00"), new BigDecimal("0.40"),
                new TaxaIVA("Normal", new BigDecimal("23")), new Categoria("Bebidas", "Bebidas"));
        responsavel = new Utilizador("armazem", "hash", "Armazem",
                PerfilUtilizador.ARMAZEM, loja);
    }

    @Test
    void criarFornecedor() {
        when(fornecedorRepository.findByNif("987654321")).thenReturn(Optional.empty());
        when(fornecedorRepository.save(any(Fornecedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FornecedorResponse response = facade.criarFornecedor(new CriarFornecedorRequest(
                "Fornecedor Norte",
                "987654321",
                "Rua Norte",
                "229000000",
                "norte@mini-formiga.pt",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        ));

        assertEquals("Fornecedor Norte", response.nome());
        assertEquals("987654321", response.nif());
        verify(auditoria).registar(TipoOperacao.FORNECEDOR_CRIADO, null, "FORNECEDOR", "Fornecedor criado");
    }

    @Test
    void criarFornecedorComNifDuplicadoFalhaEAtualizarDesativarConsultamEntidadeExistente() {
        when(fornecedorRepository.findByNif("987654321")).thenReturn(Optional.of(fornecedor));
        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.criarFornecedor(new CriarFornecedorRequest(
                "Fornecedor Norte",
                "987654321",
                "Rua Norte",
                "229000000",
                "norte@mini-formiga.pt",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        )));

        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(fornecedorRepository.findByNif("111222333")).thenReturn(Optional.empty());

        FornecedorResponse atualizado = facade.atualizarFornecedor(fornecedor.getId(), new AtualizarFornecedorRequest(
                "Fornecedor Atualizado",
                "111222333",
                "Rua Atualizada",
                "229111111",
                "novo@mini-formiga.pt",
                LocalTime.of(7, 30),
                LocalTime.of(17, 30)
        ));
        facade.desativarFornecedor(fornecedor.getId());

        assertEquals("Fornecedor Atualizado", atualizado.nome());
        assertEquals("111222333", atualizado.nif());
        assertTrue(!fornecedor.isAtivo());
        verify(auditoria).registar(TipoOperacao.FORNECEDOR_ATUALIZADO, null, "FORNECEDOR", "Fornecedor atualizado");
        verify(auditoria).registar(TipoOperacao.FORNECEDOR_DESATIVADO, null, "FORNECEDOR", "Fornecedor desativado");
    }

    @Test
    void atualizarFornecedorComNifDeOutroFornecedorFalha() {
        Fornecedor outro = new Fornecedor("Outro", "111222333", "Rua", "229111111",
                "outro@mini-formiga.pt", LocalTime.of(8, 0), LocalTime.of(18, 0));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(fornecedorRepository.findByNif("111222333")).thenReturn(Optional.of(outro));

        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.atualizarFornecedor(fornecedor.getId(), new AtualizarFornecedorRequest(
                "Fornecedor Atualizado",
                "111222333",
                "Rua Atualizada",
                "229111111",
                "novo@mini-formiga.pt",
                LocalTime.of(7, 30),
                LocalTime.of(17, 30)
        )));
    }

    @Test
    void definirCondicaoComercial() {
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId()))
                .thenReturn(Optional.empty());
        when(condicaoComercialRepository.save(any(CondicaoComercial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CondicaoComercialResponse response = facade.definirCondicaoComercial(fornecedor.getId(), new CondicaoComercialRequest(
                produto.getId(),
                new BigDecimal("0.60"),
                2,
                10,
                LocalDate.now()
        ));

        assertEquals(fornecedor.getId(), response.fornecedorId());
        assertEquals(produto.getId(), response.produtoId());
        assertEquals(new BigDecimal("0.60"), response.precoUnitario());
        assertEquals(10, response.quantidadeMinima());
    }

    @Test
    void definirCondicaoComercialAtualizaCondicaoExistenteEListaPorFornecedor() {
        CondicaoComercial existente = condicao(produto);
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(fornecedorRepository.existsById(fornecedor.getId())).thenReturn(true);
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId()))
                .thenReturn(Optional.of(existente));
        when(condicaoComercialRepository.save(existente)).thenReturn(existente);
        when(condicaoComercialRepository.findByFornecedorId(fornecedor.getId())).thenReturn(List.of(existente));

        CondicaoComercialResponse atualizada = facade.definirCondicaoComercial(fornecedor.getId(), new CondicaoComercialRequest(
                produto.getId(),
                new BigDecimal("0.75"),
                3,
                12,
                null
        ));
        List<CondicaoComercialResponse> condicoes = facade.listarCondicoesComerciais(fornecedor.getId());

        assertEquals(new BigDecimal("0.75"), atualizada.precoUnitario());
        assertEquals(12, condicoes.get(0).quantidadeMinima());
    }

    @Test
    void listarCondicoesComFornecedorInexistenteFalha() {
        when(fornecedorRepository.existsById(fornecedor.getId())).thenReturn(false);

        assertThrows(pt.miniFormiga.exception.RecursoNaoEncontradoException.class,
                () -> facade.listarCondicoesComerciais(fornecedor.getId()));
    }

    @Test
    void obterProximaGuiaRemessaUsaSequenciaPorLojaEAno() {
        int ano = LocalDate.now().getYear();
        when(lojaRepository.existsById(loja.getId())).thenReturn(true);
        when(guiaRemessaRepository.findNumerosPorLojaEPrefixo(loja.getId(), "GR/" + ano + "/%"))
                .thenReturn(List.of("GR/" + ano + "/ABC", "GR/" + ano + "/00003", "GR/" + ano + "/00002"));

        ProximaGuiaRemessaResponse response = facade.obterProximaGuiaRemessa(loja.getId());

        assertEquals("GR/" + ano + "/00004", response.numero());
    }

    @Test
    void obterProximaGuiaRemessaComLojaInexistenteFalha() {
        when(lojaRepository.existsById(loja.getId())).thenReturn(false);

        assertThrows(pt.miniFormiga.exception.RecursoNaoEncontradoException.class,
                () -> facade.obterProximaGuiaRemessa(loja.getId()));
    }

    @Test
    void criarEncomendaDefineEstadoPendenteEDataProcessamentoDoFornecedor() {
        EstadoEncomendaCodigo pendente = EstadoEncomendaCodigo.PENDENTE;
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId()))
                .thenReturn(Optional.of(condicao(produto)));
        when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EncomendaResponse response = facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 2, new BigDecimal("0.60")))
        ));

        assertEquals("PENDENTE", response.estado());
        assertEquals(new BigDecimal("1.20"), response.totalEstimado());
        assertEquals(1, response.linhas().size());
        verify(auditoria).registar(TipoOperacao.ENCOMENDA_CRIADA, null, "ENCOMENDA", "Encomenda criada");
    }

    @Test
    void criarEncomendaConsolidadaCriaUmaEncomendaPorLojaMantendoFornecedorELinhas() {
        Loja lojaPorto = new Loja("Loja Porto", "Rua Norte", "123456780");
        LocalDateTime submissao = LocalDateTime.of(2026, 5, 20, 10, 0);
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(lojaRepository.findById(lojaPorto.getId())).thenReturn(Optional.of(lojaPorto));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId()))
                .thenReturn(Optional.of(condicao(produto)));
        when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<EncomendaResponse> response = facade.criarEncomendaConsolidada(new CriarEncomendaConsolidadaRequest(
                List.of(loja.getId(), lojaPorto.getId()),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 2, new BigDecimal("0.60"))),
                submissao
        ));

        assertEquals(2, response.size());
        assertEquals(loja.getId(), response.get(0).lojaId());
        assertEquals(lojaPorto.getId(), response.get(1).lojaId());
        assertEquals(fornecedor.getId(), response.get(0).fornecedorId());
        assertEquals(fornecedor.getId(), response.get(1).fornecedorId());
        assertEquals(new BigDecimal("1.20"), response.get(0).totalEstimado());
        assertEquals(new BigDecimal("1.20"), response.get(1).totalEstimado());
        verify(encomendaRepository, times(2)).save(any(Encomenda.class));
    }

    @Test
    void criarEncomendaConsolidadaComMenosDeDuasLojasDistintasFalha() {
        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.criarEncomendaConsolidada(
                new CriarEncomendaConsolidadaRequest(
                        List.of(loja.getId(), loja.getId()),
                        fornecedor.getId(),
                        List.of(new CriarLinhaEncomendaRequest(produto.getId(), 2, new BigDecimal("0.60"))),
                        null
                )));

        verify(encomendaRepository, never()).save(any());
    }

    @Test
    void criarEncomendaVaziaFalha() {
        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(), fornecedor.getId(), List.of()
        )));
        verify(encomendaRepository, never()).save(any());
    }

    @Test
    void criarEncomendaCalculaProcessamentoDentroDoHorario() {
        EstadoEncomendaCodigo pendente = EstadoEncomendaCodigo.PENDENTE;
        LocalDateTime submissao = LocalDateTime.of(2026, 5, 20, 10, 0);
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId()))
                .thenReturn(Optional.of(condicao(produto)));
        when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EncomendaResponse response = facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 10, new BigDecimal("0.60"))),
                submissao
        ));

        assertEquals(submissao, response.dataProcessamento());
    }

    @Test
    void criarEncomendaCalculaProximoDiaUtilForaDoHorario() {
        EstadoEncomendaCodigo pendente = EstadoEncomendaCodigo.PENDENTE;
        LocalDateTime submissao = LocalDateTime.of(2026, 5, 22, 19, 0); // sexta-feira
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId()))
                .thenReturn(Optional.of(condicao(produto)));
        when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EncomendaResponse response = facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 10, new BigDecimal("0.60"))),
                submissao
        ));

        assertEquals(LocalDateTime.of(2026, 5, 25, 8, 0), response.dataProcessamento());
    }

    @Test
    void criarEncomendaSemCondicaoComercialFalha() {
        EstadoEncomendaCodigo pendente = EstadoEncomendaCodigo.PENDENTE;
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId()))
                .thenReturn(Optional.empty());

        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 2, new BigDecimal("0.60")))
        )));
    }

    @Test
    void criarEncomendaValidaFornecedorAtivoQuantidadePrecoEMinimoComercial() {
        fornecedor.desativar();
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));

        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 2, new BigDecimal("0.60")))
        )));

        fornecedor.ativar();
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId()))
                .thenReturn(Optional.of(new CondicaoComercial(fornecedor, produto, new BigDecimal("0.60"), 2, 5, LocalDate.now())));

        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 0, new BigDecimal("0.60")))
        )));
        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 6, new BigDecimal("-0.01")))
        )));
        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 3, new BigDecimal("0.60")))
        )));
    }

    @Test
    void listarEObterEncomendasGeramNumeroDocumentoSequencial() {
        Encomenda encomenda = new Encomenda(loja, fornecedor, new EstadoEncomenda("PENDENTE", "Pendente"));
        new LinhaEncomenda(encomenda, produto, 2, new BigDecimal("0.60"));
        encomenda.submeter(LocalDateTime.of(2026, 5, 20, 10, 0));
        PageRequest pageable = PageRequest.of(0, 5);
        when(encomendaRepository.findByLojaId(loja.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(encomenda), pageable, 1));
        when(encomendaRepository.findById(encomenda.getId())).thenReturn(Optional.of(encomenda));
        when(encomendaRepository.findByLojaIdAndDataSubmissaoBetweenOrderByDataSubmissaoAsc(any(), any(), any()))
                .thenReturn(List.of(encomenda));

        assertEquals("ENC/2026/00001", facade.listarEncomendas(loja.getId(), pageable).getContent().get(0).numeroDocumento());
        assertEquals("ENC/2026/00001", facade.obterEncomenda(encomenda.getId()).numeroDocumento());
    }

    @Test
    void atualizarEstadoValidaCodigoERegistaAuditoria() {
        Encomenda encomenda = new Encomenda(loja, fornecedor, new EstadoEncomenda("PENDENTE", "Pendente"));
        when(encomendaRepository.findById(encomenda.getId())).thenReturn(Optional.of(encomenda));

        EncomendaResponse atualizada = facade.atualizarEstado(encomenda.getId(), new AtualizarEstadoEncomendaRequest("ENVIADA"));

        assertEquals("ENVIADA", atualizada.estado());
        verify(auditoria).registar(TipoOperacao.ENCOMENDA_ESTADO_ATUALIZADO, null, "ENCOMENDA", "Estado de encomenda atualizado");
        assertThrows(pt.miniFormiga.exception.BusinessException.class,
                () -> facade.atualizarEstado(encomenda.getId(), new AtualizarEstadoEncomendaRequest("INVALIDO")));
    }

    @Test
    void sugerirEncomendasUsaFornecedorFiltradoEIgnoraFornecedorInativo() {
        Fornecedor fornecedorInativo = new Fornecedor("Inativo", "111222333", "Rua", "229111111",
                "inativo@mini-formiga.pt", LocalTime.of(8, 0), LocalTime.of(18, 0));
        fornecedorInativo.desativar();
        CondicaoComercial ativa = new CondicaoComercial(fornecedor, produto, new BigDecimal("0.60"), 2, 4, LocalDate.now());
        CondicaoComercial inativa = new CondicaoComercial(fornecedorInativo, produto, new BigDecimal("0.50"), 1, 4, LocalDate.now());
        when(stock.consultarStock(loja.getId())).thenReturn(List.of(new ISubStock.StockDTO(produto.getId(), loja.getId(), 2, 6, true)));
        when(condicaoComercialRepository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId()))
                .thenReturn(Optional.of(ativa));
        when(condicaoComercialRepository.findAll()).thenReturn(List.of(ativa, inativa));

        List<SugestaoEncomendaResponse> filtrada = facade.sugerirEncomendas(loja.getId(), fornecedor.getId());
        List<SugestaoEncomendaResponse> todas = facade.sugerirEncomendas(loja.getId(), null);

        assertEquals(1, filtrada.size());
        assertEquals(8, filtrada.get(0).quantidadeSugerida());
        assertEquals(1, todas.size());
        verify(stock, times(2)).getAlertasAtivos(loja.getId());
    }

    @Test
    void registarEntradaAtualizaStockEMarcaEncomendaComoRecebidaQuandoCompleta() {
        EstadoEncomendaCodigo pendente = EstadoEncomendaCodigo.PENDENTE;
        EstadoEncomendaCodigo recebida = EstadoEncomendaCodigo.RECEBIDA;
        Encomenda encomenda = new Encomenda(loja, fornecedor, pendente);
        new LinhaEncomenda(encomenda, produto, 8, new BigDecimal("0.60"));
        when(encomendaRepository.findById(encomenda.getId())).thenReturn(Optional.of(encomenda));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(responsavel.getId())).thenReturn(Optional.of(responsavel));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(guiaRemessaRepository.findByNumero("GR-1")).thenReturn(Optional.empty());
        when(guiaRemessaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entradaMercadoriaRepository.save(any(EntradaMercadoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entradaMercadoriaRepository.findByGuiaRemessaEncomendaId(encomenda.getId())).thenReturn(List.of());

        List<EntradaMercadoriaResponse> response = facade.registarEntradaMercadoria(new RegistarEntradaMercadoriaRequest(
                encomenda.getId(),
                loja.getId(),
                responsavel.getId(),
                "GR-1",
                LocalDate.now(),
                LocalDate.now(),
                List.of(new RegistarEntradaMercadoriaLinhaRequest(produto.getId(), 8, 10, "Entrega parcial"))
        ));

        assertEquals(-2, response.get(0).discrepancia());
        assertEquals("RECEBIDA", encomenda.getEstado().getCodigo());
        verify(stock).atualizarStock(produto.getId(), loja.getId(), 8);
        verify(auditoria).registar(TipoOperacao.ENTRADA_MERCADORIA_REGISTADA,
                responsavel.getId(), "ENTRADA_MERCADORIA", "Entrada de mercadoria registada");
    }

    @Test
    void registarEntradaParcialNaoMarcaEncomendaComoRecebida() {
        EstadoEncomendaCodigo pendente = EstadoEncomendaCodigo.PENDENTE;
        Encomenda encomenda = new Encomenda(loja, fornecedor, pendente);
        new LinhaEncomenda(encomenda, produto, 10, new BigDecimal("0.60"));
        when(encomendaRepository.findById(encomenda.getId())).thenReturn(Optional.of(encomenda));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(responsavel.getId())).thenReturn(Optional.of(responsavel));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(guiaRemessaRepository.findByNumero("GR-2")).thenReturn(Optional.empty());
        when(guiaRemessaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entradaMercadoriaRepository.save(any(EntradaMercadoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entradaMercadoriaRepository.findByGuiaRemessaEncomendaId(encomenda.getId())).thenReturn(List.of());

        facade.registarEntradaMercadoria(new RegistarEntradaMercadoriaRequest(
                encomenda.getId(), loja.getId(), responsavel.getId(), "GR-2",
                LocalDate.now(), LocalDate.now(),
                List.of(new RegistarEntradaMercadoriaLinhaRequest(produto.getId(), 4, 10, "Entrega parcial"))
        ));

        assertEquals("PENDENTE", encomenda.getEstado().getCodigo());
    }

    @Test
    void registarEntradaMercadoriaComVariasLinhasAtualizaStockPorProduto() {
        EstadoEncomendaCodigo pendente = EstadoEncomendaCodigo.PENDENTE;
        EstadoEncomendaCodigo recebida = EstadoEncomendaCodigo.RECEBIDA;
        Produto sandes = new Produto("5600000000226", "Sandes", new BigDecimal("2.00"), new BigDecimal("1.00"),
                new TaxaIVA("Normal", new BigDecimal("23")), new Categoria("Snacks", "Snacks"));
        Encomenda encomenda = new Encomenda(loja, fornecedor, pendente);
        new LinhaEncomenda(encomenda, produto, 8, new BigDecimal("0.60"));
        new LinhaEncomenda(encomenda, sandes, 3, new BigDecimal("1.00"));
        when(encomendaRepository.findById(encomenda.getId())).thenReturn(Optional.of(encomenda));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(responsavel.getId())).thenReturn(Optional.of(responsavel));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(produtoRepository.findById(sandes.getId())).thenReturn(Optional.of(sandes));
        when(guiaRemessaRepository.findByNumero("GR-3")).thenReturn(Optional.empty());
        when(guiaRemessaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(entradaMercadoriaRepository.save(any(EntradaMercadoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entradaMercadoriaRepository.findByGuiaRemessaEncomendaId(encomenda.getId())).thenReturn(List.of());

        List<EntradaMercadoriaResponse> response = facade.registarEntradaMercadoria(new RegistarEntradaMercadoriaRequest(
                encomenda.getId(), loja.getId(), responsavel.getId(), "GR-3",
                LocalDate.now(), LocalDate.now(),
                List.of(
                        new RegistarEntradaMercadoriaLinhaRequest(produto.getId(), 8, 8, null),
                        new RegistarEntradaMercadoriaLinhaRequest(sandes.getId(), 3, 3, null)
                )
        ));

        assertEquals(2, response.size());
        verify(stock).atualizarStock(produto.getId(), loja.getId(), 8);
        verify(stock).atualizarStock(sandes.getId(), loja.getId(), 3);
        assertEquals("RECEBIDA", encomenda.getEstado().getCodigo());
    }

    @Test
    void registarEntradaMercadoriaValidaLojaResponsavelLinhasEProdutoEncomendado() {
        Loja outraLoja = new Loja("Loja Porto", "Rua Norte", "123456780");
        Produto sandes = new Produto("5600000000226", "Sandes", new BigDecimal("2.00"), new BigDecimal("1.00"),
                new TaxaIVA("Normal", new BigDecimal("23")), new Categoria("Snacks", "Snacks"));
        Encomenda encomenda = new Encomenda(loja, fornecedor, new EstadoEncomenda("PENDENTE", "Pendente"));
        new LinhaEncomenda(encomenda, produto, 8, new BigDecimal("0.60"));
        when(encomendaRepository.findById(encomenda.getId())).thenReturn(Optional.of(encomenda));

        when(lojaRepository.findById(outraLoja.getId())).thenReturn(Optional.of(outraLoja));
        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.registarEntradaMercadoria(new RegistarEntradaMercadoriaRequest(
                encomenda.getId(), outraLoja.getId(), responsavel.getId(), "GR-9",
                LocalDate.now(), LocalDate.now(),
                List.of(new RegistarEntradaMercadoriaLinhaRequest(produto.getId(), 1, 1, null))
        )));

        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(responsavel.getId())).thenReturn(Optional.empty());
        assertThrows(pt.miniFormiga.exception.RecursoNaoEncontradoException.class, () -> facade.registarEntradaMercadoria(new RegistarEntradaMercadoriaRequest(
                encomenda.getId(), loja.getId(), responsavel.getId(), "GR-9",
                LocalDate.now(), LocalDate.now(),
                List.of(new RegistarEntradaMercadoriaLinhaRequest(produto.getId(), 1, 1, null))
        )));

        when(utilizadorRepository.findById(responsavel.getId())).thenReturn(Optional.of(responsavel));
        when(guiaRemessaRepository.findByNumero("GR-9")).thenReturn(Optional.of(new GuiaRemessa(fornecedor, encomenda, "GR-9", LocalDate.now(), LocalDate.now())));
        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.registarEntradaMercadoria(new RegistarEntradaMercadoriaRequest(
                encomenda.getId(), loja.getId(), responsavel.getId(), "GR-9",
                LocalDate.now(), LocalDate.now(), List.of()
        )));

        when(produtoRepository.findById(sandes.getId())).thenReturn(Optional.of(sandes));
        assertThrows(pt.miniFormiga.exception.BusinessException.class, () -> facade.registarEntradaMercadoria(new RegistarEntradaMercadoriaRequest(
                encomenda.getId(), loja.getId(), responsavel.getId(), "GR-9",
                LocalDate.now(), LocalDate.now(),
                List.of(new RegistarEntradaMercadoriaLinhaRequest(sandes.getId(), 1, 1, null))
        )));
    }

    @Test
    void listarEntradasMercadoriaMapeiaPaginaDoRepositorio() {
        GuiaRemessa guia = new GuiaRemessa(fornecedor, "GR-10", LocalDate.now(), LocalDate.now());
        EntradaMercadoria entrada = new EntradaMercadoria(guia, loja, responsavel, produto, 2, 1, "Sobra");
        PageRequest pageable = PageRequest.of(0, 10);
        when(entradaMercadoriaRepository.findByLojaId(loja.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(entrada), pageable, 1));

        var pagina = facade.listarEntradasMercadoria(loja.getId(), pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(1, pagina.getContent().get(0).discrepancia());
    }

    private CondicaoComercial condicao(Produto produto) {
        return new CondicaoComercial(fornecedor, produto, new BigDecimal("0.60"), 2, 1, LocalDate.now());
    }
}
