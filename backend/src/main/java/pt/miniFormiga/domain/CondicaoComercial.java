package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "condicoes_comerciais")
public class CondicaoComercial extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int prazoEntregaDias;

    @Column(nullable = false)
    private int quantidadeMinima;

    @Column(nullable = false)
    private LocalDate dataVigencia;

    protected CondicaoComercial() {
    }

    public CondicaoComercial(Fornecedor fornecedor,
                             Produto produto,
                             BigDecimal precoUnitario,
                             int prazoEntregaDias,
                             int quantidadeMinima,
                             LocalDate dataVigencia) {
        this.fornecedor = fornecedor;
        this.produto = produto;
        this.precoUnitario = precoUnitario;
        this.prazoEntregaDias = prazoEntregaDias;
        this.quantidadeMinima = quantidadeMinima;
        this.dataVigencia = dataVigencia;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public Produto getProduto() {
        return produto;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public int getQuantidadeMinima() {
        return quantidadeMinima;
    }

    public LocalDate getDataVigencia() {
        return dataVigencia;
    }
}
