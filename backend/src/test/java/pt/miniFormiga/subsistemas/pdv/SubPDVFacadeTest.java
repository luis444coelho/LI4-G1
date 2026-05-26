package pt.miniFormiga.subsistemas.pdv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.*;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.FaturaNaoEmitidaException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.*;
import pt.miniFormiga.subsistemas.sincronizacao.ISubSincronizacao;
import pt.miniFormiga.subsistemas.stock.ISubStock;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static pt.miniFormiga.subsistemas.pdv.PdvDtos.*;

@ExtendWith(MockitoExtension.class)
class SubPDVFacadeTest {

    @Mock ProdutoRepository produtoRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock TaxaIVARepository taxaIVARepository;
    @Mock FornecedorRepository fornecedorRepository;
    @Mock LojaRepository lojaRepository;
    @Mock UtilizadorRepository utilizadorRepository;
    @Mock VendaRepository vendaRepository;
    @Mock FaturaRepository faturaRepository;
    @Mock FechoCaixaRepository fechoCaixaRepository;
    @Mock DevolucaoRepository devolucaoRepository;
    @Mock ISubStock stock;
    @Mock ISubSincronizacao sincronizacao;
    @Mock AuditoriaService auditoria;

    SubPDVFacade facade;
    Loja loja;
    Utilizador operador;
    Produto produto;

    @BeforeEach
    void setUp() {
        facade = new SubPDVFacade(produtoRepository, categoriaRepository, taxaIVARepository, fornecedorRepository,
                lojaRepository, utilizadorRepository, vendaRepository, faturaRepository,
                fechoCaixaRepository, devolucaoRepository, stock, sincronizacao, auditoria);
        loja = new Loja("Loja Braga", "Rua Central", "123456789");
        operador = new Utilizador("operador", "hash", "Operador", new Perfil("FUNCIONARIO", java.util.List.of("PDV_WRITE")), loja);
        produto = new Produto("5600000000011", "Agua", new BigDecimal("1.00"), new BigDecimal("0.40"),
                new TaxaIVA("NORMAL", new BigDecimal("23")), new Categoria("Bebidas", "Bebidas"));
    }

    @Test
    void adicionarLinhaPropagaStockInsuficienteRd04() {
        Venda venda = new Venda(loja, operador);
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(stock.consultarStock(loja.getId()))
                .thenReturn(List.of(new ISubStock.StockDTO(produto.getId(), loja.getId(), 1)));

        assertThrows(StockInsuficienteException.class,
                () -> facade.adicionarLinhaVenda(venda.getId(), produto.getId(), 2));
    }

    @Test
    void pesquisarProdutoPorCodigoBarras() {
        when(produtoRepository.findByCodigoBarras("5600000000011")).thenReturn(Optional.of(produto));

        ProdutoDTO response = facade.obterProdutoPorCodigoBarras("5600000000011");

        assertEquals(produto.getId(), response.id());
        assertEquals("Agua", response.nome());
    }

    @Test
    void pesquisarProdutoPorCodigoInternoQuandoCodigoBarrasNaoExiste() {
        when(produtoRepository.findByCodigoBarras("5600000000011")).thenReturn(Optional.empty());
        when(produtoRepository.findByCodigo("5600000000011")).thenReturn(Optional.of(produto));

        ProdutoDTO response = facade.obterProdutoPorCodigoBarras("5600000000011");

        assertEquals(produto.getId(), response.id());
        verify(produtoRepository).findByCodigo("5600000000011");
    }

    @Test
    void criarProdutoResolveCategoriaTaxaIvaEFornecedor() {
        Categoria categoria = new Categoria("Mercearia", "Produtos de mercearia");
        TaxaIVA taxaIVA = new TaxaIVA("INTERMEDIA", new BigDecimal("13"));
        Fornecedor fornecedor = fornecedor();
        when(taxaIVARepository.findById(taxaIVA.getId())).thenReturn(Optional.of(taxaIVA));
        when(categoriaRepository.findById(categoria.getId())).thenReturn(Optional.of(categoria));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProdutoDTO response = facade.criarProduto(new CriarProdutoRequest(
                "5600000000097",
                "Bolachas",
                "Pacote familiar",
                new BigDecimal("2.20"),
                new BigDecimal("1.10"),
                categoria.getId(),
                taxaIVA.getId(),
                fornecedor.getId()
        ));

        assertEquals("Bolachas", response.nome());
        assertEquals("Pacote familiar", response.descricao());
        assertEquals("Mercearia", response.categoria());
        assertEquals(new BigDecimal("13"), response.taxaIva());
    }

