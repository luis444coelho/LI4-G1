package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilTest {

    @Test
    void deveValidarPermissoesDoPerfil() {
        Perfil perfil = new Perfil("GERENTE", List.of("PDV_READ", "STOCK_READ"));

        assertTrue(perfil.temPermissao("PDV_READ"));
        assertFalse(perfil.temPermissao("UTILIZADOR_WRITE"));
    }

    @Test
    void deveProtegerColecaoDePermissoesContraAlteracoesExternas() {
        List<String> permissoes = new ArrayList<>(List.of("PDV_READ"));
        Perfil perfil = new Perfil("GERENTE", permissoes);

        permissoes.add("UTILIZADOR_WRITE");

        assertFalse(perfil.temPermissao("UTILIZADOR_WRITE"));
        assertThrows(UnsupportedOperationException.class, () -> perfil.getPermissoes().add("STOCK_WRITE"));
    }
}
