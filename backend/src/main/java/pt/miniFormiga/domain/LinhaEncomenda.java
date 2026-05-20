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
@Table(name = "linhas_encomenda")
public class LinhaEncomenda extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Encomenda encomenda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalLinha = BigDecimal.ZERO;

    protected LinhaEncomenda() {
    }

    public LinhaEncomenda(Encomenda encomenda, Produto produto, int quantidade, BigDecimal precoUnitario) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade da linha de encomenda deve ser positiva");
        }
        this.encomenda = Objects.requireNonNull(encomenda, "Encomenda e obrigatoria");
        this.produto = Objects.requireNonNull(produto, "Produto e obrigatorio");
        this.quantidade = quantidade;
        this.precoUnitario = Objects.requireNonNull(precoUnitario, "Preco unitario e obrigatorio");
        if (this.precoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preco unitario nao pode ser negativo");
        }
        this.encomenda.adicionarLinha(this);
        calcularTotal();
    }

    public BigDecimal calcularTotal() {
        this.totalLinha = precoUnitario
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
        return totalLinha;
    }

    public Encomenda getEncomenda() {
        return encomenda;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public BigDecimal getTotalLinha() {
        return totalLinha;
    }
}
