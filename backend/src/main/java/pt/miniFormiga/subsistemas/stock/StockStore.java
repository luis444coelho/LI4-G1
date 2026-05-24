package pt.miniFormiga.subsistemas.stock;

import java.util.List;
import java.util.UUID;

public interface StockStore {
    List<StockItem> listar(UUID lojaId);

    StockItem obter(UUID produtoId, UUID lojaId);

    StockItem atualizarStock(UUID produtoId, UUID lojaId, int delta);

    StockItem definirNivelMinimo(UUID produtoId, UUID lojaId, int quantidade);

    StockItem atualizarLocalizacao(UUID produtoId, UUID lojaId, String corredor, String prateleira);
}
