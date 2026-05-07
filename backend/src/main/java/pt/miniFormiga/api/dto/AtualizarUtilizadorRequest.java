package pt.miniFormiga.api.dto;

import jakarta.validation.constraints.Size;

public record AtualizarUtilizadorRequest(
        @Size(min = 8, max = 120) String password,
        Boolean ativo
) {
}
