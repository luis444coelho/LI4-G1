package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "produtos_lojas",
        uniqueConstraints = @UniqueConstraint(name = "uk_produtos_lojas_produto_loja", columnNames = {"produto_id", "loja_id"}))
public class ProdutoLoja extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Column(nullable = false)
    private int quantidadeStock;

    @Column
    private Integer nivelMinimo;

    @Column
    private String corredor;

    @Column
    private String prateleira;

    @Column(nullable = false)
    private boolean ativoNaLoja = true;

    @Column(nullable = false)
    private LocalDateTime stockUpdatedAt = LocalDateTime.now();

    protected ProdutoLoja() {
    }

    public ProdutoLoja(Produto produto, Loja loja, int quantidadeStock, Integer nivelMinimo) {
        this.produto = Objects.requireNonNull(produto, "Produto e obrigatorio");
        this.loja = Objects.requireNonNull(loja, "Loja e obrigatoria");
        definirStockInicial(quantidadeStock);
        definirNivelMinimo(nivelMinimo);
    }

    public void atualizarStock(int delta) {
        int novaQuantidade = quantidadeStock + delta;
        if (novaQuantidade < 0) {
            throw new IllegalArgumentException("Quantidade de stock nao pode ser negativa");
        }
        this.quantidadeStock = novaQuantidade;
        this.stockUpdatedAt = LocalDateTime.now();
    }

    public void definirStockInicial(int quantidade) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("Quantidade de stock nao pode ser negativa");
        }
        this.quantidadeStock = quantidade;
        this.stockUpdatedAt = LocalDateTime.now();
    }

    public void definirNivelMinimo(Integer nivelMinimo) {
        if (nivelMinimo != null && nivelMinimo < 0) {
            throw new IllegalArgumentException("Nivel minimo nao pode ser negativo");
        }
        this.nivelMinimo = nivelMinimo;
        this.stockUpdatedAt = LocalDateTime.now();
    }

    public boolean precisaReposicao() {
        return nivelMinimo != null && quantidadeStock <= nivelMinimo;
    }

    public void atualizarLocalizacao(String corredor, String prateleira) {
        this.corredor = corredor;
        this.prateleira = prateleira;
        this.stockUpdatedAt = LocalDateTime.now();
    }

    public void desativarNaLoja() {
        this.ativoNaLoja = false;
    }

    public Produto getProduto() {
        return produto;
    }

    public Loja getLoja() {
        return loja;
    }

    public int getQuantidadeStock() {
        return quantidadeStock;
    }

    public Integer getNivelMinimo() {
        return nivelMinimo;
    }

    public String getCorredor() {
        return corredor;
    }

    public String getPrateleira() {
        return prateleira;
    }

    public boolean isAtivoNaLoja() {
        return ativoNaLoja;
    }

    public LocalDateTime getStockUpdatedAt() {
        return stockUpdatedAt;
    }
}
