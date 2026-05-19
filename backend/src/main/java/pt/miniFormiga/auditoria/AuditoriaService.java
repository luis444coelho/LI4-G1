package pt.miniFormiga.auditoria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pt.miniFormiga.domain.TipoOperacao;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;

@Service
public class AuditoriaService {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;

    public AuditoriaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registar(TipoOperacao tipoOperacao, UUID utilizadorId, String recurso, String descricao) {
        registar(tipoOperacao, utilizadorId, recurso, null, descricao);
    }

    public void registar(TipoOperacao tipoOperacao, UUID utilizadorId, String entidade, UUID entidadeId, String descricao) {
        LinkedHashMap<String, Object> evento = new LinkedHashMap<>();
        evento.put("dataHora", OffsetDateTime.now().toString());
        evento.put("tipoOperacao", tipoOperacao.name());
        evento.put("utilizadorId", utilizadorId == null ? "SISTEMA" : utilizadorId.toString());
        evento.put("entidade", entidade);
        evento.put("entidadeId", entidadeId == null ? null : entidadeId.toString());
        evento.put("descricao", descricao);

        try {
            AUDIT_LOGGER.info(objectMapper.writeValueAsString(evento));
        } catch (JsonProcessingException e) {
            AUDIT_LOGGER.info("{\"tipoOperacao\":\"{}\",\"utilizadorId\":\"{}\",\"entidade\":\"{}\",\"entidadeId\":\"{}\"}",
                    tipoOperacao.name(), utilizadorId, entidade, entidadeId);
        }
    }
}
