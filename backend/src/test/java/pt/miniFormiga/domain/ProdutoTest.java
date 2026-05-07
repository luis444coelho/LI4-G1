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

        assertEquals(new BigDecimal("66.67"), produto.calcularMargem());
    }

    @Test
    void deveCalcularMargemPercentagem() {
        Produto produto = criarProdutoBase();

        assertEquals(new BigDecimal("66.6700"), produto.calcularMargemPercentagem());
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
    void deveCalcularMargemZeroQuandoPrecoVendaZero() {
        Produto produto = new Produto(
                "5600000000001",
                "Produto gratis",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new TaxaIVA("Taxa Reduzida", new BigDecimal("6")),
                new Categoria("Amostras", "Amostras")
        );

        assertEquals(new BigDecimal("0.00"), produto.calcularMargem());
        assertEquals(new BigDecimal("0.0000"), produto.calcularMargemPercentagem());
    }

    @Test
    void deveAtualizarCamposOpcionaisDoProduto() {
        Produto produto = criarProdutoBase();
        Categoria higiene = new Categoria("Higiene", "Higiene pessoal");
        TaxaIVA taxaReduzida = new TaxaIVA("Taxa Reduzida", new BigDecimal("6"));

        produto.atualizar("Agua Mineral", "Sem gas", new BigDecimal("1.50"), new BigDecimal("0.60"),
                higiene, taxaReduzida, null, false);

        assertEquals("Agua Mineral", produto.getNome());
        assertEquals("Sem gas", produto.getDescricao());
        assertEquals(new BigDecimal("1.50"), produto.getPrecoVenda());
        assertEquals(new BigDecimal("0.60"), produto.getPrecoCusto());
        assertEquals(higiene, produto.getCategoria());
        assertEquals(taxaReduzida, produto.getTaxaIva());
        assertFalse(produto.isAtivo());
        assertTrue(higiene.getProdutos().contains(produto));
        assertTrue(taxaReduzida.getProdutos().contains(produto));
    }

    @Test
    void deveRejeitarTextoObrigatorioVazio() {
        Categoria categoria = new Categoria("Bebidas", "Bebidas frescas");
        TaxaIVA taxaIVA = new TaxaIVA("Taxa Normal", new BigDecimal("23"));

        assertThrows(IllegalArgumentException.class, () ->
                new Produto("   ", "Agua", new BigDecimal("1.20"), new BigDecimal("0.40"), taxaIVA, categoria));
        assertThrows(IllegalArgumentException.class, () ->
                criarProdutoBase().atualizar("   ", null, null, null, null, null, null, null));
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
