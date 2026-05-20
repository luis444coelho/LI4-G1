package pt.miniFormiga.subsistemas.stock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.MotivoAjuste;
import pt.miniFormiga.domain.NivelMinimo;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.Stock;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.repository.AlertaStockRepository;
import pt.miniFormiga.repository.InventarioFisicoRepository;
import pt.miniFormiga.repository.LinhaInventarioRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.MotivoAjusteRepository;
import pt.miniFormiga.repository.NivelMinimoRepository;
import pt.miniFormiga.repository.ProdutoRepository;
import pt.miniFormiga.repository.StockRepository;
import pt.miniFormiga.repository.UtilizadorRepository;

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

class StockFacadeTest {

    private StockRepository stockRepository;
    private NivelMinimoRepository nivelMinimoRepository;
    private AlertaStockRepository alertaStockRepository;
    private AjusteInventarioRepository ajusteInventarioRepository;
    private MotivoAjusteRepository motivoAjusteRepository;
    private InventarioFisicoRepository inventarioFisicoRepository;
    private LinhaInventarioRepository linhaInventarioRepository;
    private LojaRepository lojaRepository;
    private ProdutoRepository produtoRepository;
    private UtilizadorRepository utilizadorRepository;
    private AuditoriaService auditoria;
    private StockFacade facade;

    @BeforeEach
    void setUp() {
        stockRepository = mock(StockRepository.class);
        nivelMinimoRepository = mock(NivelMinimoRepository.class);
        alertaStockRepository = mock(AlertaStockRepository.class);
        ajusteInventarioRepository = mock(AjusteInventarioRepository.class);
        motivoAjusteRepository = mock(MotivoAjusteRepository.class);
        inventarioFisicoRepository = mock(InventarioFisicoRepository.class);
        linhaInventarioRepository = mock(LinhaInventarioRepository.class);
        lojaRepository = mock(LojaRepository.class);
        produtoRepository = mock(ProdutoRepository.class);
        utilizadorRepository = mock(UtilizadorRepository.class);
        auditoria = mock(AuditoriaService.class);
        facade = new StockFacade(
                stockRepository,
                nivelMinimoRepository,
                alertaStockRepository,
                ajusteInventarioRepository,
                motivoAjusteRepository,
                inventarioFisicoRepository,
                linhaInventarioRepository,
                lojaRepository,
                produtoRepository,
                utilizadorRepository,
                auditoria
        );
    }

