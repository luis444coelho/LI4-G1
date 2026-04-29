package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sincronizacoes")
public class Sincronizacao extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column
    private LocalDateTime dataHoraFim;

    @Column(nullable = false)
    private int quantidadeRegistos;

    @Column(length = 2000)
    private String mensagemErro;

    protected Sincronizacao() {
    }

    public Sincronizacao(Loja loja) {
        this.loja = loja;
        this.dataHoraInicio = LocalDateTime.now();
        this.quantidadeRegistos = 0;
    }

    public Loja getLoja() {
        return loja;
    }

    public LocalDateTime getDataHoraInicio() {
        return dataHoraInicio;
    }

    public LocalDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public int getQuantidadeRegistos() {
        return quantidadeRegistos;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }
}
