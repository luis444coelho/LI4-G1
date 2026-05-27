package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ajustes_inventario")
public class AjusteInventario extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_loja_id")
    private StockProdutoLoja stockProdutoLoja;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoAjusteCodigo motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 2000)
    private String observacoes;

    protected AjusteInventario() {
    }

    public AjusteInventario(Produto produto,
                            MotivoAjusteCodigo motivo,
                            Utilizador responsavel,
                            int quantidade,
                            String observacoes) {
        this.produto = produto;
        this.motivo = motivo;
        this.utilizador = responsavel;
        this.quantidade = quantidade;
        this.observacoes = observacoes;
        this.dataHora = LocalDateTime.now();
    }

    public AjusteInventario(StockProdutoLoja stockProdutoLoja,
                            MotivoAjusteCodigo motivo,
                            Utilizador responsavel,
                            int quantidade,
                            String observacoes) {
        this(stockProdutoLoja.getProduto(), motivo, responsavel, quantidade, observacoes);
        this.stockProdutoLoja = stockProdutoLoja;
    }

    public Produto getProduto() {
        return produto == null && stockProdutoLoja != null ? stockProdutoLoja.getProduto() : produto;
    }

    public StockProdutoLoja getStockProdutoLoja() {
        return stockProdutoLoja;
    }

    public MotivoAjusteCodigo getMotivo() {
        return motivo;
    }

    public MotivoAjusteCodigo getMotivoAjuste() {
        return motivo;
    }

    public Utilizador getResponsavel() {
        return utilizador;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getObservacoes() {
        return observacoes;
    }
}
