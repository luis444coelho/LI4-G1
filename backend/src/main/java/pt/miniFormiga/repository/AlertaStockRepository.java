package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.AlertaStock;

import java.util.List;
import java.util.UUID;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, UUID> {
    @EntityGraph(attributePaths = {"stock", "stock.produto", "stock.loja", "stock.nivelMinimo"})
    List<AlertaStock> findByStockLojaIdAndLidoFalseOrderByDataHoraDesc(UUID lojaId);
}
