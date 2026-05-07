package pt.miniFormiga.auditoria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pt.miniFormiga.domain.TipoOperacao;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditoriaService {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;

    public AuditoriaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registar(TipoOperacao tipoOperacao, UUID utilizadorId, String recurso, String descricao) {
        Map<String, Object> evento = Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "tipoOperacao", tipoOperacao.name(),
                "utilizadorId", utilizadorId == null ? "SISTEMA" : utilizadorId.toString(),
                "recurso", recurso,
                "descricao", descricao
        );

        try {
            AUDIT_LOGGER.info(objectMapper.writeValueAsString(evento));
        } catch (JsonProcessingException e) {
            AUDIT_LOGGER.info("{\"tipoOperacao\":\"{}\",\"utilizadorId\":\"{}\",\"recurso\":\"{}\"}",
                    tipoOperacao.name(), utilizadorId, recurso);
        }
    }
}
