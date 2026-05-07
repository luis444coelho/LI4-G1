package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.domain.LocalizacaoProduto;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.repository.LocalizacaoProdutoRepository;
import pt.miniFormiga.repository.ProdutoRepository;
import pt.miniFormiga.subsistemas.stock.StockDtos.LocalizacaoRequest;
import pt.miniFormiga.subsistemas.stock.StockDtos.LocalizacaoResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/produtos/{id}/localizacao")
public class ProdutoLocalizacaoController {

    private final ProdutoRepository produtoRepository;
    private final LocalizacaoProdutoRepository localizacaoProdutoRepository;

    public ProdutoLocalizacaoController(ProdutoRepository produtoRepository,
                                        LocalizacaoProdutoRepository localizacaoProdutoRepository) {
        this.produtoRepository = produtoRepository;
        this.localizacaoProdutoRepository = localizacaoProdutoRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_WRITE') or hasAnyRole('RESP_ARMAZEM','RESPONSAVEL_ARMAZEM')")
    @Operation(summary = "Obter localizacao de produto")
    @ApiResponse(responseCode = "200", description = "Localizacao encontrada")
    public LocalizacaoResponse obterLocalizacao(@PathVariable UUID id) {
        return localizacaoProdutoRepository.findByProdutoId(id)
                .map(LocalizacaoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("LocalizacaoProduto", id));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('STOCK_WRITE') or hasAnyRole('RESP_ARMAZEM','RESPONSAVEL_ARMAZEM')")
    @Operation(summary = "Atualizar localizacao de produto")
    @ApiResponse(responseCode = "200", description = "Localizacao atualizada")
    public LocalizacaoResponse atualizarLocalizacao(@PathVariable UUID id, @Valid @RequestBody LocalizacaoRequest request) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
        LocalizacaoProduto localizacao = localizacaoProdutoRepository.findByProdutoId(id)
                .orElseGet(() -> new LocalizacaoProduto(produto, request.corredor(), request.prateleira(), null));
        localizacao.atualizar(request.corredor(), request.prateleira(), null);
        return LocalizacaoResponse.from(localizacaoProdutoRepository.save(localizacao));
    }
}
