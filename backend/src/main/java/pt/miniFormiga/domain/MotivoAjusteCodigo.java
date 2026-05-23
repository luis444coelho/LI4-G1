package pt.miniFormiga.domain;

public enum MotivoAjusteCodigo {
    QUEBRA,
    DESPERDICIO,
    CORRECAO_ERRO,
    DEVOLUCAO,
    ENTRADA_MERCADORIA;

    public String getCodigo() {
        return name();
    }
}
