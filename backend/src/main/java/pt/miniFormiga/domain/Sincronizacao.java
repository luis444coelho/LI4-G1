package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sincronizacoes")
public class Sincronizacao extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSincronizacaoCodigo estado;

    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column
    private LocalDateTime dataHoraFim;

    @Column(nullable = false)
    private int quantidadeRegistos;

    @Column(length = 2000)
    private String mensagemErro;

    @Column
    private LocalDateTime proximaTentativa;

    @Column(nullable = false)
    private int conflitosResolvidos;

    @Lob
    @Column
    private String payloadJson;

    @Lob
    @Column
    private String conflitosJson;

    protected Sincronizacao() {
    }

    public Sincronizacao(Loja loja) {
        this(loja, EstadoSincronizacaoCodigo.PENDENTE);
    }

    public Sincronizacao(Loja loja, EstadoSincronizacaoCodigo estado) {
        this.loja = loja;
        this.estado = estado;
        this.dataHoraInicio = LocalDateTime.now();
        this.quantidadeRegistos = 0;
        this.conflitosResolvidos = 0;
    }

    public void iniciar(EstadoSincronizacaoCodigo estadoEmCurso, String payloadJson, int quantidadeRegistos) {
        this.estado = estadoEmCurso;
        this.dataHoraInicio = LocalDateTime.now();
        this.dataHoraFim = null;
        this.payloadJson = payloadJson;
        this.quantidadeRegistos = quantidadeRegistos;
        this.mensagemErro = null;
        this.proximaTentativa = null;
    }

    public void concluir(EstadoSincronizacaoCodigo estadoFinal, String payloadJson, int quantidadeRegistos, String conflitosJson, int conflitosResolvidos) {
        this.estado = estadoFinal;
        this.payloadJson = payloadJson;
        this.quantidadeRegistos = quantidadeRegistos;
        this.conflitosJson = conflitosJson;
        this.conflitosResolvidos = conflitosResolvidos;
        this.dataHoraFim = LocalDateTime.now();
        this.mensagemErro = null;
        this.proximaTentativa = null;
    }

    public void falharMantendoPendente(EstadoSincronizacaoCodigo pendente, String payloadJson, int quantidadeRegistos, String mensagemErro, LocalDateTime proximaTentativa) {
        this.estado = pendente;
        this.payloadJson = payloadJson;
        this.quantidadeRegistos = quantidadeRegistos;
        this.mensagemErro = mensagemErro;
        this.proximaTentativa = proximaTentativa;
        this.dataHoraFim = null;
    }

    public Loja getLoja() {
        return loja;
    }

    public EstadoSincronizacaoCodigo getEstado() {
        return estado;
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

    public LocalDateTime getProximaTentativa() {
        return proximaTentativa;
    }

    public int getConflitosResolvidos() {
        return conflitosResolvidos;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getConflitosJson() {
        return conflitosJson;
    }
}
