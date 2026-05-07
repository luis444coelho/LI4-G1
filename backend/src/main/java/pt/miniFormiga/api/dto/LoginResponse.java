package pt.miniFormiga.api.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String tokenType,
        String accessToken,
        UUID utilizadorId,
        String nome,
        String perfil,
        UUID lojaId,
        List<String> permissoes
) {
}
