package pt.miniFormiga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Encomenda;

import java.util.Optional;
import java.util.UUID;

public interface EncomendaRepository extends JpaRepository<Encomenda, UUID> {
    @EntityGraph(attributePaths = {"loja", "fornecedor", "estado", "linhas", "linhas.produto"})
    Page<Encomenda> findByLojaId(UUID lojaId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"loja", "fornecedor", "estado", "linhas", "linhas.produto"})
    Optional<Encomenda> findById(UUID id);
}
