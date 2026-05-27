package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

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
}
