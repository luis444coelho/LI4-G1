package pt.miniFormiga.subsistemas.utilizadores.dto;

import java.util.UUID;

public record AtualizarUtilizadorCommand(
        String nome,
        String email,
        String perfil,
        UUID lojaId,
        String password,
        Boolean ativo
) {
}
