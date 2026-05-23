package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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
    private ProdutoLoja produtoLoja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id")
    private Loja loja;

    @Transient
    private Stock stockLegado;

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

    public AlertaStock(Stock stock, int quantidadeNoMomento) {
        this(stock.getProduto(), quantidadeNoMomento);
        this.stockLegado = stock;
        this.loja = stock.getLoja();
    }

    public AlertaStock(Produto produto, int quantidadeNoMomento) {
        this.produto = produto;
        this.dataHora = LocalDateTime.now();
        this.quantidadeNoMomento = quantidadeNoMomento;
        this.lido = false;
        this.resolvido = false;
    }

    public AlertaStock(ProdutoLoja produtoLoja, int quantidadeNoMomento) {
        this(produtoLoja.getProduto(), quantidadeNoMomento);
        this.produtoLoja = produtoLoja;
        this.loja = produtoLoja.getLoja();
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
        return produto == null && produtoLoja != null ? produtoLoja.getProduto() : produto;
    }

    public ProdutoLoja getProdutoLoja() {
        return produtoLoja;
    }

    public Loja getLoja() {
        return loja == null && produtoLoja != null ? produtoLoja.getLoja() : loja;
    }

    public Stock getStock() {
        return stockLegado;
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
