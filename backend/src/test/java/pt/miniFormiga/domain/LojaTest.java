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
        Loja loja = new Loja("Loja Braga", "Rua Central 10", "123456789", "253000000");

        assertEquals("Loja Braga", loja.getNome());
        assertEquals("Rua Central 10", loja.getMorada());
        assertEquals("123456789", loja.getNif());
        assertEquals("253000000", loja.getTelefone());
        assertTrue(loja.isAtiva());
    }

    @Test
    void construtorComIdMantemIdentidadeExternaDaLoja() {
        java.util.UUID id = java.util.UUID.randomUUID();

        Loja loja = new Loja(id, "Loja Famalicao", "Rua Nova", "222333444", "252000000");

        assertEquals(id, loja.getId());
        assertEquals("Loja Famalicao", loja.getNome());
        assertEquals("Rua Nova", loja.getMorada());
        assertEquals("222333444", loja.getNif());
        assertEquals("252000000", loja.getTelefone());
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
        Utilizador utilizador = new Utilizador("gerente.viana", "$2a$10$hash", "Maria", PerfilUtilizador.GERENTE, loja);

        assertEquals(1, loja.getUtilizadores().size());
        assertTrue(loja.getUtilizadores().contains(utilizador));
        assertThrows(UnsupportedOperationException.class, () -> loja.getUtilizadores().add(utilizador));
    }
}
