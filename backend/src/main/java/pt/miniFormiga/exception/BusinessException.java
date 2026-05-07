package pt.miniFormiga.exception;

import java.util.Map;

public class BusinessException extends MiniFormigaException {
    public BusinessException(String code, String message) {
        super(code, message, Map.of());
    }

    public BusinessException(String code, String message, Map<String, Object> details) {
        super(code, message, details);
    }
}
