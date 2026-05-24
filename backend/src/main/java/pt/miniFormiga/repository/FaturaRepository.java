package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Fatura;

import java.util.Optional;
import java.util.UUID;

public interface FaturaRepository extends JpaRepository<Fatura, UUID> {
    boolean existsByVendaId(UUID vendaId);

    java.util.Optional<Fatura> findFirstByLojaIdAndSerieOrderByNumeroDesc(UUID lojaId, String serie);

    @Override
    @EntityGraph(attributePaths = {"venda", "venda.loja", "venda.linhas", "venda.linhas.produto", "venda.linhas.produto.taxaIVA"})
    Optional<Fatura> findById(UUID id);

    @EntityGraph(attributePaths = {"venda", "venda.loja", "venda.linhas", "venda.linhas.produto", "venda.linhas.produto.taxaIVA"})
    Optional<Fatura> findBySerieAndNumero(String serie, int numero);
}
