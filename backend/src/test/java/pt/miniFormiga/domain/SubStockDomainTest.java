package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubStockDomainTest {

    @Test
    void linhaInventarioCalculaDiscrepanciaPositivaNegativaEZero() {
        InventarioFisico inventario = inventario();
        Produto produto = produto();

        LinhaInventario positiva = new LinhaInventario(inventario, produto, 12, 10);
        LinhaInventario negativa = new LinhaInventario(inventario, produto, 8, 10);
        LinhaInventario zero = new LinhaInventario(inventario, produto, 10, 10);

        assertEquals(2, positiva.getDiscrepancia());
        assertEquals(-2, negativa.getDiscrepancia());
        assertEquals(0, zero.getDiscrepancia());
    }

    @Test
    void stockPrecisaReposicaoQuandoQuantidadeMenorOuIgualAoNivelMinimo() {
        Produto produto = produto();
        produto.definirStockInicial(10);

        assertFalse(produto.precisaReposicao());

        produto.definirNivelMinimo(10);

        assertTrue(produto.precisaReposicao());
    }

    @Test
    void inventarioFechadoNaoAceitaNovasLinhas() {
        InventarioFisico inventario = inventario();
        inventario.fechar();

        assertThrows(IllegalStateException.class, () -> new LinhaInventario(inventario, produto(), 1, 1));
    }

    @Test
    void entidadesSubStockDevemExporEstadoDoDiagrama() {
        Loja loja = loja();
        Produto produto = produto();
        StockProdutoLoja stockProdutoLoja = new StockProdutoLoja(produto, loja, 6, 7);
        Utilizador utilizador = new Utilizador("gerente", "hash", "Gerente", PerfilUtilizador.GERENTE, loja);
        AjusteInventario ajuste = new AjusteInventario(stockProdutoLoja, MotivoAjusteCodigo.QUEBRA, utilizador, -1, "produto partido");
        AlertaStock alerta = new AlertaStock(stockProdutoLoja, stockProdutoLoja.getQuantidadeStock());

        assertEquals(produto, stockProdutoLoja.getProduto());
        assertEquals(loja, stockProdutoLoja.getLoja());
        assertEquals(6, stockProdutoLoja.getQuantidadeStock());
        assertEquals(7, stockProdutoLoja.getNivelMinimo());
        assertEquals(stockProdutoLoja, alerta.getStockProdutoLoja());
        assertNotNull(alerta.getDataHora());
        assertEquals(stockProdutoLoja, ajuste.getStockProdutoLoja());
        assertEquals(MotivoAjusteCodigo.QUEBRA, ajuste.getMotivo());
        assertEquals(utilizador, ajuste.getResponsavel());
        assertEquals("produto partido", ajuste.getObservacoes());
        assertNotNull(ajuste.getDataHora());
    }

    @Test
    void entidadesSubStockDevemAtualizarCamposMutaveis() {
        Produto produto = produto();
        StockProdutoLoja stockProdutoLoja = new StockProdutoLoja(produto, loja(), 6, 7);
        LinhaInventario linha = new LinhaInventario(inventario(), produto, 5, 6);
        AlertaStock alerta = new AlertaStock(stockProdutoLoja, 6);

        stockProdutoLoja.atualizarStock(2);
        stockProdutoLoja.definirNivelMinimo(8);
        linha.atualizarQuantidadeContada(9);
        alerta.marcarComoLido();

        assertEquals(8, stockProdutoLoja.getQuantidadeStock());
        assertEquals(8, stockProdutoLoja.getNivelMinimo());
        assertEquals(9, linha.getQuantidadeContada());
        assertEquals(3, linha.getDiscrepancia());
        assertTrue(alerta.isLido());
    }

    @Test
    void entidadesSubStockDevemValidarValoresInvalidos() {
        StockProdutoLoja stockProdutoLoja = new StockProdutoLoja(produto(), loja(), 1, 1);
        LinhaInventario linha = new LinhaInventario(inventario(), produto(), 1, 1);

        assertThrows(IllegalArgumentException.class, () -> new StockProdutoLoja(produto(), loja(), -1, null));
        assertThrows(IllegalArgumentException.class, () -> stockProdutoLoja.atualizarStock(-2));
        assertThrows(IllegalArgumentException.class, () -> stockProdutoLoja.definirNivelMinimo(-1));
        assertThrows(IllegalArgumentException.class, () -> linha.atualizarQuantidadeContada(-1));
    }

    private InventarioFisico inventario() {
        Loja loja = loja();
        PerfilUtilizador perfil = PerfilUtilizador.ARMAZEM;
        Utilizador utilizador = new Utilizador("armazem", "hash", "Armazem", perfil, loja);
        return new InventarioFisico(loja, utilizador);
    }

    private Produto produto() {
        return new Produto("5600000000888", "Produto Stock", new BigDecimal("2.00"), new BigDecimal("1.00"),
                new TaxaIVA("Normal", new BigDecimal("23")), new Categoria("Stock", "Stock"));
    }

    private Loja loja() {
        return new Loja("Loja Stock", "Rua Stock", "111222333");
    }
}
