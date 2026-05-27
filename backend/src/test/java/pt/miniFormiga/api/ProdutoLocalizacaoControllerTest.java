package pt.miniFormiga.api;

import org.junit.jupiter.api.Test;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.subsistemas.stock.dto.StockDtos.LocalizacaoRequest;
import pt.miniFormiga.subsistemas.stock.dto.StockDtos.LocalizacaoResponse;
import pt.miniFormiga.subsistemas.stock.service.StockItem;
import pt.miniFormiga.subsistemas.stock.service.StockStore;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProdutoLocalizacaoControllerTest {

    @Test
    void atualizarLocalizacaoProdutoPersisteCorredorEPrateleira() {
        StockStore stockStore = mock(StockStore.class);
        Produto produto = produto();
        ProdutoLocalizacaoController controller = new ProdutoLocalizacaoController(stockStore);

        when(stockStore.atualizarLocalizacao(produto.getId(), null, "C2", "P5"))
                .thenReturn(new StockItem(produto, null, null, 0, null, "C2", "P5", null, null));

        LocalizacaoResponse response = controller.atualizarLocalizacao(
                produto.getId(), null, new LocalizacaoRequest("C2", "P5"));

        assertEquals(produto.getId(), response.produtoId());
        assertEquals("C2", response.corredor());
        assertEquals("P5", response.prateleira());
    }

    private Produto produto() {
        return new Produto("5600000000888", "Produto Localizacao", new BigDecimal("1.00"),
                new BigDecimal("0.50"), new TaxaIVA("Normal", new BigDecimal("23")),
                new Categoria("Stock", "Produtos em stock"));
    }
}
