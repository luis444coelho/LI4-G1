package pt.miniFormiga.subsistemas.stock;

import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.EntidadeBase;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.ProdutoLoja;

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
                        ProdutoLoja produtoLoja) {

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

    public static StockItem global(ProdutoLoja produtoLoja) {
        Loja loja = produtoLoja.getLoja();
        return new StockItem(
                produtoLoja.getProduto(),
                loja.getId(),
                loja.getNome(),
                produtoLoja.getQuantidadeStock(),
                produtoLoja.getNivelMinimo(),
                produtoLoja.getCorredor(),
                produtoLoja.getPrateleira(),
                produtoLoja.getStockUpdatedAt(),
                produtoLoja
        );
    }

    public UUID produtoId() {
        return produto.getId();
    }

    public boolean precisaReposicao() {
        return nivelMinimo != null && quantidade <= nivelMinimo;
    }

    public EntidadeBase entidadeStock() {
        return produtoLoja == null ? produto : produtoLoja;
    }
}
