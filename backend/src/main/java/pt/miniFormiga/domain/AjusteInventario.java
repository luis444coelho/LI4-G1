package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ajustes_inventario")
public class AjusteInventario extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motivo_id", nullable = false)
    private MotivoAjuste motivoAjuste;

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

    public AjusteInventario(Stock stock,
                            MotivoAjuste motivo,
                            Utilizador responsavel,
                            int quantidade,
                            String observacoes) {
        this.stock = stock;
        this.motivoAjuste = motivo;
        this.utilizador = responsavel;
        this.quantidade = quantidade;
        this.observacoes = observacoes;
        this.dataHora = LocalDateTime.now();
    }

    public Stock getStock() {
        return stock;
    }

    public MotivoAjuste getMotivo() {
        return motivoAjuste;
    }

    public MotivoAjuste getMotivoAjuste() {
        return motivoAjuste;
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
