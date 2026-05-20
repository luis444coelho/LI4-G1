package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.repository.MotivoAjusteRepository;
import pt.miniFormiga.repository.StockRepository;
import pt.miniFormiga.subsistemas.stock.ISubStock;
import pt.miniFormiga.subsistemas.stock.StockDtos.AjusteInventarioResponse;
import pt.miniFormiga.subsistemas.stock.StockDtos.AlertaStockResponse;
import pt.miniFormiga.subsistemas.stock.StockDtos.DefinirNivelMinimoRequest;
import pt.miniFormiga.subsistemas.stock.StockDtos.MotivoAjusteResponse;
import pt.miniFormiga.subsistemas.stock.StockDtos.RegistarAjusteRequest;
import pt.miniFormiga.subsistemas.stock.StockDtos.StockResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final ISubStock stock;
    private final StockRepository stockRepository;
    private final AjusteInventarioRepository ajusteInventarioRepository;
    private final MotivoAjusteRepository motivoAjusteRepository;

    public StockController(ISubStock stock,
                           StockRepository stockRepository,
                           AjusteInventarioRepository ajusteInventarioRepository,
                           MotivoAjusteRepository motivoAjusteRepository) {
        this.stock = stock;
        this.stockRepository = stockRepository;
        this.ajusteInventarioRepository = ajusteInventarioRepository;
        this.motivoAjusteRepository = motivoAjusteRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','STOCK_READ','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Consultar stock da loja")
    @ApiResponse(responseCode = "200", description = "Stock listado")
    public List<StockResponse> consultarStock(@RequestParam UUID lojaId) {
        return stockRepository.findByLojaId(lojaId).stream().map(StockResponse::from).toList();
    }

    @GetMapping("/{produtoId}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','STOCK_READ','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Consultar stock de produto")
    @ApiResponse(responseCode = "200", description = "Stock encontrado")
    public StockResponse consultarStockProduto(@PathVariable UUID produtoId, @RequestParam UUID lojaId) {
        return stockRepository.findByProdutoIdAndLojaId(produtoId, lojaId)
                .map(StockResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Stock", produtoId));
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
        return ajusteInventarioRepository.findByStockLojaId(lojaId, pageable).map(AjusteInventarioResponse::from);
    }

    @GetMapping("/motivos-ajuste")
    @PreAuthorize("hasAuthority('STOCK_WRITE')")
    @Operation(summary = "Listar motivos de ajuste")
    @ApiResponse(responseCode = "200", description = "Motivos listados")
    public List<MotivoAjusteResponse> listarMotivosAjuste() {
        return motivoAjusteRepository.findAll().stream().map(MotivoAjusteResponse::from).toList();
    }
}
