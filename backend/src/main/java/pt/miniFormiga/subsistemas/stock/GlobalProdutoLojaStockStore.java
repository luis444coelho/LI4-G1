package pt.miniFormiga.subsistemas.stock;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.ProdutoLoja;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.ProdutoLojaRepository;
import pt.miniFormiga.repository.ProdutoRepository;

import java.util.List;
import java.util.UUID;

@Service
@Profile("global | central")
public class GlobalProdutoLojaStockStore implements StockStore {

    private final ProdutoLojaRepository produtoLojaRepository;
    private final ProdutoRepository produtoRepository;
    private final LojaRepository lojaRepository;

    public GlobalProdutoLojaStockStore(ProdutoLojaRepository produtoLojaRepository,
                                       ProdutoRepository produtoRepository,
                                       LojaRepository lojaRepository) {
        this.produtoLojaRepository = produtoLojaRepository;
        this.produtoRepository = produtoRepository;
        this.lojaRepository = lojaRepository;
    }

    @Override
    public List<StockItem> listar(UUID lojaId) {
        if (lojaId == null) {
            return produtoLojaRepository.findAll().stream().map(StockItem::global).toList();
        }
        return produtoLojaRepository.findByLojaIdAndAtivoNaLojaTrue(lojaId).stream()
                .map(StockItem::global)
                .toList();
    }

    @Override
    public StockItem obter(UUID produtoId, UUID lojaId) {
        return StockItem.global(obterProdutoLoja(produtoId, lojaId));
    }

    @Override
    public StockItem atualizarStock(UUID produtoId, UUID lojaId, int delta) {
        ProdutoLoja produtoLoja = obterProdutoLoja(produtoId, lojaId);
        int novaQuantidade = produtoLoja.getQuantidadeStock() + delta;
        if (novaQuantidade < 0) {
            throw new StockInsuficienteException(produtoId, produtoLoja.getQuantidadeStock(), Math.abs(delta));
        }
        produtoLoja.atualizarStock(delta);
        return StockItem.global(produtoLoja);
    }

    @Override
    public StockItem definirNivelMinimo(UUID produtoId, UUID lojaId, int quantidade) {
        ProdutoLoja produtoLoja = obterOuCriarProdutoLoja(produtoId, lojaId);
        produtoLoja.definirNivelMinimo(quantidade);
        return StockItem.global(produtoLoja);
    }

    @Override
    public StockItem atualizarLocalizacao(UUID produtoId, UUID lojaId, String corredor, String prateleira) {
        ProdutoLoja produtoLoja = obterOuCriarProdutoLoja(produtoId, lojaId);
        produtoLoja.atualizarLocalizacao(corredor, prateleira);
        return StockItem.global(produtoLoja);
    }

    private ProdutoLoja obterProdutoLoja(UUID produtoId, UUID lojaId) {
        if (lojaId == null) {
            throw new BusinessException("LOJA_OBRIGATORIA", "Loja e obrigatoria no modo global");
        }
        return produtoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ProdutoLoja", produtoId));
    }

    private ProdutoLoja obterOuCriarProdutoLoja(UUID produtoId, UUID lojaId) {
        if (lojaId == null) {
            throw new BusinessException("LOJA_OBRIGATORIA", "Loja e obrigatoria no modo global");
        }
        return produtoLojaRepository.findByProdutoIdAndLojaId(produtoId, lojaId)
                .orElseGet(() -> {
                    Produto produto = produtoRepository.findById(produtoId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
                    Loja loja = lojaRepository.findById(lojaId)
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Loja", lojaId));
                    return produtoLojaRepository.save(new ProdutoLoja(produto, loja, 0, null));
                });
    }
}
