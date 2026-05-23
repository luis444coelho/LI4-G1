package pt.miniFormiga.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.repository.CategoriaRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoriasControllerTest {

    @Test
    void listarCategoriasDevolveCategoriasOrdenadasPorNome() {
        CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
        CategoriasController controller = new CategoriasController(categoriaRepository);
        Categoria mercearia = new Categoria("Mercearia", "Produtos alimentares");
        Categoria bebidas = new Categoria("Bebidas", "Bebidas engarrafadas");
        Sort ordenacao = Sort.by("nome");

        when(categoriaRepository.findAll(ordenacao)).thenReturn(List.of(bebidas, mercearia));

        List<CategoriasController.CategoriaResponse> response = controller.listar();

        assertEquals(2, response.size());
        assertEquals("Bebidas", response.get(0).nome());
        assertEquals("Mercearia", response.get(1).nome());
        verify(categoriaRepository).findAll(ordenacao);
    }
}
