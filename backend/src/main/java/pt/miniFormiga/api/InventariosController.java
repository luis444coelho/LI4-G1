package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.repository.InventarioFisicoRepository;
import pt.miniFormiga.repository.LinhaInventarioRepository;
import pt.miniFormiga.subsistemas.stock.ISubStock;
import pt.miniFormiga.subsistemas.stock.StockDtos.AtualizarContagemRequest;
import pt.miniFormiga.subsistemas.stock.StockDtos.IniciarInventarioRequest;
import pt.miniFormiga.subsistemas.stock.StockDtos.InventarioFisicoResponse;
import pt.miniFormiga.subsistemas.stock.StockDtos.LinhaInventarioResponse;
import pt.miniFormiga.subsistemas.stock.StockDtos.RegistarContagemRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventarios")
public class InventariosController {

    private final ISubStock stock;
    private final InventarioFisicoRepository inventarioFisicoRepository;
    private final LinhaInventarioRepository linhaInventarioRepository;

    public InventariosController(ISubStock stock,
                                 InventarioFisicoRepository inventarioFisicoRepository,
                                 LinhaInventarioRepository linhaInventarioRepository) {
        this.stock = stock;
        this.inventarioFisicoRepository = inventarioFisicoRepository;
        this.linhaInventarioRepository = linhaInventarioRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','STOCK_WRITE') or hasRole('ARMAZEM')")
    @Operation(summary = "Iniciar inventario fisico")
    @ApiResponse(responseCode = "200", description = "Inventario iniciado")
    public InventarioFisicoResponse iniciarInventario(@Valid @RequestBody IniciarInventarioRequest request) {
        return InventarioFisicoResponse.from(stock.iniciarInventarioFisico(request.lojaId(), request.utilizadorId()));
    }

    @PostMapping("/{id}/linhas")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','STOCK_WRITE') or hasRole('ARMAZEM')")
    @Operation(summary = "Registar contagem de inventario")
    @ApiResponse(responseCode = "200", description = "Contagem registada")
    public LinhaInventarioResponse registarLinha(@PathVariable UUID id, @Valid @RequestBody RegistarContagemRequest request) {
        return LinhaInventarioResponse.from(stock.registarContagemLinha(id, request.produtoId(), request.quantidade()));
    }

    @PutMapping("/{id}/linhas/{linhaId}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','STOCK_WRITE') or hasRole('ARMAZEM')")
    @Operation(summary = "Corrigir contagem de inventario")
    @ApiResponse(responseCode = "200", description = "Contagem corrigida")
    public LinhaInventarioResponse atualizarLinha(@PathVariable UUID id,
                                                  @PathVariable UUID linhaId,
                                                  @Valid @RequestBody AtualizarContagemRequest request) {
        LinhaInventario linha = linhaInventarioRepository.findByIdAndInventarioId(linhaId, id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("LinhaInventario", linhaId));
        if (linha.getInventarioFisico().isFechado()) {
            throw new BusinessException("INVENTARIO_FECHADO", "Inventario fisico ja esta fechado");
        }
        linha.atualizarQuantidadeContada(request.quantidade());
        return LinhaInventarioResponse.from(linhaInventarioRepository.save(linha));
    }

    @PostMapping("/{id}/fechar")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','STOCK_WRITE') or hasRole('ARMAZEM')")
    @Operation(summary = "Fechar inventario fisico")
    @ApiResponse(responseCode = "200", description = "Inventario fechado")
    public void fecharInventario(@PathVariable UUID id) {
        stock.fecharInventario(id);
    }

    @GetMapping("/{id}/discrepancias")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','STOCK_WRITE') or hasRole('ARMAZEM')")
    @Operation(summary = "Listar discrepancias de inventario")
    @ApiResponse(responseCode = "200", description = "Discrepancias listadas")
    public List<LinhaInventarioResponse> listarDiscrepancias(@PathVariable UUID id) {
        return stock.listarDiscrepanciasInventario(id).stream()
                .map(LinhaInventarioResponse::from)
                .toList();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ','STOCK_WRITE') or hasRole('ARMAZEM')")
    @Operation(summary = "Listar inventarios fisicos")
    @ApiResponse(responseCode = "200", description = "Inventarios listados")
    public Page<InventarioFisicoResponse> listarInventarios(@RequestParam UUID lojaId, Pageable pageable) {
        return inventarioFisicoRepository.findByLojaId(lojaId, pageable).map(InventarioFisicoResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ','STOCK_WRITE') or hasRole('ARMAZEM')")
    @Operation(summary = "Obter inventario fisico")
    @ApiResponse(responseCode = "200", description = "Inventario encontrado")
    public InventarioFisicoResponse obterInventario(@PathVariable UUID id) {
        InventarioFisico inventario = inventarioFisicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InventarioFisico", id));
        return InventarioFisicoResponse.from(inventario);
    }
}
