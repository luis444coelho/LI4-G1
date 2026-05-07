package pt.miniFormiga.api.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacao(MethodArgumentNotValidException exception) {
        Map<String, Object> details = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() == null ? "valor invalido" : fieldError.getDefaultMessage(),
                        (left, right) -> left
                ));
        return erro(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Pedido invalido", details);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> acessoNegado(AccessDeniedException exception) {
        return erro(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Sem permissao para executar a operacao", Map.of());
    }

    private ResponseEntity<ApiError> erro(HttpStatus status, String code, String message, Map<String, Object> details) {
        return ResponseEntity.status(status).body(new ApiError(code, message, details));
    }
}
