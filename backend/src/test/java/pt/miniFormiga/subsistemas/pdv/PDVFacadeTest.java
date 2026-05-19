package pt.miniFormiga.subsistemas.pdv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.*;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.*;
import pt.miniFormiga.subsistemas.sincronizacao.ISubSincronizacao;
import pt.miniFormiga.subsistemas.stock.ISubStock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static pt.miniFormiga.subsistemas.pdv.PdvDtos.*;

@ExtendWith(MockitoExtension.class)
class PDVFacadeTest {

    @Mock ProdutoRepository produtoRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock TaxaIVARepository taxaIVARepository;
    @Mock FornecedorRepository fornecedorRepository;
    @Mock LojaRepository lojaRepository;
    @Mock UtilizadorRepository utilizadorRepository;
    @Mock VendaRepository vendaRepository;
    @Mock FaturaRepository faturaRepository;
    @Mock FaturaSequenciaRepository faturaSequenciaRepository;
    @Mock FechoCaixaRepository fechoCaixaRepository;
    @Mock MeioPagamentoRepository meioPagamentoRepository;
    @Mock DevolucaoRepository devolucaoRepository;
    @Mock ISubStock stock;
    @Mock ISubSincronizacao sincronizacao;
    @Mock AuditoriaService auditoria;

    PDVFacade facade;
    Loja loja;
    Utilizador operador;
    Produto produto;

