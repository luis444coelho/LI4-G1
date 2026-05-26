package pt.miniFormiga.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class,
        SecurityConfigTest.TestController.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private MiniFormigaUserDetailsService miniFormigaUserDetailsService;

    @Test
    void swaggerUiInternoFicaPublico() throws Exception {
        mockMvc.perform(get("/api/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string("swagger"));
    }

    @Test
    void rotaProtegidaSemTokenDevolve401() throws Exception {
        mockMvc.perform(get("/api/v1/protegido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void tokenValidoPermiteAcederARotaProtegida() throws Exception {
        UserDetails userDetails = User.withUsername("operador.braga")
                .password("hash")
                .authorities("PDV_WRITE", "ROLE_FUNCIONARIO")
                .build();
        when(jwtService.extrairUsername("token-valido")).thenReturn("operador.braga");
        when(miniFormigaUserDetailsService.loadUserByUsername("operador.braga")).thenReturn(userDetails);
        when(jwtService.tokenValido("token-valido", userDetails)).thenReturn(true);

        mockMvc.perform(get("/api/v1/protegido")
                        .header("Authorization", "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(content().string("protegido"));
    }

    @Test
    void tokenValidoSemAutoridadeNecessariaDevolve403() throws Exception {
        UserDetails userDetails = User.withUsername("operador.braga")
                .password("hash")
                .authorities("PDV_WRITE", "ROLE_FUNCIONARIO")
                .build();
        when(jwtService.extrairUsername("token-funcionario")).thenReturn("operador.braga");
        when(miniFormigaUserDetailsService.loadUserByUsername("operador.braga")).thenReturn(userDetails);
        when(jwtService.tokenValido("token-funcionario", userDetails)).thenReturn(true);

        mockMvc.perform(get("/api/v1/admin")
                        .header("Authorization", "Bearer token-funcionario"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @RestController
    static class TestController {

        @GetMapping("/api/swagger-ui/index.html")
        String swagger() {
            return "swagger";
        }

        @GetMapping("/api/v1/protegido")
        String protegido() {
            return "protegido";
        }

        @GetMapping("/api/v1/admin")
        @PreAuthorize("hasAuthority('GLOBAL_ADMIN')")
        String admin() {
            return "admin";
        }
    }
}
