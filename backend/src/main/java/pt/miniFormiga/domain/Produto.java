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

    @Column(name = "codigo_barras", unique = true)
    private String codigoBarras;

    @Column(nullable = false)
    private String nome;

    @Column(length = 1000)
    private String descricao;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_principal_id")
    private Fornecedor fornecedorPrincipal;

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
        this.codigo = validarTexto(codigo, "Codigo de barras e obrigatorio");
        this.codigoBarras = this.codigo;
        this.nome = validarTexto(nome, "Nome do produto e obrigatorio");
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
        if (precoVenda.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return precoVenda.subtract(precoCusto)
                .divide(precoVenda, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularMargemPercentagem() {
        if (precoVenda.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return precoVenda.subtract(precoCusto)
                .divide(precoVenda, 4, RoundingMode.HALF_UP)
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

    public void atualizar(String nome,
                          String descricao,
                          BigDecimal precoVenda,
                          BigDecimal precoCusto,
                          Categoria categoria,
                          TaxaIVA taxaIVA,
                          Fornecedor fornecedorPrincipal,
                          Boolean ativo) {
        if (nome != null) {
            this.nome = validarTexto(nome, "Nome do produto e obrigatorio");
        }
        if (descricao != null) {
            this.descricao = descricao;
        }
        if (precoVenda != null) {
            this.precoVenda = precoVenda;
        }
        if (precoCusto != null) {
            this.precoCusto = precoCusto;
        }
        if (categoria != null) {
            this.categoria = categoria;
            categoria.adicionarProduto(this);
        }
        if (taxaIVA != null) {
            TaxaIVA.validarPercentagem(taxaIVA.getPercentagem());
            this.taxaIVA = taxaIVA;
            taxaIVA.adicionarProduto(this);
        }
        if (fornecedorPrincipal != null) {
            this.fornecedorPrincipal = fornecedorPrincipal;
        }
        if (ativo != null) {
            this.ativo = ativo;
        }
    }

    public String getCodigo() {
        return codigo;
    }

    public String getCodigoBarras() {
        return codigoBarras == null ? codigo : codigoBarras;
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

    public TaxaIVA getTaxaIva() {
        return taxaIVA;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public Fornecedor getFornecedorPrincipal() {
        return fornecedorPrincipal;
    }

    public boolean isAtivo() {
        return ativo;
    }

    private String validarTexto(String valor, String mensagem) {
        String normalizado = Objects.requireNonNull(valor, mensagem).trim();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException(mensagem);
        }
        return normalizado;
    }
}
