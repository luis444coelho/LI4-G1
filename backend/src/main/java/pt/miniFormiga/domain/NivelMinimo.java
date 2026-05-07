package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "niveis_minimos")
public class NivelMinimo extends EntidadeBase {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false)
    private LocalDateTime dataDefinicao;

    protected NivelMinimo() {
    }

    public NivelMinimo(Stock stock, int quantidade) {
        this.stock = stock;
        this.quantidade = quantidade;
        this.dataDefinicao = LocalDateTime.now();
        if (this.stock != null) {
            this.stock.definirNivelMinimo(this);
        }
    }

    public void atualizarQuantidade(int quantidade) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("Nivel minimo nao pode ser negativo");
        }
        this.quantidade = quantidade;
        this.dataDefinicao = LocalDateTime.now();
    }

    public Stock getStock() {
        return stock;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public LocalDateTime getDataDefinicao() {
        return dataDefinicao;
    }
}
