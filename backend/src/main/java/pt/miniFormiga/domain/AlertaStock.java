package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Column(nullable = false)
    private boolean resolvido;

    @Column
    private LocalDateTime dataResolucao;

    @ManyToMany
    @JoinTable(
            name = "alerta_stock_destinatarios",
            joinColumns = @JoinColumn(name = "alerta_id"),
            inverseJoinColumns = @JoinColumn(name = "utilizador_id")
    )
    private List<Utilizador> destinatarios = new ArrayList<>();

    protected AlertaStock() {
    }

    public AlertaStock(Stock stock, int quantidadeNoMomento) {
        this.stock = stock;
        this.dataHora = LocalDateTime.now();
        this.quantidadeNoMomento = quantidadeNoMomento;
        this.lido = false;
        this.resolvido = false;
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
        if (utilizador != null && !destinatarios.contains(utilizador)) {
            destinatarios.add(utilizador);
        }
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

    public boolean isResolvido() {
        return resolvido;
    }

    public LocalDateTime getDataResolucao() {
        return dataResolucao;
    }

    public List<Utilizador> getDestinatarios() {
        return Collections.unmodifiableList(destinatarios);
    }
}
