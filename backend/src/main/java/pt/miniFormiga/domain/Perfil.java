package pt.miniFormiga.domain;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
// legacy non-entity
// legacy table removed
public class Perfil extends EntidadeBase {
    @Column(nullable = false, unique = true)
    private String nome;
    @ElementCollection
    @CollectionTable(name = "perfil_permissoes", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "permissao", nullable = false)
    private List<String> permissoes = new ArrayList<>();
    @OneToMany(mappedBy = "perfil")
    private List<Utilizador> utilizadores = new ArrayList<>();
    protected Perfil() {
    }
    public Perfil(String nome, List<String> permissoes) {
        this.nome = validarTexto(nome, "Nome do perfil e obrigatorio").toUpperCase();
        this.permissoes = permissoes == null ? new ArrayList<>() : new ArrayList<>(permissoes);
    }
    public boolean temPermissao(String permissao) {
        return permissoes.contains(permissao) || permissoes.contains("GLOBAL_ADMIN");
    }
    void adicionarUtilizador(Utilizador utilizador) {
        if (utilizador != null && !utilizadores.contains(utilizador)) {
            utilizadores.add(utilizador);
        }
    }
    public String getNome() {
        return nome;
    }
    public List<String> getPermissoes() {
        return Collections.unmodifiableList(permissoes);
    }
    public List<Utilizador> getUtilizadores() {
        return Collections.unmodifiableList(utilizadores);
    }
    private String validarTexto(String valor, String mensagem) {
        String normalizado = Objects.requireNonNull(valor, mensagem).trim();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException(mensagem);
        }
        return normalizado;
    }
}
