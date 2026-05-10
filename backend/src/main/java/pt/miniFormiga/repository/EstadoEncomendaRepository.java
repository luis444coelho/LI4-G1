package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.EstadoEncomenda;

import java.util.Optional;
import java.util.UUID;

public interface EstadoEncomendaRepository extends JpaRepository<EstadoEncomenda, UUID> {
    Optional<EstadoEncomenda> findByCodigo(String codigo);
}
