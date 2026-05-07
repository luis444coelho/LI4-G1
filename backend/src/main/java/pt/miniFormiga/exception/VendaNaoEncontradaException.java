package pt.miniFormiga.exception;

import java.util.Map;
import java.util.UUID;

public class VendaNaoEncontradaException extends BusinessException {
    public VendaNaoEncontradaException(UUID vendaId) {
        super("VENDA_NAO_ENCONTRADA", "Venda nao encontrada", Map.of("vendaId", vendaId));
    }
}
