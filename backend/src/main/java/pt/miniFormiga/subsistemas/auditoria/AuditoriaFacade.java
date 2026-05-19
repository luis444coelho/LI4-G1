package pt.miniFormiga.subsistemas.auditoria;

import org.springframework.stereotype.Service;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.TipoOperacao;

import java.util.UUID;

@Service
public class AuditoriaFacade implements ISubAuditoria {

    private final AuditoriaService auditoriaService;

    public AuditoriaFacade(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    public void registar(TipoOperacao tipoOperacao, UUID utilizadorId, String recurso, String descricao) {
        auditoriaService.registar(tipoOperacao, utilizadorId, recurso, descricao);
    }
}
