package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdutoTest {

    @Test
    void deveCalcularMargem() {
        Produto produto = criarProdutoBase();

        assertEquals(new BigDecimal("0.80"), produto.calcularMargem());
    }

    @Test
    void deveCalcularMargemPercentagem() {
        Produto produto = criarProdutoBase();

        assertEquals(new BigDecimal("200.0000"), produto.calcularMargemPercentagem());
    }

    @Test
    void deveCalcularPrecoComIVA() {
        Produto produto = criarProdutoBase();

        assertEquals(new BigDecimal("1.48"), produto.calcularPrecoComIVA());
    }

    @Test
    void deveAssociarProdutoACategoriaETaxaIva() {
        Produto produto = criarProdutoBase();

        assertTrue(produto.getCategoria().getProdutos().contains(produto));
        assertTrue(produto.getTaxaIVA().getProdutos().contains(produto));
    }

    @Test
    void devePermitirDesativarProduto() {
        Produto produto = criarProdutoBase();

        produto.desativar();

        assertFalse(produto.isAtivo());
    }

    @Test
    void deveExigirTaxaIvaECategoria() {
        Categoria categoria = new Categoria("Bebidas", "Bebidas frescas");
        TaxaIVA taxaIVA = new TaxaIVA("Taxa Normal", new BigDecimal("23"));

        assertThrows(NullPointerException.class, () ->
                new Produto("5601234567890", "Agua 1.5L", new BigDecimal("1.20"), new BigDecimal("0.40"), null, categoria));
        assertThrows(NullPointerException.class, () ->
                new Produto("5601234567890", "Agua 1.5L", new BigDecimal("1.20"), new BigDecimal("0.40"), taxaIVA, null));
    }

    private Produto criarProdutoBase() {
        Categoria categoria = new Categoria("Bebidas", "Bebidas frescas");
        TaxaIVA taxaIVA = new TaxaIVA("Taxa Normal", new BigDecimal("23"));
        return new Produto(
                "5601234567890",
                "Agua 1.5L",
                new BigDecimal("1.20"),
                new BigDecimal("0.40"),
                taxaIVA,
                categoria
        );
    }
}
