package pt.miniFormiga.subsistemas.stock;

import org.junit.jupiter.api.Test;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.StockProdutoLoja;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.StockProdutoLojaRepository;
import pt.miniFormiga.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalStockProdutoLojaStoreTest {

    private final StockProdutoLojaRepository stockProdutoLojaRepository = mock(StockProdutoLojaRepository.class);
    private final ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
    private final LojaRepository lojaRepository = mock(LojaRepository.class);
    private final GlobalStockProdutoLojaStore store = new GlobalStockProdutoLojaStore(
            stockProdutoLojaRepository,
            produtoRepository,
            lojaRepository
    );

    @Test
    void listarSemLojaDevolveStockDeTodasAsLojas() {
        StockProdutoLoja braga = stockProdutoLoja(5, 10);
        StockProdutoLoja porto = stockProdutoLoja(20, 5);
        when(stockProdutoLojaRepository.findAll()).thenReturn(List.of(braga, porto));

        List<StockItem> stock = store.listar(null);

        assertEquals(2, stock.size());
        assertEquals(braga.getLoja().getId(), stock.get(0).lojaId());
        assertTrue(stock.get(0).precisaReposicao());
    }

    @Test
    void listarPorLojaFiltraProdutosAtivosDaLoja() {
        StockProdutoLoja stockProdutoLoja = stockProdutoLoja(12, 5);
        UUID lojaId = stockProdutoLoja.getLoja().getId();
        when(stockProdutoLojaRepository.findByLojaIdAndAtivoNaLojaTrue(lojaId)).thenReturn(List.of(stockProdutoLoja));

        List<StockItem> stock = store.listar(lojaId);

        assertEquals(1, stock.size());
        assertEquals(lojaId, stock.get(0).lojaId());
        assertEquals(12, stock.get(0).quantidade());
    }

    @Test
    void atualizarStockUsaStockProdutoLojaExistenteEProtegeStockNegativo() {
        StockProdutoLoja stockProdutoLoja = stockProdutoLoja(4, 10);
        UUID produtoId = stockProdutoLoja.getProduto().getId();
        UUID lojaId = stockProdutoLoja.getLoja().getId();
        when(stockProdutoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)).thenReturn(Optional.of(stockProdutoLoja));

        StockItem atualizado = store.atualizarStock(produtoId, lojaId, 3);

        assertEquals(7, atualizado.quantidade());
        assertThrows(StockInsuficienteException.class, () -> store.atualizarStock(produtoId, lojaId, -8));
    }

    @Test
    void definirNivelMinimoCriaStockProdutoLojaQuandoNaoExiste() {
        Produto produto = produto();
        Loja loja = loja();
        when(stockProdutoLojaRepository.findByProdutoIdAndLojaId(produto.getId(), loja.getId())).thenReturn(Optional.empty());
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(stockProdutoLojaRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockItem item = store.definirNivelMinimo(produto.getId(), loja.getId(), 6);

        assertEquals(6, item.nivelMinimo());
        assertEquals(loja.getId(), item.lojaId());
        verify(stockProdutoLojaRepository).save(org.mockito.ArgumentMatchers.any(StockProdutoLoja.class));
    }

    @Test
    void localizacaoTambemCriaStockProdutoLojaEValidaLojaObrigatoria() {
        Produto produto = produto();
        Loja loja = loja();
        when(stockProdutoLojaRepository.findByProdutoIdAndLojaId(produto.getId(), loja.getId())).thenReturn(Optional.empty());
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(stockProdutoLojaRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockItem item = store.atualizarLocalizacao(produto.getId(), loja.getId(), "A2", "P4");

        assertEquals("A2", item.corredor());
        assertEquals("P4", item.prateleira());
        assertThrows(BusinessException.class, () -> store.obter(produto.getId(), null));
    }

    @Test
    void obterFalhaQuandoStockProdutoLojaNaoExiste() {
        UUID produtoId = UUID.randomUUID();
        UUID lojaId = UUID.randomUUID();
        when(stockProdutoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> store.obter(produtoId, lojaId));
    }

    private StockProdutoLoja stockProdutoLoja(int quantidade, Integer minimo) {
        return new StockProdutoLoja(produto(), loja(), quantidade, minimo);
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
