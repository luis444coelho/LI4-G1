package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "utilizadores")
public class Utilizador extends EntidadeBase {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @OneToMany(mappedBy = "utilizador")
    private List<LogAuditoria> logsAuditoria = new ArrayList<>();

    protected Utilizador() {
    }

    public Utilizador(String username, String passwordHash, String nome, Perfil perfil, Loja loja) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nome = nome;
        this.perfil = Objects.requireNonNull(perfil, "Perfil e obrigatorio");
        this.loja = Objects.requireNonNull(loja, "Loja e obrigatoria");
        this.ativo = true;
        this.perfil.adicionarUtilizador(this);
        this.loja.adicionarUtilizador(this);
    }

    public boolean temPermissao(String modulo) {
        return ativo && perfil != null && perfil.temPermissao(modulo);
    }

    public void desativar() {
        this.ativo = false;
    }

    void adicionarLogAuditoria(LogAuditoria logAuditoria) {
        if (logAuditoria != null && !logsAuditoria.contains(logAuditoria)) {
            logsAuditoria.add(logAuditoria);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNome() {
        return nome;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public Loja getLoja() {
        return loja;
    }

    public List<LogAuditoria> getLogsAuditoria() {
        return Collections.unmodifiableList(logsAuditoria);
    }
}
