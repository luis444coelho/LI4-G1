package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_stock")
public class AlertaStock extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private int quantidadeNoMomento;

    @Column(nullable = false)
    private boolean lido;

    protected AlertaStock() {
    }

    public AlertaStock(Stock stock, int quantidadeNoMomento) {
        this.stock = stock;
        this.dataHora = LocalDateTime.now();
        this.quantidadeNoMomento = quantidadeNoMomento;
        this.lido = false;
    }

    public void marcarComoLido() {
        this.lido = true;
    }

    public Stock getStock() {
        return stock;
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

}
