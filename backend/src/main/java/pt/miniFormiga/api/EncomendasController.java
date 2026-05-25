package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.subsistemas.encomendas.ISubEncomendas;

import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.encomendas.EncomendasDtos.*;

@RestController
@RequestMapping("/api/v1/encomendas")
@Tag(name = "ENCOMENDAS", description = "Criacao e acompanhamento de encomendas")
public class EncomendasController {

    private final ISubEncomendas encomendas;

    public EncomendasController(ISubEncomendas encomendas) {
        this.encomendas = encomendas;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar encomendas por loja")
    @ApiResponse(responseCode = "200", description = "Encomendas listadas")
    public Page<EncomendaResponse> listar(@RequestParam UUID lojaId, Pageable pageable) {
        return encomendas.listarEncomendas(lojaId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE')")
    @Operation(summary = "Criar encomenda")
    @ApiResponse(responseCode = "201", description = "Encomenda criada")
    public EncomendaResponse criar(@Valid @RequestBody CriarEncomendaRequest request) {
        return encomendas.criarEncomenda(request);
    }

    @PostMapping("/consolidada")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE')")
    @Operation(summary = "Criar encomenda consolidada para varias lojas")
    @ApiResponse(responseCode = "201", description = "Encomendas criadas")
    public List<EncomendaResponse> criarConsolidada(@Valid @RequestBody CriarEncomendaConsolidadaRequest request) {
        return encomendas.criarEncomendaConsolidada(request);
    }

    @GetMapping("/sugestoes")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Gerar sugestoes automaticas de encomenda")
    @ApiResponse(responseCode = "200", description = "Sugestoes geradas")
    public List<SugestaoEncomendaResponse> sugestoes(@RequestParam UUID lojaId,
                                                     @RequestParam(required = false) UUID fornecedorId) {
        return encomendas.sugerirEncomendas(lojaId, fornecedorId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Obter encomenda")
    @ApiResponse(responseCode = "200", description = "Encomenda encontrada")
    public EncomendaResponse obter(@PathVariable UUID id) {
        return encomendas.obterEncomenda(id);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE')")
    @Operation(summary = "Atualizar estado de encomenda")
    @ApiResponse(responseCode = "200", description = "Estado atualizado")
    public EncomendaResponse atualizarEstado(@PathVariable UUID id,
                                             @Valid @RequestBody AtualizarEstadoEncomendaRequest request) {
        return encomendas.atualizarEstado(id, request);
    }
}
