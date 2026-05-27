package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.subsistemas.catalogo.repository.CategoriaRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "CATEGORIAS", description = "Categorias de produtos para filtros e catalogo")
public class CategoriasController {

    private final CategoriaRepository categoriaRepository;

    public CategoriasController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public record CategoriaResponse(UUID id, String nome, String descricao) {
        static CategoriaResponse from(Categoria categoria) {
            return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('GLOBAL_ADMIN','PDV_WRITE','STOCK_WRITE','RELATORIOS_READ')")
    @Operation(summary = "Listar categorias de produtos")
    @ApiResponse(responseCode = "200", description = "Categorias listadas")
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll(Sort.by("nome")).stream()
                .map(CategoriaResponse::from)
                .toList();
    }
}
