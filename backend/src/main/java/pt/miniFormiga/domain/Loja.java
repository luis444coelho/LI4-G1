package pt.miniFormiga.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lojas")
public class Loja extends EntidadeBase {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String morada;

    @Column(nullable = false, unique = true, length = 9)
    private String nif;

    @Column
    private String telefone;

    @Column(nullable = false)
    private boolean ativa = true;

    @OneToMany(mappedBy = "loja", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Utilizador> utilizadores = new ArrayList<>();

    protected Loja() {
    }

    public Loja(String nome, String morada, String nif) {
        this(nome, morada, nif, null);
    }

    public Loja(String nome, String morada, String nif, String telefone) {
        this.nome = nome;
        this.morada = morada;
        this.nif = nif;
        this.telefone = telefone;
        this.ativa = true;
    }

    public Loja(UUID id, String nome, String morada, String nif, String telefone) {
        this(nome, morada, nif, telefone);
        definirId(id);
    }

    public FechoCaixa efetuarFechoCaixa(LocalDate data) {
        return new FechoCaixa(this, data);
    }

    public Sincronizacao iniciarSincronizacao() {
        return new Sincronizacao(this);
    }

    public void ativar() {
        this.ativa = true;
    }

    public void desativar() {
        this.ativa = false;
    }

    void adicionarUtilizador(Utilizador utilizador) {
        if (utilizador != null && !utilizadores.contains(utilizador)) {
            utilizadores.add(utilizador);
        }
    }

    public String getNome() {
        return nome;
    }

    public String getMorada() {
        return morada;
    }

    public String getNif() {
        return nif;
    }

    public String getTelefone() {
        return telefone;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public List<Utilizador> getUtilizadores() {
        return Collections.unmodifiableList(utilizadores);
    }
}
