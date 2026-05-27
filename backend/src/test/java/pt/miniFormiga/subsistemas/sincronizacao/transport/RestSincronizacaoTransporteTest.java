package pt.miniFormiga.subsistemas.sincronizacao.transport;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pt.miniFormiga.subsistemas.sincronizacao.transport.SincronizacaoTransporte.ResultadoTransmissao;

class RestSincronizacaoTransporteTest {

    @Test
    void transmitirFalhaSemUrlCentralConfigurado() {
        RestSincronizacaoTransporte transporte = new RestSincronizacaoTransporte("   ", "token");

        var resultado = transporte.transmitir(null);

        assertFalse(resultado.sucesso());
        assertEquals("Servidor central nao configurado", resultado.mensagemErro());
    }

    @Test
    void transmitirEnviaPayloadComTokenEMapeiaRespostaDoServidorCentral() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        RestSincronizacaoTransporte transporte = transporteComRestTemplate(
                " https://central.example/sync ",
                " token-sync ",
                restTemplate
        );
        ResultadoTransmissao esperado = ResultadoTransmissao.concluida();
        when(restTemplate.postForObject(eq("https://central.example/sync"), any(HttpEntity.class), eq(ResultadoTransmissao.class)))
                .thenReturn(esperado);

        var resultado = transporte.transmitir(null);

        ArgumentCaptor<HttpEntity<?>> entity = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).postForObject(eq("https://central.example/sync"), entity.capture(), eq(ResultadoTransmissao.class));
        assertEquals(esperado, resultado);
        assertEquals("token-sync", entity.getValue().getHeaders().getFirst("X-Sync-Token"));
    }

    @Test
    void transmitirAssumeSucessoQuandoServidorRespondeSemCorpo() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        RestSincronizacaoTransporte transporte = transporteComRestTemplate(
                "https://central.example/sync",
                "   ",
                restTemplate
        );
        when(restTemplate.postForObject(eq("https://central.example/sync"), any(HttpEntity.class), eq(ResultadoTransmissao.class)))
                .thenReturn(null);

        var resultado = transporte.transmitir(null);

        ArgumentCaptor<HttpEntity<?>> entity = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).postForObject(eq("https://central.example/sync"), entity.capture(), eq(ResultadoTransmissao.class));
        assertTrue(resultado.sucesso());
        assertTrue(resultado.conflitos().isEmpty());
        assertFalse(entity.getValue().getHeaders().containsKey("X-Sync-Token"));
    }

    @Test
    void transmitirConverteErrosHttpEmFalhaDeTransmissao() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        RestSincronizacaoTransporte transporte = transporteComRestTemplate(
                "https://central.example/sync",
                "token",
                restTemplate
        );
        when(restTemplate.postForObject(eq("https://central.example/sync"), any(HttpEntity.class), eq(ResultadoTransmissao.class)))
                .thenThrow(new RestClientException("503 Service Unavailable"));

        var resultado = transporte.transmitir(null);

        assertFalse(resultado.sucesso());
        assertEquals("503 Service Unavailable", resultado.mensagemErro());
    }

    private RestSincronizacaoTransporte transporteComRestTemplate(String url,
                                                                 String token,
                                                                 RestTemplate restTemplate) throws ReflectiveOperationException {
        RestSincronizacaoTransporte transporte = new RestSincronizacaoTransporte(url, token);
        Field field = RestSincronizacaoTransporte.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        field.set(transporte, restTemplate);
        return transporte;
    }
}
