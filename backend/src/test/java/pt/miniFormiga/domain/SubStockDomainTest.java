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
        Stock stock = new Stock(produto(), loja(), 10);

        assertFalse(stock.precisaReposicao());

        new NivelMinimo(stock, 10);

        assertTrue(stock.precisaReposicao());
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
        Stock stock = new Stock(produto, loja, 6);
        NivelMinimo nivelMinimo = new NivelMinimo(stock, 7);
        MotivoAjuste motivo = new MotivoAjuste("QUEBRA", "Produto danificado");
        Utilizador utilizador = new Utilizador("gerente", "hash", "Gerente", new Perfil("GERENTE", List.of("STOCK_WRITE")), loja);
        AjusteInventario ajuste = new AjusteInventario(stock, motivo, utilizador, -1, "produto partido");
        AlertaStock alerta = new AlertaStock(stock, stock.getQuantidade());
        LocalizacaoProduto localizacao = new LocalizacaoProduto(produto, "A", "3", "Perto da caixa");

        assertEquals(produto, stock.getProduto());
        assertEquals(loja, stock.getLoja());
        assertEquals(6, stock.getQuantidade());
        assertNotNull(stock.getDataDefinicao());
        assertTrue(stock.getAjustesInventario().isEmpty());
        assertTrue(stock.getAlertasStock().isEmpty());
        assertEquals(stock, nivelMinimo.getStock());
        assertNotNull(nivelMinimo.getDataDefinicao());
        assertEquals(stock, alerta.getStock());
        assertNotNull(alerta.getDataHora());
        assertEquals("QUEBRA", motivo.getCodigo());
        assertEquals("Produto danificado", motivo.getDescricao());
        assertEquals(stock, ajuste.getStock());
        assertEquals(motivo, ajuste.getMotivo());
        assertEquals(utilizador, ajuste.getResponsavel());
        assertEquals("produto partido", ajuste.getObservacoes());
        assertNotNull(ajuste.getDataHora());
        assertEquals(produto, localizacao.getProduto());
        assertEquals("A", localizacao.getCorredor());
        assertEquals("3", localizacao.getPrateleira());
        assertEquals("Perto da caixa", localizacao.getDescricao());
    }

    @Test
    void entidadesSubStockDevemAtualizarCamposMutaveis() {
        Produto produto = produto();
        Stock stock = new Stock(produto, loja(), 6);
        NivelMinimo nivelMinimo = new NivelMinimo(stock, 7);
        LinhaInventario linha = new LinhaInventario(inventario(), produto, 5, 6);
        LocalizacaoProduto localizacao = new LocalizacaoProduto(produto, "A", "3", "Perto da caixa");
        AlertaStock alerta = new AlertaStock(stock, 6);

        stock.atualizarQuantidade(2);
        nivelMinimo.atualizarQuantidade(8);
        linha.atualizarQuantidadeContada(9);
        localizacao.atualizar("B", "1", "Entrada");
        alerta.marcarComoLido();

        assertEquals(8, stock.getQuantidade());
        assertEquals(8, nivelMinimo.getQuantidade());
        assertEquals(9, linha.getQuantidadeContada());
        assertEquals(3, linha.getDiscrepancia());
        assertEquals("B", localizacao.getCorredor());
        assertEquals("1", localizacao.getPrateleira());
        assertEquals("Entrada", localizacao.getDescricao());
        assertTrue(alerta.isLido());
    }

    @Test
    void entidadesSubStockDevemValidarValoresInvalidos() {
        Stock stock = new Stock(produto(), loja(), 1);
        NivelMinimo nivelMinimo = new NivelMinimo(stock, 1);
        LinhaInventario linha = new LinhaInventario(inventario(), produto(), 1, 1);

        assertThrows(IllegalArgumentException.class, () -> new Stock(produto(), loja(), -1));
        assertThrows(IllegalArgumentException.class, () -> stock.atualizarQuantidade(-2));
        assertThrows(IllegalArgumentException.class, () -> nivelMinimo.atualizarQuantidade(-1));
        assertThrows(IllegalArgumentException.class, () -> linha.atualizarQuantidadeContada(-1));
    }

    private InventarioFisico inventario() {
        Loja loja = loja();
        Perfil perfil = new Perfil("RESP_ARMAZEM", List.of("STOCK_WRITE"));
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
