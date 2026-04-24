package pt.miniFormiga.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "lojas")
public class Loja extends EntidadeBase {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String morada;

    @Column(nullable = false, unique = true, length = 9)
    private String nif;

    @Column(nullable = false)
    private boolean ativa = true;

    @OneToMany(mappedBy = "loja", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Utilizador> utilizadores = new ArrayList<>();

    protected Loja() {
    }

    public Loja(String nome, String morada, String nif) {
        this.nome = nome;
        this.morada = morada;
        this.nif = nif;
        this.ativa = true;
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

    public boolean isAtiva() {
        return ativa;
    }

    public List<Utilizador> getUtilizadores() {
        return Collections.unmodifiableList(utilizadores);
    }
}