    @Test
    void criarProdutoSemTaxaIvaFalhaAntesDeConsultarRepositorios() {
        BusinessException exception = assertThrows(BusinessException.class, () -> facade.criarProduto(
                new CriarProdutoRequest(
                        "5600000000097",
                        "Bolachas",
                        null,
                        new BigDecimal("2.20"),
                        new BigDecimal("1.10"),
                        UUID.randomUUID(),
                        null,
                        null
                )));

        assertEquals("TAXA_IVA_OBRIGATORIA", exception.getCode());
        verify(taxaIVARepository, never()).findById(any());
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void atualizarProdutoAlteraDadosCategoriaTaxaFornecedorEEstado() {
        Categoria novaCategoria = new Categoria("Higiene", "Higiene pessoal");
        TaxaIVA novaTaxa = new TaxaIVA("REDUZIDA", new BigDecimal("6"));
        Fornecedor fornecedor = fornecedor();
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(categoriaRepository.findById(novaCategoria.getId())).thenReturn(Optional.of(novaCategoria));
        when(taxaIVARepository.findById(novaTaxa.getId())).thenReturn(Optional.of(novaTaxa));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));

        ProdutoDTO response = facade.atualizarProduto(produto.getId(), new AtualizarProdutoRequest(
                "Agua 1L",
                "Garrafa maior",
                new BigDecimal("1.30"),
                new BigDecimal("0.55"),
                novaCategoria.getId(),
                novaTaxa.getId(),
                fornecedor.getId(),
                false
        ));

