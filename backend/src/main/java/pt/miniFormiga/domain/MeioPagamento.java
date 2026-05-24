package pt.miniFormiga.domain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
// legacy non-entity
// legacy table removed
public class MeioPagamento extends EntidadeBase {
    @Column(nullable = false, unique = true)
    private String tipo;
    @Column(nullable = false)
    private String descricao;
    protected MeioPagamento() {
    }
    public MeioPagamento(String tipo, String descricao) {
        this.tipo = Objects.requireNonNull(tipo, "Tipo e obrigatorio").trim().toUpperCase(Locale.ROOT);
        this.descricao = Objects.requireNonNull(descricao, "Descricao e obrigatoria").trim();
    }
    public String getTipo() {
        return tipo;
    }
    public String getDescricao() {
        return descricao;
    }
}
