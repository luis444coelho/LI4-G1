package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LojaTest {

    @Test
    void deveCriarLojaAtivaPorDefeito() {
        Loja loja = new Loja("Loja Braga", "Rua Central 10", "123456789");

        assertEquals("Loja Braga", loja.getNome());
        assertTrue(loja.isAtiva());
    }

    @Test
    void devePermitirDesativarEAtivarLoja() {
        Loja loja = new Loja("Loja Porto", "Rua Norte 20", "987654321");

        loja.desativar();
        assertFalse(loja.isAtiva());

        loja.ativar();
        assertTrue(loja.isAtiva());
    }

    @Test
    void deveAssociarUtilizadorCriadoALoja() {
        Loja loja = new Loja("Loja Viana", "Rua do Mercado", "111444777");
        Perfil perfil = new Perfil("GERENTE", List.of("UTILIZADOR_WRITE"));
        Utilizador utilizador = new Utilizador("gerente.viana", "$2a$10$hash", "Maria", perfil, loja);

        assertEquals(1, loja.getUtilizadores().size());
        assertTrue(loja.getUtilizadores().contains(utilizador));
        assertThrows(UnsupportedOperationException.class, () -> loja.getUtilizadores().add(utilizador));
    }
}
