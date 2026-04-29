package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "estados_sincronizacao")
public class EstadoSincronizacao extends EntidadeBase {

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    protected EstadoSincronizacao() {
    }

    public EstadoSincronizacao(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }
}
