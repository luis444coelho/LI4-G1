package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.MeioPagamento;

import java.util.Optional;
import java.util.UUID;

public interface MeioPagamentoRepository extends JpaRepository<MeioPagamento, UUID> {
    Optional<MeioPagamento> findByTipo(String tipo);
}
