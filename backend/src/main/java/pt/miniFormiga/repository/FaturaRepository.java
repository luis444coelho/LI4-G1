package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Fatura;

import java.util.Optional;
import java.util.UUID;

public interface FaturaRepository extends JpaRepository<Fatura, UUID> {
    boolean existsByVendaId(UUID vendaId);

    @Override
    @EntityGraph(attributePaths = {"venda"})
    Optional<Fatura> findById(UUID id);
}
