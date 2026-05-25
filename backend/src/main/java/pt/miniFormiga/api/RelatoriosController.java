package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.subsistemas.relatorios.ISubRelatorios;

import java.time.LocalDate;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.relatorios.RelatoriosDtos.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "RELATORIOS", description = "Dashboard, relatorios e exportacao contabilistica")
public class RelatoriosController {

    private final ISubRelatorios relatorios;

    public RelatoriosController(ISubRelatorios relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ')")
    @Operation(summary = "Consultar dashboard operacional")
    @ApiResponse(responseCode = "200", description = "Dashboard calculado")
    public DashboardResponse dashboard(@RequestParam(required = false) UUID lojaId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                       @RequestParam(required = false) UUID categoriaId,
                                       @RequestParam(required = false) UUID produtoId,
                                       @RequestParam(required = false) String turno) {
        return relatorios.obterDashboard(new RelatorioFiltro(lojaId, inicio, fim, categoriaId, produtoId, turno));
    }

    @GetMapping("/relatorios/vendas")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ')")
    @Operation(summary = "Consultar relatorio de vendas")
    @ApiResponse(responseCode = "200", description = "Relatorio de vendas calculado")
    public RelatorioVendasResponse vendas(@RequestParam(required = false) UUID lojaId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                          @RequestParam(required = false) UUID categoriaId,
                                          @RequestParam(required = false) UUID produtoId,
                                          @RequestParam(required = false) String turno) {
        return relatorios.relatorioVendas(new RelatorioFiltro(lojaId, inicio, fim, categoriaId, produtoId, turno));
    }

    @GetMapping("/relatorios/stock")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ')")
    @Operation(summary = "Consultar relatorio de stock")
    @ApiResponse(responseCode = "200", description = "Relatorio de stock calculado")
    public RelatorioStockResponse stock(@RequestParam(required = false) UUID lojaId,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                        @RequestParam(required = false) UUID categoriaId,
                                        @RequestParam(required = false) UUID produtoId) {
        return relatorios.relatorioStock(new RelatorioFiltro(lojaId, inicio, fim, categoriaId, produtoId, null));
    }

    @GetMapping("/relatorios/rentabilidade")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ')")
    @Operation(summary = "Consultar relatorio de rentabilidade")
    @ApiResponse(responseCode = "200", description = "Relatorio de rentabilidade calculado")
    public RelatorioRentabilidadeResponse rentabilidade(@RequestParam(required = false) UUID lojaId,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                                        @RequestParam(required = false) UUID categoriaId,
                                                        @RequestParam(required = false) UUID produtoId,
                                                        @RequestParam(required = false) String turno) {
        return relatorios.relatorioRentabilidade(new RelatorioFiltro(lojaId, inicio, fim, categoriaId, produtoId, turno));
    }

    @PostMapping("/relatorios/exportar")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ')")
    @Operation(summary = "Exportar relatorio")
    @ApiResponse(responseCode = "200", description = "Relatorio exportado")
    public ResponseEntity<byte[]> exportar(@Valid @RequestBody ExportarRelatorioRequest request) {
        ExportacaoRelatorio exportacao = relatorios.exportar(request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportacao.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportacao.nomeFicheiro() + "\"")
                .body(exportacao.conteudo());
    }
}
