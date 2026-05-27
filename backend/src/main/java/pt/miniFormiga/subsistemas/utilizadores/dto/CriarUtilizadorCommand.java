package pt.miniFormiga.subsistemas.utilizadores.dto;

import java.util.UUID;

public record CriarUtilizadorCommand(
        String username,
        String password,
        String nome,
        String email,
        String perfil,
        UUID lojaId,
        String lojaNome
) {
}
