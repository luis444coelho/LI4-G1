package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
public class Stock extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false)
    private LocalDateTime dataUltimaAtualizacao;

    @OneToOne(mappedBy = "stock", fetch = FetchType.LAZY)
    private NivelMinimo nivelMinimo;

    protected Stock() {
    }

    public Stock(Produto produto, Loja loja, int quantidade) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("Quantidade de stock nao pode ser negativa");
        }
        this.produto = produto;
        this.loja = loja;
        this.quantidade = quantidade;
        this.dataUltimaAtualizacao = LocalDateTime.now();
    }

    public void atualizarQuantidade(int delta) {
        int novaQuantidade = quantidade + delta;
        if (novaQuantidade < 0) {
            throw new IllegalArgumentException("Quantidade de stock nao pode ser negativa");
        }
        this.quantidade = novaQuantidade;
        this.dataUltimaAtualizacao = LocalDateTime.now();
    }

    public boolean estaAbaixoMinimo() {
        return nivelMinimo != null && quantidade < nivelMinimo.getQuantidade();
    }

    void definirNivelMinimo(NivelMinimo nivelMinimo) {
        this.nivelMinimo = nivelMinimo;
    }

    public Produto getProduto() {
        return produto;
    }

    public Loja getLoja() {
        return loja;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public LocalDateTime getDataUltimaAtualizacao() {
        return dataUltimaAtualizacao;
    }

    public NivelMinimo getNivelMinimo() {
        return nivelMinimo;
    }
}
