package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.AlertaStock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, UUID> {
    @Override
    @EntityGraph(attributePaths = {"stock", "stock.produto", "stock.loja", "stock.nivelMinimo", "destinatarios"})
    Optional<AlertaStock> findById(UUID id);

    boolean existsByStockIdAndResolvidoFalse(UUID stockId);

    @EntityGraph(attributePaths = {"stock", "stock.produto", "stock.loja", "stock.nivelMinimo", "destinatarios"})
    List<AlertaStock> findByStockLojaIdAndResolvidoFalseOrderByDataHoraDesc(UUID lojaId);

    @EntityGraph(attributePaths = {"stock", "stock.produto", "stock.produto.categoria", "stock.loja", "stock.nivelMinimo", "destinatarios"})
    List<AlertaStock> findByResolvidoFalseOrderByDataHoraDesc();
}
