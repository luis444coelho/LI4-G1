package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.MotivoAjuste;

import java.util.Optional;
import java.util.UUID;

public interface MotivoAjusteRepository extends JpaRepository<MotivoAjuste, UUID> {
    Optional<MotivoAjuste> findByCodigo(String codigo);
}
