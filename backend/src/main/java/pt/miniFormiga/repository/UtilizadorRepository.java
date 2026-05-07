package pt.miniFormiga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Utilizador;

import java.util.Optional;
import java.util.UUID;

public interface UtilizadorRepository extends JpaRepository<Utilizador, UUID> {
    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"perfil", "loja"})
    Optional<Utilizador> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = {"perfil", "loja"})
    Optional<Utilizador> findById(UUID id);

    @EntityGraph(attributePaths = {"perfil", "loja"})
    Page<Utilizador> findAll(Pageable pageable);
}
