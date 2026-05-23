package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilizadorTest {

    @Test
    void deveAutorizarQuandoUtilizadorAtivoETemPermissao() {
        Loja loja = new Loja("Loja Matosinhos", "Rua Sul", "111222333");
        Perfil perfil = new Perfil("FUNCIONARIO", List.of("PDV_WRITE"));
        Utilizador utilizador = new Utilizador("operador1", "$2a$10$hash", "Joao", perfil, loja);

        assertTrue(utilizador.temPermissao("PDV_WRITE"));
        assertFalse(utilizador.temPermissao("STOCK_WRITE"));
    }

    @Test
    void naoDeveAutorizarQuandoUtilizadorDesativado() {
        Loja loja = new Loja("Loja Gaia", "Rua Centro", "123123123");
        Perfil perfil = new Perfil("FUNCIONARIO", List.of("PDV_WRITE"));
        Utilizador utilizador = new Utilizador("operador2", "$2a$10$hash", "Ana", perfil, loja);

        utilizador.desativar();

        assertFalse(utilizador.temPermissao("PDV_WRITE"));
    }

    @Test
    void deveManterLigacoesComPerfilELoja() {
        Loja loja = new Loja("Loja Barcelos", "Rua da Feira", "555444333");
        Perfil perfil = new Perfil("FUNCIONARIO", List.of("PDV_WRITE"));
        Utilizador utilizador = new Utilizador("operador3", "$2a$10$hash", "Luis", perfil, loja);

        assertEquals(loja, utilizador.getLoja());
        assertEquals(PerfilUtilizador.FUNCIONARIO, utilizador.getPerfil());
        assertTrue(loja.getUtilizadores().contains(utilizador));
        assertTrue(perfil.getUtilizadores().isEmpty());
    }

    @Test
    void deveExigirPerfilELoja() {
        Loja loja = new Loja("Loja Aveiro", "Rua do Forum", "333222111");
        Perfil perfil = new Perfil("GERENTE", List.of("UTILIZADOR_WRITE"));

        assertThrows(NullPointerException.class, () -> new Utilizador("u1", "hash", "Nome", null, loja));
        assertThrows(NullPointerException.class, () -> new Utilizador("u2", "hash", "Nome", perfil, null));
    }
}
