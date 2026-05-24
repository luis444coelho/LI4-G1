package pt.miniFormiga.domain;

import pt.miniFormiga.subsistemas.utilizadores.Permissao;

import java.util.List;

public enum PerfilUtilizador {
    GESTOR(List.of(
            Permissao.GLOBAL_ADMIN,
            Permissao.UTILIZADORES_READ,
            Permissao.UTILIZADORES_WRITE,
            Permissao.UTILIZADORES_DELETE,
            Permissao.RELATORIOS_READ,
            Permissao.SINCRONIZACAO_WRITE
    )),
    GERENTE(List.of(
            Permissao.UTILIZADORES_READ,
            Permissao.UTILIZADORES_WRITE,
            Permissao.STOCK_WRITE,
            Permissao.ENCOMENDAS_WRITE,
            Permissao.RELATORIOS_READ
    )),
    FUNCIONARIO(List.of(Permissao.PDV_WRITE)),
    ARMAZEM(List.of(
            Permissao.STOCK_READ,
            Permissao.STOCK_WRITE,
            Permissao.ENCOMENDAS_WRITE
    ));

    private final List<String> permissoes;

    PerfilUtilizador(List<String> permissoes) {
        this.permissoes = List.copyOf(permissoes);
    }

    public List<String> getPermissoes() {
        return permissoes;
    }

    public String getNome() {
        return name();
    }

    public boolean temPermissao(String permissao) {
        return permissoes.contains(permissao);
    }
}
