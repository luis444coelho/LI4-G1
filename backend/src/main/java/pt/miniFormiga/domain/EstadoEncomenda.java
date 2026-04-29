package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "estados_encomenda")
public class EstadoEncomenda extends EntidadeBase {

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    protected EstadoEncomenda() {
    }

    public EstadoEncomenda(String codigo, String descricao) {
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
