package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoriaTest {

    @Test
    void deveCriarCategoriaComNomeEDescricao() {
        Categoria categoria = new Categoria("Bebidas", "Sumos e refrigerantes");

        assertEquals("Bebidas", categoria.getNome());
        assertEquals("Sumos e refrigerantes", categoria.getDescricao());
    }

    @Test
    void deveAssociarProdutoACategoria() {
        Categoria categoria = new Categoria("Snacks", "Batatas fritas e aperitivos");
        TaxaIVA taxaIVA = new TaxaIVA("Taxa Normal", new BigDecimal("23"));
        Produto produto = new Produto("5600000000001", "Batatas", new BigDecimal("1.50"), new BigDecimal("0.90"), taxaIVA, categoria);

        assertTrue(categoria.getProdutos().contains(produto));
    }
}
