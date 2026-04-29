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
    @JoinColumn(name = "nivel_minimo_id", nullable = false)
    private NivelMinimo nivelMinimo;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private int quantidadeNoMomento;

    @Column(nullable = false)
    private boolean lido;

    @ManyToMany
    @JoinTable(
            name = "alerta_stock_destinatarios",
            joinColumns = @JoinColumn(name = "alerta_stock_id"),
            inverseJoinColumns = @JoinColumn(name = "utilizador_id")
    )
    private List<Utilizador> destinatarios = new ArrayList<>();

    protected AlertaStock() {
    }

    public AlertaStock(NivelMinimo nivelMinimo, int quantidadeNoMomento) {
        this.nivelMinimo = nivelMinimo;
        this.dataHora = LocalDateTime.now();
        this.quantidadeNoMomento = quantidadeNoMomento;
        this.lido = false;
    }

    public void associarUtilizadores(List<Utilizador> utilizadores) {
        destinatarios.clear();
        if (utilizadores != null) {
            for (Utilizador utilizador : utilizadores) {
                if (utilizador != null && !destinatarios.contains(utilizador)) {
                    destinatarios.add(utilizador);
                }
            }
        }
    }

    public NivelMinimo getNivelMinimo() {
        return nivelMinimo;
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

    public List<Utilizador> getDestinatarios() {
        return Collections.unmodifiableList(destinatarios);
    }
}