    @BeforeEach
    void setUp() {
        facade = new PDVFacade(produtoRepository, categoriaRepository, taxaIVARepository, fornecedorRepository,
                lojaRepository, utilizadorRepository, vendaRepository, faturaRepository, faturaSequenciaRepository,
                fechoCaixaRepository, meioPagamentoRepository, devolucaoRepository, stock, sincronizacao, auditoria);
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
    void emissaoFaturaUsaSequenciaIninterruptaRd03() {
        Venda venda = vendaFinalizada();
        FaturaSequencia sequencia = new FaturaSequencia("A/2026");
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(faturaRepository.existsByVendaId(venda.getId())).thenReturn(false);
        when(faturaSequenciaRepository.findBySerie(anyString())).thenReturn(Optional.of(sequencia));
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fatura primeira = facade.emitirFatura(venda.getId(), null, null);

        assertEquals("A/2026/00001", primeira.getNumeroFatura());
        assertEquals(1, sequencia.getUltimoNumero());
        verify(auditoria).registar(TipoOperacao.FATURA_EMITIDA, operador.getId(), "FATURA", "Fatura emitida");
    }

    @Test
    void emitirFaturaSimplificadaSemNifAteMilEuros() {
        Venda venda = vendaFinalizada();
        FaturaSequencia sequencia = new FaturaSequencia("A/2026");
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(faturaRepository.existsByVendaId(venda.getId())).thenReturn(false);
        when(faturaSequenciaRepository.findBySerie(anyString())).thenReturn(Optional.of(sequencia));
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fatura fatura = facade.emitirFatura(venda.getId(), null, null);

        assertEquals("SIMPLIFICADA", fatura.getTipo());
    }

    @Test
    void emitirFaturaCompletaQuandoClienteForneceNif() {
        Venda venda = vendaFinalizada();
        FaturaSequencia sequencia = new FaturaSequencia("A/2026");
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(faturaRepository.existsByVendaId(venda.getId())).thenReturn(false);
        when(faturaSequenciaRepository.findBySerie(anyString())).thenReturn(Optional.of(sequencia));
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fatura fatura = facade.emitirFatura(venda.getId(), "123456789", "Cliente");

        assertEquals("COMPLETA", fatura.getTipo());
        assertEquals("123456789", fatura.getNifCliente());
    }

    @Test
    void finalizarVendaComMeioPagamentoInvalidoFalha() {
        Venda venda = vendaFinalizada();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(meioPagamentoRepository.findById(produto.getId())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> facade.finalizarVenda(venda.getId(), produto.getId()));
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
        MeioPagamento cartao = new MeioPagamento("CARTAO", "Cartao");
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(stock.consultarStock(loja.getId()))
                .thenReturn(List.of(new ISubStock.StockDTO(produto.getId(), loja.getId(), 5)));
        when(meioPagamentoRepository.findById(cartao.getId())).thenReturn(Optional.of(cartao));
        when(vendaRepository.save(venda)).thenReturn(venda);

        LinhaVenda linha = facade.adicionarLinhaVenda(venda.getId(), produto.getId(), 2);
        Venda finalizada = facade.finalizarVenda(venda.getId(), cartao.getId());

        assertEquals(2, linha.getQuantidade());
        assertEquals(cartao, finalizada.getMeioPagamento());
        verify(stock).consultarStock(loja.getId());
        verify(stock).atualizarStock(produto.getId(), loja.getId(), -2);
        verify(auditoria).registar(TipoOperacao.VENDA_FINALIZADA, operador.getId(), "VENDA", "Venda finalizada");
    }

    @Test
    void finalizarVendaAceitaNumerarioCartaoEMbWay() {
        for (String tipo : List.of("NUMERARIO", "CARTAO", "MBWAY")) {
            Venda venda = vendaComLinhaAberta();
            MeioPagamento meioPagamento = new MeioPagamento(tipo, "Pagamento " + tipo);
            when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
            when(meioPagamentoRepository.findById(meioPagamento.getId())).thenReturn(Optional.of(meioPagamento));
            when(vendaRepository.save(venda)).thenReturn(venda);

            Venda finalizada = facade.finalizarVenda(venda.getId(), meioPagamento.getId());

            assertEquals(tipo, finalizada.getMeioPagamento().getTipo());
        }
        verify(stock, times(3)).atualizarStock(produto.getId(), loja.getId(), -1);
    }

    @Test
    void finalizarVendaPropagaBloqueioDeStockNegativoNoMomentoDaEscrita() {
        Venda venda = vendaComLinhaAberta();
        MeioPagamento numerario = new MeioPagamento("NUMERARIO", "Numerario");
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        when(meioPagamentoRepository.findById(numerario.getId())).thenReturn(Optional.of(numerario));
        doThrow(new StockInsuficienteException(produto.getId(), 0, 1))
                .when(stock).atualizarStock(produto.getId(), loja.getId(), -1);

        assertThrows(StockInsuficienteException.class, () -> facade.finalizarVenda(venda.getId(), numerario.getId()));

        verify(vendaRepository, never()).save(venda);
    }

    @Test
    void garantirMeiosPagamentoObrigatoriosCriaNumerarioCartaoEMbway() {
        when(meioPagamentoRepository.findByTipo(anyString())).thenReturn(Optional.empty());
        when(meioPagamentoRepository.save(any(MeioPagamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.garantirMeiosPagamentoObrigatorios();

        verify(meioPagamentoRepository).findByTipo("NUMERARIO");
        verify(meioPagamentoRepository).findByTipo("CARTAO");
        verify(meioPagamentoRepository).findByTipo("MBWAY");
        verify(meioPagamentoRepository, times(3)).save(any(MeioPagamento.class));
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
    void confirmarFechoCaixaRegistaAuditoriaEAgendaSincronizacao() {
        FechoCaixa fecho = new FechoCaixa(loja, operador, java.time.LocalDate.now(), List.of(vendaFinalizada()));
        when(fechoCaixaRepository.findById(fecho.getId())).thenReturn(Optional.of(fecho));

        facade.confirmarFechoCaixa(fecho.getId(), "sem discrepancias");

        verify(auditoria).registar(TipoOperacao.FECHO_CAIXA_CONFIRMADO,
                operador.getId(), "FECHO_CAIXA", "Fecho de caixa confirmado");
        verify(sincronizacao).agendarSincronizacao(loja.getId());
    }

    @Test
    void devolucaoReverteStock() {
        Venda venda = vendaFinalizada();
        when(vendaRepository.findById(venda.getId())).thenReturn(Optional.of(venda));
        FaturaSequencia sequencia = new FaturaSequencia("NC/2026");
        when(faturaSequenciaRepository.findBySerie(anyString())).thenReturn(Optional.of(sequencia));
        when(devolucaoRepository.save(any(Devolucao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VendaDTO devolucao = facade.processarDevolucao(venda.getId(), new ProcessarDevolucaoRequest(produto.getId(), 1));

        assertEquals(venda.getId(), devolucao.id());
        verify(stock).atualizarStock(produto.getId(), loja.getId(), 1);
        verify(devolucaoRepository).save(any(Devolucao.class));
    }

    @Test
    void registarVendaForaHorarioGeraAuditoria() {
        Venda vendaForaHorario = new Venda(loja, operador);
        ReflectionTestUtils.setField(vendaForaHorario, "dataHora", LocalDateTime.of(2026, 5, 17, 22, 0));
        PDVFacade facadeComVendaForaHorario = new PDVFacade(produtoRepository, categoriaRepository, taxaIVARepository, fornecedorRepository,
                lojaRepository, utilizadorRepository, vendaRepository, faturaRepository, faturaSequenciaRepository,
                fechoCaixaRepository, meioPagamentoRepository, devolucaoRepository, stock, sincronizacao, auditoria) {
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
}
