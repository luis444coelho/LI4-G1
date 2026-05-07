package pt.miniFormiga.exception;

import java.util.Map;
import java.util.UUID;

public class RecursoNaoEncontradoException extends BusinessException {
    public RecursoNaoEncontradoException(String recurso, UUID id) {
        super("RECURSO_NAO_ENCONTRADO", recurso + " nao encontrado", Map.of("recurso", recurso, "id", id));
    }
}
