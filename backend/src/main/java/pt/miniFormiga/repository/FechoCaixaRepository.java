package pt.miniFormiga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.FechoCaixa;

import java.util.Optional;
import java.util.UUID;

public interface FechoCaixaRepository extends JpaRepository<FechoCaixa, UUID> {
    @EntityGraph(attributePaths = {"loja", "responsavel"})
    Page<FechoCaixa> findByLojaId(UUID lojaId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"loja", "responsavel", "vendas"})
    Optional<FechoCaixa> findById(UUID id);
}
