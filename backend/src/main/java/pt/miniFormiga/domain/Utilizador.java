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
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "utilizadores")
public class Utilizador extends EntidadeBase implements Persistable<java.util.UUID> {

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
    private int tentativasFalhadas = 0;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PerfilUtilizador perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    protected Utilizador() {
    }

    public Utilizador(String username, String passwordHash, String nome, Perfil perfil, Loja loja) {
        this(username, passwordHash, nome, null, perfil == null ? null : perfil(perfil.getNome()), loja);
    }

    public Utilizador(String username, String passwordHash, String nome, String email, Perfil perfil, Loja loja) {
        this(username, passwordHash, nome, email, perfil == null ? null : perfil(perfil.getNome()), loja);
    }

    public Utilizador(String username, String passwordHash, String nome, String email, PerfilUtilizador perfil, Loja loja) {
        this.username = validarTexto(username, "Username e obrigatorio");
        this.passwordHash = validarTexto(passwordHash, "Password e obrigatoria");
        this.nome = validarTexto(nome, "Nome e obrigatorio");
        this.email = normalizarEmail(email);
        this.perfil = Objects.requireNonNull(perfil, "Perfil e obrigatorio");
        this.loja = Objects.requireNonNull(loja, "Loja e obrigatoria");
        this.ativo = true;
        this.dataCriacao = LocalDateTime.now();
        this.loja.adicionarUtilizador(this);
    }

    public boolean autenticar(String password) {
        return ativo && Objects.equals(passwordHash, password);
    }

    public void alterarPassword(String nova) {
        this.passwordHash = validarTexto(nova, "Password e obrigatoria");
    }

    public void atualizarDados(String nome, String email, Perfil perfil, Loja loja) {
        atualizarDados(nome, email, perfil == null ? null : perfil(perfil.getNome()), loja);
    }

    public void atualizarDados(String nome, String email, PerfilUtilizador perfil, Loja loja) {
        if (nome != null) {
            this.nome = validarTexto(nome, "Nome e obrigatorio");
        }
        if (email != null) {
            this.email = normalizarEmail(email);
        }
        if (perfil != null && !perfil.equals(this.perfil)) {
            this.perfil = perfil;
        }
        if (loja != null && !loja.equals(this.loja)) {
            this.loja = loja;
            this.loja.adicionarUtilizador(this);
        }
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
        this.tentativasFalhadas = 0;
    }

    public void registarFalhaAutenticacao(int limiteFalhas) {
        this.tentativasFalhadas++;
        if (this.tentativasFalhadas >= limiteFalhas) {
            desativar();
        }
    }

    public void registarAutenticacaoComSucesso() {
        this.tentativasFalhadas = 0;
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

    public int getTentativasFalhadas() {
        return tentativasFalhadas;
    }

    public PerfilUtilizador getPerfil() {
        return perfil;
    }

    public Loja getLoja() {
        return loja;
    }

    @Override
    @Transient
    public boolean isNew() {
        return getCreatedAt() == null;
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

    private static PerfilUtilizador perfil(String nome) {
        if ("RESPONSAVEL_ARMAZEM".equals(nome)) {
            return PerfilUtilizador.ARMAZEM;
        }
        return PerfilUtilizador.valueOf(nome);
    }
}
