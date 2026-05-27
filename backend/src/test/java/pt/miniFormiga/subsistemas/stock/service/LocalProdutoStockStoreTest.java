package pt.miniFormiga.subsistemas.stock.service;

import org.junit.jupiter.api.Test;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.subsistemas.catalogo.repository.ProdutoRepository;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LocalProdutoStockStoreTest {

    @Test
    void listarSemLojaNaoConsultaRepositorioDeLojas() {
        ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        LocalProdutoStockStore store = new LocalProdutoStockStore(produtoRepository, lojaRepository);
        Produto produto = produto();
        when(produtoRepository.findAll()).thenReturn(List.of(produto));

        List<StockItem> itens = store.listar(null);

        assertEquals(1, itens.size());
        assertEquals(produto.getId(), itens.getFirst().produtoId());
        assertNull(itens.getFirst().lojaId());
        assertNull(itens.getFirst().lojaNome());
        verifyNoInteractions(lojaRepository);
    }

    @Test
    void obterComLojaMapeiaNomeDaLoja() {
        ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        LocalProdutoStockStore store = new LocalProdutoStockStore(produtoRepository, lojaRepository);
        Produto produto = produto();
        Loja loja = loja();
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));

        StockItem item = store.obter(produto.getId(), loja.getId());

        assertEquals(loja.getId(), item.lojaId());
        assertEquals("Loja Braga", item.lojaNome());
    }

    @Test
    void atualizarStockAteZeroEAtualizarLocalizacao() {
        ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        LocalProdutoStockStore store = new LocalProdutoStockStore(produtoRepository, lojaRepository);
        Produto produto = produto();
        produto.definirStockInicial(3);
        Loja loja = loja();
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));

        StockItem semStock = store.atualizarStock(produto.getId(), loja.getId(), -3);
        StockItem localizado = store.atualizarLocalizacao(produto.getId(), loja.getId(), "A2", "P4");

        assertEquals(0, semStock.quantidade());
        assertEquals("A2", localizado.corredor());
        assertEquals("P4", localizado.prateleira());
        assertThrows(StockInsuficienteException.class,
                () -> store.atualizarStock(produto.getId(), loja.getId(), -1));
    }

    @Test
    void obterProdutoInexistenteFalha() {
        ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
        LojaRepository lojaRepository = mock(LojaRepository.class);
        LocalProdutoStockStore store = new LocalProdutoStockStore(produtoRepository, lojaRepository);
        UUID produtoId = UUID.randomUUID();
        when(produtoRepository.findById(produtoId)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> store.obter(produtoId, null));
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
