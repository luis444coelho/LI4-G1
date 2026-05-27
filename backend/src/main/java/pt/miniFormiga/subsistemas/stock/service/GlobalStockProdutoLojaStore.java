package pt.miniFormiga.subsistemas.stock.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.StockProdutoLoja;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;
import pt.miniFormiga.subsistemas.stock.repository.StockProdutoLojaRepository;
import pt.miniFormiga.subsistemas.catalogo.repository.ProdutoRepository;

import java.util.List;
import java.util.UUID;

@Service
@Profile("global | central")
@Transactional
public class GlobalStockProdutoLojaStore implements StockStore {

    private final StockProdutoLojaRepository stockProdutoLojaRepository;
    private final ProdutoRepository produtoRepository;
    private final LojaRepository lojaRepository;

    public GlobalStockProdutoLojaStore(StockProdutoLojaRepository stockProdutoLojaRepository,
                                       ProdutoRepository produtoRepository,
                                       LojaRepository lojaRepository) {
        this.stockProdutoLojaRepository = stockProdutoLojaRepository;
        this.produtoRepository = produtoRepository;
        this.lojaRepository = lojaRepository;
    }

    @Override
    public List<StockItem> listar(UUID lojaId) {
        if (lojaId == null) {
            return stockProdutoLojaRepository.findAll().stream().map(StockItem::global).toList();
        }
        return stockProdutoLojaRepository.findByLojaIdAndAtivoNaLojaTrue(lojaId).stream()
                .map(StockItem::global)
                .toList();
    }

    @Override
    public StockItem obter(UUID produtoId, UUID lojaId) {
        return StockItem.global(obterStockProdutoLoja(produtoId, lojaId));
    }

    @Override
    public StockItem atualizarStock(UUID produtoId, UUID lojaId, int delta) {
        StockProdutoLoja stockProdutoLoja = obterStockProdutoLoja(produtoId, lojaId);
        int novaQuantidade = stockProdutoLoja.getQuantidadeStock() + delta;
        if (novaQuantidade < 0) {
            throw new StockInsuficienteException(produtoId, stockProdutoLoja.getQuantidadeStock(), Math.abs(delta));
        }
        stockProdutoLoja.atualizarStock(delta);
        return StockItem.global(stockProdutoLoja);
    }

    @Override
    public StockItem definirNivelMinimo(UUID produtoId, UUID lojaId, int quantidade) {
        StockProdutoLoja stockProdutoLoja = obterOuCriarStockProdutoLoja(produtoId, lojaId);
        stockProdutoLoja.definirNivelMinimo(quantidade);
        return StockItem.global(stockProdutoLoja);
    }

    @Override
    public StockItem atualizarLocalizacao(UUID produtoId, UUID lojaId, String corredor, String prateleira) {
        StockProdutoLoja stockProdutoLoja = obterOuCriarStockProdutoLoja(produtoId, lojaId);
        stockProdutoLoja.atualizarLocalizacao(corredor, prateleira);
        return StockItem.global(stockProdutoLoja);
    }

    private StockProdutoLoja obterStockProdutoLoja(UUID produtoId, UUID lojaId) {
        if (lojaId == null) {
            throw new BusinessException("LOJA_OBRIGATORIA", "Loja e obrigatoria no modo global");
        }
        return stockProdutoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("StockProdutoLoja", produtoId));
    }

    private StockProdutoLoja obterOuCriarStockProdutoLoja(UUID produtoId, UUID lojaId) {
        if (lojaId == null) {
            throw new BusinessException("LOJA_OBRIGATORIA", "Loja e obrigatoria no modo global");
        }
        return stockProdutoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)
                .orElseGet(() -> {
                    Produto produto = produtoRepository.findById(produtoId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
                    Loja loja = lojaRepository.findById(lojaId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Loja", lojaId));
                    return stockProdutoLojaRepository.save(new StockProdutoLoja(produto, loja, 0, null));
                });
    }
}
