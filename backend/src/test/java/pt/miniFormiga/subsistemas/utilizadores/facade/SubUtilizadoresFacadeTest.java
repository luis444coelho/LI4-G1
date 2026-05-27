package pt.miniFormiga.subsistemas.utilizadores.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubUtilizadoresFacadeTest {

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.subsistemas.auditoria.facade.ISubAuditoria;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;
import pt.miniFormiga.subsistemas.utilizadores.CredenciaisInvalidasException;
import pt.miniFormiga.subsistemas.utilizadores.RecursoNaoEncontradoException;
import pt.miniFormiga.subsistemas.utilizadores.RegraNegocioException;
import pt.miniFormiga.subsistemas.utilizadores.dto.AtualizarUtilizadorCommand;
import pt.miniFormiga.subsistemas.utilizadores.dto.CriarUtilizadorCommand;
import pt.miniFormiga.subsistemas.utilizadores.repository.UtilizadorRepository;
    private UtilizadorRepository utilizadorRepository;
    private LojaRepository lojaRepository;
    private PasswordEncoder passwordEncoder;
    private ISubAuditoria auditoriaService;
    private SubUtilizadoresFacade facade;

    private Loja lojaBraga;
    private Loja lojaGuimaraes;
    private PerfilUtilizador funcionario;
    private PerfilUtilizador gerente;
    private Utilizador utilizador;

    @BeforeEach
    void setUp() {
        utilizadorRepository = mock(UtilizadorRepository.class);
        lojaRepository = mock(LojaRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditoriaService = mock(ISubAuditoria.class);
        facade = new SubUtilizadoresFacade(
                utilizadorRepository,
                lojaRepository,
                passwordEncoder,
                auditoriaService
        );

        lojaBraga = new Loja("Loja Braga", "Rua Central", "123456789");
        lojaGuimaraes = new Loja("Loja Guimaraes", "Rua Nova", "987654321");
        funcionario = PerfilUtilizador.FUNCIONARIO;
        gerente = PerfilUtilizador.GERENTE;
        utilizador = new Utilizador("operador", "hash-antigo", "Operador", "operador@mini.pt", funcionario, lojaBraga);
    }

    @Test
    void atualizarUtilizadorPermiteAlterarDadosPerfilLojaPasswordEEstado() {
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));
        when(lojaRepository.findById(lojaGuimaraes.getId())).thenReturn(Optional.of(lojaGuimaraes));
        when(passwordEncoder.encode("NovaSenha2026")).thenReturn("hash-novo");

        Utilizador atualizado = facade.atualizarUtilizador(utilizador.getId(), new AtualizarUtilizadorCommand(
                "Novo Nome",
                "NOVO@MINI.PT",
                "GERENTE",
                lojaGuimaraes.getId(),
                "NovaSenha2026",
                false
        ));

        assertSame(utilizador, atualizado);
        assertEquals("Novo Nome", atualizado.getNome());
        assertEquals("novo@mini.pt", atualizado.getEmail());
        assertEquals(PerfilUtilizador.GERENTE, atualizado.getPerfil());
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
    void autenticarComCredenciaisValidasRegistaLoginELimpaFalhas() {
        utilizador.registarFalhaAutenticacao(5);
        when(utilizadorRepository.findByUsername("operador")).thenReturn(Optional.of(utilizador));
        when(passwordEncoder.matches("certa", "hash-antigo")).thenReturn(true);

        Utilizador autenticado = facade.autenticar("operador", "certa");

        assertSame(utilizador, autenticado);
        assertEquals(0, autenticado.getTentativasFalhadas());
        verify(auditoriaService).registar(TipoOperacao.LOGIN, utilizador.getId(), "AUTH_LOGIN", "Login efetuado");
    }

    @Test
    void utilizadoresDemoRecuperamLoginComPasswordUnicaDeTeste() {
        List<Utilizador> utilizadoresDemo = List.of(
                new Utilizador("gestor.formiga", "hash-antigo", "Sr. Formiga", "gestor@mini.pt", PerfilUtilizador.GESTOR, lojaBraga),
                new Utilizador("gerente.braga", "hash-antigo", "Gerente Braga", "gerente@mini.pt", gerente, lojaBraga),
                new Utilizador("operador.braga", "hash-antigo", "Operador Braga", "operador@mini.pt", funcionario, lojaBraga),
                new Utilizador("armazem.braga", "hash-antigo", "Armazem Braga", "armazem@mini.pt", PerfilUtilizador.ARMAZEM, lojaBraga)
        );
        utilizadoresDemo.forEach(Utilizador::desativar);
        utilizadoresDemo.forEach(demo -> {
            when(utilizadorRepository.findByUsername(demo.getUsername())).thenReturn(Optional.of(demo));
            when(passwordEncoder.matches("MiniFormiga2026!", "hash-antigo")).thenReturn(false);
        });
        when(passwordEncoder.encode("MiniFormiga2026!")).thenReturn("hash-demo");

        for (Utilizador demo : utilizadoresDemo) {
            Utilizador autenticado = facade.autenticar(demo.getUsername(), "MiniFormiga2026!");

            assertSame(demo, autenticado);
            assertTrue(autenticado.isAtivo());
            assertEquals("hash-demo", autenticado.getPasswordHash());
        }
    }

    @Test
    void listarUtilizadoresPorLojaUsaRepositorioFiltrado() {
        facade.listarUtilizadoresPorLoja(lojaBraga.getId(), PageRequest.of(0, 10));

        verify(utilizadorRepository).findByLojaId(lojaBraga.getId(), PageRequest.of(0, 10));
    }

    @Test
    void listarUtilizadoresUsaRepositorioPaginado() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(utilizadorRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(utilizador), pageable, 1));

        var pagina = facade.listarUtilizadores(pageable);

        assertEquals(1, pagina.getTotalElements());
        verify(utilizadorRepository).findAll(pageable);
    }

    @Test
    void criarUtilizadorComLojaExistenteCodificaPasswordEAudita() {
        when(utilizadorRepository.existsByUsername("novo")).thenReturn(false);
        when(lojaRepository.findById(lojaBraga.getId())).thenReturn(Optional.of(lojaBraga));
        when(passwordEncoder.encode("segura")).thenReturn("hash-seguro");
        when(utilizadorRepository.save(any(Utilizador.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Utilizador criado = facade.criarUtilizador(new CriarUtilizadorCommand(
                "novo",
                "segura",
                "Novo",
                "NOVO@MINI.PT",
                "FUNCIONARIO",
                lojaBraga.getId(),
                null
        ));

        assertEquals("novo", criado.getUsername());
        assertEquals("hash-seguro", criado.getPasswordHash());
        assertEquals("novo@mini.pt", criado.getEmail());
        assertEquals(PerfilUtilizador.FUNCIONARIO, criado.getPerfil());
        assertSame(lojaBraga, criado.getLoja());
        verify(auditoriaService).registar(TipoOperacao.UTILIZADOR_CRIADO, criado.getId(), "UTILIZADOR_CRIADO", "Utilizador criado");
    }

    @Test
    void criarUtilizadorComUsernameDuplicadoFalhaAntesDeCodificarPassword() {
        when(utilizadorRepository.existsByUsername("operador")).thenReturn(true);

        assertThrows(RegraNegocioException.class, () -> facade.criarUtilizador(new CriarUtilizadorCommand(
                "operador",
                "segura",
                "Operador",
                null,
                "FUNCIONARIO",
                lojaBraga.getId(),
                null
        )));

        verify(passwordEncoder, never()).encode(any());
        verify(utilizadorRepository, never()).save(any());
    }

    @Test
    void criarGerenteComNomeDeLojaCriaLojaNovaComNifTemporarioDisponivel() {
        when(utilizadorRepository.existsByUsername("gerente.nova")).thenReturn(false);
        when(lojaRepository.findByNif("900000000")).thenReturn(Optional.of(lojaBraga));
        when(lojaRepository.findByNif("900000001")).thenReturn(Optional.empty());
        when(lojaRepository.save(any(Loja.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("segura")).thenReturn("hash-gerente");
        when(utilizadorRepository.save(any(Utilizador.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Utilizador criado = facade.criarUtilizador(new CriarUtilizadorCommand(
                "gerente.nova",
                "segura",
                "Gerente Nova",
                null,
                "GERENTE",
                null,
                " Nova Loja "
        ));

        assertEquals("Nova Loja", criado.getLoja().getNome());
        assertEquals("900000001", criado.getLoja().getNif());
        assertEquals(PerfilUtilizador.GERENTE, criado.getPerfil());
    }

    @Test
    void criarUtilizadorSemLojaOuComPerfilInvalidoFalha() {
        when(utilizadorRepository.existsByUsername("sem.loja")).thenReturn(false);
        assertThrows(RecursoNaoEncontradoException.class, () -> facade.criarUtilizador(new CriarUtilizadorCommand(
                "sem.loja",
                "segura",
                "Sem Loja",
                null,
                "FUNCIONARIO",
                null,
                null
        )));

        when(utilizadorRepository.existsByUsername("perfil.invalido")).thenReturn(false);
        assertThrows(RecursoNaoEncontradoException.class, () -> facade.criarUtilizador(new CriarUtilizadorCommand(
                "perfil.invalido",
                "segura",
                "Perfil Invalido",
                null,
                "INVALIDO",
                lojaBraga.getId(),
                null
        )));
    }

    @Test
    void atualizarUtilizadorComCamposOmitidosMantemDadosEPermiteReativar() {
        utilizador.desativar();
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));

        Utilizador atualizado = facade.atualizarUtilizador(utilizador.getId(), new AtualizarUtilizadorCommand(
                null,
                null,
                null,
                null,
                " ",
                true
        ));

        assertSame(utilizador, atualizado);
        assertEquals("Operador", atualizado.getNome());
        assertEquals("operador@mini.pt", atualizado.getEmail());
        assertEquals(PerfilUtilizador.FUNCIONARIO, atualizado.getPerfil());
        assertSame(lojaBraga, atualizado.getLoja());
        assertEquals("hash-antigo", atualizado.getPasswordHash());
        assertTrue(atualizado.isAtivo());
    }

    @Test
    void atualizarUtilizadorComLojaInexistenteFalha() {
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));
        when(lojaRepository.findById(lojaGuimaraes.getId())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> facade.atualizarUtilizador(utilizador.getId(), new AtualizarUtilizadorCommand(
                null,
                null,
                null,
                lojaGuimaraes.getId(),
                null,
                null
        )));
    }

    @Test
    void obterEDesativarUtilizadorTratamExistenciaEAuditoria() {
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.of(utilizador));

        assertSame(utilizador, facade.obterUtilizador(utilizador.getId()));
        facade.desativarUtilizador(utilizador.getId());

        assertFalse(utilizador.isAtivo());
        verify(auditoriaService).registar(TipoOperacao.UTILIZADOR_DESATIVADO, utilizador.getId(), "UTILIZADOR_DESATIVADO", "Utilizador desativado");
    }

    @Test
    void obterUtilizadorInexistenteFalha() {
        when(utilizadorRepository.findById(utilizador.getId())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> facade.obterUtilizador(utilizador.getId()));
    }

    @Test
    void novoUtilizadorComUuidAtribuidoEConsideradoNovoAtePersistir() {
        Utilizador novo = new Utilizador(
                "novo.operador",
                "hash",
                "Novo Operador",
                "novo.operador@mini.pt",
                funcionario,
                lojaBraga
        );

        org.junit.jupiter.api.Assertions.assertTrue(novo.isNew());
    }
}
