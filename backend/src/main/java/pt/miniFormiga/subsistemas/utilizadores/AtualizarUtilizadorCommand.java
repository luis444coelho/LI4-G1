package pt.miniFormiga.subsistemas.utilizadores;

public record AtualizarUtilizadorCommand(
        String password,
        Boolean ativo
) {
}
