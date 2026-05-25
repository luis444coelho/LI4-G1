package pt.miniFormiga.subsistemas.relatorios;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.EstadoSincronizacaoCodigo;
import pt.miniFormiga.domain.LinhaVenda;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.MeioPagamento;
import pt.miniFormiga.domain.NivelMinimo;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.Stock;
import pt.miniFormiga.domain.Sincronizacao;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.domain.Venda;
import pt.miniFormiga.repository.AlertaStockRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.SincronizacaoRepository;
import pt.miniFormiga.repository.VendaRepository;
import pt.miniFormiga.subsistemas.stock.StockStore;
import pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoPayload;
import pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.VendaRelatorioSync;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pt.miniFormiga.subsistemas.relatorios.RelatoriosDtos.*;

class RelatoriosFacadeTest {

    private VendaRepository vendaRepository;
    private AlertaStockRepository alertaStockRepository;
    private LojaRepository lojaRepository;
    private StockStore stockStore;
    private SincronizacaoRepository sincronizacaoRepository;
    private RelatoriosFacade facade;

    private Loja loja;
    private Utilizador operador;
    private Produto produto;

    @BeforeEach
    void setUp() {
        vendaRepository = mock(VendaRepository.class);
        alertaStockRepository = mock(AlertaStockRepository.class);
        lojaRepository = mock(LojaRepository.class);
        stockStore = mock(StockStore.class);
        sincronizacaoRepository = mock(SincronizacaoRepository.class);
        facade = new RelatoriosFacade(
                vendaRepository,
                alertaStockRepository,
                lojaRepository,
                stockStore,
                sincronizacaoRepository,
                new ObjectMapper().findAndRegisterModules()
        );

        loja = new Loja("Loja Braga", "Rua Central", "123456789");
        operador = new Utilizador("operador", "hash", "Operador",
                new Perfil("FUNCIONARIO", List.of("PDV_WRITE")), loja);
        produto = new Produto("5600000000011", "Agua", new BigDecimal("2.00"), new BigDecimal("0.75"),
                new TaxaIVA("Taxa Normal", new BigDecimal("23")), new Categoria("Bebidas", "Bebidas frias"));
    }

    @Test
    void dashboardDevolveKpisComVendasMargemLojasEAlertas() {
        Venda venda = vendaFinalizada(2, LocalDateTime.of(2026, 5, 10, 12, 0));
        AlertaStock alerta = alertaStock();

        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of(venda));
        when(lojaRepository.findAll()).thenReturn(List.of(loja));
        when(alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc()).thenReturn(List.of(alerta));

        DashboardResponse response = facade.obterDashboard(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        assertEquals(new BigDecimal("4.92"), response.totalVendas());
        assertEquals(new BigDecimal("0.92"), response.totalIva());
        assertEquals(new BigDecimal("2.50"), response.margem());
        assertEquals(1, response.numeroVendas());
        assertEquals(1, response.numeroLojasComVendas());
        assertEquals(1, response.totalLojas());
        assertEquals(1, response.alertasAtivos());
    }

