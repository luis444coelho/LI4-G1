package pt.miniFormiga.exception;

import java.util.Map;
import java.util.UUID;

public class StockInsuficienteException extends BusinessException {
    public StockInsuficienteException(UUID produtoId, int disponivel, int solicitado) {
        super("STOCK_INSUFICIENTE",
                "Stock insuficiente para o produto",
                Map.of("produtoId", produtoId, "quantidadeDisponivel", disponivel, "quantidadeSolicitada", solicitado));
    }
}
