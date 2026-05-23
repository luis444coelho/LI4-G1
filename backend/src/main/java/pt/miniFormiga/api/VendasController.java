package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.miniFormiga.subsistemas.pdv.ISubPDV;

import java.time.LocalDate;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.pdv.PdvDtos.*;

@RestController
@RequestMapping("/api/v1/vendas")
@Tag(name = "VENDAS", description = "Operacoes transacionais do ponto de venda")
public class VendasController {
    private final ISubPDV pdv;

    public VendasController(ISubPDV pdv) {
        this.pdv = pdv;
    }

    public record IniciarVendaRequest(@NotNull UUID lojaId, @NotNull UUID operadorId) { }
    public record RegistarVendaRequest(@NotNull UUID lojaId, @NotNull UUID utilizadorId) { }

    @PostMapping
    @PreAuthorize("hasAuthority('PDV_WRITE')")
    @Operation(summary = "Iniciar venda")
    @ApiResponse(responseCode = "200", description = "Venda aberta")
    public VendaDTO iniciar(@Valid @RequestBody RegistarVendaRequest request) {
        return VendaDTO.from(pdv.registarVenda(request.lojaId(), request.utilizadorId()));
    }

    @PostMapping("/{id}/linhas")
    @PreAuthorize("hasAuthority('PDV_WRITE')")
    @Operation(summary = "Adicionar linha a venda")
    @ApiResponse(responseCode = "200", description = "Linha adicionada")
    public VendaDTO adicionarLinha(@PathVariable UUID id, @Valid @RequestBody AdicionarLinhaRequest request) {
        pdv.adicionarLinhaVenda(id, request.produtoId(), request.quantidade());
        return pdv.obterVenda(id);
    }

    @DeleteMapping("/{id}/linhas/{linhaId}")
    @PreAuthorize("hasAuthority('PDV_WRITE')")
    @Operation(summary = "Remover linha da venda")
    @ApiResponse(responseCode = "200", description = "Linha removida")
    public VendaDTO removerLinha(@PathVariable UUID id, @PathVariable UUID linhaId) {
        pdv.anularLinhaVenda(id, linhaId);
        return pdv.obterVenda(id);
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAuthority('PDV_WRITE')")
    @Operation(summary = "Finalizar venda")
    @ApiResponse(responseCode = "200", description = "Venda finalizada")
    public VendaDTO finalizar(@PathVariable UUID id, @Valid @RequestBody FinalizarVendaRequest request) {
        return VendaDTO.from(pdv.finalizarVenda(id, request.meioPagamento()));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyAuthority('PDV_WRITE','STOCK_WRITE')")
    @Operation(summary = "Anular venda")
    @ApiResponse(responseCode = "200", description = "Venda anulada")
    public void anular(@PathVariable UUID id) {
        pdv.anularVenda(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Obter venda")
    @ApiResponse(responseCode = "200", description = "Venda encontrada")
    public VendaDTO obter(@PathVariable UUID id) {
        return pdv.obterVenda(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','RELATORIOS_READ')")
    @Operation(summary = "Listar vendas")
    @ApiResponse(responseCode = "200", description = "Vendas listadas")
    public Page<VendaDTO> listar(@RequestParam UUID lojaId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                 Pageable pageable) {
        return pdv.listarVendas(lojaId, inicio, fim, pageable);
    }

    @PostMapping("/{id}/fatura")
    @PreAuthorize("hasAuthority('PDV_WRITE')")
    @Operation(summary = "Emitir fatura")
    @ApiResponse(responseCode = "200", description = "Fatura emitida")
    public FaturaDTO emitirFatura(@PathVariable UUID id, @Valid @RequestBody EmitirFaturaRequest request) {
        return FaturaDTO.from(pdv.emitirFatura(id, request.nifCliente(), request.nomeCliente()));
    }

    @PostMapping("/{id}/devolucao")
    @PreAuthorize("hasAuthority('PDV_WRITE')")
    @Operation(summary = "Processar devolucao")
    @ApiResponse(responseCode = "200", description = "Devolucao processada")
    public VendaDTO devolucao(@PathVariable UUID id, @Valid @RequestBody ProcessarDevolucaoRequest request) {
        if (pdv instanceof pt.miniFormiga.subsistemas.pdv.PDVFacade facade) {
            return facade.processarDevolucao(id, request);
        }
        throw new IllegalStateException("SubPDV nao suporta devolucoes nesta implementacao");
    }
}
