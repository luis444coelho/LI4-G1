package pt.miniFormiga.subsistemas.pdv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.*;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.*;
import pt.miniFormiga.subsistemas.sincronizacao.ISubSincronizacao;
import pt.miniFormiga.subsistemas.stock.ISubStock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
                fechoCaixaRepository, meioPagamentoRepository, stock, sincronizacao, auditoria);
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

        LinhaVenda linha = facade.adicionarLinhaVenda(venda.getId(), produto.getId(), 2);
        Venda finalizada = facade.finalizarVenda(venda.getId(), cartao.getId());

        assertEquals(2, linha.getQuantidade());
        assertEquals(cartao, finalizada.getMeioPagamento());
        verify(stock).consultarStock(loja.getId());
        verify(stock).atualizarStock(produto.getId(), loja.getId(), -2);
        verify(auditoria).registar(TipoOperacao.VENDA_FINALIZADA, operador.getId(), "VENDA", "Venda finalizada");
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

        VendaDTO devolucao = facade.processarDevolucao(venda.getId(), new ProcessarDevolucaoRequest(produto.getId(), 1));

        assertEquals(venda.getId(), devolucao.id());
        verify(stock).atualizarStock(produto.getId(), loja.getId(), 1);
    }

    private Venda vendaFinalizada() {
        Venda venda = new Venda(loja, operador);
        new LinhaVenda(venda, produto, 1);
        venda.finalizar(new MeioPagamento("NUMERARIO", "Numerario"));
        return venda;
    }
}
