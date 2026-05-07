package pt.miniFormiga.exception;

public class FaturaNaoEmitidaException extends BusinessException {
    public FaturaNaoEmitidaException(String motivo) {
        super("FATURA_NAO_EMITIDA", motivo);
    }
}
