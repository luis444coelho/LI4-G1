package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "localizacoes_produto")
public class LocalizacaoProduto extends EntidadeBase {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private String corredor;

    @Column(nullable = false)
    private String prateleira;

    @Column(length = 1000)
    private String descricao;

    protected LocalizacaoProduto() {
    }

    public LocalizacaoProduto(Produto produto, String corredor, String prateleira, String descricao) {
        this.produto = produto;
        this.corredor = corredor;
        this.prateleira = prateleira;
        this.descricao = descricao;
    }

    public Produto getProduto() {
        return produto;
    }

    public String getCorredor() {
        return corredor;
    }

    public String getPrateleira() {
        return prateleira;
    }

    public String getDescricao() {
        return descricao;
    }
}
