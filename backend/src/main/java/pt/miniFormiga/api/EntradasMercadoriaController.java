package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.subsistemas.encomendas.facade.ISubEncomendas;

import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.*;

@RestController
@RequestMapping("/api/v1/entradas-mercadoria")
@Tag(name = "ENTRADAS_MERCADORIA", description = "Rececao de mercadoria com guia de remessa")
public class EntradasMercadoriaController {

    private final ISubEncomendas encomendas;

    public EntradasMercadoriaController(ISubEncomendas encomendas) {
        this.encomendas = encomendas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE','STOCK_WRITE')")
    @Operation(summary = "Registar entrada de mercadoria")
    @ApiResponse(responseCode = "201", description = "Entrada registada")
    public List<EntradaMercadoriaResponse> registar(@Valid @RequestBody RegistarEntradaMercadoriaRequest request) {
        return encomendas.registarEntradaMercadoria(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar entradas de mercadoria")
    @ApiResponse(responseCode = "200", description = "Entradas listadas")
    public Page<EntradaMercadoriaResponse> listar(@RequestParam UUID lojaId, Pageable pageable) {
        return encomendas.listarEntradasMercadoria(lojaId, pageable);
    }

    @GetMapping("/proxima-guia")
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','ENCOMENDAS_WRITE','STOCK_WRITE')")
    @Operation(summary = "Obter proximo numero de guia de remessa")
    @ApiResponse(responseCode = "200", description = "Numero calculado")
    public ProximaGuiaRemessaResponse proximaGuia(@RequestParam UUID lojaId) {
        return encomendas.obterProximaGuiaRemessa(lojaId);
    }
}
