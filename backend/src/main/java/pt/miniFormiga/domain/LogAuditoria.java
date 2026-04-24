package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "logs_auditoria")
public class LogAuditoria extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false, updatable = false)
    private Utilizador utilizador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoOperacao tipoOperacao;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, length = 2000, updatable = false)
    private String descricao;

    @Column(nullable = false, updatable = false)
    private String entidadeAfetada;

    @Column(nullable = false, updatable = false)
    private UUID entidadeId;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String dadosAntes;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String dadosDepois;

    protected LogAuditoria() {
    }

    public LogAuditoria(Utilizador utilizador,
                        TipoOperacao tipoOperacao,
                        String descricao,
                        String entidadeAfetada,
                        UUID entidadeId,
                        String dadosAntes,
                        String dadosDepois) {
        this.utilizador = Objects.requireNonNull(utilizador, "Utilizador e obrigatorio");
        this.tipoOperacao = Objects.requireNonNull(tipoOperacao, "Tipo de operacao e obrigatorio");
        this.dataHora = LocalDateTime.now();
        this.descricao = Objects.requireNonNull(descricao, "Descricao e obrigatoria");
        this.entidadeAfetada = Objects.requireNonNull(entidadeAfetada, "Entidade afetada e obrigatoria");
        this.entidadeId = Objects.requireNonNull(entidadeId, "Identificador da entidade e obrigatorio");
        this.dadosAntes = dadosAntes;
        this.dadosDepois = dadosDepois;
        this.utilizador.adicionarLogAuditoria(this);
    }

    @PrePersist
    private void prePersistDataHora() {
        if (this.dataHora == null) {
            this.dataHora = LocalDateTime.now();
        }
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    public TipoOperacao getTipoOperacao() {
        return tipoOperacao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getEntidadeAfetada() {
        return entidadeAfetada;
    }

    public UUID getEntidadeId() {
        return entidadeId;
    }

    public String getDadosAntes() {
        return dadosAntes;
    }

    public String getDadosDepois() {
        return dadosDepois;
    }
}
