package pt.miniFormiga.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.miniFormiga.api.error.ApiExceptionHandler;
import pt.miniFormiga.subsistemas.auditoria.ISubAuditoria;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.security.JwtService;
import pt.miniFormiga.security.MiniFormigaUserDetailsService;
import pt.miniFormiga.subsistemas.utilizadores.CredenciaisInvalidasException;
import pt.miniFormiga.subsistemas.utilizadores.ISubUtilizadores;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ISubUtilizadores utilizadores;
    private MiniFormigaUserDetailsService userDetailsService;
    private JwtService jwtService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        utilizadores = mock(ISubUtilizadores.class);
        userDetailsService = mock(MiniFormigaUserDetailsService.class);
        jwtService = mock(JwtService.class);
        ISubAuditoria auditoriaService = mock(ISubAuditoria.class);
        AuthController controller = new AuthController(utilizadores, userDetailsService, jwtService, auditoriaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void loginComCredenciaisValidasDevolveJwt() throws Exception {
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Perfil perfil = new Perfil("GESTOR", List.of(Permissao.GLOBAL_ADMIN));
        Utilizador utilizador = new Utilizador("gestor.formiga", "hash", "Sr. Formiga", "gestor@mini.pt", perfil, loja);
        UserDetails userDetails = User.withUsername("gestor.formiga")
                .password("hash")
                .authorities(Permissao.GLOBAL_ADMIN, "ROLE_GESTOR")
                .build();

        when(utilizadores.autenticar("gestor.formiga", "MiniFormiga2026!")).thenReturn(utilizador);
        when(userDetailsService.loadUserByUsername("gestor.formiga")).thenReturn(userDetails);
        when(jwtService.emitirToken(utilizador, userDetails)).thenReturn("jwt-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginPayload("gestor.formiga", "MiniFormiga2026!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.perfil").value("GESTOR"))
                .andExpect(jsonPath("$.permissoes", hasItem(Permissao.GLOBAL_ADMIN)));
    }

    @Test
    void loginComCredenciaisInvalidasDevolve401() throws Exception {
        when(utilizadores.autenticar("gestor.formiga", "errada"))
                .thenThrow(new CredenciaisInvalidasException("Credenciais invalidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginPayload("gestor.formiga", "errada"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_CREDENTIALS_INVALID"));
    }

    private record LoginPayload(String username, String password) {
    }
}
