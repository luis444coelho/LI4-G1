package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.StockProdutoLoja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockProdutoLojaRepository extends JpaRepository<StockProdutoLoja, UUID> {

    @EntityGraph(attributePaths = {"produto", "produto.categoria", "produto.taxaIVA", "loja"})
    Optional<StockProdutoLoja> findByProdutoIdAndLojaId(UUID produtoId, UUID lojaId);

    @EntityGraph(attributePaths = {"produto", "produto.categoria", "produto.taxaIVA", "loja"})
    List<StockProdutoLoja> findByLojaIdAndAtivoNaLojaTrue(UUID lojaId);

    @Override
    @EntityGraph(attributePaths = {"produto", "produto.categoria", "produto.taxaIVA", "loja"})
    List<StockProdutoLoja> findAll();
}
