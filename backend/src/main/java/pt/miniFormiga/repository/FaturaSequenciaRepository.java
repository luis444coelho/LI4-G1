package pt.miniFormiga.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import pt.miniFormiga.domain.FaturaSequencia;

import java.util.Optional;
import java.util.UUID;

public interface FaturaSequenciaRepository extends JpaRepository<FaturaSequencia, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FaturaSequencia> findBySerie(String serie);
}
