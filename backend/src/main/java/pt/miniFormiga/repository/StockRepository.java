package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import pt.miniFormiga.domain.Stock;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {
    @Override
    @EntityGraph(attributePaths = {"produto", "produto.categoria", "loja", "nivelMinimo"})
    List<Stock> findAll();

    @EntityGraph(attributePaths = {"produto", "produto.categoria", "loja", "nivelMinimo"})
    Optional<Stock> findByProdutoIdAndLojaId(UUID produtoId, UUID lojaId);

    @EntityGraph(attributePaths = {"produto", "produto.categoria", "loja", "nivelMinimo"})
    List<Stock> findByLojaId(UUID lojaId);
}
