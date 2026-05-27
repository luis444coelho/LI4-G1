package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.miniFormiga.subsistemas.stock.dto.StockDtos.LocalizacaoRequest;
import pt.miniFormiga.subsistemas.stock.dto.StockDtos.LocalizacaoResponse;
import pt.miniFormiga.subsistemas.stock.service.StockStore;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/produtos/{id}/localizacao")
public class ProdutoLocalizacaoController {

    private final StockStore stockStore;

    public ProdutoLocalizacaoController(StockStore stockStore) {
        this.stockStore = stockStore;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_WRITE') or hasRole('ARMAZEM')")
    @Operation(summary = "Obter localizacao de produto")
    @ApiResponse(responseCode = "200", description = "Localizacao encontrada")
    public LocalizacaoResponse obterLocalizacao(@PathVariable UUID id, @RequestParam(required = false) UUID lojaId) {
        return LocalizacaoResponse.from(stockStore.obter(id, lojaId));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('STOCK_WRITE') or hasRole('ARMAZEM')")
    @Operation(summary = "Atualizar localizacao de produto")
    @ApiResponse(responseCode = "200", description = "Localizacao atualizada")
    public LocalizacaoResponse atualizarLocalizacao(@PathVariable UUID id,
                                                    @RequestParam(required = false) UUID lojaId,
                                                    @Valid @RequestBody LocalizacaoRequest request) {
        return LocalizacaoResponse.from(stockStore.atualizarLocalizacao(id, lojaId, request.corredor(), request.prateleira()));
    }
}
