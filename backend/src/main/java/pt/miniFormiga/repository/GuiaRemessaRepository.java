package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.GuiaRemessa;

import java.util.Optional;
import java.util.UUID;

public interface GuiaRemessaRepository extends JpaRepository<GuiaRemessa, UUID> {
    @EntityGraph(attributePaths = {"fornecedor", "encomenda"})
    Optional<GuiaRemessa> findByNumero(String numero);
}
