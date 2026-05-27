package pt.miniFormiga.subsistemas.auditoria.facade;

import pt.miniFormiga.domain.TipoOperacao;

import java.util.UUID;

public interface ISubAuditoria {
    void registar(TipoOperacao tipoOperacao, UUID utilizadorId, String recurso, String descricao);

    void registar(TipoOperacao tipoOperacao, UUID utilizadorId, String entidade, UUID entidadeId, String descricao);
}
