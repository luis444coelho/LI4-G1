package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilTest {

    @Test
    void deveValidarPermissoesDoPerfil() {
        PerfilUtilizador perfil = PerfilUtilizador.GERENTE;

        assertTrue(perfil.temPermissao("STOCK_WRITE"));
        assertFalse(perfil.temPermissao("PDV_WRITE"));
    }

    @Test
    void deveProtegerColecaoDePermissoesContraAlteracoesExternas() {
        List<String> permissoes = PerfilUtilizador.GERENTE.getPermissoes();

        assertThrows(UnsupportedOperationException.class, () -> permissoes.add("STOCK_WRITE"));
    }

    @Test
    void normalizaNomePermiteGlobalAdminERegistaUtilizadoresSemDuplicar() {
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Perfil perfil = new Perfil(" gestor ", List.of("GLOBAL_ADMIN"));
        Utilizador utilizador = new Utilizador("gestor", "hash", "Gestor", perfil, loja);

        perfil.adicionarUtilizador(utilizador);
        perfil.adicionarUtilizador(utilizador);
        perfil.adicionarUtilizador(null);

        assertEquals("GESTOR", perfil.getNome());
        assertTrue(perfil.temPermissao("QUALQUER_PERMISSAO"));
        assertEquals(1, perfil.getUtilizadores().size());
        assertThrows(UnsupportedOperationException.class, () -> perfil.getUtilizadores().clear());
    }

    @Test
    void rejeitaNomeNuloOuVazioEPermissoesNulasCriamListaVazia() {
        Perfil semPermissoes = new Perfil("FUNCIONARIO", null);

        assertTrue(semPermissoes.getPermissoes().isEmpty());
        assertThrows(NullPointerException.class, () -> new Perfil(null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Perfil("   ", List.of()));
    }
}
