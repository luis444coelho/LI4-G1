package pt.miniFormiga.subsistemas.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.domain.MotivoAjusteCodigo;

import java.time.LocalDateTime;
import java.util.List;
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
                                String corredor,
                                String prateleira,
                                boolean precisaReposicao) {
        public static StockResponse from(StockItem item) {
            return new StockResponse(
                    item.produtoId(),
                    item.produto().getNome(),
                    item.lojaId(),
                    item.quantidade(),
                    item.nivelMinimo(),
                    item.corredor(),
                    item.prateleira(),
                    item.precisaReposicao()
            );
        }
    }

    public record AlertaStockResponse(UUID id,
                                      UUID produtoId,
                                      String produto,
                                      UUID lojaId,
                                      LocalDateTime dataHora,
                                      int quantidadeNoMomento,
                                      boolean lido,
                                      boolean resolvido,
                                      LocalDateTime dataResolucao,
                                      List<UUID> destinatarios) {
        public static AlertaStockResponse from(AlertaStock alerta) {
            return new AlertaStockResponse(
                    alerta.getId(),
                    alerta.getProduto().getId(),
                    alerta.getProduto().getNome(),
                    alerta.getLoja() == null ? null : alerta.getLoja().getId(),
                    alerta.getDataHora(),
                    alerta.getQuantidadeNoMomento(),
                    alerta.isLido(),
                    alerta.isResolvido(),
                    alerta.getDataResolucao(),
                    alerta.getDestinatarios().stream().map(utilizador -> utilizador.getId()).toList()
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
                    ajuste.getProduto().getId(),
                    ajuste.getProduto().getNome(),
                    ajuste.getStockProdutoLoja() == null ? null : ajuste.getStockProdutoLoja().getLoja().getId(),
                    ajuste.getQuantidade(),
                    ajuste.getMotivoAjuste().getCodigo(),
                    ajuste.getUtilizador().getId(),
                    ajuste.getDataHora()
            );
        }
    }

    public record MotivoAjusteResponse(UUID id, String codigo, String descricao) {
        public static MotivoAjusteResponse from(MotivoAjusteCodigo motivo) {
            return new MotivoAjusteResponse(null, motivo.name(), motivo.name());
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
        public static LocalizacaoResponse from(StockItem item) {
            return new LocalizacaoResponse(item.produtoId(), item.corredor(), item.prateleira());
        }
    }
}
