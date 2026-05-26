package pt.miniFormiga.subsistemas.stock;

import org.junit.jupiter.api.Test;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.ProdutoLoja;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.ProdutoLojaRepository;
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

class GlobalProdutoLojaStockStoreTest {

    private final ProdutoLojaRepository produtoLojaRepository = mock(ProdutoLojaRepository.class);
    private final ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
    private final LojaRepository lojaRepository = mock(LojaRepository.class);
    private final GlobalProdutoLojaStockStore store = new GlobalProdutoLojaStockStore(
            produtoLojaRepository,
            produtoRepository,
            lojaRepository
    );

    @Test
    void listarSemLojaDevolveStockDeTodasAsLojas() {
        ProdutoLoja braga = produtoLoja(5, 10);
        ProdutoLoja porto = produtoLoja(20, 5);
        when(produtoLojaRepository.findAll()).thenReturn(List.of(braga, porto));

        List<StockItem> stock = store.listar(null);

        assertEquals(2, stock.size());
        assertEquals(braga.getLoja().getId(), stock.get(0).lojaId());
        assertTrue(stock.get(0).precisaReposicao());
    }

    @Test
    void listarPorLojaFiltraProdutosAtivosDaLoja() {
        ProdutoLoja produtoLoja = produtoLoja(12, 5);
        UUID lojaId = produtoLoja.getLoja().getId();
        when(produtoLojaRepository.findByLojaIdAndAtivoNaLojaTrue(lojaId)).thenReturn(List.of(produtoLoja));

        List<StockItem> stock = store.listar(lojaId);

        assertEquals(1, stock.size());
        assertEquals(lojaId, stock.get(0).lojaId());
        assertEquals(12, stock.get(0).quantidade());
    }

    @Test
    void atualizarStockUsaProdutoLojaExistenteEProtegeStockNegativo() {
        ProdutoLoja produtoLoja = produtoLoja(4, 10);
        UUID produtoId = produtoLoja.getProduto().getId();
        UUID lojaId = produtoLoja.getLoja().getId();
        when(produtoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)).thenReturn(Optional.of(produtoLoja));

        StockItem atualizado = store.atualizarStock(produtoId, lojaId, 3);

        assertEquals(7, atualizado.quantidade());
        assertThrows(StockInsuficienteException.class, () -> store.atualizarStock(produtoId, lojaId, -8));
    }

    @Test
    void definirNivelMinimoCriaProdutoLojaQuandoNaoExiste() {
        Produto produto = produto();
        Loja loja = loja();
        when(produtoLojaRepository.findByProdutoIdAndLojaId(produto.getId(), loja.getId())).thenReturn(Optional.empty());
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(produtoLojaRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockItem item = store.definirNivelMinimo(produto.getId(), loja.getId(), 6);

        assertEquals(6, item.nivelMinimo());
        assertEquals(loja.getId(), item.lojaId());
        verify(produtoLojaRepository).save(org.mockito.ArgumentMatchers.any(ProdutoLoja.class));
    }

    @Test
    void localizacaoTambemCriaProdutoLojaEValidaLojaObrigatoria() {
        Produto produto = produto();
        Loja loja = loja();
        when(produtoLojaRepository.findByProdutoIdAndLojaId(produto.getId(), loja.getId())).thenReturn(Optional.empty());
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(produtoLojaRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockItem item = store.atualizarLocalizacao(produto.getId(), loja.getId(), "A2", "P4");

        assertEquals("A2", item.corredor());
        assertEquals("P4", item.prateleira());
        assertThrows(BusinessException.class, () -> store.obter(produto.getId(), null));
    }

    @Test
    void obterFalhaQuandoProdutoLojaNaoExiste() {
        UUID produtoId = UUID.randomUUID();
        UUID lojaId = UUID.randomUUID();
        when(produtoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> store.obter(produtoId, lojaId));
    }

    private ProdutoLoja produtoLoja(int quantidade, Integer minimo) {
        return new ProdutoLoja(produto(), loja(), quantidade, minimo);
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
