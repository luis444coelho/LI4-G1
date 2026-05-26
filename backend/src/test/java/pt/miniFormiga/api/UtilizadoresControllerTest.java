package pt.miniFormiga.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.utilizadores.ISubUtilizadores;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", new Perfil("GERENTE", List.of(Permissao.UTILIZADORES_WRITE)), lojaPropria);
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
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", new Perfil("GERENTE", List.of(Permissao.UTILIZADORES_WRITE)), lojaPropria);
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
        Utilizador gerenteBraga = new Utilizador("gerente.braga", "hash", "Gerente", new Perfil("GERENTE", List.of(Permissao.UTILIZADORES_WRITE)), lojaPropria);
        Utilizador operadorPorto = new Utilizador("operador.porto", "hash", "Operador Porto", new Perfil("FUNCIONARIO", List.of(Permissao.PDV_WRITE)), outraLoja);
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
        Utilizador criado = new Utilizador("teste.gestor.1", "hash", "Teste Gestor", "teste.gestor.1@mini.pt", new Perfil("FUNCIONARIO", List.of(Permissao.PDV_WRITE)), loja);
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
