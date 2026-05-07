package pt.miniFormiga.exception;

import java.util.Map;

public abstract class MiniFormigaException extends RuntimeException {
    private final String code;
    private final Map<String, Object> details;

    protected MiniFormigaException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details == null ? Map.of() : details;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
