package pt.miniFormiga.subsistemas.auditoria;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import pt.miniFormiga.domain.TipoOperacao;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubAuditoriaFacadeTest {

    @Test
    void registarEscreveJsonComCamposNormalizados() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SubAuditoriaFacade facade = new SubAuditoriaFacade(objectMapper);
        Logger logger = (Logger) LoggerFactory.getLogger("AUDIT");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        UUID utilizadorId = UUID.randomUUID();
        UUID entidadeId = UUID.randomUUID();

        try {
            facade.registar(TipoOperacao.ACESSO, utilizadorId, "UTILIZADOR", entidadeId, "Operacao auditada");
        } finally {
            logger.detachAppender(appender);
        }

        assertEquals(1, appender.list.size());
        JsonNode json = objectMapper.readTree(appender.list.get(0).getFormattedMessage());
        assertTrue(json.hasNonNull("dataHora"));
        assertEquals("ACESSO", json.get("tipoOperacao").asText());
        assertEquals(utilizadorId.toString(), json.get("utilizadorId").asText());
        assertEquals("UTILIZADOR", json.get("entidade").asText());
        assertEquals(entidadeId.toString(), json.get("entidadeId").asText());
        assertEquals("Operacao auditada", json.get("descricao").asText());
    }
}
