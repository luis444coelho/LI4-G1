package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.subsistemas.encomendas.facade.ISubEncomendas;

import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.*;

@RestController
@RequestMapping("/api/v1/fornecedores")
@Tag(name = "FORNECEDORES", description = "Gestao de fornecedores e condicoes comerciais")
public class FornecedoresController {

    private final ISubEncomendas encomendas;

    public FornecedoresController(ISubEncomendas encomendas) {
        this.encomendas = encomendas;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar fornecedores")
    @ApiResponse(responseCode = "200", description = "Fornecedores listados")
    public Page<FornecedorResponse> listar(Pageable pageable) {
        return encomendas.listarFornecedores(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Obter fornecedor")
    @ApiResponse(responseCode = "200", description = "Fornecedor encontrado")
    public FornecedorResponse obter(@PathVariable UUID id) {
        return encomendas.obterFornecedor(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GLOBAL_ADMIN')")
    @Operation(summary = "Registar fornecedor")
    @ApiResponse(responseCode = "201", description = "Fornecedor criado")
    public FornecedorResponse criar(@Valid @RequestBody CriarFornecedorRequest request) {
        return encomendas.criarFornecedor(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GLOBAL_ADMIN')")
    @Operation(summary = "Atualizar fornecedor")
    @ApiResponse(responseCode = "200", description = "Fornecedor atualizado")
    public FornecedorResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarFornecedorRequest request) {
        return encomendas.atualizarFornecedor(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('GLOBAL_ADMIN')")
    @Operation(summary = "Desativar fornecedor")
    @ApiResponse(responseCode = "204", description = "Fornecedor desativado")
    public void desativar(@PathVariable UUID id) {
        encomendas.desativarFornecedor(id);
    }

    @GetMapping("/{id}/condicoes")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar condicoes comerciais do fornecedor")
    @ApiResponse(responseCode = "200", description = "Condicoes listadas")
    public List<CondicaoComercialResponse> listarCondicoes(@PathVariable UUID id) {
        return encomendas.listarCondicoesComerciais(id);
    }

    @PostMapping("/{id}/condicoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GLOBAL_ADMIN')")
    @Operation(summary = "Definir condicao comercial")
    @ApiResponse(responseCode = "201", description = "Condicao comercial registada")
    public CondicaoComercialResponse definirCondicao(@PathVariable UUID id,
                                                     @Valid @RequestBody CondicaoComercialRequest request) {
        return encomendas.definirCondicaoComercial(id, request);
    }
}
