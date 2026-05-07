package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.miniFormiga.subsistemas.pdv.ISubPDV;

import java.util.UUID;

import static pt.miniFormiga.subsistemas.pdv.PdvDtos.*;

@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "PRODUTOS", description = "Catalogo de produtos do PDV")
public class ProdutosController {
    private final ISubPDV pdv;

    public ProdutosController(ISubPDV pdv) {
        this.pdv = pdv;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar produtos")
    @ApiResponse(responseCode = "200", description = "Produtos listados")
    public Page<ProdutoDTO> listar(@RequestParam(required = false) UUID lojaId, Pageable pageable) {
        return pdv.listarProdutos(lojaId, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','STOCK_WRITE')")
    @Operation(summary = "Criar produto")
    @ApiResponse(responseCode = "200", description = "Produto criado")
    public ProdutoDTO criar(@Valid @RequestBody CriarProdutoRequest request) {
        return pdv.criarProduto(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Obter produto por id")
    @ApiResponse(responseCode = "200", description = "Produto encontrado")
    public ProdutoDTO obter(@PathVariable UUID id) {
        return pdv.obterProdutoPorId(id);
    }

    @GetMapping("/barcode/{codigo}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Obter produto por codigo de barras")
    @ApiResponse(responseCode = "200", description = "Produto encontrado")
    public ProdutoDTO obterPorCodigo(@PathVariable String codigo) {
        return pdv.obterProdutoPorCodigoBarras(codigo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','STOCK_WRITE')")
    @Operation(summary = "Atualizar produto")
    @ApiResponse(responseCode = "200", description = "Produto atualizado")
    public ProdutoDTO atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarProdutoRequest request) {
        return pdv.atualizarProduto(id, request);
    }
}
