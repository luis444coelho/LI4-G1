package pt.miniFormiga.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.miniFormiga.api.error.ApiExceptionHandler;
import pt.miniFormiga.api.dto.AtualizarUtilizadorRequest;
import pt.miniFormiga.api.dto.CriarUtilizadorRequest;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;
import pt.miniFormiga.subsistemas.utilizadores.dto.AtualizarUtilizadorCommand;
import pt.miniFormiga.subsistemas.utilizadores.dto.CriarUtilizadorCommand;
import pt.miniFormiga.subsistemas.utilizadores.facade.ISubUtilizadores;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;
import pt.miniFormiga.subsistemas.utilizadores.repository.UtilizadorRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UtilizadoresControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void utilizadorSemGlobalAdminFicaLimitadoALojaPropriaAoListarUtilizadores() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                lojaRepository,
                utilizadorRepository
        );
        Loja lojaPropria = new Loja("Loja Braga", "Rua Central", "123456789");
        Loja outraLoja = new Loja("Loja Porto", "Rua Norte", "987654321");
        PerfilUtilizador gerente = PerfilUtilizador.GERENTE;
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

    @Test
    void gestorListaTodosOuFiltraPorLojaQuandoLojaIdEIndicado() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                mock(LojaRepository.class),
                mock(UtilizadorRepository.class)
        );
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador operador = new Utilizador("operador", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, loja);
        PageRequest pageable = PageRequest.of(0, 10);
        when(utilizadores.listarUtilizadores(pageable)).thenReturn(new PageImpl<>(List.of(operador), pageable, 1));
        when(utilizadores.listarUtilizadoresPorLoja(loja.getId(), pageable)).thenReturn(new PageImpl<>(List.of(operador), pageable, 1));

        assertEquals(1, controller.listar(null, pageable, authenticationGestor()).getTotalElements());
        assertEquals(1, controller.listar(loja.getId(), pageable, authenticationGestor()).getTotalElements());

        verify(utilizadores).listarUtilizadores(pageable);
        verify(utilizadores).listarUtilizadoresPorLoja(loja.getId(), pageable);
    }

    @Test
    void gerenteNaoPodeCriarUtilizadorNoutraLoja() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                lojaRepository,
                utilizadorRepository
        );
        Loja lojaPropria = new Loja("Loja Braga", "Rua Central", "123456789");
        Loja outraLoja = new Loja("Loja Porto", "Rua Norte", "987654321");
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", PerfilUtilizador.GERENTE, lojaPropria);
        Authentication authentication = authenticationGerente();
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(gerenteBraga));

        CriarUtilizadorRequest request = new CriarUtilizadorRequest(
                "operador.porto",
                "MiniFormiga2026!",
                "Operador Porto",
                "operador.porto@mini.pt",
                "FUNCIONARIO",
                null,
                outraLoja.getId(),
                null
        );

        assertThrows(AccessDeniedException.class, () -> controller.criar(request, authentication));
    }

    @Test
    void gerenteNaoPodeCriarPerfilGerente() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                lojaRepository,
                utilizadorRepository
        );
        Loja lojaPropria = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", PerfilUtilizador.GERENTE, lojaPropria);
        Authentication authentication = authenticationGerente();
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(gerenteBraga));

        CriarUtilizadorRequest request = new CriarUtilizadorRequest(
                "novo.gerente",
                "MiniFormiga2026!",
                "Novo Gerente",
                "novo.gerente@mini.pt",
                "GERENTE",
                null,
                lojaPropria.getId(),
                null
        );

        assertThrows(AccessDeniedException.class, () -> controller.criar(request, authentication));
    }

    @Test
    void gerenteNaoPodeCriarNovaLojaNemCriarSemPerfilOuSemLoja() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                lojaRepository,
                utilizadorRepository
        );
        Loja lojaPropria = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", PerfilUtilizador.GERENTE, lojaPropria);
        Authentication authentication = authenticationGerente();
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(gerenteBraga));

        assertThrows(AccessDeniedException.class, () -> controller.criar(new CriarUtilizadorRequest(
                "gerente.nova",
                "MiniFormiga2026!",
                "Gerente Nova",
                null,
                "FUNCIONARIO",
                null,
                lojaPropria.getId(),
                "Nova Loja"
        ), authentication));
        assertThrows(IllegalArgumentException.class, () -> controller.criar(new CriarUtilizadorRequest(
                "sem.perfil",
                "MiniFormiga2026!",
                "Sem Perfil",
                null,
                null,
                null,
                lojaPropria.getId(),
                null
        ), authenticationGestor()));
        assertThrows(IllegalArgumentException.class, () -> controller.criar(new CriarUtilizadorRequest(
                "sem.loja",
                "MiniFormiga2026!",
                "Sem Loja",
                null,
                "FUNCIONARIO",
                null,
                null,
                null
        ), authenticationGestor()));
    }

    @Test
    void gerenteNaoPodeAtualizarUtilizadorDeOutraLoja() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                lojaRepository,
                utilizadorRepository
        );
        Loja lojaPropria = new Loja("Loja Braga", "Rua Central", "123456789");
        Loja outraLoja = new Loja("Loja Porto", "Rua Norte", "987654321");
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", PerfilUtilizador.GERENTE, lojaPropria);
        Utilizador operadorPorto = new Utilizador("operador.porto", "hash", "Operador Porto", PerfilUtilizador.FUNCIONARIO, outraLoja);
        Authentication authentication = authenticationGerente();
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(gerenteBraga));
        when(utilizadores.obterUtilizador(operadorPorto.getId())).thenReturn(operadorPorto);

        AtualizarUtilizadorRequest request = new AtualizarUtilizadorRequest(
                "Operador Atualizado",
                "operador@mini.pt",
                "FUNCIONARIO",
                null,
                lojaPropria.getId(),
                null,
                true
        );

        assertThrows(AccessDeniedException.class, () -> controller.atualizar(operadorPorto.getId(), request, authentication));
    }

    @Test
    void gerenteAtualizaUtilizadorDaPropriaLojaComPerfilOperacional() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                mock(LojaRepository.class),
                utilizadorRepository
        );
        Loja lojaPropria = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", PerfilUtilizador.GERENTE, lojaPropria);
        Utilizador operador = new Utilizador("operador.braga", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, lojaPropria);
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(gerenteBraga));
        when(utilizadores.obterUtilizador(operador.getId())).thenReturn(operador);
        when(utilizadores.atualizarUtilizador(any(), any())).thenReturn(operador);

        var response = controller.atualizar(operador.getId(), new AtualizarUtilizadorRequest(
                "Operador Atualizado",
                "operador@mini.pt",
                null,
                "ARMAZEM",
                lojaPropria.getId(),
                null,
                true
        ), authenticationGerente());

        assertEquals("operador.braga", response.username());
        ArgumentCaptor<AtualizarUtilizadorCommand> command = ArgumentCaptor.forClass(AtualizarUtilizadorCommand.class);
        verify(utilizadores).atualizarUtilizador(any(), command.capture());
        assertEquals("ARMAZEM", command.getValue().perfil());
    }

    @Test
    void gerenteNaoPodePromoverUtilizadorNemGerirSemAutenticacaoValida() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                mock(LojaRepository.class),
                utilizadorRepository
        );
        Loja lojaPropria = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", PerfilUtilizador.GERENTE, lojaPropria);
        Utilizador operador = new Utilizador("operador.braga", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, lojaPropria);
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(gerenteBraga));
        when(utilizadores.obterUtilizador(operador.getId())).thenReturn(operador);

        assertThrows(AccessDeniedException.class, () -> controller.atualizar(operador.getId(), new AtualizarUtilizadorRequest(
                null,
                null,
                "GESTOR",
                null,
                lojaPropria.getId(),
                null,
                true
        ), authenticationGerente()));
        assertThrows(AccessDeniedException.class, () -> controller.listar(null, PageRequest.of(0, 1), null));
    }

    @Test
    void gestorCriaUtilizadorComPayloadDaUi() throws Exception {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                lojaRepository,
                utilizadorRepository
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador criado = new Utilizador("teste.gestor.1", "hash", "Teste Gestor", "teste.gestor.1@mini.pt", PerfilUtilizador.FUNCIONARIO, loja);
        Authentication authentication = authenticationGestor();
        when(utilizadores.criarUtilizador(any())).thenReturn(criado);

        mockMvc.perform(post("/api/v1/utilizadores")
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CriarUtilizadorPayload(
                                "teste.gestor.1",
                                "MiniFormiga2026!",
                                "Teste Gestor",
                                "teste.gestor.1@mini.pt",
                                "FUNCIONARIO",
                                loja.getId()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("teste.gestor.1"))
                .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"));
    }

    @Test
    void gestorCriaUtilizadorComPerfilIdLegadoEControllerEnviaCommandNormalizado() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                mock(LojaRepository.class),
                mock(UtilizadorRepository.class)
        );
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador criado = new Utilizador("perfil.legado", "hash", "Perfil Legado", PerfilUtilizador.FUNCIONARIO, loja);
        when(utilizadores.criarUtilizador(any())).thenReturn(criado);

        controller.criar(new CriarUtilizadorRequest(
                "perfil.legado",
                "MiniFormiga2026!",
                "Perfil Legado",
                null,
                null,
                "FUNCIONARIO",
                loja.getId(),
                null
        ), authenticationGestor());

        ArgumentCaptor<CriarUtilizadorCommand> command = ArgumentCaptor.forClass(CriarUtilizadorCommand.class);
        verify(utilizadores).criarUtilizador(command.capture());
        assertEquals("FUNCIONARIO", command.getValue().perfil());
    }

    @Test
    void listarLojasRemoveDuplicadosPorNomeParaGestor() throws Exception {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                lojaRepository,
                utilizadorRepository
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        Loja braga = new Loja("Loja Braga", "Rua Central", "123456789");
        Loja bragaDuplicada = new Loja(" Loja Braga ", "Morada sincronizada", "900000000");
        Loja porto = new Loja("Loja Porto", "Rua Norte", "223456789");
        when(lojaRepository.findAll()).thenReturn(List.of(bragaDuplicada, porto, braga));

        mockMvc.perform(get("/api/v1/utilizadores/lojas")
                        .principal(authenticationGestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Loja Braga"))
                .andExpect(jsonPath("$[1].nome").value("Loja Porto"));
    }

    @Test
    void listarLojasParaGerenteDevolveApenasLojaDoUtilizadorAtual() {
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                mock(ISubUtilizadores.class),
                mock(LojaRepository.class),
                utilizadorRepository
        );
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", PerfilUtilizador.GERENTE, loja);
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(gerenteBraga));

        var lojas = controller.listarLojas(authenticationGerente());

        assertEquals(1, lojas.size());
        assertEquals(loja.getId(), lojas.get(0).id());
    }

    @Test
    void obterValidaEscopoEPerfilParaGerente() {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        UtilizadorRepository utilizadorRepository = mock(UtilizadorRepository.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                mock(LojaRepository.class),
                utilizadorRepository
        );
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador gerente = new Utilizador("gerente.braga", "hash", "Gerente", PerfilUtilizador.GERENTE, loja);
        Utilizador operador = new Utilizador("operador.braga", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, loja);
        Utilizador gestor = new Utilizador("gestor.formiga", "hash", "Gestor", PerfilUtilizador.GESTOR, loja);
        when(utilizadorRepository.findByUsername("gerente.braga")).thenReturn(Optional.of(gerente));
        when(utilizadores.obterUtilizador(operador.getId())).thenReturn(operador);
        when(utilizadores.obterUtilizador(gestor.getId())).thenReturn(gestor);

        assertEquals("operador.braga", controller.obter(operador.getId(), authenticationGerente()).username());
        assertThrows(AccessDeniedException.class, () -> controller.obter(gestor.getId(), authenticationGerente()));
    }

    @Test
    void desativarDelegadoAoSubsistema() throws Exception {
        ISubUtilizadores utilizadores = mock(ISubUtilizadores.class);
        UtilizadoresController controller = new UtilizadoresController(
                utilizadores,
                mock(LojaRepository.class),
                mock(UtilizadorRepository.class)
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/utilizadores/{id}", id)
                        .principal(authenticationGestor()))
                .andExpect(status().isNoContent());

        verify(utilizadores).desativarUtilizador(id);
    }

    @Test
    void listarPerfisExpoePerfisEPermissoesDaAplicacao() {
        UtilizadoresController controller = new UtilizadoresController(
                mock(ISubUtilizadores.class),
                mock(LojaRepository.class),
                mock(UtilizadorRepository.class)
        );

        var perfis = controller.listarPerfis();

        assertTrue(perfis.stream().anyMatch(perfil ->
                perfil.id().equals("GERENTE") && perfil.permissoes().contains(Permissao.STOCK_WRITE)
        ));
        assertTrue(perfis.stream().anyMatch(perfil ->
                perfil.id().equals("FUNCIONARIO") && perfil.permissoes().contains(Permissao.PDV_WRITE)
        ));
    }

    private Authentication authenticationGerente() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("gerente.braga");
        when(authentication.getAuthorities()).thenAnswer(invocation -> List.of(
                new SimpleGrantedAuthority(Permissao.UTILIZADORES_READ),
                new SimpleGrantedAuthority(Permissao.UTILIZADORES_WRITE),
                new SimpleGrantedAuthority("ROLE_GERENTE")
        ));
        return authentication;
    }

    private Authentication authenticationGestor() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("gestor.formiga");
        when(authentication.getAuthorities()).thenAnswer(invocation -> List.of(
                new SimpleGrantedAuthority(Permissao.GLOBAL_ADMIN),
                new SimpleGrantedAuthority(Permissao.UTILIZADORES_WRITE),
                new SimpleGrantedAuthority("ROLE_GESTOR")
        ));
        return authentication;
    }

    private record CriarUtilizadorPayload(String username,
                                          String password,
                                          String nome,
                                          String email,
                                          String perfil,
                                          UUID lojaId) {
    }
}
