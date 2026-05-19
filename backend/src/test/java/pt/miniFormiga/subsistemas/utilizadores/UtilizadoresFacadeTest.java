package pt.miniFormiga.subsistemas.utilizadores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.PerfilRepository;
import pt.miniFormiga.repository.UtilizadorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UtilizadoresFacadeTest {

    private UtilizadorRepository utilizadorRepository;
    private PerfilRepository perfilRepository;
    private LojaRepository lojaRepository;
    private PasswordEncoder passwordEncoder;
    private AuditoriaService auditoriaService;
    private UtilizadoresFacade facade;

    private Loja lojaBraga;
    private Loja lojaGuimaraes;
    private Perfil funcionario;
    private Perfil gerente;
    private Utilizador utilizador;

    @BeforeEach
    void setUp() {
        utilizadorRepository = mock(UtilizadorRepository.class);
        perfilRepository = mock(PerfilRepository.class);
        lojaRepository = mock(LojaRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditoriaService = mock(AuditoriaService.class);
        facade = new UtilizadoresFacade(
                utilizadorRepository,
                perfilRepository,
                lojaRepository,
                passwordEncoder,
                auditoriaService
        );

        lojaBraga = new Loja("Loja Braga", "Rua Central", "123456789");
        lojaGuimaraes = new Loja("Loja Guimaraes", "Rua Nova", "987654321");
        funcionario = new Perfil("FUNCIONARIO", List.of(Permissao.PDV_WRITE));
        gerente = new Perfil("GERENTE", List.of(Permissao.UTILIZADORES_WRITE));
        utilizador = new Utilizador("operador", "hash-antigo", "Operador", "operador@mini.pt", funcionario, lojaBraga);
    }

    @Test
    void atualizarUtilizadorPermiteAlterarDadosPerfilLojaPasswordEEstado() {
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));
        when(perfilRepository.findById(gerente.getId())).thenReturn(Optional.of(gerente));
        when(lojaRepository.findById(lojaGuimaraes.getId())).thenReturn(Optional.of(lojaGuimaraes));
        when(passwordEncoder.encode("NovaSenha2026")).thenReturn("hash-novo");

        Utilizador atualizado = facade.atualizarUtilizador(utilizador.getId(), new AtualizarUtilizadorCommand(
                "Novo Nome",
                "NOVO@MINI.PT",
                gerente.getId(),
                lojaGuimaraes.getId(),
                "NovaSenha2026",
                false
        ));

        assertSame(utilizador, atualizado);
        assertEquals("Novo Nome", atualizado.getNome());
        assertEquals("novo@mini.pt", atualizado.getEmail());
        assertSame(gerente, atualizado.getPerfil());
        assertSame(lojaGuimaraes, atualizado.getLoja());
        assertEquals("hash-novo", atualizado.getPasswordHash());
        assertFalse(atualizado.isAtivo());
        verify(auditoriaService).registar(TipoOperacao.UTILIZADOR_EDITADO, utilizador.getId(), "UTILIZADOR_ATUALIZADO", "Utilizador atualizado");
    }

    @Test
    void autenticarComPasswordInvalidaRegistaFalhaEAoFimDeCincoTentativasBloqueiaConta() {
        when(utilizadorRepository.findByUsername("operador")).thenReturn(Optional.of(utilizador));
        when(passwordEncoder.matches("errada", "hash-antigo")).thenReturn(false);

        for (int i = 0; i < 5; i++) {
            assertThrows(CredenciaisInvalidasException.class, () -> facade.autenticar("operador", "errada"));
        }

        assertFalse(utilizador.isAtivo());
        assertEquals(5, utilizador.getTentativasFalhadas());
        verify(auditoriaService, times(5)).registar(TipoOperacao.LOGIN_FALHADO, utilizador.getId(), "AUTH_LOGIN", "Credenciais invalidas");
        verify(auditoriaService).registar(TipoOperacao.CONTA_BLOQUEADA, utilizador.getId(), "UTILIZADOR", "Conta bloqueada apos 5 tentativas falhadas");
    }

    @Test
    void autenticarComUsernameInexistenteRegistaTentativaFalhadaSemUtilizador() {
        when(utilizadorRepository.findByUsername("desconhecido")).thenReturn(Optional.empty());

        assertThrows(CredenciaisInvalidasException.class, () -> facade.autenticar("desconhecido", "password"));

        verify(auditoriaService).registar(TipoOperacao.LOGIN_FALHADO, null, "AUTH_LOGIN", "Tentativa de login com username inexistente");
    }

    @Test
    void listarUtilizadoresPorLojaUsaRepositorioFiltrado() {
        facade.listarUtilizadoresPorLoja(lojaBraga.getId(), PageRequest.of(0, 10));

        verify(utilizadorRepository).findByLojaId(lojaBraga.getId(), PageRequest.of(0, 10));
    }
}
