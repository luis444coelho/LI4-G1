package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdutoLojaTest {

    @Test
    void criarProdutoLojaDefineStockNivelMinimoELocalizacaoPorLoja() {
        ProdutoLoja produtoLoja = new ProdutoLoja(produto(), loja(), 8, 10);
        LocalDateTime criadoEm = produtoLoja.getStockUpdatedAt();

        assertEquals(8, produtoLoja.getQuantidadeStock());
        assertEquals(10, produtoLoja.getNivelMinimo());
        assertTrue(produtoLoja.precisaReposicao());
        assertTrue(produtoLoja.isAtivoNaLoja());
        assertNotNull(criadoEm);

        produtoLoja.atualizarLocalizacao("C1", "P2");

        assertEquals("C1", produtoLoja.getCorredor());
        assertEquals("P2", produtoLoja.getPrateleira());
        assertFalse(produtoLoja.getStockUpdatedAt().isBefore(criadoEm));
    }

    @Test
    void atualizarStockNaoPermiteQuantidadeNegativa() {
        ProdutoLoja produtoLoja = new ProdutoLoja(produto(), loja(), 3, null);

        produtoLoja.atualizarStock(2);

        assertEquals(5, produtoLoja.getQuantidadeStock());
        assertFalse(produtoLoja.precisaReposicao());
        assertThrows(IllegalArgumentException.class, () -> produtoLoja.atualizarStock(-6));
    }

    @Test
    void validacoesProtegemInvariantesDeStockDaLoja() {
        Produto produto = produto();
        Loja loja = loja();

        assertThrows(NullPointerException.class, () -> new ProdutoLoja(null, loja, 1, 1));
        assertThrows(NullPointerException.class, () -> new ProdutoLoja(produto, null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProdutoLoja(produto, loja, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ProdutoLoja(produto, loja, 1, -1));
    }

    @Test
    void desativarNaLojaMantemProdutoMasRemoveDisponibilidadeLocal() {
        ProdutoLoja produtoLoja = new ProdutoLoja(produto(), loja(), 20, 5);

        produtoLoja.desativarNaLoja();

        assertFalse(produtoLoja.isAtivoNaLoja());
        assertFalse(produtoLoja.precisaReposicao());
    }

    private Produto produto() {
        return new Produto("5600000012345", "Agua Loja", new BigDecimal("1.00"),
                new BigDecimal("0.40"), new TaxaIVA("Normal", new BigDecimal("23")),
                new Categoria("Bebidas", "Bebidas frias"));
    }

    private Loja loja() {
        return new Loja("Loja Braga", "Rua Central", "123456789");
    }
}
