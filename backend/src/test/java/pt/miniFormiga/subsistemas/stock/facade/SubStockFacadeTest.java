package pt.miniFormiga.subsistemas.stock.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.miniFormiga.subsistemas.auditoria.facade.ISubAuditoria;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.subsistemas.stock.repository.AjusteInventarioRepository;
import pt.miniFormiga.subsistemas.stock.repository.AlertaStockRepository;
import pt.miniFormiga.subsistemas.stock.repository.InventarioFisicoRepository;
import pt.miniFormiga.subsistemas.stock.repository.LinhaInventarioRepository;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;
import pt.miniFormiga.subsistemas.catalogo.repository.ProdutoRepository;
import pt.miniFormiga.subsistemas.stock.service.LocalProdutoStockStore;
import pt.miniFormiga.subsistemas.stock.service.StockStore;
import pt.miniFormiga.subsistemas.utilizadores.repository.UtilizadorRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubStockFacadeTest {

    private AlertaStockRepository alertaStockRepository;
    private AjusteInventarioRepository ajusteInventarioRepository;
    private InventarioFisicoRepository inventarioFisicoRepository;
    private LinhaInventarioRepository linhaInventarioRepository;
    private LojaRepository lojaRepository;
    private ProdutoRepository produtoRepository;
    private UtilizadorRepository utilizadorRepository;
    private ISubAuditoria auditoria;
    private SubStockFacade facade;

    @BeforeEach
    void setUp() {
        alertaStockRepository = mock(AlertaStockRepository.class);
        ajusteInventarioRepository = mock(AjusteInventarioRepository.class);
        inventarioFisicoRepository = mock(InventarioFisicoRepository.class);
        linhaInventarioRepository = mock(LinhaInventarioRepository.class);
        lojaRepository = mock(LojaRepository.class);
        produtoRepository = mock(ProdutoRepository.class);
        utilizadorRepository = mock(UtilizadorRepository.class);
        auditoria = mock(ISubAuditoria.class);
        StockStore stockStore = new LocalProdutoStockStore(produtoRepository, lojaRepository);
        facade = new SubStockFacade(
                alertaStockRepository,
                ajusteInventarioRepository,
                inventarioFisicoRepository,
                linhaInventarioRepository,
                lojaRepository,
                utilizadorRepository,
                auditoria,
                stockStore
        );
    }

    @Test
    void atualizarStockComDeltaNegativoSuficienteAtualizaQuantidade() {
        StockFixture stock = stock(10);
        whenStock(stock);
        when(alertaStockRepository.existsByProdutoIdAndResolvidoFalse(stock.getProduto().getId())).thenReturn(false);

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), -4);

        assertEquals(6, stock.getProduto().getQuantidadeStock());
        verify(alertaStockRepository, never()).save(any());
    }

    @Test
    void atualizarStockComDeltaNegativoInsuficienteFalhaComValoresCorretos() {
        StockFixture stock = stock(2);
        whenStock(stock);

        StockInsuficienteException exception = assertThrows(StockInsuficienteException.class,
                () -> facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), -3));

        assertEquals(stock.getProduto().getId(), exception.getDetails().get("produtoId"));
        assertEquals(2, exception.getDetails().get("quantidadeDisponivel"));
        assertEquals(3, exception.getDetails().get("quantidadeSolicitada"));
    }

    @Test
    void atualizarStockComDeltaPositivoIncrementaQuantidade() {
        StockFixture stock = stock(2);
        whenStock(stock);

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), 5);

        assertEquals(7, stock.getProduto().getQuantidadeStock());
    }

    @Test
    void atualizarStockAbaixoDoNivelMinimoCriaAlerta() {
        StockFixture stock = stock(12);
        stock.definirNivelMinimo(10);
        whenStock(stock);
        when(alertaStockRepository.existsByProdutoIdAndResolvidoFalse(stock.getProduto().getId())).thenReturn(false);
        when(alertaStockRepository.save(any(AlertaStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), -2);

        assertEquals(10, stock.getProduto().getQuantidadeStock());
        verify(alertaStockRepository).save(any(AlertaStock.class));
    }

    @Test
    void atualizarStockSemNivelMinimoNaoCriaAlerta() {
        StockFixture stock = stock(12);
        whenStock(stock);

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), -10);

        assertEquals(2, stock.getProduto().getQuantidadeStock());
        verify(alertaStockRepository, never()).save(any());
    }

    @Test
    void definirNivelMinimoComStockJaAbaixoEmiteAlertaImediatamente() {
        StockFixture stock = stock(5);
        whenStock(stock);
        when(alertaStockRepository.existsByProdutoIdAndResolvidoFalse(stock.getProduto().getId())).thenReturn(false);

        facade.definirNivelMinimo(stock.getProduto().getId(), stock.getLoja().getId(), 10);

        assertEquals(10, stock.getProduto().getNivelMinimo());
        verify(alertaStockRepository).save(any(AlertaStock.class));
    }

    @Test
    void definirNivelMinimoAbaixoDoStockNaoCriaAlerta() {
        StockFixture stock = stock(12);
        whenStock(stock);
        facade.definirNivelMinimo(stock.getProduto().getId(), stock.getLoja().getId(), 10);

        assertEquals(10, stock.getProduto().getNivelMinimo());
        verify(alertaStockRepository, never()).save(any());
    }

    @Test
    void alertaAtivoExistenteImpedeDuplicacao() {
        StockFixture stock = stock(5);
        stock.definirNivelMinimo(10);
        whenStock(stock);
        when(alertaStockRepository.existsByProdutoIdAndResolvidoFalse(stock.getProduto().getId())).thenReturn(true);

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), 0);

        verify(alertaStockRepository, never()).save(any());
    }

    @Test
    void alertaNovoFicaAssociadoAGestorEGerenteDaLoja() {
        StockFixture stock = stock(5);
        stock.definirNivelMinimo(10);
        Utilizador gestor = utilizador(stock.getLoja(), "GESTOR");
        Utilizador gerente = utilizador(stock.getLoja(), "GERENTE");

        whenStock(stock);
        when(alertaStockRepository.existsByProdutoIdAndResolvidoFalse(stock.getProduto().getId())).thenReturn(false);
        when(alertaStockRepository.save(any(AlertaStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), 0);

        ArgumentCaptor<AlertaStock> captor = forClass(AlertaStock.class);
        verify(alertaStockRepository).save(captor.capture());
        assertEquals(stock.getProduto(), captor.getValue().getProduto());
        assertEquals(List.of(), captor.getValue().getDestinatarios());
    }

    @Test
    void iniciarInventarioFisicoComInventarioAbertoFalha() {
        Loja loja = loja();

        when(inventarioFisicoRepository.existsByLojaIdAndFechadoFalse(loja.getId())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> facade.iniciarInventarioFisico(loja.getId(), utilizador(loja).getId()));

        assertEquals("INVENTARIO_JA_ABERTO", exception.getCode());
    }

    @Test
    void registarContagemLinhaEmInventarioFechadoFalha() {
        Loja loja = loja();
        InventarioFisico inventario = new InventarioFisico(loja, utilizador(loja));
        inventario.fechar();

        when(inventarioFisicoRepository.findById(inventario.getId())).thenReturn(Optional.of(inventario));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> facade.registarContagemLinha(inventario.getId(), produto().getId(), 1));

        assertEquals("INVENTARIO_FECHADO", exception.getCode());
    }

    @Test
    void registarContagemLinhaCriaSnapshotECalculaDiscrepancia() {
        StockFixture stock = stock(8);
        InventarioFisico inventario = new InventarioFisico(stock.getLoja(), utilizador(stock.getLoja()));
        Produto produto = stock.getProduto();

        when(inventarioFisicoRepository.findById(inventario.getId())).thenReturn(Optional.of(inventario));
        whenStock(stock);
        when(linhaInventarioRepository.save(any(LinhaInventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LinhaInventario linha = facade.registarContagemLinha(inventario.getId(), produto.getId(), 6);

        assertEquals(8, linha.getQuantidadeSistema());
        assertEquals(-2, linha.getDiscrepancia());
    }

    @Test
    void fecharInventarioPreencheDataFechoEAtualizaStockParaQuantidadeContada() {
        StockFixture stock = stock(8);
        InventarioFisico inventario = new InventarioFisico(stock.getLoja(), utilizador(stock.getLoja()));
        new LinhaInventario(inventario, stock.getProduto(), 3, 8);

        when(inventarioFisicoRepository.findById(inventario.getId())).thenReturn(Optional.of(inventario));
        whenStock(stock);

        facade.fecharInventario(inventario.getId());

        assertTrue(inventario.isFechado());
        assertNotNull(inventario.getDataFecho());
        assertEquals(3, stock.getProduto().getQuantidadeStock());
        assertEquals(3, inventario.getLinhas().getFirst().getQuantidadeSistema());
        assertEquals(0, inventario.getLinhas().getFirst().getDiscrepancia());
        verify(auditoria).registar(TipoOperacao.INVENTARIO_FECHADO,
                inventario.getResponsavel().getId(), "INVENTARIO_FISICO", "Inventario fisico fechado");
    }

    @Test
    void registarAjusteComMotivoQuebraAtualizaStockEAudita() {
        StockFixture stock = stock(10);
        Utilizador utilizador = utilizador(stock.getLoja());

        whenStock(stock);
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));
        when(ajusteInventarioRepository.save(any(AjusteInventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AjusteInventario ajuste = facade.registarAjuste(stock.getProduto().getId(), stock.getLoja().getId(), -2, "QUEBRA", utilizador.getId());

        assertEquals(8, stock.getProduto().getQuantidadeStock());
        assertEquals(-2, ajuste.getQuantidade());
        assertEquals("QUEBRA", ajuste.getMotivoAjuste().getCodigo());
        verify(auditoria).registar(TipoOperacao.AJUSTE_STOCK, utilizador.getId(), "STOCK", "Ajuste de stock registado");
    }

    @Test
    void registarAjustePositivoAumentaStock() {
        StockFixture stock = stock(10);
        Utilizador utilizador = utilizador(stock.getLoja());

        whenStock(stock);
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));
        when(ajusteInventarioRepository.save(any(AjusteInventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AjusteInventario ajuste = facade.registarAjuste(stock.getProduto().getId(), stock.getLoja().getId(), 4,
                "CORRECAO_ERRO", utilizador.getId());

        assertEquals(14, stock.getProduto().getQuantidadeStock());
        assertEquals(4, ajuste.getQuantidade());
    }

    @Test
    void registarAjusteNegativoNaoPermiteStockNegativo() {
        StockFixture stock = stock(3);
        Utilizador utilizador = utilizador(stock.getLoja());

        whenStock(stock);
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));

        assertThrows(StockInsuficienteException.class,
                () -> facade.registarAjuste(stock.getProduto().getId(), stock.getLoja().getId(), -4, "QUEBRA", utilizador.getId()));
        verify(ajusteInventarioRepository, never()).save(any());
    }

    @Test
    void getAlertasAtivosRetornaApenasRepositorioOrdenado() {
        Loja loja = loja();
        StockFixture stock = new StockFixture(produto(), loja, 5);
        AlertaStock recente = new AlertaStock(stock.getProduto(), stock.getLoja(), 5);
        AlertaStock antigo = new AlertaStock(stock.getProduto(), stock.getLoja(), 7);
        when(alertaStockRepository.findAtivosByLojaId(loja.getId()))
                .thenReturn(List.of(recente, antigo));

        List<AlertaStock> alertas = facade.getAlertasAtivos(loja.getId());

        assertEquals(List.of(recente, antigo), alertas);
        assertFalse(alertas.get(0).isLido());
    }

    @Test
    void getAlertasAtivosCriaAlertaQuandoStockJaEstaAbaixoDoMinimo() {
        Loja loja = loja();
        Produto produto = produto();
        produto.definirStockInicial(5);
        produto.definirNivelMinimo(10);
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(produtoRepository.findAll()).thenReturn(List.of(produto));
        when(alertaStockRepository.existsByProdutoIdAndResolvidoFalse(produto.getId())).thenReturn(false);
        when(alertaStockRepository.existsByProdutoIdAndResolvidoTrue(produto.getId())).thenReturn(false);
        when(alertaStockRepository.findAtivosByLojaId(loja.getId())).thenReturn(List.of());

        facade.getAlertasAtivos(loja.getId());

        verify(alertaStockRepository).save(any(AlertaStock.class));
    }

    @Test
    void resolverAlertaFechaCicloDeVida() {
        StockFixture stock = stock(4);
        AlertaStock alerta = new AlertaStock(stock.getProduto(), stock.getLoja(), 4);
        when(alertaStockRepository.findById(alerta.getId())).thenReturn(Optional.of(alerta));
        when(alertaStockRepository.save(alerta)).thenReturn(alerta);

        AlertaStock resolvido = facade.resolverAlerta(alerta.getId());

        assertTrue(resolvido.isResolvido());
        assertTrue(resolvido.isLido());
        assertNotNull(resolvido.getDataResolucao());
    }

    @Test
    void iniciarInventarioFisicoCriaLinhasParaTodoOStockDaLoja() {
        Loja loja = loja();
        Utilizador utilizador = utilizador(loja);
        StockFixture agua = new StockFixture(produto("Agua"), loja, 8);
        StockFixture pao = new StockFixture(produto("Pao"), loja, 4);
        agua.getProduto().definirStockInicial(8);
        pao.getProduto().definirStockInicial(4);

        when(inventarioFisicoRepository.existsByLojaIdAndFechadoFalse(loja.getId())).thenReturn(false);
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(produtoRepository.findAll()).thenReturn(List.of(agua.getProduto(), pao.getProduto()));
        when(inventarioFisicoRepository.save(any(InventarioFisico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventarioFisico inventario = facade.iniciarInventarioFisico(loja.getId(), utilizador.getId());

        assertEquals(2, inventario.getLinhas().size());
        assertEquals(2, inventario.getTotalDiscrepancias());
        assertEquals(List.of(agua.getProduto(), pao.getProduto()),
                inventario.getLinhas().stream().map(LinhaInventario::getProduto).toList());
    }

    @Test
    void registarContagemLinhaAtualizaLinhaExistenteDoInventario() {
        StockFixture stock = stock(8);
        InventarioFisico inventario = new InventarioFisico(stock.getLoja(), utilizador(stock.getLoja()));
        LinhaInventario linha = new LinhaInventario(inventario, stock.getProduto(), 0, 8);

        when(inventarioFisicoRepository.findById(inventario.getId())).thenReturn(Optional.of(inventario));
        whenStock(stock);
        when(linhaInventarioRepository.findByInventarioIdAndProdutoId(inventario.getId(), stock.getProduto().getId()))
                .thenReturn(Optional.of(linha));
        when(linhaInventarioRepository.save(linha)).thenReturn(linha);

        LinhaInventario atualizada = facade.registarContagemLinha(inventario.getId(), stock.getProduto().getId(), 6);

        assertEquals(6, atualizada.getQuantidadeContada());
        assertEquals(-2, atualizada.getDiscrepancia());
    }

    private void whenStock(StockFixture stock) {
        stock.getProduto().definirStockInicial(stock.getQuantidade());
        if (stock.getNivelMinimo() != null) {
            stock.getProduto().definirNivelMinimo(stock.getNivelMinimo());
        }
        when(produtoRepository.findById(stock.getProduto().getId())).thenReturn(Optional.of(stock.getProduto()));
        when(lojaRepository.findById(stock.getLoja().getId())).thenReturn(Optional.of(stock.getLoja()));
    }

    private StockFixture stock(int quantidade) {
        StockFixture stock = new StockFixture(produto(), loja(), quantidade);
        stock.getProduto().definirStockInicial(quantidade);
        return stock;
    }

    private Produto produto() {
        return produto("Produto");
    }

    private Produto produto(String nome) {
        return new Produto("5600000000999", nome, new BigDecimal("1.00"), new BigDecimal("0.40"),
                new TaxaIVA("Normal", new BigDecimal("23")), new Categoria("Categoria Stock", "Stock"));
    }

    private Loja loja() {
        return new Loja("Loja Stock", "Rua Stock", "111222333");
    }

    private Utilizador utilizador(Loja loja) {
        return utilizador(loja, "GERENTE");
    }

    private Utilizador utilizador(Loja loja, String perfil) {
        return new Utilizador("gerente.stock", "hash", "Gerente Stock",
                PerfilUtilizador.valueOf(perfil), loja);
    }

    private static class StockFixture {
        private final Produto produto;
        private final Loja loja;
        private final int quantidade;
        private Integer nivelMinimo;

        StockFixture(Produto produto, Loja loja, int quantidade) {
            this.produto = produto;
            this.loja = loja;
            this.quantidade = quantidade;
        }

        void definirNivelMinimo(int nivelMinimo) {
            this.nivelMinimo = nivelMinimo;
            this.produto.definirNivelMinimo(nivelMinimo);
        }

        Produto getProduto() {
            return produto;
        }

        Loja getLoja() {
            return loja;
        }

        int getQuantidade() {
            return quantidade;
        }

        Integer getNivelMinimo() {
            return nivelMinimo;
        }
    }
}
