package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "devolucoes")
public class Devolucao extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorCreditado;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, unique = true)
    private String numeroDocumento;

    protected Devolucao() {
    }

    public Devolucao(Venda venda, Produto produto, int quantidade, BigDecimal valorCreditado, String numeroDocumento) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade de devolucao deve ser positiva");
        }
        this.venda = Objects.requireNonNull(venda, "Venda e obrigatoria");
        this.produto = Objects.requireNonNull(produto, "Produto e obrigatorio");
        this.quantidade = quantidade;
        this.valorCreditado = Objects.requireNonNull(valorCreditado, "Valor creditado e obrigatorio");
        this.numeroDocumento = Objects.requireNonNull(numeroDocumento, "Numero do documento e obrigatorio");
        this.dataHora = LocalDateTime.now();
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

    public BigDecimal getValorCreditado() {
        return valorCreditado;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }
}
