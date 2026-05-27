package pt.miniFormiga.subsistemas.stock.service;

import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.EntidadeBase;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.StockProdutoLoja;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockItem(Produto produto,
                        UUID lojaId,
                        String lojaNome,
                        int quantidade,
                        Integer nivelMinimo,
                        String corredor,
                        String prateleira,
                        LocalDateTime atualizadoEm,
                        StockProdutoLoja stockProdutoLoja) {

    public static StockItem local(Produto produto, UUID lojaId, String lojaNome) {
        return new StockItem(
                produto,
                lojaId,
                lojaNome,
                produto.getQuantidadeStock(),
                produto.getNivelMinimo(),
                produto.getCorredor(),
                produto.getPrateleira(),
                produto.getStockUpdatedAt(),
                null
        );
    }

    public static StockItem global(StockProdutoLoja stockProdutoLoja) {
        Loja loja = stockProdutoLoja.getLoja();
        return new StockItem(
                stockProdutoLoja.getProduto(),
                loja.getId(),
                loja.getNome(),
                stockProdutoLoja.getQuantidadeStock(),
                stockProdutoLoja.getNivelMinimo(),
                stockProdutoLoja.getCorredor(),
                stockProdutoLoja.getPrateleira(),
                stockProdutoLoja.getStockUpdatedAt(),
                stockProdutoLoja
        );
    }

    public UUID produtoId() {
        return produto.getId();
    }

    public boolean precisaReposicao() {
        return nivelMinimo != null && quantidade <= nivelMinimo;
    }

    public EntidadeBase entidadeStock() {
        return stockProdutoLoja == null ? produto : stockProdutoLoja;
    }
}
