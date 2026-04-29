package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LogAuditoriaTest {

    @Test
    void deveCriarLogAuditoriaComDadosObrigatorios() {
        Loja loja = new Loja("Loja Lisboa", "Rua Augusta", "456456456");
        Perfil perfil = new Perfil("GERENTE", List.of("UTILIZADOR_WRITE"));
        Utilizador utilizador = new Utilizador("gerente", "$2a$10$hash", "Manuel", perfil, loja);

        UUID entidadeId = UUID.randomUUID();
        LogAuditoria log = new LogAuditoria(
                utilizador,
                TipoOperacao.UTILIZADOR_CRIADO,
                "Criacao de utilizador",
                "Utilizador",
                entidadeId,
                null,
                "{\"username\":\"novo\"}"
        );

        assertEquals(TipoOperacao.UTILIZADOR_CRIADO, log.getTipoOperacao());
        assertEquals(entidadeId, log.getEntidadeId());
        assertNotNull(log.getDataHora());
        assertTrue(log.getDataHora().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(utilizador.getLogsAuditoria().contains(log));
    }

    @Test
    void naoDeveTerSettersPublicos() {
        Method[] methods = LogAuditoria.class.getMethods();

        boolean existeSetter = false;
        for (Method method : methods) {
            if (method.getName().startsWith("set")) {
                existeSetter = true;
                break;
            }
        }

        assertFalse(existeSetter);
    }
}
