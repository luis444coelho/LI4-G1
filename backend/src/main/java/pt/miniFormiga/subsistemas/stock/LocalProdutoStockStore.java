package pt.miniFormiga.subsistemas.stock;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.ProdutoRepository;

import java.util.List;
import java.util.UUID;

@Service
@Profile("!global & !central")
@Transactional
public class LocalProdutoStockStore implements StockStore {

    private final ProdutoRepository produtoRepository;
    private final LojaRepository lojaRepository;

    public LocalProdutoStockStore(ProdutoRepository produtoRepository, LojaRepository lojaRepository) {
        this.produtoRepository = produtoRepository;
        this.lojaRepository = lojaRepository;
    }

    @Override
    public List<StockItem> listar(UUID lojaId) {
        String lojaNome = lojaId == null ? null : lojaRepository.findById(lojaId).map(Loja::getNome).orElse(null);
        return produtoRepository.findAll().stream()
                .map(produto -> StockItem.local(produto, lojaId, lojaNome))
                .toList();
    }

    @Override
    public StockItem obter(UUID produtoId, UUID lojaId) {
        Produto produto = obterProduto(produtoId);
        String lojaNome = lojaId == null ? null : lojaRepository.findById(lojaId).map(Loja::getNome).orElse(null);
        return StockItem.local(produto, lojaId, lojaNome);
    }

    @Override
    public StockItem atualizarStock(UUID produtoId, UUID lojaId, int delta) {
        Produto produto = obterProduto(produtoId);
        int novaQuantidade = produto.getQuantidadeStock() + delta;
        if (novaQuantidade < 0) {
            throw new StockInsuficienteException(produtoId, produto.getQuantidadeStock(), Math.abs(delta));
        }
        produto.atualizarStock(delta);
        return obter(produtoId, lojaId);
    }

    @Override
    public StockItem definirNivelMinimo(UUID produtoId, UUID lojaId, int quantidade) {
        Produto produto = obterProduto(produtoId);
        produto.definirNivelMinimo(quantidade);
        return obter(produtoId, lojaId);
    }

    @Override
    public StockItem atualizarLocalizacao(UUID produtoId, UUID lojaId, String corredor, String prateleira) {
        Produto produto = obterProduto(produtoId);
        produto.atualizarLocalizacao(corredor, prateleira);
        return obter(produtoId, lojaId);
    }

    private Produto obterProduto(UUID produtoId) {
        return produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
    }
}
