package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilTest {

    @Test
    void deveValidarPermissoesDoPerfil() {
        PerfilUtilizador perfil = PerfilUtilizador.GERENTE;

        assertTrue(perfil.temPermissao(Permissao.STOCK_WRITE));
        assertFalse(perfil.temPermissao(Permissao.PDV_WRITE));
    }

    @Test
    void deveProtegerColecaoDePermissoesContraAlteracoesExternas() {
        var permissoes = PerfilUtilizador.GERENTE.getPermissoes();

        assertTrue(permissoes.contains(Permissao.STOCK_WRITE));
        assertThrows(UnsupportedOperationException.class, () -> permissoes.add(Permissao.STOCK_WRITE));
    }

    @Test
    void nomeDoPerfilCorrespondeAoCodigoDoEnum() {
        assertEquals("GESTOR", PerfilUtilizador.GESTOR.getNome());
        assertEquals("GERENTE", PerfilUtilizador.GERENTE.getNome());
        assertEquals("FUNCIONARIO", PerfilUtilizador.FUNCIONARIO.getNome());
        assertEquals("ARMAZEM", PerfilUtilizador.ARMAZEM.getNome());
    }

    @Test
    void gestorTemPermissaoAdministrativaQuePerfisOperacionaisNaoTem() {
        assertTrue(PerfilUtilizador.GESTOR.temPermissao(Permissao.GLOBAL_ADMIN));
        assertFalse(PerfilUtilizador.GERENTE.temPermissao(Permissao.GLOBAL_ADMIN));
        assertFalse(PerfilUtilizador.FUNCIONARIO.temPermissao(Permissao.GLOBAL_ADMIN));
        assertFalse(PerfilUtilizador.ARMAZEM.temPermissao(Permissao.GLOBAL_ADMIN));
    }
}
