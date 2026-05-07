package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import pt.miniFormiga.subsistemas.utilizadores.AtualizarUtilizadorCommand;
import pt.miniFormiga.subsistemas.utilizadores.CriarUtilizadorCommand;
import pt.miniFormiga.subsistemas.utilizadores.ISubUtilizadores;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/utilizadores")
@Tag(name = "UTILIZADORES", description = "Gestao de utilizadores e perfis de acesso")
public class UtilizadoresController {

    private final ISubUtilizadores utilizadores;

    public UtilizadoresController(ISubUtilizadores utilizadores) {
        this.utilizadores = utilizadores;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','UTILIZADORES_READ')")
    @Operation(summary = "Listar utilizadores")
    @ApiResponse(responseCode = "200", description = "Pagina de utilizadores")
    public Page<UtilizadorResponse> listar(Pageable pageable) {
        return utilizadores.listarUtilizadores(pageable).map(UtilizadorResponse::from);
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
                request.perfilId(),
                request.lojaId()
        )));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','UTILIZADORES_WRITE')")
    @Operation(summary = "Atualizar utilizador")
    @ApiResponse(responseCode = "200", description = "Utilizador atualizado")
    public UtilizadorResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarUtilizadorRequest request) {
        return UtilizadorResponse.from(utilizadores.atualizarUtilizador(id, new AtualizarUtilizadorCommand(
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
}
