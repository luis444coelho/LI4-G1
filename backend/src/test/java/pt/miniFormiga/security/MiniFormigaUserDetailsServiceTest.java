package pt.miniFormiga.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiniFormigaUserDetailsServiceTest {

    private final UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
    private final MiniFormigaUserDetailsService service = new MiniFormigaUserDetailsService(utilizadorRepository);

    @Test
    void loadUserByUsernameMapeiaRolePermissoesEEstadoAtivo() {
        Utilizador utilizador = new Utilizador(
                "gerente.braga",
                "hash",
                "Gerente Braga",
                "gerente@mini-formiga.pt",
                PerfilUtilizador.GERENTE,
                new Loja("Loja Braga", "Rua Central", "123456789")
        );
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(utilizador));

        var userDetails = service.loadUserByUsername("gerente.braga");

        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GERENTE")));
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Permissao.STOCK_WRITE)));
    }

    @Test
    void utilizadorInativoFicaDesativadoNoSpringSecurity() {
        Utilizador utilizador = new Utilizador(
                "operador.braga",
                "hash",
                "Operador Braga",
                "operador@mini-formiga.pt",
                PerfilUtilizador.FUNCIONARIO,
                new Loja("Loja Braga", "Rua Central", "123456789")
        );
        utilizador.desativar();
        when(utilizadorRepository.findByUsername("operador.braga")).thenReturn(Optional.of(utilizador));

        var userDetails = service.loadUserByUsername("operador.braga");

        assertFalse(userDetails.isEnabled());
    }

    @Test
    void utilizadorInexistenteFalhaComUsernameNotFound() {
        when(utilizadorRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("fantasma"));
    }
}
