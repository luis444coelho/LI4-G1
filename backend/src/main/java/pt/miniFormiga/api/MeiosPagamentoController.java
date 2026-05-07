package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.repository.MeioPagamentoRepository;

import java.util.List;

import static pt.miniFormiga.subsistemas.pdv.PdvDtos.MeioPagamentoDTO;

@RestController
@RequestMapping("/api/v1/meios-pagamento")
@Tag(name = "MEIOS_PAGAMENTO", description = "Meios de pagamento disponiveis no PDV")
public class MeiosPagamentoController {
    private final MeioPagamentoRepository repository;

    public MeiosPagamentoController(MeioPagamentoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar meios de pagamento")
    @ApiResponse(responseCode = "200", description = "Meios de pagamento listados")
    public List<MeioPagamentoDTO> listar() {
        return repository.findAll().stream().map(MeioPagamentoDTO::from).toList();
    }
}
