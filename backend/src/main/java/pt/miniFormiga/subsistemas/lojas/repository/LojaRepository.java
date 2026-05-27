package pt.miniFormiga.subsistemas.lojas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Loja;

import java.util.Optional;
import java.util.UUID;

public interface LojaRepository extends JpaRepository<Loja, UUID> {
    Optional<Loja> findByNif(String nif);
}
