package pt.miniFormiga.api.error;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.miniFormiga.exception.MiniFormigaException;
import pt.miniFormiga.subsistemas.utilizadores.CredenciaisInvalidasException;
import pt.miniFormiga.subsistemas.utilizadores.RecursoNaoEncontradoException;
import pt.miniFormiga.subsistemas.utilizadores.RegraNegocioException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MiniFormigaException.class)
    public ResponseEntity<ApiError> miniFormiga(MiniFormigaException exception) {
        HttpStatus status = exception.getCode().contains("NAO_ENCONTRAD") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return erro(status, exception.getCode(), exception.getMessage(), exception.getDetails());
    }

    @ExceptionHandler({CredenciaisInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<ApiError> credenciaisInvalidas(RuntimeException exception) {
        return erro(HttpStatus.UNAUTHORIZED, "AUTH_CREDENTIALS_INVALID", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> naoEncontrado(RecursoNaoEncontradoException exception) {
        return erro(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), Map.of());
    }

    @ExceptionHandler({RegraNegocioException.class, IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> regraNegocio(RuntimeException exception) {
        return erro(HttpStatus.BAD_REQUEST, "DOMAIN_RULE_VIOLATION", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridadeDados(DataIntegrityViolationException exception) {
        LOGGER.warn("Violacao de integridade de dados", exception);
        String detalhe = mensagemRaiz(exception);
        String normalizado = detalhe.toLowerCase();
        if (normalizado.contains("utilizadores") && normalizado.contains("username")) {
            return erro(HttpStatus.BAD_REQUEST, "USERNAME_DUPLICADO", "Username ja existe", Map.of("detalhe", detalhe));
        }
        if (normalizado.contains("perfil_id")) {
            return erro(HttpStatus.BAD_REQUEST, "SCHEMA_LEGADO", "Schema antigo da base de dados: reinicie com rebuild ou limpe os volumes", Map.of("detalhe", detalhe));
        }
        return erro(HttpStatus.BAD_REQUEST, "DATA_INTEGRITY_VIOLATION", "Dados invalidos ou duplicados", Map.of("detalhe", detalhe));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacao(MethodArgumentNotValidException exception) {
        Map<String, Object> details = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null ? "valor invalido" : fieldError.getDefaultMessage(),
                        (left, right) -> left
                ));
        String campos = details.keySet().stream()
                .map(Object::toString)
                .sorted()
                .collect(Collectors.joining(", "));
        String message = campos.isBlank() ? "Pedido invalido" : "Pedido invalido: " + campos;
        return erro(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> pedidoInvalido(HttpMessageNotReadableException exception) {
        return erro(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "Pedido invalido", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> acessoNegado(AccessDeniedException exception) {
        return erro(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Sem permissao para executar a operacao", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> erroInterno(Exception exception) {
        LOGGER.error("Erro interno ao executar a operacao", exception);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erro interno ao executar a operacao", Map.of());
    }

    private ResponseEntity<ApiError> erro(HttpStatus status, String code, String message, Map<String, Object> details) {
        return ResponseEntity.status(status).body(new ApiError(code, message, details));
    }

    private String mensagemRaiz(Throwable throwable) {
        Throwable atual = throwable;
        while (atual.getCause() != null) {
            atual = atual.getCause();
        }
        return atual.getMessage() == null ? throwable.getMessage() : atual.getMessage();
    }
}
