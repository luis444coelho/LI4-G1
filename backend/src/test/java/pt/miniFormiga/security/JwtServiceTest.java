package pt.miniFormiga.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "MiniFormigaTestSecretKeyForJwtHs256WithAtLeast32Bytes";

    @Test
    void tokenEmitidoTransportaUsernameEAutoridadesDoUtilizador() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 30));
        Utilizador utilizador = utilizador("gestor.formiga");
        UserDetails details = User.withUsername("gestor.formiga")
                .password("hash")
                .authorities(Permissao.GLOBAL_ADMIN, "ROLE_GESTOR")
                .build();

        String token = jwtService.emitirToken(utilizador, details);

        assertEquals("gestor.formiga", jwtService.extrairUsername(token));
        assertTrue(jwtService.tokenValido(token, details));
    }

    @Test
    void tokenValidoRejeitaOutroUtilizadorOuAssinaturaErrada() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 30));
        UserDetails details = User.withUsername("gestor.formiga")
                .password("hash")
                .authorities(Permissao.GLOBAL_ADMIN)
                .build();
        String token = jwtService.emitirToken(utilizador("gestor.formiga"), details);
        UserDetails outro = User.withUsername("operador.braga")
                .password("hash")
                .authorities(Permissao.PDV_WRITE)
                .build();

        assertFalse(jwtService.tokenValido(token, outro));
        assertThrows(RuntimeException.class, () -> jwtService.extrairUsername(token + "alterado"));
    }

    private Utilizador utilizador(String username) {
        return new Utilizador(
                username,
                "hash",
                "Utilizador Teste",
                username + "@mini-formiga.pt",
                new Perfil("GESTOR", List.of(Permissao.GLOBAL_ADMIN)),
                new Loja("Loja Braga", "Rua Central", "123456789")
        );
    }
}
