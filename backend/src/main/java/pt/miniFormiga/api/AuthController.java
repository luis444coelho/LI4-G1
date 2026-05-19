package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.api.dto.LoginRequest;
import pt.miniFormiga.api.dto.LoginResponse;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.security.JwtService;
import pt.miniFormiga.security.MiniFormigaUserDetailsService;
import pt.miniFormiga.subsistemas.utilizadores.ISubUtilizadores;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "AUTH", description = "Autenticacao JWT do Mini-Formiga")
public class AuthController {

    private final ISubUtilizadores utilizadores;
    private final MiniFormigaUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    public AuthController(ISubUtilizadores utilizadores,
                          MiniFormigaUserDetailsService userDetailsService,
                          JwtService jwtService,
                          AuditoriaService auditoriaService) {
        this.utilizadores = utilizadores;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar utilizador", description = "Devolve um Bearer token JWT para chamadas autenticadas.")
    @ApiResponse(responseCode = "200", description = "Autenticacao efetuada")
    @ApiResponse(responseCode = "401", description = "Credenciais invalidas")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Utilizador utilizador = utilizadores.autenticar(request.username(), request.password());
        UserDetails userDetails = userDetailsService.loadUserByUsername(utilizador.getUsername());
        return new LoginResponse(
                "Bearer",
                jwtService.emitirToken(utilizador, userDetails),
                utilizador.getId(),
                utilizador.getNome(),
                utilizador.getPerfil().getNome(),
                utilizador.getLoja().getId(),
                utilizador.getPerfil().getPermissoes()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Registar logout", description = "JWT e stateless; o endpoint regista auditoria do logout.")
    @ApiResponse(responseCode = "204", description = "Logout registado")
    public void logout(@AuthenticationPrincipal UserDetails principal) {
        String username = principal == null ? "utilizador desconhecido" : principal.getUsername();
        auditoriaService.registar(TipoOperacao.LOGOUT, null, "AUTH_LOGOUT", "Logout de " + username);
    }
}
