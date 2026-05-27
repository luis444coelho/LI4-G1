package pt.miniFormiga.subsistemas.utilizadores.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.Utilizador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtilizadorRepository extends JpaRepository<Utilizador, UUID> {
    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"loja"})
    Optional<Utilizador> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = {"loja"})
    Optional<Utilizador> findById(UUID id);

    @EntityGraph(attributePaths = {"loja"})
    Page<Utilizador> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"loja"})
    Page<Utilizador> findByLojaId(UUID lojaId, Pageable pageable);

    @EntityGraph(attributePaths = {"loja"})
    Page<Utilizador> findByLojaIdAndPerfilIn(UUID lojaId, List<PerfilUtilizador> perfis, Pageable pageable);

    @EntityGraph(attributePaths = {"loja"})
    java.util.List<Utilizador> findByAtivoTrueAndPerfilIn(List<PerfilUtilizador> perfis);

    @EntityGraph(attributePaths = {"loja"})
    java.util.List<Utilizador> findByAtivoTrueAndLojaIdAndPerfilIn(UUID lojaId, List<PerfilUtilizador> perfis);
}
