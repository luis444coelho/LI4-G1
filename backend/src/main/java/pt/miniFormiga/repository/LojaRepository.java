package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Loja;

import java.util.UUID;

public interface LojaRepository extends JpaRepository<Loja, UUID> {
}
