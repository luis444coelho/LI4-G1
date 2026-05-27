package pt.miniFormiga.api.error;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.subsistemas.utilizadores.RecursoNaoEncontradoException;
import pt.miniFormiga.subsistemas.utilizadores.RegraNegocioException;

import java.lang.reflect.Method;
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
        assertEquals("SCHEMA_LEGADO", schema.getBody().code());
        assertEquals("DATA_INTEGRITY_VIOLATION", generico.getBody().code());
        assertTrue(generico.getBody().details().containsKey("detalhe"));
    }

    @Test
    void validacaoAgrupaCamposOrdenadosEUsaMensagemSeguraQuandoCampoNaoTemMensagem() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "username", null, false, null, null, "obrigatorio"));
        binding.addError(new FieldError("request", "email", null, false, null, null, null));
        binding.addError(new FieldError("request", "username", null, false, null, null, "duplicado"));

        var response = handler.validacao(new MethodArgumentNotValidException(methodParameter(), binding));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().code());
        assertEquals("Pedido invalido: email, username", response.getBody().message());
        assertEquals("valor invalido", response.getBody().details().get("email"));
        assertEquals("obrigatorio", response.getBody().details().get("username"));
    }

    @Test
    void validacaoSemCamposMantemMensagemGenerica() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");

        var response = handler.validacao(new MethodArgumentNotValidException(methodParameter(), binding));

        assertEquals("Pedido invalido", response.getBody().message());
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        Method method = ApiExceptionHandlerTest.class.getDeclaredMethod("endpointTeste", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void endpointTeste(String request) {
    }
}
