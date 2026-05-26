package pt.miniFormiga.subsistemas.sincronizacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RestSincronizacaoTransporteTest {

    @Test
    void transmitirFalhaSemUrlCentralConfigurado() {
        RestSincronizacaoTransporte transporte = new RestSincronizacaoTransporte("   ", "token");

        var resultado = transporte.transmitir(null);

        assertFalse(resultado.sucesso());
        assertEquals("Servidor central nao configurado", resultado.mensagemErro());
    }
}
