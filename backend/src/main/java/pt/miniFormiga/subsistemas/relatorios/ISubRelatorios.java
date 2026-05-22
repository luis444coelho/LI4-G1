package pt.miniFormiga.subsistemas.relatorios;

import static pt.miniFormiga.subsistemas.relatorios.RelatoriosDtos.*;

public interface ISubRelatorios {
    DashboardResponse obterDashboard(RelatorioFiltro filtro);

    RelatorioVendasResponse relatorioVendas(RelatorioFiltro filtro);

    RelatorioStockResponse relatorioStock(RelatorioFiltro filtro);

    RelatorioRentabilidadeResponse relatorioRentabilidade(RelatorioFiltro filtro);

    ExportacaoRelatorio exportar(ExportarRelatorioRequest request);
}