    @Test
    void filtroPorPeriodoAlteraResultados() {
        Venda vendaMaio = vendaFinalizada(1, LocalDateTime.of(2026, 5, 10, 12, 0));
        LocalDateTime inicioMaio = LocalDate.of(2026, 5, 1).atStartOfDay();
        LocalDateTime fimMaio = LocalDate.of(2026, 6, 1).atStartOfDay();
        LocalDateTime inicioJunho = LocalDate.of(2026, 6, 1).atStartOfDay();
        LocalDateTime fimJunho = LocalDate.of(2026, 7, 1).atStartOfDay();

        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(inicioMaio, fimMaio))
                .thenReturn(List.of(vendaMaio));
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(inicioJunho, fimJunho))
                .thenReturn(List.of());

        RelatorioVendasResponse maio = facade.relatorioVendas(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        RelatorioVendasResponse junho = facade.relatorioVendas(
                new RelatorioFiltro(null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null));

        assertEquals(new BigDecimal("2.46"), maio.totalComIva());
        assertEquals(new BigDecimal("0.00"), junho.totalComIva());
    }

    @Test
    void relatorioDeRentabilidadeCalculaMargemCorretamente() {
        Venda venda = vendaFinalizada(2, LocalDateTime.of(2026, 5, 10, 12, 0));
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of(venda));

        RelatorioRentabilidadeResponse response = facade.relatorioRentabilidade(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        assertEquals(new BigDecimal("4.00"), response.receitaSemIva());
        assertEquals(new BigDecimal("1.50"), response.custoTotal());
        assertEquals(new BigDecimal("2.50"), response.margemTotal());
        assertEquals(new BigDecimal("62.50"), response.margemPercentagem());
        assertEquals(2, response.produtos().get(0).quantidadeVendida());
    }

    @Test
    void exportacaoCsvContemColunasContabilisticasEsperadas() {
        Venda venda = vendaFinalizada(1, LocalDateTime.of(2026, 5, 10, 12, 0));
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of(venda));

        ExportacaoRelatorio exportacao = facade.exportar(new ExportarRelatorioRequest(
                "vendas", "csv", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        String csv = new String(exportacao.conteudo(), StandardCharsets.UTF_8);
        assertEquals("mini-formiga-vendas.csv", exportacao.nomeFicheiro());
        assertTrue(csv.startsWith("data,descricao,valor,iva,loja"));
        assertTrue(csv.contains("Venda " + venda.getId()));
        assertTrue(csv.contains("Loja Braga"));
    }

    @Test
    void exportacaoPdfVendasIncluiLinhasDoRelatorioFiltrado() {
        Venda venda = vendaFinalizada(1, LocalDateTime.of(2026, 5, 10, 12, 0));
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of(venda));

        ExportacaoRelatorio exportacao = facade.exportar(new ExportarRelatorioRequest(
                "vendas", "pdf", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        String pdf = new String(exportacao.conteudo(), StandardCharsets.UTF_8);
        assertEquals("mini-formiga-vendas.pdf", exportacao.nomeFicheiro());
        assertEquals("application/pdf", exportacao.mediaType());
        assertTrue(pdf.startsWith("%PDF-1.4"));
        assertTrue(pdf.contains("Mini-Formiga - Relatorio de vendas"));
        assertTrue(pdf.contains("Agua"));
        assertTrue(pdf.contains("Loja Braga"));
    }

    @Test
    void relatorioVendasFiltraPorProdutoETurno() {
        Produto sumo = new Produto("5600000000028", "Sumo", new BigDecimal("3.00"), new BigDecimal("1.20"),
                new TaxaIVA("Taxa Normal", new BigDecimal("23")), new Categoria("Bebidas", "Bebidas frias"));
        Venda vendaAguaManha = vendaFinalizada(produto, 1, LocalDateTime.of(2026, 5, 10, 9, 0));
        Venda vendaAguaTarde = vendaFinalizada(produto, 1, LocalDateTime.of(2026, 5, 10, 16, 0));
        Venda vendaSumoManha = vendaFinalizada(sumo, 1, LocalDateTime.of(2026, 5, 10, 10, 0));
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of(vendaAguaManha, vendaAguaTarde, vendaSumoManha));

        RelatorioVendasResponse response = facade.relatorioVendas(new RelatorioFiltro(
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                null,
                produto.getId(),
                "MANHA"
        ));

        assertEquals(produto.getId(), response.produtoId());
        assertEquals("MANHA", response.turno());
        assertEquals(new BigDecimal("2.46"), response.totalComIva());
        assertEquals(1, response.numeroVendas());
        assertEquals(1, response.linhas().size());
        assertEquals(produto.getId(), response.linhas().get(0).produtoId());
    }

    @Test
    void exportacaoXlsxGeraFicheiroExcelComMediaTypeCorreto() {
        Venda venda = vendaFinalizada(1, LocalDateTime.of(2026, 5, 10, 12, 0));
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of(venda));

        ExportacaoRelatorio exportacao = facade.exportar(new ExportarRelatorioRequest(
                "vendas", "xlsx", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        assertEquals("mini-formiga-vendas.xlsx", exportacao.nomeFicheiro());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", exportacao.mediaType());
        assertEquals('P', (char) exportacao.conteudo()[0]);
        assertEquals('K', (char) exportacao.conteudo()[1]);
    }

    @Test
    void relatoriosCentraisUsamLinhasSincronizadasComIvaECusto() throws Exception {
        VendaRelatorioSync linha = new VendaRelatorioSync(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 5, 10, 12, 0),
                loja.getId(),
                loja.getNome(),
                produto.getId(),
                produto.getNome(),
                produto.getCategoria().getId(),
                produto.getCategoria().getNome(),
                2,
                new BigDecimal("4.00"),
                new BigDecimal("0.92"),
                new BigDecimal("4.92"),
                new BigDecimal("1.50"),
                new BigDecimal("2.50")
        );
        SincronizacaoPayload payload = new SincronizacaoPayload(
                loja.getId(),
                LocalDateTime.of(2026, 5, 10, 13, 0),
                null,
                Map.of(),
                null,
                List.of(linha),
                List.of()
        );
        Sincronizacao sync = new Sincronizacao(loja, EstadoSincronizacaoCodigo.CONCLUIDA);
        sync.concluir(EstadoSincronizacaoCodigo.CONCLUIDA,
                new ObjectMapper().findAndRegisterModules().writeValueAsString(payload),
                1,
                "[]",
                0);
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of());
        when(sincronizacaoRepository.findByEstadoInOrderByDataHoraFimDesc(any()))
                .thenReturn(List.of(sync));

        RelatorioVendasResponse vendas = facade.relatorioVendas(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        RelatorioRentabilidadeResponse rentabilidade = facade.relatorioRentabilidade(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        ExportacaoRelatorio pdf = facade.exportar(new ExportarRelatorioRequest(
                "vendas", "pdf", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        String conteudoPdf = new String(pdf.conteudo(), StandardCharsets.UTF_8);

        assertEquals(new BigDecimal("0.92"), vendas.totalIva());
        assertEquals(new BigDecimal("4.92"), vendas.totalComIva());
        assertEquals(new BigDecimal("1.50"), rentabilidade.custoTotal());
        assertEquals(new BigDecimal("2.50"), rentabilidade.margemTotal());
        assertEquals(1, vendas.numeroVendas());
        assertTrue(conteudoPdf.contains("Agua"));
        assertTrue(!conteudoPdf.contains("Sem vendas no periodo selecionado."));
    }

    @Test
    void dashboardCentralAgregaUltimaSincronizacaoDeCadaLoja() throws Exception {
        Loja lojaPorto = new Loja(UUID.randomUUID(), "Loja Porto", "Rua Porto", "223456789", "222000000");
        Loja lojaTecnicaAntiga = new Loja(UUID.randomUUID(), "Loja Tecnica", "Rua Sync", "923456789", "900000000");
        VendaRelatorioSync linhaBraga = vendaSync(loja.getId(), loja.getNome(), new BigDecimal("4.92"));
        VendaRelatorioSync linhaPorto = vendaSync(lojaPorto.getId(), lojaPorto.getNome(), new BigDecimal("2.46"));

        Sincronizacao syncPorto = syncComPayload(lojaPorto, linhaPorto, LocalDateTime.of(2026, 5, 10, 15, 0));
        Sincronizacao syncBraga = syncComPayload(loja, linhaBraga, LocalDateTime.of(2026, 5, 10, 14, 0));

        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of());
        when(sincronizacaoRepository.findByEstadoInOrderByDataHoraFimDesc(any()))
                .thenReturn(List.of(syncPorto, syncBraga));
        when(lojaRepository.findAll()).thenReturn(List.of(loja, lojaPorto, lojaTecnicaAntiga));
        when(alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc()).thenReturn(List.of());

        DashboardResponse response = facade.obterDashboard(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        assertEquals(new BigDecimal("7.38"), response.totalVendas());
        assertEquals(2, response.numeroVendas());
        assertEquals(2, response.numeroLojasComVendas());
        assertEquals(2, response.totalLojas());
        assertEquals(2, response.vendasPorLoja().size());
        assertTrue(response.vendasPorLoja().stream().anyMatch(loja -> "Loja Braga".equals(loja.loja())));
        assertTrue(response.vendasPorLoja().stream().anyMatch(loja -> "Loja Porto".equals(loja.loja())));
    }

    @Test
    void dashboardCalculaMargemComLinhasRecarregadasDaPersistencia() {
        Venda venda = vendaFinalizada(2, LocalDateTime.of(2026, 5, 10, 12, 0));
        venda.getLinhas().forEach(linha -> org.springframework.test.util.ReflectionTestUtils.setField(linha, "totalLinha", BigDecimal.ZERO));
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of(venda));
        when(lojaRepository.findAll()).thenReturn(List.of(loja));
        when(alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc()).thenReturn(List.of());

        DashboardResponse response = facade.obterDashboard(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        assertEquals(new BigDecimal("4.92"), response.totalVendas());
        assertEquals(new BigDecimal("2.50"), response.margem());
    }

    @Test
    void exportacaoSemDadosDevolveRespostaControlada() {
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of());

        ExportacaoRelatorio exportacao = facade.exportar(new ExportarRelatorioRequest(
                "vendas", "csv", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        String csv = new String(exportacao.conteudo(), StandardCharsets.UTF_8);
        assertEquals("mini-formiga-vendas.csv", exportacao.nomeFicheiro());
        assertEquals("text/csv;charset=UTF-8", exportacao.mediaType());
        assertEquals("data,descricao,valor,iva,loja\n", csv);
    }

    private Venda vendaFinalizada(int quantidade, LocalDateTime dataHora) {
        return vendaFinalizada(produto, quantidade, dataHora);
    }

    private Venda vendaFinalizada(Produto produtoVenda, int quantidade, LocalDateTime dataHora) {
        Venda venda = new Venda(loja, operador);
        new LinhaVenda(venda, produtoVenda, quantidade);
        venda.finalizar(new MeioPagamento("NUMERARIO", "Numerario"));
        venda.calcularTotais();
        ReflectionTestUtils.setField(venda, "dataHora", dataHora);
        return venda;
    }

    private AlertaStock alertaStock() {
        Stock stock = new Stock(produto, loja, 5);
        new NivelMinimo(stock, 10);
        return new AlertaStock(stock, 5);
    }

    private VendaRelatorioSync vendaSync(UUID lojaId, String lojaNome, BigDecimal valorComIva) {
        BigDecimal valorSemIva = valorComIva.divide(new BigDecimal("1.23"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal iva = valorComIva.subtract(valorSemIva);
        BigDecimal custo = new BigDecimal("0.75");
        return new VendaRelatorioSync(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 5, 10, 12, 0),
                lojaId,
                lojaNome,
                produto.getId(),
                produto.getNome(),
                produto.getCategoria().getId(),
                produto.getCategoria().getNome(),
                1,
                valorSemIva,
                iva,
                valorComIva,
                custo,
                valorSemIva.subtract(custo)
        );
    }

    private Sincronizacao syncComPayload(Loja lojaSync, VendaRelatorioSync linha, LocalDateTime geradoEm) throws Exception {
        SincronizacaoPayload payload = new SincronizacaoPayload(
                lojaSync.getId(),
                geradoEm,
                null,
                Map.of(),
                null,
                List.of(linha),
                List.of()
        );
        Sincronizacao sync = new Sincronizacao(lojaSync, EstadoSincronizacaoCodigo.CONCLUIDA);
        sync.concluir(EstadoSincronizacaoCodigo.CONCLUIDA,
                new ObjectMapper().findAndRegisterModules().writeValueAsString(payload),
                1,
                "[]",
                0);
        return sync;
    }
}
