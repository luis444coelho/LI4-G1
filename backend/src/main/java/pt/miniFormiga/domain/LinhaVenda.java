package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "linhas_venda")
public class LinhaVenda extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxaIvaPercentagem;

    @Transient
    private BigDecimal totalLinha = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean anulada;

    protected LinhaVenda() {
    }

    public LinhaVenda(Venda venda, Produto produto, int quantidade) {
        this(venda, produto, quantidade, produto == null ? null : produto.getPrecoVenda());
    }

    public LinhaVenda(Venda venda, Produto produto, int quantidade, BigDecimal precoUnitario) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade da linha de venda deve ser positiva");
        }
        this.venda = Objects.requireNonNull(venda, "Venda e obrigatoria");
        this.produto = Objects.requireNonNull(produto, "Produto e obrigatorio");
        this.quantidade = quantidade;
        this.precoUnitario = Objects.requireNonNull(precoUnitario, "Preco unitario e obrigatorio");
        this.taxaIvaPercentagem = produto.getTaxaIVA().getPercentagem();
        this.anulada = false;
        calcularTotal();
        this.venda.adicionarLinha(this);
    }

    public void calcularTotal() {
        if (anulada) {
            this.totalLinha = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }
        this.totalLinha = precoUnitario
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void anular() {
        this.anulada = true;
        calcularTotal();
    }

    public Venda getVenda() {
        return venda;
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

    public BigDecimal getTaxaIvaPercentagem() {
        return taxaIvaPercentagem;
    }

    public BigDecimal getTotalLinha() {
        return totalLinha;
    }

    public boolean isAnulada() {
        return anulada;
    }
}
