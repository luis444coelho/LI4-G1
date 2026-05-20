package pt.miniFormiga.subsistemas.sincronizacao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoPayload;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoTransporte.ResultadoTransmissao;

@Component
public class RestSincronizacaoTransporte implements SincronizacaoTransporte {

    private final RestTemplate restTemplate;
    private final String centralUrl;

    public RestSincronizacaoTransporte(@Value("${mini-formiga.sincronizacao.central-url:}") String centralUrl) {
        this.restTemplate = new RestTemplate();
        this.centralUrl = centralUrl == null ? "" : centralUrl.trim();
    }

    @Override
    public ResultadoTransmissao transmitir(SincronizacaoPayload payload) {
        if (centralUrl.isBlank()) {
            return ResultadoTransmissao.falha("Servidor central nao configurado");
        }
        try {
            ResultadoTransmissao resposta = restTemplate.postForObject(centralUrl, payload, ResultadoTransmissao.class);
            return resposta == null ? ResultadoTransmissao.concluida() : resposta;
        } catch (RestClientException ex) {
            return ResultadoTransmissao.falha(ex.getMessage());
        }
    }
}
