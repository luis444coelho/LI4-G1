package pt.miniFormiga.subsistemas.sincronizacao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoPayload;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoTransporte.ResultadoTransmissao;

@Component
public class RestSincronizacaoTransporte implements SincronizacaoTransporte {

    private final RestTemplate restTemplate;
    private final String centralUrl;
    private final String syncToken;

    public RestSincronizacaoTransporte(@Value("${mini-formiga.sincronizacao.central-url:}") String centralUrl,
                                       @Value("${mini-formiga.sincronizacao.token:MiniFormigaSyncDevToken}") String syncToken) {
        this.restTemplate = new RestTemplate();
        this.centralUrl = centralUrl == null ? "" : centralUrl.trim();
        this.syncToken = syncToken == null ? "" : syncToken.trim();
    }

    @Override
    public ResultadoTransmissao transmitir(SincronizacaoPayload payload) {
        if (centralUrl.isBlank()) {
            return ResultadoTransmissao.falha("Servidor central nao configurado");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            if (!syncToken.isBlank()) {
                headers.set("X-Sync-Token", syncToken);
            }
            ResultadoTransmissao resposta = restTemplate.postForObject(centralUrl, new HttpEntity<>(payload, headers), ResultadoTransmissao.class);
            return resposta == null ? ResultadoTransmissao.concluida() : resposta;
        } catch (RestClientException ex) {
            return ResultadoTransmissao.falha(ex.getMessage());
        }
    }
}
