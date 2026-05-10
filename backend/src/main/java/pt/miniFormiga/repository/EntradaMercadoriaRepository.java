package pt.miniFormiga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.EntradaMercadoria;

import java.util.Optional;
import java.util.UUID;

public interface EntradaMercadoriaRepository extends JpaRepository<EntradaMercadoria, UUID> {
    @EntityGraph(attributePaths = {"guiaRemessa", "guiaRemessa.encomenda", "loja", "responsavel"})
    Page<EntradaMercadoria> findByLojaId(UUID lojaId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"guiaRemessa", "guiaRemessa.encomenda", "loja", "responsavel"})
    Optional<EntradaMercadoria> findById(UUID id);
}
