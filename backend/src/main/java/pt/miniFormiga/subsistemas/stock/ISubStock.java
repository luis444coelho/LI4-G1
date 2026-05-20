package pt.miniFormiga.subsistemas.stock;

import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;

import java.util.List;
import java.util.UUID;

public interface ISubStock {
    List<StockDTO> consultarStock(UUID lojaId);

    void atualizarStock(UUID produtoId, UUID lojaId, int delta);

    void definirNivelMinimo(UUID produtoId, UUID lojaId, int quantidade);

    AjusteInventario registarAjuste(UUID produtoId, UUID lojaId, int quantidade, String motivo, UUID utilizadorId);

    InventarioFisico iniciarInventarioFisico(UUID lojaId, UUID utilizadorId);

    LinhaInventario registarContagemLinha(UUID inventarioId, UUID produtoId, int quantidade);

    void fecharInventario(UUID inventarioId);

    List<AlertaStock> getAlertasAtivos(UUID lojaId);

    AlertaStock marcarAlertaLido(UUID alertaId);

    AlertaStock resolverAlerta(UUID alertaId);

    List<LinhaInventario> listarDiscrepanciasInventario(UUID inventarioId);

    record StockDTO(UUID produtoId, UUID lojaId, int quantidade, Integer nivelMinimo, boolean precisaReposicao) {
        public StockDTO(UUID produtoId, UUID lojaId, int quantidade) {
            this(produtoId, lojaId, quantidade, null, false);
        }
    }
}
