package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Categoria;

import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
    Optional<Categoria> findByNome(String nome);
}
