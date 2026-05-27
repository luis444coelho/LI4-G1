package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.subsistemas.sincronizacao.facade.ISubSincronizacao;

import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.ConflitoSincronizacaoResponse;
import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.IniciarSincronizacaoRequest;
import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.SincronizacaoResponse;

@RestController
@RequestMapping("/api/v1/sincronizacao")
@Tag(name = "SINCRONIZACAO", description = "Sincronizacao entre loja local e servidor central")
public class SincronizacaoController {

    private final ISubSincronizacao sincronizacao;

    public SincronizacaoController(ISubSincronizacao sincronizacao) {
        this.sincronizacao = sincronizacao;
    }

    @PostMapping("/iniciar")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','SINCRONIZACAO_WRITE','STOCK_WRITE')")
    @Operation(summary = "Iniciar sincronizacao com servidor central")
    @ApiResponse(responseCode = "200", description = "Sincronizacao processada ou mantida pendente")
    public SincronizacaoResponse iniciar(@Valid @RequestBody IniciarSincronizacaoRequest request) {
        return sincronizacao.iniciarSincronizacao(request.lojaId());
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','SINCRONIZACAO_WRITE','RELATORIOS_READ','STOCK_WRITE')")
    @Operation(summary = "Consultar estado da sincronizacao atual")
    @ApiResponse(responseCode = "200", description = "Estado devolvido")
    public SincronizacaoResponse estado(@RequestParam UUID lojaId) {
        return sincronizacao.estadoAtual(lojaId);
    }

    @GetMapping("/historico")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','SINCRONIZACAO_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar historico de sincronizacoes")
    @ApiResponse(responseCode = "200", description = "Historico listado")
    public Page<SincronizacaoResponse> historico(@RequestParam UUID lojaId, Pageable pageable) {
        return sincronizacao.historico(lojaId, pageable);
    }

    @GetMapping("/conflitos")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','SINCRONIZACAO_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar conflitos resolvidos por last-write-wins")
    @ApiResponse(responseCode = "200", description = "Conflitos listados")
    public List<ConflitoSincronizacaoResponse> conflitos(@RequestParam UUID lojaId) {
        return sincronizacao.conflitos(lojaId);
    }
}
