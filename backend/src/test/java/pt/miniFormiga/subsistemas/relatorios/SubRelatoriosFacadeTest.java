package pt.miniFormiga.subsistemas.relatorios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.LinhaVenda;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.MeioPagamento;
import pt.miniFormiga.domain.NivelMinimo;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.Stock;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.domain.Venda;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.repository.AlertaStockRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.VendaRepository;
import pt.miniFormiga.subsistemas.sincronizacao.IConsultaSincronizacao;
import pt.miniFormiga.subsistemas.sincronizacao.IConsultaSincronizacao.DadosRelatorioSincronizado;
import pt.miniFormiga.subsistemas.sincronizacao.IConsultaSincronizacao.VendaRelatorioSincronizada;
import pt.miniFormiga.subsistemas.stock.StockStore;
import pt.miniFormiga.subsistemas.stock.StockItem;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pt.miniFormiga.subsistemas.relatorios.RelatoriosDtos.*;

class SubRelatoriosFacadeTest {

    private VendaRepository vendaRepository;
    private AlertaStockRepository alertaStockRepository;
    private LojaRepository lojaRepository;
    private StockStore stockStore;
    private IConsultaSincronizacao consultaSincronizacao;
    private SubRelatoriosFacade facade;

    private Loja loja;
    private Utilizador operador;
    private Produto produto;

