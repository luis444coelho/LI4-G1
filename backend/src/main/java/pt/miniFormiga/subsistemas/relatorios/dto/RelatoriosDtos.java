package pt.miniFormiga.subsistemas.relatorios.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class RelatoriosDtos {
    private RelatoriosDtos() {
    }

    public record RelatorioFiltro(UUID lojaId,
                                  LocalDate inicio,
                                  LocalDate fim,
                                  UUID categoriaId,
                                  UUID produtoId,
                                  String turno) {
        public RelatorioFiltro(UUID lojaId, LocalDate inicio, LocalDate fim, UUID categoriaId) {
            this(lojaId, inicio, fim, categoriaId, null, null);
        }
    }

    public record PeriodoResponse(LocalDate inicio, LocalDate fim) {
    }

    public record DashboardResponse(PeriodoResponse periodo,
                                    BigDecimal totalVendas,
                                    BigDecimal totalIva,
                                    BigDecimal margem,
                                    long numeroVendas,
                                    long numeroLojasComVendas,
                                    long totalLojas,
                                    long alertasAtivos,
                                    BigDecimal ticketMedio,
                                    List<VendasPorLojaResponse> vendasPorLoja) {
    }

    public record RelatorioVendasResponse(PeriodoResponse periodo,
                                          UUID lojaId,
                                          UUID categoriaId,
                                          UUID produtoId,
                                          String turno,
                                          BigDecimal totalSemIva,
                                          BigDecimal totalIva,
                                          BigDecimal totalComIva,
                                          BigDecimal margem,
                                          long numeroVendas,
                                          List<VendasPorLojaResponse> vendasPorLoja,
                                          List<VendasPorDiaResponse> vendasPorDia,
                                          List<LinhaVendaRelatorioResponse> linhas) {
    }

    public record VendasPorLojaResponse(UUID lojaId,
                                        String loja,
                                        BigDecimal total,
                                        BigDecimal iva,
                                        BigDecimal margem,
                                        long numeroVendas) {
    }

    public record VendasPorDiaResponse(LocalDate data,
                                       BigDecimal total,
                                       BigDecimal iva,
                                       BigDecimal margem,
                                       long numeroVendas) {
    }

    public record LinhaVendaRelatorioResponse(UUID vendaId,
                                              LocalDateTime dataHora,
                                              UUID lojaId,
                                              String loja,
                                              UUID produtoId,
                                              String produto,
                                              String categoria,
                                              int quantidade,
                                              BigDecimal valorSemIva,
                                              BigDecimal iva,
                                              BigDecimal valorComIva,
                                              BigDecimal margem) {
    }

    public record RelatorioStockResponse(UUID lojaId,
                                         UUID categoriaId,
                                         long totalProdutos,
                                         long totalUnidades,
                                         BigDecimal valorStockPrecoCusto,
                                         long produtosReposicao,
                                         long alertasAtivos,
                                         List<StockItemResponse> itens) {
    }

    public record StockItemResponse(UUID produtoId,
                                    String produto,
                                    String categoria,
                                    UUID lojaId,
                                    String loja,
                                    int quantidade,
                                    Integer nivelMinimo,
                                    boolean precisaReposicao,
                                    BigDecimal valorPrecoCusto,
                                    LocalDateTime dataUltimaAtualizacao) {
    }

    public record RelatorioRentabilidadeResponse(PeriodoResponse periodo,
                                                 UUID lojaId,
                                                 UUID categoriaId,
                                                 UUID produtoId,
                                                 String turno,
                                                 BigDecimal receitaSemIva,
                                                 BigDecimal custoTotal,
                                                 BigDecimal margemTotal,
                                                 BigDecimal margemPercentagem,
                                                 List<RentabilidadeProdutoResponse> produtos,
                                                 List<RentabilidadeCategoriaResponse> categorias) {
    }

    public record RentabilidadeProdutoResponse(UUID produtoId,
                                               String produto,
                                               String categoria,
                                               int quantidadeVendida,
                                               BigDecimal receitaSemIva,
                                               BigDecimal custo,
                                               BigDecimal margem,
                                               BigDecimal margemPercentagem) {
    }

    public record RentabilidadeCategoriaResponse(String categoria,
                                                 int quantidadeVendida,
                                                 BigDecimal receitaSemIva,
                                                 BigDecimal custo,
                                                 BigDecimal margem,
                                                 BigDecimal margemPercentagem) {
    }

    public record ExportarRelatorioRequest(@NotBlank String tipo,
                                           @NotBlank String formato,
                                           UUID lojaId,
                                           LocalDate inicio,
                                           LocalDate fim,
                                           UUID categoriaId,
                                           UUID produtoId,
                                           String turno) {
        public ExportarRelatorioRequest(String tipo,
                                        String formato,
                                        UUID lojaId,
                                        LocalDate inicio,
                                        LocalDate fim,
                                        UUID categoriaId) {
            this(tipo, formato, lojaId, inicio, fim, categoriaId, null, null);
        }
    }

    public record ExportacaoRelatorio(String nomeFicheiro, String mediaType, byte[] conteudo) {
    }
}
