package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "alertas_stock")
public class AlertaStock extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_loja_id")
    private StockProdutoLoja stockProdutoLoja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id")
    private Loja loja;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private int quantidadeNoMomento;

    @Column(nullable = false)
    private boolean lido;

    @Column(nullable = false)
    private boolean resolvido;

    @Column
    private LocalDateTime dataResolucao;

    protected AlertaStock() {
    }

    public AlertaStock(Produto produto, int quantidadeNoMomento) {
        this.produto = produto;
        this.dataHora = LocalDateTime.now();
        this.quantidadeNoMomento = quantidadeNoMomento;
        this.lido = false;
        this.resolvido = false;
    }

    public AlertaStock(Produto produto, Loja loja, int quantidadeNoMomento) {
        this(produto, quantidadeNoMomento);
        this.loja = loja;
    }

    public AlertaStock(StockProdutoLoja stockProdutoLoja, int quantidadeNoMomento) {
        this(stockProdutoLoja.getProduto(), quantidadeNoMomento);
        this.stockProdutoLoja = stockProdutoLoja;
        this.loja = stockProdutoLoja.getLoja();
    }

    public void marcarComoLido() {
        this.lido = true;
    }

    public void resolver() {
        this.resolvido = true;
        this.lido = true;
        this.dataResolucao = LocalDateTime.now();
    }

    public void adicionarDestinatario(Utilizador utilizador) {
        // Destinatarios sao derivados por perfil/regra, nao persistidos.
    }

    public Produto getProduto() {
        return produto == null && stockProdutoLoja != null ? stockProdutoLoja.getProduto() : produto;
    }

    public StockProdutoLoja getStockProdutoLoja() {
        return stockProdutoLoja;
    }

    public Loja getLoja() {
        return loja == null && stockProdutoLoja != null ? stockProdutoLoja.getLoja() : loja;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public int getQuantidadeNoMomento() {
        return quantidadeNoMomento;
    }

    public boolean isLido() {
        return lido;
    }

    public boolean isResolvido() {
        return resolvido;
    }

    public LocalDateTime getDataResolucao() {
        return dataResolucao;
    }

    public List<Utilizador> getDestinatarios() {
        return List.of();
    }
}
