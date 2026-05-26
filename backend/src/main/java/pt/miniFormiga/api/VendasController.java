package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Devolucao;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.repository.DevolucaoRepository;
import pt.miniFormiga.repository.FaturaRepository;
import pt.miniFormiga.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.pdv.ISubPDV;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.pdv.PdvDtos.*;

@RestController
@RequestMapping("/api/v1/vendas")
@Tag(name = "VENDAS", description = "Operacoes transacionais do ponto de venda")
public class VendasController {
    private final ISubPDV pdv;
    private final FaturaRepository faturaRepository;
    private final DevolucaoRepository devolucaoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuditoriaService auditoria;

    public VendasController(ISubPDV pdv,
                            FaturaRepository faturaRepository,
                            DevolucaoRepository devolucaoRepository,
                            UtilizadorRepository utilizadorRepository,
                            AuditoriaService auditoria) {
        this.pdv = pdv;
        this.faturaRepository = faturaRepository;
        this.devolucaoRepository = devolucaoRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.auditoria = auditoria;
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
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar vendas")
    @ApiResponse(responseCode = "200", description = "Vendas listadas")
    public Page<VendaDTO> listar(@RequestParam UUID lojaId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                 @RequestParam(defaultValue = "false") boolean porFechar,
                                 Pageable pageable) {
        if (porFechar) {
            return pdv.listarVendasPorFechar(lojaId, inicio, fim, pageable);
        }
        return pdv.listarVendas(lojaId, inicio, fim, pageable);
    }

    @PostMapping("/{id}/fatura")
    @PreAuthorize("hasAuthority('PDV_WRITE')")
    @Operation(summary = "Emitir fatura")
    @ApiResponse(responseCode = "200", description = "Fatura emitida")
    public FaturaDTO emitirFatura(@PathVariable UUID id, @Valid @RequestBody EmitirFaturaRequest request) {
        return FaturaDTO.from(pdv.emitirFatura(id, request.nifCliente(), request.nomeCliente()));
    }

    @GetMapping("/faturas")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar faturas da loja")
    @ApiResponse(responseCode = "200", description = "Faturas listadas")
    public Page<FaturaDTO> listarFaturas(@RequestParam UUID lojaId,
                                         @RequestParam(required = false) String cliente,
                                         Pageable pageable) {
        String termo = cliente == null || cliente.isBlank() ? null : cliente.trim();
        return faturaRepository.pesquisarPorLojaECliente(lojaId, termo, pageable)
                .map(FaturaDTO::from);
    }

    @GetMapping("/faturas/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Obter fatura")
    @ApiResponse(responseCode = "200", description = "Fatura encontrada")
    public FaturaDTO obterFatura(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        FaturaDTO fatura = pdv.obterFatura(id);
        auditoria.registar(TipoOperacao.FATURA_CONSULTADA, utilizadorId(principal), "FATURA", id, "Fatura consultada");
        return fatura;
    }

    @GetMapping("/faturas/{id}/documento")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Emitir documento fiscal da fatura")
    @ApiResponse(responseCode = "200", description = "Documento fiscal emitido")
    public ResponseEntity<byte[]> documentoFiscal(@PathVariable UUID id,
                                                  @RequestParam(defaultValue = "FATURA") String tipo,
                                                  @AuthenticationPrincipal UserDetails principal) {
        byte[] conteudo = pdv.gerarDocumentoFiscal(id, tipo);
        String tipoNormalizado = tipo == null ? "FATURA" : tipo.trim().toUpperCase();
        auditoria.registar(TipoOperacao.FATURA_CONSULTADA, utilizadorId(principal), "FATURA", id,
                tipoNormalizado + " emitido para consulta");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"mini-formiga-" + tipoNormalizado.toLowerCase() + "-" + id + ".pdf\"")
                .body(conteudo);
    }

    @GetMapping("/faturas/numero")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Obter fatura por numero")
    @ApiResponse(responseCode = "200", description = "Fatura encontrada")
    public FaturaDTO obterFaturaPorNumero(@RequestParam String numeroFatura,
                                          @AuthenticationPrincipal UserDetails principal) {
        NumeroFatura numero = parseNumeroFatura(numeroFatura);
        FaturaDTO fatura = faturaRepository.findBySerieAndNumero(numero.serie(), numero.numero())
                .map(FaturaDTO::from)
                .orElseThrow(() -> new BusinessException("FATURA_NAO_ENCONTRADA", "Fatura nao encontrada"));
        auditoria.registar(TipoOperacao.FATURA_CONSULTADA, utilizadorId(principal), "FATURA", fatura.id(), "Fatura consultada");
        return fatura;
    }

    @PostMapping("/{id}/devolucao")
    @PreAuthorize("hasAuthority('PDV_WRITE')")
    @Operation(summary = "Processar devolucao")
    @ApiResponse(responseCode = "200", description = "Devolucao processada")
    public VendaDTO devolucao(@PathVariable UUID id, @Valid @RequestBody ProcessarDevolucaoRequest request) {
        if (pdv instanceof pt.miniFormiga.subsistemas.pdv.SubPDVFacade facade) {
            return facade.processarDevolucao(id, request);
        }
        throw new IllegalStateException("SubPDV nao suporta devolucoes nesta implementacao");
    }

    @GetMapping("/devolucoes")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar devolucoes da loja")
    @ApiResponse(responseCode = "200", description = "Devolucoes listadas")
    public java.util.List<DevolucaoDTO> listarDevolucoes(@RequestParam UUID lojaId) {
        return devolucaoRepository.findByVendaLojaIdOrderByDataHoraDesc(lojaId).stream()
                .map(DevolucaoDTO::from)
                .toList();
    }

    public record DevolucaoDTO(UUID id,
                               UUID vendaId,
                               UUID produtoId,
                               String produto,
                               int quantidade,
                               BigDecimal valorCreditado,
                               LocalDateTime dataHora,
                               String numeroDocumento) {
        static DevolucaoDTO from(Devolucao devolucao) {
            return new DevolucaoDTO(
                    devolucao.getId(),
                    devolucao.getVenda().getId(),
                    devolucao.getProduto().getId(),
                    devolucao.getProduto().getNome(),
                    devolucao.getQuantidade(),
                    devolucao.getValorCreditado(),
                    devolucao.getDataHora(),
                    devolucao.getNumeroDocumento()
            );
        }
    }

    private static NumeroFatura parseNumeroFatura(String numeroFatura) {
        if (numeroFatura == null || numeroFatura.isBlank()) {
            throw new BusinessException("NUMERO_FATURA_INVALIDO", "Numero de fatura invalido");
        }
        String normalizado = numeroFatura.trim();
        int ultimoSeparador = normalizado.lastIndexOf('/');
        if (ultimoSeparador <= 0 || ultimoSeparador == normalizado.length() - 1) {
            throw new BusinessException("NUMERO_FATURA_INVALIDO", "Numero de fatura invalido");
        }
        String serie = normalizado.substring(0, ultimoSeparador);
        try {
            int numero = Integer.parseInt(normalizado.substring(ultimoSeparador + 1));
            return new NumeroFatura(serie, numero);
        } catch (NumberFormatException exception) {
            throw new BusinessException("NUMERO_FATURA_INVALIDO", "Numero de fatura invalido");
        }
    }

    private record NumeroFatura(String serie, int numero) {
    }

    private UUID utilizadorId(UserDetails principal) {
        if (principal == null) {
            return null;
        }
        return utilizadorRepository.findByUsername(principal.getUsername())
                .map(pt.miniFormiga.domain.Utilizador::getId)
                .orElse(null);
    }
}
