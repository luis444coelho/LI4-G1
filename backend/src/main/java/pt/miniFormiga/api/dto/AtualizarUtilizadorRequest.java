package pt.miniFormiga.api.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AtualizarUtilizadorRequest(
        @Size(max = 120) String nome,
        @jakarta.validation.constraints.Email @Size(max = 160) String email,
        UUID perfilId,
        UUID lojaId,
        @Size(min = 8, max = 120) String password,
        Boolean ativo
) {
}
