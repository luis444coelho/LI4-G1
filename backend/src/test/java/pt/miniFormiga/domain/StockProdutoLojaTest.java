package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockProdutoLojaTest {

    @Test
    void criarStockProdutoLojaDefineStockNivelMinimoELocalizacaoPorLoja() {
        StockProdutoLoja stockProdutoLoja = new StockProdutoLoja(produto(), loja(), 8, 10);
        LocalDateTime criadoEm = stockProdutoLoja.getStockUpdatedAt();

        assertEquals(8, stockProdutoLoja.getQuantidadeStock());
        assertEquals(10, stockProdutoLoja.getNivelMinimo());
        assertTrue(stockProdutoLoja.precisaReposicao());
        assertTrue(stockProdutoLoja.isAtivoNaLoja());
        assertNotNull(criadoEm);

        stockProdutoLoja.atualizarLocalizacao("C1", "P2");

        assertEquals("C1", stockProdutoLoja.getCorredor());
        assertEquals("P2", stockProdutoLoja.getPrateleira());
        assertFalse(stockProdutoLoja.getStockUpdatedAt().isBefore(criadoEm));
    }

    @Test
    void atualizarStockNaoPermiteQuantidadeNegativa() {
        StockProdutoLoja stockProdutoLoja = new StockProdutoLoja(produto(), loja(), 3, null);

        stockProdutoLoja.atualizarStock(2);

        assertEquals(5, stockProdutoLoja.getQuantidadeStock());
        assertFalse(stockProdutoLoja.precisaReposicao());
        assertThrows(IllegalArgumentException.class, () -> stockProdutoLoja.atualizarStock(-6));
    }

    @Test
    void validacoesProtegemInvariantesDeStockDaLoja() {
        Produto produto = produto();
        Loja loja = loja();

        assertThrows(NullPointerException.class, () -> new StockProdutoLoja(null, loja, 1, 1));
        assertThrows(NullPointerException.class, () -> new StockProdutoLoja(produto, null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new StockProdutoLoja(produto, loja, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new StockProdutoLoja(produto, loja, 1, -1));
    }

    @Test
    void desativarNaLojaMantemProdutoMasRemoveDisponibilidadeLocal() {
        StockProdutoLoja stockProdutoLoja = new StockProdutoLoja(produto(), loja(), 20, 5);

        stockProdutoLoja.desativarNaLoja();

        assertFalse(stockProdutoLoja.isAtivoNaLoja());
        assertFalse(stockProdutoLoja.precisaReposicao());
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
