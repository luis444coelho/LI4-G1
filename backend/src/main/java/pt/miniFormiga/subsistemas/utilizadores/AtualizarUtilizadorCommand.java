package pt.miniFormiga.subsistemas.utilizadores;

import java.util.UUID;

public record AtualizarUtilizadorCommand(
        String nome,
        String email,
        UUID perfilId,
        UUID lojaId,
        String password,
        Boolean ativo
) {
}
