package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Devolucao;

import java.util.UUID;

public interface DevolucaoRepository extends JpaRepository<Devolucao, UUID> {
}
