package pt.miniFormiga.domain;

public enum EstadoSincronizacaoCodigo {
    PENDENTE,
    EM_CURSO,
    CONCLUIDA,
    FALHADA,
    COM_CONFLITOS;

    public String getCodigo() {
        return name();
    }
}
