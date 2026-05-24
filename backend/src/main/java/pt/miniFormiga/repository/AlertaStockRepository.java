package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.AlertaStock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, UUID> {
    @Override
    @EntityGraph(attributePaths = {"produto", "produtoLoja", "produtoLoja.produto", "loja"})
    Optional<AlertaStock> findById(UUID id);

    boolean existsByProdutoIdAndResolvidoFalse(UUID produtoId);

    boolean existsByProdutoLojaIdAndResolvidoFalse(UUID produtoLojaId);

    @EntityGraph(attributePaths = {"produto", "produtoLoja", "produtoLoja.produto", "loja"})
    List<AlertaStock> findByResolvidoFalseOrderByDataHoraDesc();

    @EntityGraph(attributePaths = {"produto", "produtoLoja", "produtoLoja.produto", "loja"})
    List<AlertaStock> findByLojaIdAndResolvidoFalseOrderByDataHoraDesc(UUID lojaId);

}
