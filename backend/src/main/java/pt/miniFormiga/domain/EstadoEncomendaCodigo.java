package pt.miniFormiga.domain;

public enum EstadoEncomendaCodigo {
    PENDENTE,
    ENVIADA,
    RECEBIDA,
    CANCELADA;

    public String getCodigo() {
        return name();
    }
}
