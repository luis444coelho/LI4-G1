package pt.miniFormiga.subsistemas.relatorios.facade;

import static pt.miniFormiga.subsistemas.relatorios.dto.RelatoriosDtos.*;

public interface ISubRelatorios {
    DashboardResponse obterDashboard(RelatorioFiltro filtro);

    RelatorioVendasResponse relatorioVendas(RelatorioFiltro filtro);

    RelatorioStockResponse relatorioStock(RelatorioFiltro filtro);

    RelatorioRentabilidadeResponse relatorioRentabilidade(RelatorioFiltro filtro);

    ExportacaoRelatorio exportar(ExportarRelatorioRequest request);
}
