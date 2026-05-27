package pt.miniFormiga.api.error;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.subsistemas.utilizadores.RecursoNaoEncontradoException;
import pt.miniFormiga.subsistemas.utilizadores.RegraNegocioException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void miniFormigaMapeiaCodigosNaoEncontradoPara404() {
        var response = handler.miniFormiga(new BusinessException(
                "VENDA_NAO_ENCONTRADA",
                "Venda nao encontrada",
                Map.of("vendaId", "123")
        ));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("VENDA_NAO_ENCONTRADA", response.getBody().code());
        assertEquals("123", response.getBody().details().get("vendaId"));
    }

    @Test
    void errosDeAutenticacaoDominioEAcessoTêmCodigosEstaveis() {
        assertEquals("AUTH_CREDENTIALS_INVALID",
                handler.credenciaisInvalidas(new BadCredentialsException("credenciais invalidas")).getBody().code());
        assertEquals("RESOURCE_NOT_FOUND",
                handler.naoEncontrado(new RecursoNaoEncontradoException("sem registo")).getBody().code());
        assertEquals("DOMAIN_RULE_VIOLATION",
                handler.regraNegocio(new RegraNegocioException("regra violada")).getBody().code());
        assertEquals("ACCESS_DENIED",
                handler.acessoNegado(new AccessDeniedException("sem permissao")).getBody().code());
    }

    @Test
    void pedidoInvalidoEErroInternoUsamMensagensSeguras() {
        var pedido = handler.pedidoInvalido(new HttpMessageNotReadableException(
                "json invalido",
                new MockHttpInputMessage(new byte[0])
        ));
        var interno = handler.erroInterno(new IllegalStateException("detalhe tecnico"));

        assertEquals(HttpStatus.BAD_REQUEST, pedido.getStatusCode());
        assertEquals("INVALID_REQUEST_BODY", pedido.getBody().code());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, interno.getStatusCode());
        assertEquals("INTERNAL_ERROR", interno.getBody().code());
        assertEquals("Erro interno ao executar a operacao", interno.getBody().message());
    }

    @Test
    void integridadeDeDadosNormalizaErrosConhecidos() {
        var username = handler.integridadeDados(new DataIntegrityViolationException(
                "erro",
                new RuntimeException("violacao tabela utilizadores coluna username")
        ));
        var schema = handler.integridadeDados(new DataIntegrityViolationException(
                "erro",
                new RuntimeException("no such column: perfil_id")
        ));
        var generico = handler.integridadeDados(new DataIntegrityViolationException("duplicado"));

        assertEquals("USERNAME_DUPLICADO", username.getBody().code());
        assertEquals("SCHEMA_DESATUALIZADO", schema.getBody().code());
        assertEquals("DATA_INTEGRITY_VIOLATION", generico.getBody().code());
        assertTrue(generico.getBody().details().containsKey("detalhe"));
    }
}
