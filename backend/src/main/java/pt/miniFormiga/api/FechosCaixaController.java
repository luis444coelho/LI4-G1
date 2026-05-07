package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.miniFormiga.subsistemas.pdv.ISubPDV;

import java.util.UUID;

import static pt.miniFormiga.subsistemas.pdv.PdvDtos.*;

@RestController
@RequestMapping("/api/v1/fechos-caixa")
@Tag(name = "FECHOS_CAIXA", description = "Fecho diario do caixa")
public class FechosCaixaController {
    private final ISubPDV pdv;

    public FechosCaixaController(ISubPDV pdv) {
        this.pdv = pdv;
    }

    public record CriarFechoRequest(@NotNull UUID lojaId, @NotNull UUID utilizadorId) { }
    public record ConfirmarFechoRequest(String observacoesDiscrepancia) { }

    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_WRITE')")
    @Operation(summary = "Registar fecho de caixa")
    @ApiResponse(responseCode = "200", description = "Fecho registado")
    public FechoCaixaDTO criar(@Valid @RequestBody CriarFechoRequest request) {
        return FechoCaixaDTO.from(pdv.registarFechoCaixa(request.lojaId(), request.utilizadorId()));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('STOCK_WRITE')")
    @Operation(summary = "Confirmar fecho de caixa")
    @ApiResponse(responseCode = "200", description = "Fecho confirmado")
    public FechoCaixaDTO confirmar(@PathVariable UUID id, @Valid @RequestBody ConfirmarFechoRequest request) {
        return FechoCaixaDTO.from(pdv.confirmarFechoCaixa(id, request.observacoesDiscrepancia()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ')")
    @Operation(summary = "Listar fechos de caixa")
    @ApiResponse(responseCode = "200", description = "Fechos listados")
    public Page<FechoCaixaDTO> listar(@RequestParam UUID lojaId, Pageable pageable) {
        return pdv.listarFechosCaixa(lojaId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ')")
    @Operation(summary = "Obter fecho de caixa")
    @ApiResponse(responseCode = "200", description = "Fecho encontrado")
    public FechoCaixaDTO obter(@PathVariable UUID id) {
        return pdv.obterFechoCaixa(id);
    }
}
