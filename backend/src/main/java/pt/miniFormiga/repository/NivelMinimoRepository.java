package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.NivelMinimo;

import java.util.Optional;
import java.util.UUID;

public interface NivelMinimoRepository extends JpaRepository<NivelMinimo, UUID> {
    Optional<NivelMinimo> findByStockId(UUID stockId);
}
