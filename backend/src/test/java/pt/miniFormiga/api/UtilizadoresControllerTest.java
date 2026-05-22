package pt.miniFormiga.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.PerfilRepository;
import pt.miniFormiga.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.utilizadores.ISubUtilizadores;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UtilizadoresControllerTest {

    @Test
    void utilizadorSemGlobalAdminFicaLimitadoALojaPropriaAoListarUtilizadores() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        PerfilRepository perfilRepository = mock(PerfilRepository.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                perfilRepository,
                lojaRepository,
                utilizadorRepository
        );
        Loja lojaPropria = new Loja("Loja Braga", "Rua Central", "123456789");
        Loja outraLoja = new Loja("Loja Porto", "Rua Norte", "987654321");
        Perfil gerente = new Perfil("GERENTE", List.of(Permissao.UTILIZADORES_READ));
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", gerente, lojaPropria);
        PageRequest pageable = PageRequest.of(0, 10);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("gerente.braga");
        when(authentication.getAuthorities()).thenAnswer(invocation -> List.of(
                new SimpleGrantedAuthority(Permissao.UTILIZADORES_READ),
                new SimpleGrantedAuthority("ROLE_GERENTE")
        ));
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(gerenteBraga));
        when(utilizadores.listarUtilizadoresPorLoja(lojaPropria.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(gerenteBraga), pageable, 1));

        var response = controller.listar(outraLoja.getId(), pageable, authentication);

        assertEquals(1, response.getTotalElements());
        assertEquals(lojaPropria.getId(), response.getContent().get(0).lojaId());
        verify(utilizadores).listarUtilizadoresPorLoja(lojaPropria.getId(), pageable);
    }
}
