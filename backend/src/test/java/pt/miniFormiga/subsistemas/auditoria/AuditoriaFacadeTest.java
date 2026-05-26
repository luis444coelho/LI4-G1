package pt.miniFormiga.subsistemas.auditoria;

import org.junit.jupiter.api.Test;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.TipoOperacao;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditoriaFacadeTest {

    @Test
    void registarDelegaNoServicoDeAuditoria() {
        AuditoriaService auditoriaService = mock(AuditoriaService.class);
        AuditoriaFacade facade = new AuditoriaFacade(auditoriaService);
        UUID utilizadorId = UUID.randomUUID();

        facade.registar(TipoOperacao.LOGIN, utilizadorId, "AUTH_LOGIN", "Login efetuado");

        verify(auditoriaService).registar(TipoOperacao.LOGIN, utilizadorId, "AUTH_LOGIN", "Login efetuado");
    }
}
