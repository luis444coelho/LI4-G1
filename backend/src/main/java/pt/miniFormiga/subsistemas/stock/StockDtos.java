package pt.miniFormiga.subsistemas.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.domain.LocalizacaoProduto;
import pt.miniFormiga.domain.MotivoAjuste;
import pt.miniFormiga.domain.Stock;

import java.time.LocalDateTime;
import java.util.UUID;

public final class StockDtos {
    private StockDtos() {
    }

    public record DefinirNivelMinimoRequest(@NotNull UUID lojaId, @Min(0) int quantidade) {
    }

    public record RegistarAjusteRequest(@NotNull UUID produtoId,
                                        @NotNull UUID lojaId,
                                        int quantidade,
                                        @NotBlank String motivo,
                                        @NotNull UUID utilizadorId) {
    }

    public record IniciarInventarioRequest(@NotNull UUID lojaId, @NotNull UUID utilizadorId) {
    }

    public record RegistarContagemRequest(@NotNull UUID produtoId, @Min(0) int quantidade) {
    }

    public record AtualizarContagemRequest(@Min(0) int quantidade) {
    }

    public record LocalizacaoRequest(@NotBlank String corredor, @NotBlank String prateleira) {
    }

    public record StockResponse(UUID produtoId,
                                String produto,
                                UUID lojaId,
                                int quantidade,
                                Integer nivelMinimo,
                                boolean precisaReposicao) {
        public static StockResponse from(Stock stock) {
            return new StockResponse(
                    stock.getProduto().getId(),
                    stock.getProduto().getNome(),
                    stock.getLoja().getId(),
                    stock.getQuantidade(),
                    stock.getNivelMinimo() == null ? null : stock.getNivelMinimo().getQuantidade(),
                    stock.precisaReposicao()
            );
        }
    }

    public record AlertaStockResponse(UUID id,
                                      UUID produtoId,
                                      String produto,
                                      UUID lojaId,
                                      LocalDateTime dataHora,
                                      int quantidadeNoMomento,
                                      boolean lido) {
        public static AlertaStockResponse from(AlertaStock alerta) {
            Stock stock = alerta.getStock();
            return new AlertaStockResponse(
                    alerta.getId(),
                    stock.getProduto().getId(),
                    stock.getProduto().getNome(),
                    stock.getLoja().getId(),
                    alerta.getDataHora(),
                    alerta.getQuantidadeNoMomento(),
                    alerta.isLido()
            );
        }
    }

    public record AjusteInventarioResponse(UUID id,
                                           UUID produtoId,
                                           String produto,
                                           UUID lojaId,
                                           int quantidade,
                                           String motivo,
                                           UUID utilizadorId,
                                           LocalDateTime dataHora) {
        public static AjusteInventarioResponse from(AjusteInventario ajuste) {
            return new AjusteInventarioResponse(
                    ajuste.getId(),
                    ajuste.getStock().getProduto().getId(),
                    ajuste.getStock().getProduto().getNome(),
                    ajuste.getStock().getLoja().getId(),
                    ajuste.getQuantidade(),
                    ajuste.getMotivoAjuste().getCodigo(),
                    ajuste.getUtilizador().getId(),
                    ajuste.getDataHora()
            );
        }
    }

    public record MotivoAjusteResponse(UUID id, String codigo, String descricao) {
        public static MotivoAjusteResponse from(MotivoAjuste motivo) {
            return new MotivoAjusteResponse(motivo.getId(), motivo.getCodigo(), motivo.getDescricao());
        }
    }

    public record InventarioFisicoResponse(UUID id,
                                           UUID lojaId,
                                           UUID utilizadorId,
                                           LocalDateTime dataInicio,
                                           LocalDateTime dataFecho,
                                           boolean fechado,
                                           int totalDiscrepancias) {
        public static InventarioFisicoResponse from(InventarioFisico inventario) {
            return new InventarioFisicoResponse(
                    inventario.getId(),
                    inventario.getLoja().getId(),
                    inventario.getResponsavel().getId(),
                    inventario.getDataInicio(),
                    inventario.getDataFecho(),
                    inventario.isFechado(),
                    inventario.getTotalDiscrepancias()
            );
        }
    }

    public record LinhaInventarioResponse(UUID id,
                                          UUID produtoId,
                                          String produto,
                                          int quantidadeContada,
                                          int quantidadeSistema,
                                          int discrepancia) {
        public static LinhaInventarioResponse from(LinhaInventario linha) {
            return new LinhaInventarioResponse(
                    linha.getId(),
                    linha.getProduto().getId(),
                    linha.getProduto().getNome(),
                    linha.getQuantidadeContada(),
                    linha.getQuantidadeSistema(),
                    linha.getDiscrepancia()
            );
        }
    }

    public record LocalizacaoResponse(UUID produtoId, String corredor, String prateleira) {
        public static LocalizacaoResponse from(LocalizacaoProduto localizacao) {
            return new LocalizacaoResponse(
                    localizacao.getProduto().getId(),
                    localizacao.getCorredor(),
                    localizacao.getPrateleira()
            );
        }
    }
}
