package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.EstadoSincronizacao;

import java.util.Optional;
import java.util.UUID;

public interface EstadoSincronizacaoRepository extends JpaRepository<EstadoSincronizacao, UUID> {
    Optional<EstadoSincronizacao> findByCodigo(String codigo);
}
