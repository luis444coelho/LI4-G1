package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "taxas_iva")
public class TaxaIVA extends EntidadeBase {

    private static final BigDecimal TAXA_REDUZIDA = new BigDecimal("6");
    private static final BigDecimal TAXA_INTERMEDIA = new BigDecimal("13");
    private static final BigDecimal TAXA_NORMAL = new BigDecimal("23");

    @Column(nullable = false, unique = true)
    private String descricao;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentagem;

    @OneToMany(mappedBy = "taxaIVA")
    private List<Produto> produtos = new ArrayList<>();

    protected TaxaIVA() {
    }

    public TaxaIVA(String descricao, BigDecimal percentagem) {
        this.descricao = descricao;
        validarPercentagem(percentagem);
        this.percentagem = percentagem;
    }

    public static void validarPercentagem(BigDecimal percentagem) {
        if (percentagem == null || !(TAXA_REDUZIDA.compareTo(percentagem) == 0
                || TAXA_INTERMEDIA.compareTo(percentagem) == 0
                || TAXA_NORMAL.compareTo(percentagem) == 0)) {
            throw new IllegalArgumentException("Taxa de IVA invalida. Valores permitidos: 6, 13, 23");
        }
    }

    void adicionarProduto(Produto produto) {
        if (produto != null && !produtos.contains(produto)) {
            produtos.add(produto);
        }
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPercentagem() {
        return percentagem;
    }

    public List<Produto> getProdutos() {
        return Collections.unmodifiableList(produtos);
    }
}
