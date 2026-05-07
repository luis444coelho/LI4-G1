package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.LinhaInventario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinhaInventarioRepository extends JpaRepository<LinhaInventario, UUID> {
    @EntityGraph(attributePaths = {"inventario", "produto"})
    Optional<LinhaInventario> findByIdAndInventarioId(UUID id, UUID inventarioId);

    @EntityGraph(attributePaths = {"produto"})
    List<LinhaInventario> findByInventarioIdAndDiscrepanciaNot(UUID inventarioId, int discrepancia);
}