    @Test
    void atualizarStockComDeltaNegativoSuficienteAtualizaQuantidade() {
        Stock stock = stock(10);
        whenStock(stock);
        when(alertaStockRepository.existsByStockIdAndResolvidoFalse(stock.getId())).thenReturn(false);

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), -4);

        assertEquals(6, stock.getQuantidade());
        verify(alertaStockRepository, never()).save(any());
    }

    @Test
    void atualizarStockComDeltaNegativoInsuficienteFalhaComValoresCorretos() {
        Stock stock = stock(2);
        whenStock(stock);

        StockInsuficienteException exception = assertThrows(StockInsuficienteException.class,
                () -> facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), -3));

        assertEquals(stock.getProduto().getId(), exception.getDetails().get("produtoId"));
        assertEquals(2, exception.getDetails().get("quantidadeDisponivel"));
        assertEquals(3, exception.getDetails().get("quantidadeSolicitada"));
    }

    @Test
    void atualizarStockComDeltaPositivoIncrementaQuantidade() {
        Stock stock = stock(2);
        whenStock(stock);

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), 5);

        assertEquals(7, stock.getQuantidade());
    }

    @Test
    void atualizarStockAbaixoDoNivelMinimoCriaAlerta() {
        Stock stock = stock(12);
        new NivelMinimo(stock, 10);
        whenStock(stock);
        when(alertaStockRepository.existsByStockIdAndResolvidoFalse(stock.getId())).thenReturn(false);
        when(alertaStockRepository.save(any(AlertaStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), -2);

        assertEquals(10, stock.getQuantidade());
        verify(alertaStockRepository).save(any(AlertaStock.class));
    }

    @Test
    void atualizarStockSemNivelMinimoNaoCriaAlerta() {
        Stock stock = stock(12);
        whenStock(stock);

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), -10);

        assertEquals(2, stock.getQuantidade());
        verify(alertaStockRepository, never()).save(any());
    }

    @Test
    void definirNivelMinimoComStockJaAbaixoEmiteAlertaImediatamente() {
        Stock stock = stock(5);
        whenStock(stock);
        when(alertaStockRepository.existsByStockIdAndResolvidoFalse(stock.getId())).thenReturn(false);
        when(nivelMinimoRepository.findByStockId(stock.getId())).thenReturn(Optional.empty());
        when(nivelMinimoRepository.save(any(NivelMinimo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.definirNivelMinimo(stock.getProduto().getId(), stock.getLoja().getId(), 10);

        assertEquals(10, stock.getNivelMinimo().getQuantidade());
        verify(alertaStockRepository).save(any(AlertaStock.class));
    }

    @Test
    void definirNivelMinimoAbaixoDoStockNaoCriaAlerta() {
        Stock stock = stock(12);
        whenStock(stock);
        when(nivelMinimoRepository.findByStockId(stock.getId())).thenReturn(Optional.empty());
        when(nivelMinimoRepository.save(any(NivelMinimo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.definirNivelMinimo(stock.getProduto().getId(), stock.getLoja().getId(), 10);

        assertEquals(10, stock.getNivelMinimo().getQuantidade());
        verify(alertaStockRepository, never()).save(any());
    }

    @Test
    void alertaAtivoExistenteImpedeDuplicacao() {
        Stock stock = stock(5);
        new NivelMinimo(stock, 10);
        whenStock(stock);
        when(alertaStockRepository.existsByStockIdAndResolvidoFalse(stock.getId())).thenReturn(true);

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), 0);

        verify(alertaStockRepository, never()).save(any());
    }

    @Test
    void alertaNovoFicaAssociadoAGestorEGerenteDaLoja() {
        Stock stock = stock(5);
        new NivelMinimo(stock, 10);
        Utilizador gestor = utilizador(stock.getLoja(), "GESTOR");
        Utilizador gerente = utilizador(stock.getLoja(), "GERENTE");

        whenStock(stock);
        when(alertaStockRepository.existsByStockIdAndResolvidoFalse(stock.getId())).thenReturn(false);
        when(utilizadorRepository.findByAtivoTrueAndPerfilNomeIn(List.of("GESTOR"))).thenReturn(List.of(gestor));
        when(utilizadorRepository.findByAtivoTrueAndLojaIdAndPerfilNomeIn(stock.getLoja().getId(), List.of("GERENTE")))
                .thenReturn(List.of(gerente));
        when(alertaStockRepository.save(any(AlertaStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.atualizarStock(stock.getProduto().getId(), stock.getLoja().getId(), 0);

        ArgumentCaptor<AlertaStock> captor = forClass(AlertaStock.class);
        verify(alertaStockRepository).save(captor.capture());
        assertEquals(List.of(gestor, gerente), captor.getValue().getDestinatarios());
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
        Stock stock = stock(8);
        InventarioFisico inventario = new InventarioFisico(stock.getLoja(), utilizador(stock.getLoja()));
        Produto produto = stock.getProduto();

        when(inventarioFisicoRepository.findById(inventario.getId())).thenReturn(Optional.of(inventario));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        whenStock(stock);
        when(linhaInventarioRepository.save(any(LinhaInventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LinhaInventario linha = facade.registarContagemLinha(inventario.getId(), produto.getId(), 6);

        assertEquals(8, linha.getQuantidadeSistema());
        assertEquals(-2, linha.getDiscrepancia());
    }

    @Test
    void fecharInventarioPreencheDataFechoENaoAlteraStock() {
        Stock stock = stock(8);
        InventarioFisico inventario = new InventarioFisico(stock.getLoja(), utilizador(stock.getLoja()));
        new LinhaInventario(inventario, stock.getProduto(), 3, 8);

        when(inventarioFisicoRepository.findById(inventario.getId())).thenReturn(Optional.of(inventario));

        facade.fecharInventario(inventario.getId());

        assertTrue(inventario.isFechado());
        assertNotNull(inventario.getDataFecho());
        assertEquals(8, stock.getQuantidade());
        verify(auditoria).registar(TipoOperacao.INVENTARIO_FECHADO,
                inventario.getResponsavel().getId(), "INVENTARIO_FISICO", "Inventario fisico fechado");
    }

    @Test
    void registarAjusteComMotivoQuebraAtualizaStockEAudita() {
        Stock stock = stock(10);
        Utilizador utilizador = utilizador(stock.getLoja());
        MotivoAjuste motivo = new MotivoAjuste("QUEBRA", "Produto danificado ou partido");

        whenStock(stock);
        when(motivoAjusteRepository.findByCodigo("QUEBRA")).thenReturn(Optional.of(motivo));
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));
        when(ajusteInventarioRepository.save(any(AjusteInventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AjusteInventario ajuste = facade.registarAjuste(stock.getProduto().getId(), stock.getLoja().getId(), -2, "QUEBRA", utilizador.getId());

        assertEquals(8, stock.getQuantidade());
        assertEquals(-2, ajuste.getQuantidade());
        assertEquals("QUEBRA", ajuste.getMotivoAjuste().getCodigo());
        verify(auditoria).registar(TipoOperacao.AJUSTE_STOCK, utilizador.getId(), "STOCK", "Ajuste de stock registado");
    }

    @Test
    void registarAjustePositivoAumentaStock() {
        Stock stock = stock(10);
        Utilizador utilizador = utilizador(stock.getLoja());
        MotivoAjuste motivo = new MotivoAjuste("CORRECAO_ERRO", "Correcao de erro de registo");

        whenStock(stock);
        when(motivoAjusteRepository.findByCodigo("CORRECAO_ERRO")).thenReturn(Optional.of(motivo));
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));
        when(ajusteInventarioRepository.save(any(AjusteInventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AjusteInventario ajuste = facade.registarAjuste(stock.getProduto().getId(), stock.getLoja().getId(), 4,
                "CORRECAO_ERRO", utilizador.getId());

        assertEquals(14, stock.getQuantidade());
        assertEquals(4, ajuste.getQuantidade());
    }

    @Test
    void registarAjusteNegativoNaoPermiteStockNegativo() {
        Stock stock = stock(3);
        Utilizador utilizador = utilizador(stock.getLoja());
        MotivoAjuste motivo = new MotivoAjuste("QUEBRA", "Produto danificado ou partido");

        whenStock(stock);
        when(motivoAjusteRepository.findByCodigo("QUEBRA")).thenReturn(Optional.of(motivo));
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));

        assertThrows(StockInsuficienteException.class,
                () -> facade.registarAjuste(stock.getProduto().getId(), stock.getLoja().getId(), -4, "QUEBRA", utilizador.getId()));
        verify(ajusteInventarioRepository, never()).save(any());
    }

    @Test
    void getAlertasAtivosRetornaApenasRepositorioOrdenado() {
        Loja loja = loja();
        Stock stock = new Stock(produto(), loja, 5);
        AlertaStock recente = new AlertaStock(stock, 5);
        AlertaStock antigo = new AlertaStock(stock, 7);
        when(alertaStockRepository.findByStockLojaIdAndResolvidoFalseOrderByDataHoraDesc(loja.getId()))
                .thenReturn(List.of(recente, antigo));

        List<AlertaStock> alertas = facade.getAlertasAtivos(loja.getId());

        assertEquals(List.of(recente, antigo), alertas);
        assertFalse(alertas.get(0).isLido());
    }

    @Test
    void resolverAlertaFechaCicloDeVida() {
        AlertaStock alerta = new AlertaStock(stock(4), 4);
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
        Stock agua = new Stock(produto("Agua"), loja, 8);
        Stock pao = new Stock(produto("Pao"), loja, 4);

        when(inventarioFisicoRepository.existsByLojaIdAndFechadoFalse(loja.getId())).thenReturn(false);
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));
        when(stockRepository.findByLojaId(loja.getId())).thenReturn(List.of(agua, pao));
        when(inventarioFisicoRepository.save(any(InventarioFisico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventarioFisico inventario = facade.iniciarInventarioFisico(loja.getId(), utilizador.getId());

        assertEquals(2, inventario.getLinhas().size());
        assertEquals(2, inventario.getTotalDiscrepancias());
        assertEquals(List.of(agua.getProduto(), pao.getProduto()),
                inventario.getLinhas().stream().map(LinhaInventario::getProduto).toList());
    }

    @Test
    void registarContagemLinhaAtualizaLinhaExistenteDoInventario() {
        Stock stock = stock(8);
        InventarioFisico inventario = new InventarioFisico(stock.getLoja(), utilizador(stock.getLoja()));
        LinhaInventario linha = new LinhaInventario(inventario, stock.getProduto(), 0, 8);

        when(inventarioFisicoRepository.findById(inventario.getId())).thenReturn(Optional.of(inventario));
        when(produtoRepository.findById(stock.getProduto().getId())).thenReturn(Optional.of(stock.getProduto()));
        whenStock(stock);
        when(linhaInventarioRepository.findByInventarioIdAndProdutoId(inventario.getId(), stock.getProduto().getId()))
                .thenReturn(Optional.of(linha));
        when(linhaInventarioRepository.save(linha)).thenReturn(linha);

        LinhaInventario atualizada = facade.registarContagemLinha(inventario.getId(), stock.getProduto().getId(), 6);

        assertEquals(6, atualizada.getQuantidadeContada());
        assertEquals(-2, atualizada.getDiscrepancia());
    }

    private void whenStock(Stock stock) {
        when(stockRepository.findByProdutoIdAndLojaId(stock.getProduto().getId(), stock.getLoja().getId()))
                .thenReturn(Optional.of(stock));
    }

    private Stock stock(int quantidade) {
        return new Stock(produto(), loja(), quantidade);
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
                new Perfil(perfil, List.of("STOCK_WRITE")), loja);
    }
}
