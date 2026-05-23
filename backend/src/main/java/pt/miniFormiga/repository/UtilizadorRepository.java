package pt.miniFormiga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Utilizador;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtilizadorRepository extends JpaRepository<Utilizador, UUID> {
    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"perfil", "perfil.permissoes", "loja"})
    Optional<Utilizador> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = {"perfil", "perfil.permissoes", "loja"})
    Optional<Utilizador> findById(UUID id);

    @EntityGraph(attributePaths = {"perfil", "perfil.permissoes", "loja"})
    Page<Utilizador> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"perfil", "perfil.permissoes", "loja"})
    Page<Utilizador> findByLojaId(UUID lojaId, Pageable pageable);

    @EntityGraph(attributePaths = {"perfil", "perfil.permissoes", "loja"})
    List<Utilizador> findByAtivoTrueAndPerfilNomeIn(List<String> perfis);

    @EntityGraph(attributePaths = {"perfil", "perfil.permissoes", "loja"})
    List<Utilizador> findByAtivoTrueAndLojaIdAndPerfilNomeIn(UUID lojaId, List<String> perfis);
}
