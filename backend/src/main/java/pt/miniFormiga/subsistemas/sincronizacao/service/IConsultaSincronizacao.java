package pt.miniFormiga.subsistemas.sincronizacao.service;

import pt.miniFormiga.subsistemas.relatorios.dto.RelatoriosDtos.DashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IConsultaSincronizacao {
    List<DadosRelatorioSincronizado> dadosRelatorio(UUID lojaId);

    record DadosRelatorioSincronizado(UUID lojaId,
                                      DashboardResponse dashboard,
                                      List<VendaRelatorioSincronizada> vendasRelatorio) {
    }

    record VendaRelatorioSincronizada(UUID vendaId,
                                      LocalDateTime dataHora,
                                      UUID lojaId,
                                      String loja,
                                      UUID produtoId,
                                      String produto,
                                      UUID categoriaId,
                                      String categoria,
                                      int quantidade,
                                      BigDecimal valorSemIva,
                                      BigDecimal iva,
                                      BigDecimal valorComIva,
                                      BigDecimal custo,
                                      BigDecimal margem) {
    }
}
