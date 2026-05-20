package pt.miniFormiga.api;

import org.junit.jupiter.api.Test;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.LocalizacaoProduto;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.repository.LocalizacaoProdutoRepository;
import pt.miniFormiga.repository.ProdutoRepository;
import pt.miniFormiga.subsistemas.stock.StockDtos.LocalizacaoRequest;
import pt.miniFormiga.subsistemas.stock.StockDtos.LocalizacaoResponse;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProdutoLocalizacaoControllerTest {

    @Test
    void atualizarLocalizacaoProdutoPersisteCorredorEPrateleira() {
        ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
        LocalizacaoProdutoRepository localizacaoRepository = mock(LocalizacaoProdutoRepository.class);
        Produto produto = produto();
        ProdutoLocalizacaoController controller = new ProdutoLocalizacaoController(produtoRepository, localizacaoRepository);

        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(localizacaoRepository.findByProdutoId(produto.getId())).thenReturn(Optional.empty());
        when(localizacaoRepository.save(any(LocalizacaoProduto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalizacaoResponse response = controller.atualizarLocalizacao(
                produto.getId(), new LocalizacaoRequest("C2", "P5"));

        assertEquals(produto.getId(), response.produtoId());
        assertEquals("C2", response.corredor());
        assertEquals("P5", response.prateleira());
        verify(localizacaoRepository).save(any(LocalizacaoProduto.class));
    }

    private Produto produto() {
        return new Produto("5600000000888", "Produto Localizacao", new BigDecimal("1.00"),
                new BigDecimal("0.50"), new TaxaIVA("Normal", new BigDecimal("23")),
                new Categoria("Stock", "Produtos em stock"));
    }
}
