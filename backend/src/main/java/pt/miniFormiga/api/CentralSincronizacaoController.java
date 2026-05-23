package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.subsistemas.sincronizacao.ServidorCentralSincronizacaoService;

import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoPayload;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoTransporte.ResultadoTransmissao;

@RestController
@Profile("central")
@RequestMapping("/api/v1/central/sincronizacao")
@Tag(name = "SINCRONIZACAO-CENTRAL", description = "Rececao de sincronizacoes das lojas no perfil central")
public class CentralSincronizacaoController {

    private final ServidorCentralSincronizacaoService servidorCentral;
    private final String syncToken;

    public CentralSincronizacaoController(ServidorCentralSincronizacaoService servidorCentral,
                                          @Value("${mini-formiga.sincronizacao.token:MiniFormigaSyncDevToken}") String syncToken) {
        this.servidorCentral = servidorCentral;
        this.syncToken = syncToken == null ? "" : syncToken.trim();
    }

    @PostMapping("/receber")
    @Operation(summary = "Receber payload de sincronizacao de uma loja")
    @ApiResponse(responseCode = "200", description = "Payload integrado ou aceite com conflitos resolvidos")
    public ResultadoTransmissao receber(@RequestHeader(value = "X-Sync-Token", required = false) String token,
                                        @RequestBody SincronizacaoPayload payload) {
        if (!syncToken.isBlank() && !syncToken.equals(token)) {
            throw new BusinessException("SYNC_TOKEN_INVALIDO", "Token de sincronizacao invalido");
        }
        return servidorCentral.receber(payload);
    }
}
