package pt.miniFormiga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.FechoCaixa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FechoCaixaRepository extends JpaRepository<FechoCaixa, UUID> {
    @EntityGraph(attributePaths = {"loja", "responsavel"})
    Page<FechoCaixa> findByLojaId(UUID lojaId, Pageable pageable);

    boolean existsByLojaIdAndData(UUID lojaId, LocalDate data);

    boolean existsByLojaIdAndDataAndConfirmadoTrue(UUID lojaId, LocalDate data);

    @EntityGraph(attributePaths = {"loja", "vendas"})
    List<FechoCaixa> findByLojaIdAndConfirmadoTrueAndDataBetween(UUID lojaId, LocalDate inicio, LocalDate fim);

    @Override
    @EntityGraph(attributePaths = {"loja", "responsavel", "vendas"})
    Optional<FechoCaixa> findById(UUID id);
}
