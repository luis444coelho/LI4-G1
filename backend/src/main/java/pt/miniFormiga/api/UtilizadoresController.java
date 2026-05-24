package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.api.dto.AtualizarUtilizadorRequest;
import pt.miniFormiga.api.dto.CriarUtilizadorRequest;
import pt.miniFormiga.api.dto.UtilizadorResponse;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.utilizadores.AtualizarUtilizadorCommand;
import pt.miniFormiga.subsistemas.utilizadores.CriarUtilizadorCommand;
import pt.miniFormiga.subsistemas.utilizadores.ISubUtilizadores;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/utilizadores")
@Tag(name = "UTILIZADORES", description = "Gestao de utilizadores e perfis de acesso")
public class UtilizadoresController {

    private final ISubUtilizadores utilizadores;
    private final LojaRepository lojaRepository;
    private final UtilizadorRepository utilizadorRepository;

    public UtilizadoresController(ISubUtilizadores utilizadores,
                                  LojaRepository lojaRepository,
                                  UtilizadorRepository utilizadorRepository) {
        this.utilizadores = utilizadores;
        this.lojaRepository = lojaRepository;
        this.utilizadorRepository = utilizadorRepository;
    }

    public record PerfilResponse(UUID id, String nome, List<String> permissoes) {
        static PerfilResponse from(PerfilUtilizador perfil) {
            return new PerfilResponse(null, perfil.getNome(), perfil.getPermissoes());
        }
    }

    public record LojaResponse(UUID id, String nome, String morada, String nif, boolean ativa) {
        static LojaResponse from(Loja loja) {
            return new LojaResponse(loja.getId(), loja.getNome(), loja.getMorada(), loja.getNif(), loja.isAtiva());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','UTILIZADORES_READ')")
    @Operation(summary = "Listar utilizadores")
    @ApiResponse(responseCode = "200", description = "Pagina de utilizadores")
    public Page<UtilizadorResponse> listar(@org.springframework.web.bind.annotation.RequestParam(required = false) UUID lojaId,
                                           Pageable pageable,
                                           Authentication authentication) {
        if (temAutoridade(authentication, "GLOBAL_ADMIN")) {
            return (lojaId == null
                    ? utilizadores.listarUtilizadores(pageable)
                    : utilizadores.listarUtilizadoresPorLoja(lojaId, pageable))
                    .map(UtilizadorResponse::from);
        }

        Utilizador atual = utilizadorAtual(authentication);
        return utilizadores.listarUtilizadoresPorLoja(atual.getLoja().getId(), pageable).map(UtilizadorResponse::from);
    }

    @GetMapping("/perfis")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','UTILIZADORES_READ','UTILIZADORES_WRITE')")
    @Operation(summary = "Listar perfis de acesso")
    @ApiResponse(responseCode = "200", description = "Perfis listados")
    public List<PerfilResponse> listarPerfis() {
        return java.util.Arrays.stream(PerfilUtilizador.values()).map(PerfilResponse::from).toList();
    }

    @GetMapping("/lojas")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','UTILIZADORES_READ','UTILIZADORES_WRITE')")
    @Operation(summary = "Listar lojas")
    @ApiResponse(responseCode = "200", description = "Lojas listadas")
    public List<LojaResponse> listarLojas(Authentication authentication) {
        if (temAutoridade(authentication, "GLOBAL_ADMIN")) {
            return lojaRepository.findAll().stream().map(LojaResponse::from).toList();
        }
        return List.of(LojaResponse.from(utilizadorAtual(authentication).getLoja()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','UTILIZADORES_READ')")
    @Operation(summary = "Obter utilizador")
    @ApiResponse(responseCode = "200", description = "Utilizador encontrado")
    @ApiResponse(responseCode = "404", description = "Utilizador inexistente")
    public UtilizadorResponse obter(@PathVariable UUID id) {
        return UtilizadorResponse.from(utilizadores.obterUtilizador(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','UTILIZADORES_WRITE')")
    @Operation(summary = "Criar utilizador")
    @ApiResponse(responseCode = "201", description = "Utilizador criado")
    @ApiResponse(responseCode = "400", description = "Pedido invalido")
    public UtilizadorResponse criar(@Valid @RequestBody CriarUtilizadorRequest request) {
        return UtilizadorResponse.from(utilizadores.criarUtilizador(new CriarUtilizadorCommand(
                request.username(),
                request.password(),
                request.nome(),
                request.email(),
                request.perfil(),
                request.lojaId()
        )));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','UTILIZADORES_WRITE')")
    @Operation(summary = "Atualizar utilizador")
    @ApiResponse(responseCode = "200", description = "Utilizador atualizado")
    public UtilizadorResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarUtilizadorRequest request) {
        return UtilizadorResponse.from(utilizadores.atualizarUtilizador(id, new AtualizarUtilizadorCommand(
                request.nome(),
                request.email(),
                request.perfil(),
                request.lojaId(),
                request.password(),
                request.ativo()
        )));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','UTILIZADORES_DELETE')")
    @Operation(summary = "Desativar utilizador")
    @ApiResponse(responseCode = "204", description = "Utilizador desativado")
    public void desativar(@PathVariable UUID id) {
        utilizadores.desativarUtilizador(id);
    }

    private boolean temAutoridade(Authentication authentication, String autoridade) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(autoridade));
    }

    private Utilizador utilizadorAtual(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Utilizador autenticado nao encontrado");
        }
        return utilizadorRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Utilizador autenticado nao encontrado"));
    }
}