    @BeforeEach
    void setUp() {
        vendaRepository = mock(VendaRepository.class);
        alertaStockRepository = mock(AlertaStockRepository.class);
        lojaRepository = mock(LojaRepository.class);
        stockStore = mock(StockStore.class);
        consultaSincronizacao = mock(IConsultaSincronizacao.class);
        facade = new SubRelatoriosFacade(
                vendaRepository,
                alertaStockRepository,
                lojaRepository,
                stockStore,
                consultaSincronizacao
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
    void exportacaoPdfStockMostraNomeDaLojaEmVezDoIdentificador() {
        when(stockStore.listar(loja.getId())).thenReturn(List.of(
                new pt.miniFormiga.subsistemas.stock.StockItem(produto, loja.getId(), loja.getNome(), 5, 10, null, null, null, null)
        ));

        ExportacaoRelatorio exportacao = facade.exportar(new ExportarRelatorioRequest(
                "stock", "pdf", loja.getId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        String pdf = new String(exportacao.conteudo(), StandardCharsets.UTF_8);
        assertTrue(pdf.contains("Loja: Loja Braga"));
        assertTrue(!pdf.contains("Loja: " + loja.getId()));
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
        VendaRelatorioSincronizada linha = new VendaRelatorioSincronizada(
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
        DadosRelatorioSincronizado dados = new DadosRelatorioSincronizado(
                loja.getId(),
                null,
                List.of(linha)
        );
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of());
        when(consultaSincronizacao.dadosRelatorio(null)).thenReturn(List.of(dados));

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
        VendaRelatorioSincronizada linhaBraga = vendaSync(loja.getId(), loja.getNome(), new BigDecimal("4.92"));
        VendaRelatorioSincronizada linhaPorto = vendaSync(lojaPorto.getId(), lojaPorto.getNome(), new BigDecimal("2.46"));

        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of());
        when(consultaSincronizacao.dadosRelatorio(null)).thenReturn(List.of(
                new DadosRelatorioSincronizado(lojaPorto.getId(), null, List.of(linhaPorto)),
                new DadosRelatorioSincronizado(loja.getId(), null, List.of(linhaBraga))
        ));
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

    @Test
    void relatorioStockFiltraPorCategoriaProdutoEContaAlertasAtivos() {
        Produto sumo = new Produto("5600000000028", "Sumo", new BigDecimal("3.00"), new BigDecimal("1.20"),
                produto.getTaxaIVA(), produto.getCategoria());
        Produto champo = new Produto("5600000000035", "Champo", new BigDecimal("4.00"), new BigDecimal("2.00"),
                produto.getTaxaIVA(), new Categoria("Higiene", "Higiene pessoal"));
        when(stockStore.listar(loja.getId())).thenReturn(List.of(
                new StockItem(produto, loja.getId(), loja.getNome(), 5, 10, null, null, LocalDateTime.of(2026, 5, 10, 12, 0), null),
                new StockItem(sumo, loja.getId(), loja.getNome(), 7, 5, null, null, null, null),
                new StockItem(champo, loja.getId(), loja.getNome(), 2, 8, null, null, null, null)
        ));
        when(alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc())
                .thenReturn(List.of(alertaStock(), new AlertaStock(new Stock(champo, loja, 2), 2)));

        RelatorioStockResponse response = facade.relatorioStock(new RelatorioFiltro(
                loja.getId(),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                produto.getCategoria().getId(),
                produto.getId(),
                null
        ));

        assertEquals(1, response.totalProdutos());
        assertEquals(5, response.totalUnidades());
        assertEquals(1, response.produtosReposicao());
        assertEquals(1, response.alertasAtivos());
        assertEquals(new BigDecimal("3.75"), response.valorStockPrecoCusto());
        assertEquals("Agua", response.itens().get(0).produto());
    }

    @Test
    void relatorioStockResolveNomeDaLojaQuandoItemNaoTemNome() {
        when(stockStore.listar(loja.getId())).thenReturn(List.of(
                new StockItem(produto, loja.getId(), null, 3, null, null, null, null, null)
        ));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc()).thenReturn(List.of());

        RelatorioStockResponse response = facade.relatorioStock(new RelatorioFiltro(
                loja.getId(),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                null
        ));

        assertEquals("Loja Braga", response.itens().get(0).loja());
        assertEquals(new BigDecimal("2.25"), response.valorStockPrecoCusto());
    }

    @Test
    void relatoriosValidamPeriodoTurnoTipoEFormato() {
        BusinessException periodo = assertThrows(BusinessException.class,
                () -> facade.relatorioVendas(new RelatorioFiltro(
                        null,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 5, 31),
                        null
                )));
        BusinessException turno = assertThrows(BusinessException.class,
                () -> facade.relatorioVendas(new RelatorioFiltro(
                        null,
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31),
                        null,
                        null,
                        "madrugada"
                )));
        BusinessException tipo = assertThrows(BusinessException.class,
                () -> facade.exportar(new ExportarRelatorioRequest(
                        "financeiro", "csv", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null
                )));
        BusinessException formato = assertThrows(BusinessException.class,
                () -> facade.exportar(new ExportarRelatorioRequest(
                        "vendas", "xml", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null
                )));

        assertEquals("PERIODO_INVALIDO", periodo.getCode());
        assertEquals("RELATORIO_TURNO_INVALIDO", turno.getCode());
        assertEquals("RELATORIO_TIPO_INVALIDO", tipo.getCode());
        assertEquals("RELATORIO_FORMATO_INVALIDO", formato.getCode());
    }

    @Test
    void exportacoesDashboardStockERentabilidadeCobremCsvPdfEXlsx() {
        Venda venda = vendaFinalizada(1, LocalDateTime.of(2026, 5, 10, 12, 0));
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of(venda));
        when(lojaRepository.findAll()).thenReturn(List.of(loja));
        when(alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc()).thenReturn(List.of(alertaStock()));
        when(stockStore.listar(loja.getId())).thenReturn(List.of(
                new StockItem(produto, loja.getId(), loja.getNome(), 5, 10, null, null, null, null)
        ));

        ExportacaoRelatorio dashboardCsv = facade.exportar(new ExportarRelatorioRequest(
                "dashboard", "csv", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        ExportacaoRelatorio dashboardPdf = facade.exportar(new ExportarRelatorioRequest(
                "dashboard", "pdf", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        ExportacaoRelatorio dashboardXlsx = facade.exportar(new ExportarRelatorioRequest(
                "dashboard", "xlsx", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        ExportacaoRelatorio stockCsv = facade.exportar(new ExportarRelatorioRequest(
                "stock", "csv", loja.getId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        ExportacaoRelatorio stockXlsx = facade.exportar(new ExportarRelatorioRequest(
                "stock", "xlsx", loja.getId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        ExportacaoRelatorio rentabilidadeCsv = facade.exportar(new ExportarRelatorioRequest(
                "rentabilidade", "csv", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        ExportacaoRelatorio rentabilidadePdf = facade.exportar(new ExportarRelatorioRequest(
                "rentabilidade", "pdf", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        ExportacaoRelatorio rentabilidadeXlsx = facade.exportar(new ExportarRelatorioRequest(
                "rentabilidade", "xlsx", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        assertTrue(new String(dashboardCsv.conteudo(), StandardCharsets.UTF_8).contains("total_vendas,2.46"));
        assertTrue(new String(dashboardPdf.conteudo(), StandardCharsets.UTF_8).contains("Mini-Formiga - Dashboard"));
        assertEquals('P', (char) dashboardXlsx.conteudo()[0]);
        assertTrue(new String(stockCsv.conteudo(), StandardCharsets.UTF_8).contains("Agua,Bebidas,Loja Braga,5,10,true,3.75"));
        assertEquals('K', (char) stockXlsx.conteudo()[1]);
        assertTrue(new String(rentabilidadeCsv.conteudo(), StandardCharsets.UTF_8)
                .startsWith("tipo,nome,receita,custo,margem,margem_percentagem"));
        assertTrue(new String(rentabilidadePdf.conteudo(), StandardCharsets.UTF_8).contains("Mini-Formiga - Relatorio de rentabilidade"));
        assertEquals("mini-formiga-rentabilidade.xlsx", rentabilidadeXlsx.nomeFicheiro());
    }

    @Test
    void exportacaoCsvEscapaVirgulasAspasEQuebrasDeLinha() {
        Loja lojaComVirgula = new Loja("Loja \"Centro\", Braga", "Rua Central", "123456789");
        Produto produtoComVirgula = new Produto("5600000000042", "Agua,\nPremium", new BigDecimal("2.00"), new BigDecimal("0.75"),
                produto.getTaxaIVA(), produto.getCategoria());
        Utilizador operadorLocal = new Utilizador("operador.local", "hash", "Operador",
                new Perfil("FUNCIONARIO", List.of("PDV_WRITE")), lojaComVirgula);
        Venda venda = new Venda(lojaComVirgula, operadorLocal);
        new LinhaVenda(venda, produtoComVirgula, 1);
        venda.finalizar(new MeioPagamento("NUMERARIO", "Numerario"));
        venda.calcularTotais();
        ReflectionTestUtils.setField(venda, "dataHora", LocalDateTime.of(2026, 5, 10, 12, 0));
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of(venda));

        ExportacaoRelatorio exportacao = facade.exportar(new ExportarRelatorioRequest(
                "vendas", "csv", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        String csv = new String(exportacao.conteudo(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"Venda " + venda.getId() + " - Agua,\nPremium\""));
        assertTrue(csv.contains("\"Loja \"\"Centro\"\", Braga\""));
    }

    @Test
    void relatorioVendasComLojaUsaRepositorioEspecificoEFiltraTurnosTardeENoite() {
        Venda vendaAntesAbertura = vendaFinalizada(1, LocalDateTime.of(2026, 5, 10, 7, 29));
        Venda vendaTarde = vendaFinalizada(1, LocalDateTime.of(2026, 5, 10, 14, 0));
        Venda vendaNoite = vendaFinalizada(1, LocalDateTime.of(2026, 5, 10, 20, 1));
        when(vendaRepository.findByLojaIdAndAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(eq(loja.getId()), any(), any()))
                .thenReturn(List.of(vendaAntesAbertura, vendaTarde, vendaNoite));

        RelatorioVendasResponse tarde = facade.relatorioVendas(new RelatorioFiltro(
                loja.getId(),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                null,
                null,
                "tarde"
        ));
        RelatorioVendasResponse noite = facade.relatorioVendas(new RelatorioFiltro(
                loja.getId(),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                null,
                null,
                "NOITE"
        ));

        assertEquals(new BigDecimal("2.46"), tarde.totalComIva());
        assertEquals(new BigDecimal("4.92"), noite.totalComIva());
        assertEquals("TARDE", tarde.turno());
        assertEquals("NOITE", noite.turno());
        verify(vendaRepository, times(2)).findByLojaIdAndAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(eq(loja.getId()), any(), any());
    }

    @Test
    void payloadCentralComDashboardAgregadoAlimentaDashboardVendasERentabilidade() throws Exception {
        DashboardResponse dashboard = new DashboardResponse(
                new PeriodoResponse(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new BigDecimal("12.30"),
                new BigDecimal("2.30"),
                new BigDecimal("6.00"),
                3,
                1,
                1,
                0,
                new BigDecimal("4.10"),
                List.of(new VendasPorLojaResponse(loja.getId(), loja.getNome(), new BigDecimal("12.30"), new BigDecimal("2.30"), new BigDecimal("6.00"), 3))
        );
        DadosRelatorioSincronizado dados = new DadosRelatorioSincronizado(
                loja.getId(),
                dashboard,
                List.of()
        );
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of());
        when(consultaSincronizacao.dadosRelatorio(null)).thenReturn(List.of(dados));
        when(alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc()).thenReturn(List.of());

        DashboardResponse dashboardResponse = facade.obterDashboard(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        RelatorioVendasResponse vendas = facade.relatorioVendas(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        RelatorioRentabilidadeResponse rentabilidade = facade.relatorioRentabilidade(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        ExportacaoRelatorio pdf = facade.exportar(new ExportarRelatorioRequest(
                "vendas", "pdf", null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));
        String conteudoPdf = new String(pdf.conteudo(), StandardCharsets.UTF_8);

        assertEquals(new BigDecimal("12.30"), dashboardResponse.totalVendas());
        assertEquals(new BigDecimal("12.30"), vendas.totalComIva());
        assertEquals(new BigDecimal("6.00"), rentabilidade.margemTotal());
        assertEquals(new BigDecimal("60.00"), rentabilidade.margemPercentagem());
        assertTrue(conteudoPdf.contains("Loja Braga | vendas 3 | total 12.30"));
    }

    @Test
    void semDadosSincronizadosDashboardCentralFicaVazio() {
        when(vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(any(), any()))
                .thenReturn(List.of());
        when(consultaSincronizacao.dadosRelatorio(null)).thenReturn(List.of());
        when(alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc()).thenReturn(List.of());
        when(lojaRepository.findAll()).thenReturn(List.of(loja));

        DashboardResponse response = facade.obterDashboard(
                new RelatorioFiltro(null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), null));

        assertEquals(new BigDecimal("0.00"), response.totalVendas());
        assertEquals(0, response.numeroVendas());
        assertEquals(1, response.totalLojas());
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

    private VendaRelatorioSincronizada vendaSync(UUID lojaId, String lojaNome, BigDecimal valorComIva) {
        BigDecimal valorSemIva = valorComIva.divide(new BigDecimal("1.23"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal iva = valorComIva.subtract(valorSemIva);
        BigDecimal custo = new BigDecimal("0.75");
        return new VendaRelatorioSincronizada(
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

}
