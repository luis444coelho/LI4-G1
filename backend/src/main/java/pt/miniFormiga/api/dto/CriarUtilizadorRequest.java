package pt.miniFormiga.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CriarUtilizadorRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(min = 8, max = 120) String password,
        @NotBlank @Size(max = 120) String nome,
        @Email @Size(max = 160) String email,
        String perfil,
        String perfilId,
        UUID lojaId,
        @Size(max = 120) String lojaNome
) {
    public String perfilEfetivo() {
        return perfil != null && !perfil.isBlank() ? perfil : perfilId;
    }
}
