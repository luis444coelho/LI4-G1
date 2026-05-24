package pt.miniFormiga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Venda;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendaRepository extends JpaRepository<Venda, UUID> {
    @Override
    @EntityGraph(attributePaths = {"loja", "utilizador", "linhas", "linhas.produto", "linhas.produto.taxaIVA"})
    Optional<Venda> findById(UUID id);

    @EntityGraph(attributePaths = {"loja", "utilizador", "linhas", "linhas.produto", "linhas.produto.categoria", "linhas.produto.taxaIVA"})
    Page<Venda> findByLojaIdAndDataHoraBetween(UUID lojaId, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);

    @EntityGraph(attributePaths = {"loja", "linhas", "linhas.produto", "linhas.produto.categoria", "linhas.produto.taxaIVA"})
    List<Venda> findByLojaIdAndAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(UUID lojaId, LocalDateTime inicio, LocalDateTime fim);

    @EntityGraph(attributePaths = {"loja", "linhas", "linhas.produto", "linhas.produto.categoria", "linhas.produto.taxaIVA"})
    List<Venda> findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);
}
