package pt.miniFormiga.api.dto;

import pt.miniFormiga.domain.Utilizador;

import java.util.List;
import java.util.UUID;

public record UtilizadorResponse(
        UUID id,
        String username,
        String nome,
        String email,
        boolean ativo,
        String perfil,
        String perfilId,
        UUID lojaId,
        String loja,
        List<String> permissoes,
        int tentativasFalhadas,
        long version
) {
    public static UtilizadorResponse from(Utilizador utilizador) {
        return new UtilizadorResponse(
                utilizador.getId(),
                utilizador.getUsername(),
                utilizador.getNome(),
                utilizador.getEmail(),
                utilizador.isAtivo(),
                utilizador.getPerfil().getNome(),
                utilizador.getPerfil().getNome(),
                utilizador.getLoja().getId(),
                utilizador.getLoja().getNome(),
                utilizador.getPerfil().getPermissoes(),
                utilizador.getTentativasFalhadas(),
                utilizador.getVersion()
        );
    }
}