        assertEquals("Agua 1L", response.nome());
        assertEquals("Garrafa maior", response.descricao());
        assertEquals("Higiene", response.categoria());
        assertEquals(new BigDecimal("6"), response.taxaIva());
        assertEquals(false, response.ativo());
        assertEquals(fornecedor, produto.getFornecedorPrincipal());
    }

    @Test
    void listarProdutosMapeiaPaginaDeDominioParaDto() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(produtoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(produto), pageable, 1));

        var page = facade.listarProdutos(loja.getId(), pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("Agua", page.getContent().get(0).nome());
    }

    @Test
    void emissaoFaturaUsaSequenciaIninterruptaRd03() {
        Venda venda = vendaFinalizada();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(faturaRepository.existsByVendaId(venda.getId())).thenReturn(false);
        when(faturaRepository.findFirstByLojaIdAndSerieOrderByNumeroDesc(eq(loja.getId()), anyString())).thenReturn(Optional.empty());
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fatura primeira = facade.emitirFatura(venda.getId(), null, null);

        assertEquals("A/2026/00001", primeira.getNumeroFatura());
        assertEquals(1, primeira.getNumeroSequencial());
        verify(auditoria).registar(TipoOperacao.FATURA_EMITIDA, operador.getId(), "FATURA", "Fatura emitida");
    }

    @Test
    void emitirFaturaSimplificadaSemNifAteMilEuros() {
        Venda venda = vendaFinalizada();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(faturaRepository.existsByVendaId(venda.getId())).thenReturn(false);
        when(faturaRepository.findFirstByLojaIdAndSerieOrderByNumeroDesc(eq(loja.getId()), anyString())).thenReturn(Optional.empty());
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fatura fatura = facade.emitirFatura(venda.getId(), null, null);

        assertEquals("SIMPLIFICADA", fatura.getTipo());
    }

    @Test
    void emitirFaturaCompletaQuandoClienteForneceNif() {
        Venda venda = vendaFinalizada();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(faturaRepository.existsByVendaId(venda.getId())).thenReturn(false);
        when(faturaRepository.findFirstByLojaIdAndSerieOrderByNumeroDesc(eq(loja.getId()), anyString())).thenReturn(Optional.empty());
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fatura fatura = facade.emitirFatura(venda.getId(), "123456789", "Cliente");

        assertEquals("COMPLETA", fatura.getTipo());
        assertEquals("123456789", fatura.getNifCliente());
    }

    @Test
    void gerarDocumentoFiscalDisponibilizaFaturaERecibo() {
        Venda venda = vendaFinalizada();
        Fatura fatura = new Fatura(venda, "00001", "A/2026", "SIMPLIFICADA", null, null);
        fatura.emitir();
        when(faturaRepository.findById(fatura.getId())).thenReturn(Optional.of(fatura));

        String conteudoFatura = new String(facade.gerarDocumentoFiscal(fatura.getId(), "FATURA"), StandardCharsets.UTF_8);
        String conteudoRecibo = new String(facade.gerarDocumentoFiscal(fatura.getId(), "RECIBO"), StandardCharsets.UTF_8);

        assertTrue(conteudoFatura.contains("Fatura A/2026/1"));
        assertTrue(conteudoRecibo.contains("Recibo A/2026/1"));
        assertTrue(conteudoRecibo.contains("Fatura associada: A/2026/00001"));
        assertTrue(conteudoRecibo.contains("Total recebido: 1.23"));
    }

    @Test
    void finalizarVendaComMeioPagamentoInvalidoFalha() {
        Venda venda = vendaFinalizada();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));

        assertThrows(BusinessException.class, () -> facade.finalizarVenda(venda.getId(), produto.getId().toString()));
    }

    @Test
    void emitirFaturaDeVendaAbertaOuDuplicadaFalha() {
        Venda aberta = new Venda(loja, operador);
        Venda finalizada = vendaFinalizada();
        when(vendaRepository.findById(aberta.getId())).thenReturn(Optional.of(aberta));
        when(vendaRepository.findById(finalizada.getId())).thenReturn(Optional.of(finalizada));
        when(faturaRepository.existsByVendaId(finalizada.getId())).thenReturn(true);

        FaturaNaoEmitidaException abertaException = assertThrows(FaturaNaoEmitidaException.class,
                () -> facade.emitirFatura(aberta.getId(), null, null));
        FaturaNaoEmitidaException duplicadaException = assertThrows(FaturaNaoEmitidaException.class,
                () -> facade.emitirFatura(finalizada.getId(), null, null));

        assertEquals("FATURA_NAO_EMITIDA", abertaException.getCode());
        assertEquals("FATURA_NAO_EMITIDA", duplicadaException.getCode());
        verify(faturaRepository, never()).save(any());
    }

    @Test
    void obterFaturaEDocumentoFiscalInvalidoCobremContratos() {
        Venda venda = vendaFinalizada();
        Fatura fatura = new Fatura(venda, "00003", "A/2026", "SIMPLIFICADA", null, null);
        fatura.emitir();
        when(faturaRepository.findById(fatura.getId())).thenReturn(Optional.of(fatura));

        FaturaDTO dto = facade.obterFatura(fatura.getId());
        BusinessException exception = assertThrows(BusinessException.class,
                () -> facade.gerarDocumentoFiscal(fatura.getId(), "nota"));

        assertEquals("A/2026/00003", dto.numeroFatura());
        assertEquals(new BigDecimal("1.00"), dto.totalSemIva());
        assertEquals("DOCUMENTO_FISCAL_INVALIDO", exception.getCode());
    }

    @Test
    void faturaCompletaSemNifFalhaRd02() {
        Venda venda = vendaFinalizada();
        new LinhaVenda(venda, new Produto("p2", "Caro", new BigDecimal("1000.00"), BigDecimal.ONE,
                new TaxaIVA("NORMAL2", new BigDecimal("23")), new Categoria("Caros", "Caros")), 1);
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(faturaRepository.existsByVendaId(venda.getId())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> facade.emitirFatura(venda.getId(), null, null));

        assertEquals("NIF_OBRIGATORIO", exception.getCode());
    }

    @Test
    void fluxoCompletoVendaAdicionaLinhaEFinaliza() {
        Venda venda = new Venda(loja, operador);
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(stock.consultarStock(loja.getId()))
                .thenReturn(List.of(new ISubStock.StockDTO(produto.getId(), loja.getId(), 5)));
        when(vendaRepository.save(venda)).thenReturn(venda);

        LinhaVenda linha = facade.adicionarLinhaVenda(venda.getId(), produto.getId(), 2);
        Venda finalizada = facade.finalizarVenda(venda.getId(), "CARTAO");

        assertEquals(2, linha.getQuantidade());
        assertEquals(MeioPagamentoTipo.CARTAO, finalizada.getMeioPagamento());
        verify(stock).consultarStock(loja.getId());
        verify(stock).atualizarStock(produto.getId(), loja.getId(), -2);
        verify(auditoria).registar(TipoOperacao.VENDA_FINALIZADA, operador.getId(), "VENDA", "Venda finalizada");
        verify(sincronizacao).iniciarSincronizacao(loja.getId());
    }

    @Test
    void finalizarVendaAgendaSincronizacaoSeTransmissaoImediataFalhar() {
        Venda venda = vendaComLinhaAberta();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(vendaRepository.save(venda)).thenReturn(venda);
        doThrow(new BusinessException("SINCRONIZACAO_INDISPONIVEL", "Servidor central indisponivel"))
                .when(sincronizacao).iniciarSincronizacao(loja.getId());

        Venda finalizada = facade.finalizarVenda(venda.getId(), "NUMERARIO");

        assertEquals(MeioPagamentoTipo.NUMERARIO, finalizada.getMeioPagamento());
        verify(sincronizacao).agendarSincronizacao(loja.getId());
    }

    @Test
    void finalizarVendaAceitaNumerarioCartaoEMbWay() {
        for (String tipo : List.of("NUMERARIO", "CARTAO", "MBWAY")) {
            Venda venda = vendaComLinhaAberta();
            when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
            when(vendaRepository.save(venda)).thenReturn(venda);

            Venda finalizada = facade.finalizarVenda(venda.getId(), tipo);

            assertEquals(MeioPagamentoTipo.valueOf(tipo), finalizada.getMeioPagamento());
        }
        verify(stock, times(3)).atualizarStock(produto.getId(), loja.getId(), -1);
    }

    @Test
    void finalizarVendaPropagaBloqueioDeStockNegativoNoMomentoDaEscrita() {
        Venda venda = vendaComLinhaAberta();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        doThrow(new StockInsuficienteException(produto.getId(), 0, 1))
                .when(stock).atualizarStock(produto.getId(), loja.getId(), -1);

        assertThrows(StockInsuficienteException.class, () -> facade.finalizarVenda(venda.getId(), "NUMERARIO"));

        verify(vendaRepository, never()).save(venda);
    }

    @Test
    void meiosPagamentoObrigatoriosSaoEnumDoDominio() {
        assertEquals(List.of(MeioPagamentoTipo.NUMERARIO, MeioPagamentoTipo.CARTAO, MeioPagamentoTipo.MBWAY),
                List.of(MeioPagamentoTipo.values()));
    }

    @Test
    void registarVendaComTresProdutosPersisteLinhasPorCascade() {
        Venda venda = new Venda(loja, operador);
        Produto sandes = new Produto("5600000000028", "Sandes", new BigDecimal("2.50"), new BigDecimal("1.20"),
                new TaxaIVA("REDUZIDA", new BigDecimal("6")), new Categoria("Snacks", "Snacks"));
        Produto champo = new Produto("5600000000035", "Champo", new BigDecimal("3.50"), new BigDecimal("1.80"),
                new TaxaIVA("NORMAL", new BigDecimal("23")), new Categoria("Higiene", "Higiene"));
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(produtoRepository.findById(sandes.getId())).thenReturn(Optional.of(sandes));
        when(produtoRepository.findById(champo.getId())).thenReturn(Optional.of(champo));
        when(stock.consultarStock(loja.getId())).thenReturn(List.of(
                new ISubStock.StockDTO(produto.getId(), loja.getId(), 5),
                new ISubStock.StockDTO(sandes.getId(), loja.getId(), 5),
                new ISubStock.StockDTO(champo.getId(), loja.getId(), 5)
        ));
        when(vendaRepository.save(venda)).thenReturn(venda);

        facade.adicionarLinhaVenda(venda.getId(), produto.getId(), 1);
        facade.adicionarLinhaVenda(venda.getId(), sandes.getId(), 1);
        facade.adicionarLinhaVenda(venda.getId(), champo.getId(), 1);

        assertEquals(3, venda.getLinhas().size());
        verify(vendaRepository, times(3)).save(venda);
    }

    @Test
    void adicionarLinhaComQuantidadeNaoPositivaFalhaAntesDeConsultarProduto() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> facade.adicionarLinhaVenda(UUID.randomUUID(), produto.getId(), 0));

        assertEquals("QUANTIDADE_LINHA_INVALIDA", exception.getCode());
        verify(produtoRepository, never()).findById(any());
    }

    @Test
    void adicionarLinhaFalhaQuandoVendaNaoEstaAbertaOuProdutoEstaInativo() {
        Venda vendaFinalizada = vendaFinalizada();
        Venda vendaAberta = new Venda(loja, operador);
        Produto produtoInativo = new Produto("5600000000042", "Produto inativo", BigDecimal.ONE, BigDecimal.ONE,
                produto.getTaxaIVA(), produto.getCategoria());
        produtoInativo.desativar();
        when(vendaRepository.findById(vendaFinalizada.getId())).thenReturn(Optional.of(vendaFinalizada));
        when(vendaRepository.findById(vendaAberta.getId())).thenReturn(Optional.of(vendaAberta));
        when(produtoRepository.findById(produtoInativo.getId())).thenReturn(Optional.of(produtoInativo));

        BusinessException vendaException = assertThrows(BusinessException.class,
                () -> facade.adicionarLinhaVenda(vendaFinalizada.getId(), produto.getId(), 1));
        BusinessException produtoException = assertThrows(BusinessException.class,
                () -> facade.adicionarLinhaVenda(vendaAberta.getId(), produtoInativo.getId(), 1));

        assertEquals("VENDA_NAO_ABERTA", vendaException.getCode());
        assertEquals("PRODUTO_INATIVO", produtoException.getCode());
        verify(stock, never()).consultarStock(loja.getId());
    }

    @Test
    void removerLinhaAntesDeFinalizarRecalculaTotalESoAnulaLinhaSelecionada() {
        Venda venda = new Venda(loja, operador);
        LinhaVenda linha1 = new LinhaVenda(venda, produto, 1);
        Produto sandes = new Produto("5600000000028", "Sandes", new BigDecimal("2.50"), new BigDecimal("1.20"),
                new TaxaIVA("REDUZIDA", new BigDecimal("6")), new Categoria("Snacks", "Snacks"));
        LinhaVenda linha2 = new LinhaVenda(venda, sandes, 1);
        venda.calcularTotais();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));

        facade.anularLinhaVenda(venda.getId(), linha1.getId());

        assertEquals(true, linha1.isAnulada());
        assertEquals(false, linha2.isAnulada());
        assertEquals(new BigDecimal("2.65"), venda.getTotalComIVA());
        verify(vendaRepository).save(venda);
    }

    @Test
    void confirmarFechoCaixaRegistaTotaisAuditoriaEIniciaSincronizacao() {
        FechoCaixa fecho = new FechoCaixa(loja, operador, java.time.LocalDate.now(), List.of(vendaFinalizada()));
        when(fechoCaixaRepository.findById(fecho.getId())).thenReturn(Optional.of(fecho));

        FechoCaixa confirmado = facade.confirmarFechoCaixa(fecho.getId(), "sem discrepancias");

        assertEquals(true, confirmado.isConfirmado());
        assertEquals(new BigDecimal("1.23"), confirmado.getTotalNumerario());
        assertEquals(new BigDecimal("1.23"), confirmado.getTotalGeral());
        assertEquals("sem discrepancias", confirmado.getObservacoesDiscrepancia());
        verify(auditoria).registar(TipoOperacao.FECHO_CAIXA_CONFIRMADO,
                operador.getId(), "FECHO_CAIXA", "Fecho de caixa confirmado");
        verify(sincronizacao).iniciarSincronizacao(loja.getId());
    }

    @Test
    void devolucaoReverteStock() {
        Venda venda = vendaFinalizada();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(devolucaoRepository.save(any(Devolucao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendaDTO devolucao = facade.processarDevolucao(venda.getId(), new ProcessarDevolucaoRequest(produto.getId(), 1));

        assertEquals(venda.getId(), devolucao.id());
        verify(stock).atualizarStock(produto.getId(), loja.getId(), 1);
        verify(devolucaoRepository).save(any(Devolucao.class));
    }

    @Test
    void devolucaoFalhaParaVendaAbertaProdutoInexistenteOuQuantidadeSuperior() {
        Venda aberta = new Venda(loja, operador);
        Venda finalizada = vendaFinalizada();
        when(vendaRepository.findById(aberta.getId())).thenReturn(Optional.of(aberta));
        when(vendaRepository.findById(finalizada.getId())).thenReturn(Optional.of(finalizada));

        BusinessException abertaException = assertThrows(BusinessException.class,
                () -> facade.processarDevolucao(aberta.getId(), new ProcessarDevolucaoRequest(produto.getId(), 1)));
        BusinessException produtoException = assertThrows(BusinessException.class,
                () -> facade.processarDevolucao(finalizada.getId(), new ProcessarDevolucaoRequest(UUID.randomUUID(), 1)));
        BusinessException quantidadeException = assertThrows(BusinessException.class,
                () -> facade.processarDevolucao(finalizada.getId(), new ProcessarDevolucaoRequest(produto.getId(), 2)));

        assertEquals("VENDA_NAO_FINALIZADA", abertaException.getCode());
        assertEquals("PRODUTO_NAO_EXISTE_NA_VENDA", produtoException.getCode());
        assertEquals("QUANTIDADE_DEVOLUCAO_INVALIDA", quantidadeException.getCode());
        verify(stock, never()).atualizarStock(any(), any(), anyInt());
    }

    @Test
    void registarFechoCaixaComVendasPorFecharCalculaTotaisEGrava() {
        Venda vendaNumerario = vendaFinalizada();
        Venda vendaCartao = vendaComLinhaAberta();
        vendaCartao.finalizar(new MeioPagamento("CARTAO", "Cartao"));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
        when(fechoCaixaRepository.existsByLojaIdAndData(loja.getId(), LocalDate.now())).thenReturn(false);
        when(vendaRepository.findVendasPorFechar(eq(loja.getId()), any(), any()))
                .thenReturn(List.of(vendaNumerario, vendaCartao));
        when(fechoCaixaRepository.save(any(FechoCaixa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FechoCaixa fecho = facade.registarFechoCaixa(loja.getId(), operador.getId());

        assertEquals(new BigDecimal("1.23"), fecho.getTotalNumerario());
        assertEquals(new BigDecimal("1.23"), fecho.getTotalCartao());
        assertEquals(new BigDecimal("2.46"), fecho.getTotalGeral());
        verify(auditoria).registar(TipoOperacao.FECHO_CAIXA_INICIADO, operador.getId(), "FECHO_CAIXA", "Fecho de caixa iniciado");
    }

    @Test
    void registarFechoCaixaDuplicadoFalhaAntesDeConsultarVendas() {
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
        when(fechoCaixaRepository.existsByLojaIdAndData(loja.getId(), LocalDate.now())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> facade.registarFechoCaixa(loja.getId(), operador.getId()));

        assertEquals("FECHO_CAIXA_JA_EXISTE", exception.getCode());
        verify(vendaRepository, never()).findVendasPorFechar(any(), any(), any());
    }

    @Test
    void caixaFechadaBloqueiaRegistoDeVendaEFinalizacao() {
        Venda venda = vendaComLinhaAberta();
        when(fechoCaixaRepository.existsByLojaIdAndDataAndConfirmadoTrue(loja.getId(), LocalDate.now())).thenReturn(true);
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));

        BusinessException registo = assertThrows(BusinessException.class,
                () -> facade.registarVenda(loja.getId(), operador.getId()));
        BusinessException finalizacao = assertThrows(BusinessException.class,
                () -> facade.finalizarVenda(venda.getId(), "NUMERARIO"));

        assertEquals("CAIXA_FECHADA", registo.getCode());
        assertEquals("CAIXA_FECHADA", finalizacao.getCode());
        verify(lojaRepository, never()).findById(loja.getId());
    }

    @Test
    void metodosDeConsultaDeVendasEFechosMapeiamRepositorios() {
        Venda venda = vendaFinalizada();
        FechoCaixa fecho = new FechoCaixa(loja, operador, LocalDate.now(), List.of(venda));
        PageRequest pageable = PageRequest.of(0, 10);
        when(vendaRepository.findByLojaIdAndDataHoraBetween(eq(loja.getId()), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(venda), pageable, 1));
        when(vendaRepository.findVendasPorFechar(eq(loja.getId()), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(venda), pageable, 1));
        when(vendaRepository.findByLojaIdAndDataHoraBetween(eq(loja.getId()), any(), any(), eq(org.springframework.data.domain.Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(venda)));
        when(fechoCaixaRepository.findByLojaId(loja.getId(), pageable)).thenReturn(new PageImpl<>(List.of(fecho), pageable, 1));
        when(fechoCaixaRepository.findByLojaId(loja.getId(), org.springframework.data.domain.Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(fecho)));
        when(fechoCaixaRepository.findById(fecho.getId())).thenReturn(Optional.of(fecho));

        assertEquals(1, facade.listarVendas(loja.getId(), LocalDate.now(), LocalDate.now(), pageable).getTotalElements());
        assertEquals(1, facade.listarVendasPorFechar(loja.getId(), LocalDate.now(), LocalDate.now(), pageable).getTotalElements());
        assertEquals(1, facade.getVendasPorLoja(loja.getId(), LocalDate.now(), LocalDate.now()).size());
        assertEquals(1, facade.listarFechosCaixa(loja.getId(), pageable).getTotalElements());
        assertEquals(1, facade.getFechoCaixaByLoja(loja.getId()).size());
        assertEquals(fecho.getId(), facade.obterFechoCaixa(fecho.getId()).id());
    }

    @Test
    void registarVendaForaHorarioGeraAuditoria() {
        Venda vendaForaHorario = new Venda(loja, operador);
        ReflectionTestUtils.setField(vendaForaHorario, "dataHora", LocalDateTime.of(2026, 5, 17, 22, 0));
        SubPDVFacade facadeComVendaForaHorario = new SubPDVFacade(produtoRepository, categoriaRepository, taxaIVARepository, fornecedorRepository,
                lojaRepository, utilizadorRepository, vendaRepository, faturaRepository,
                fechoCaixaRepository, devolucaoRepository, stock, sincronizacao, auditoria) {
            @Override
            protected Venda criarVenda(Loja loja, Utilizador operador) {
                return vendaForaHorario;
            }
        };
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
        when(vendaRepository.save(vendaForaHorario)).thenReturn(vendaForaHorario);

        facadeComVendaForaHorario.registarVenda(loja.getId(), operador.getId());

        verify(auditoria).registar(TipoOperacao.VENDA_FORA_HORARIO, operador.getId(), "VENDA", "Venda fora do horario normal");
    }

    private Venda vendaFinalizada() {
        Venda venda = new Venda(loja, operador);
        new LinhaVenda(venda, produto, 1);
        venda.finalizar(new MeioPagamento("NUMERARIO", "Numerario"));
        return venda;
    }

    private Venda vendaComLinhaAberta() {
        Venda venda = new Venda(loja, operador);
        new LinhaVenda(venda, produto, 1);
        return venda;
    }

    private Fornecedor fornecedor() {
        return new Fornecedor(
                "Fornecedor Norte",
                "222333444",
                "Rua Norte",
                "253000000",
                "fornecedor@mini.pt",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );
    }
}
