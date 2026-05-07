package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
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

    @Column
    private String email;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

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
        this(username, passwordHash, nome, null, perfil, loja);
    }

    public Utilizador(String username, String passwordHash, String nome, String email, Perfil perfil, Loja loja) {
        this.username = validarTexto(username, "Username e obrigatorio");
        this.passwordHash = validarTexto(passwordHash, "Password e obrigatoria");
        this.nome = validarTexto(nome, "Nome e obrigatorio");
        this.email = normalizarEmail(email);
        this.perfil = Objects.requireNonNull(perfil, "Perfil e obrigatorio");
        this.loja = Objects.requireNonNull(loja, "Loja e obrigatoria");
        this.ativo = true;
        this.dataCriacao = LocalDateTime.now();
        this.perfil.adicionarUtilizador(this);
        this.loja.adicionarUtilizador(this);
    }

    public boolean autenticar(String password) {
        return ativo && Objects.equals(passwordHash, password);
    }

    public void alterarPassword(String nova) {
        this.passwordHash = validarTexto(nova, "Password e obrigatoria");
    }

    @PrePersist
    private void prePersistDataCriacao() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
    }

    public boolean temPermissao(String modulo) {
        return ativo && perfil != null && perfil.temPermissao(modulo);
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
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

    public String getEmail() {
        return email;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
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

    private String validarTexto(String valor, String mensagem) {
        String normalizado = Objects.requireNonNull(valor, mensagem).trim();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException(mensagem);
        }
        return normalizado;
    }

    private String normalizarEmail(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim().toLowerCase();
    }
}
