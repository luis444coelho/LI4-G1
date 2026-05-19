package pt.miniFormiga.subsistemas.auditoria;

import pt.miniFormiga.domain.TipoOperacao;

import java.util.UUID;

public interface ISubAuditoria {
    void registar(TipoOperacao tipoOperacao, UUID utilizadorId, String recurso, String descricao);
}
