package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "produtos")
public class Produto extends EntidadeBase {

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoVenda;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoCusto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxa_iva_id", nullable = false)
    private TaxaIVA taxaIVA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Produto() {
    }

    public Produto(String codigo,
                   String nome,
                   BigDecimal precoVenda,
                   BigDecimal precoCusto,
                   TaxaIVA taxaIVA,
                   Categoria categoria) {
        this.codigo = codigo;
        this.nome = nome;
        this.precoVenda = Objects.requireNonNull(precoVenda, "Preco de venda e obrigatorio");
        this.precoCusto = Objects.requireNonNull(precoCusto, "Preco de custo e obrigatorio");
        this.taxaIVA = Objects.requireNonNull(taxaIVA, "Taxa de IVA e obrigatoria");
        this.categoria = Objects.requireNonNull(categoria, "Categoria e obrigatoria");
        TaxaIVA.validarPercentagem(this.taxaIVA.getPercentagem());
        this.ativo = true;
        this.taxaIVA.adicionarProduto(this);
        this.categoria.adicionarProduto(this);
    }

    public BigDecimal calcularMargem() {
        return precoVenda.subtract(precoCusto);
    }

    public BigDecimal calcularMargemPercentagem() {
        if (precoCusto.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return calcularMargem()
                .divide(precoCusto, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    public BigDecimal calcularPrecoComIVA() {
        BigDecimal fatorIva = BigDecimal.ONE.add(
                taxaIVA.getPercentagem().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );
        return precoVenda.multiply(fatorIva).setScale(2, RoundingMode.HALF_UP);
    }

    public void desativar() {
        this.ativo = false;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public BigDecimal getPrecoCusto() {
        return precoCusto;
    }

    public TaxaIVA getTaxaIVA() {
        return taxaIVA;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
