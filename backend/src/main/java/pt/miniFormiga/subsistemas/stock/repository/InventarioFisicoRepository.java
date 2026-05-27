package pt.miniFormiga.subsistemas.stock.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.InventarioFisico;

import java.util.Optional;
import java.util.UUID;

public interface InventarioFisicoRepository extends JpaRepository<InventarioFisico, UUID> {
    boolean existsByLojaIdAndFechadoFalse(UUID lojaId);

    @EntityGraph(attributePaths = {"loja", "responsavel", "linhas", "linhas.produto"})
    Optional<InventarioFisico> findById(UUID id);

    @EntityGraph(attributePaths = {"loja", "responsavel"})
    Page<InventarioFisico> findByLojaId(UUID lojaId, Pageable pageable);
}
