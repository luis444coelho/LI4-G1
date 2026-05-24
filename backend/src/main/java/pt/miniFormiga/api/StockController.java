package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.MotivoAjusteCodigo;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.subsistemas.stock.ISubStock;
import pt.miniFormiga.subsistemas.stock.StockStore;
import pt.miniFormiga.subsistemas.stock.StockDtos.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final ISubStock stock;
    private final AjusteInventarioRepository ajusteInventarioRepository;
    private final StockStore stockStore;

    public StockController(ISubStock stock,
                           AjusteInventarioRepository ajusteInventarioRepository,
                           StockStore stockStore) {
        this.stock = stock;
        this.ajusteInventarioRepository = ajusteInventarioRepository;
        this.stockStore = stockStore;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','STOCK_READ','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Consultar stock da loja")
    @ApiResponse(responseCode = "200", description = "Stock listado")
    public List<StockResponse> consultarStock(@RequestParam UUID lojaId) {
        return stockStore.listar(lojaId).stream().map(StockResponse::from).toList();
    }

    @GetMapping("/{produtoId}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','STOCK_READ','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Consultar stock de produto")
    @ApiResponse(responseCode = "200", description = "Stock encontrado")
    public StockResponse consultarStockProduto(@PathVariable UUID produtoId, @RequestParam UUID lojaId) {
        return StockResponse.from(stockStore.obter(produtoId, lojaId));
    }

    @PutMapping("/{produtoId}/nivel-minimo")
    @PreAuthorize("hasAuthority('STOCK_WRITE')")
    @Operation(summary = "Definir nivel minimo")
    @ApiResponse(responseCode = "200", description = "Nivel minimo definido")
    public void definirNivelMinimo(@PathVariable UUID produtoId, @Valid @RequestBody DefinirNivelMinimoRequest request) {
        stock.definirNivelMinimo(produtoId, request.lojaId(), request.quantidade());
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ','STOCK_WRITE')")
    @Operation(summary = "Listar alertas ativos")
    @ApiResponse(responseCode = "200", description = "Alertas listados")
    public List<AlertaStockResponse> getAlertasAtivos(@RequestParam UUID lojaId) {
        return stock.getAlertasAtivos(lojaId).stream().map(AlertaStockResponse::from).toList();
    }

    @PatchMapping("/alertas/{alertaId}/lido")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ','STOCK_WRITE')")
    @Operation(summary = "Marcar alerta como lido")
    @ApiResponse(responseCode = "200", description = "Alerta marcado como lido")
    public AlertaStockResponse marcarAlertaLido(@PathVariable UUID alertaId) {
        return AlertaStockResponse.from(stock.marcarAlertaLido(alertaId));
    }

    @PatchMapping("/alertas/{alertaId}/resolver")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ','STOCK_WRITE')")
    @Operation(summary = "Resolver alerta de stock")
    @ApiResponse(responseCode = "200", description = "Alerta resolvido")
    public AlertaStockResponse resolverAlerta(@PathVariable UUID alertaId) {
        return AlertaStockResponse.from(stock.resolverAlerta(alertaId));
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasAuthority('STOCK_WRITE')")
    @Operation(summary = "Registar ajuste de inventario")
    @ApiResponse(responseCode = "200", description = "Ajuste registado")
    public AjusteInventarioResponse registarAjuste(@Valid @RequestBody RegistarAjusteRequest request) {
        AjusteInventario ajuste = stock.registarAjuste(
                request.produtoId(), request.lojaId(), request.quantidade(), request.motivo(), request.utilizadorId());
        return AjusteInventarioResponse.from(ajuste);
    }

    @GetMapping("/ajustes")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ','STOCK_WRITE')")
    @Operation(summary = "Listar ajustes de inventario")
    @ApiResponse(responseCode = "200", description = "Ajustes listados")
    public Page<AjusteInventarioResponse> listarAjustes(@RequestParam UUID lojaId, Pageable pageable) {
        return ajusteInventarioRepository.findAll(pageable).map(AjusteInventarioResponse::from);
    }

    @GetMapping("/motivos-ajuste")
    @PreAuthorize("hasAuthority('STOCK_WRITE')")
    @Operation(summary = "Listar motivos de ajuste")
    @ApiResponse(responseCode = "200", description = "Motivos listados")
    public List<MotivoAjusteResponse> listarMotivosAjuste() {
        return Arrays.stream(MotivoAjusteCodigo.values()).map(MotivoAjusteResponse::from).toList();
    }
}
