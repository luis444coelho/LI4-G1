package pt.miniFormiga.subsistemas.pdv.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @EntityGraph(attributePaths = {"venda", "venda.loja", "venda.linhas", "venda.linhas.produto", "venda.linhas.produto.taxaIVA"})
    @Query("""
            select f from Fatura f
            where f.lojaId = :lojaId
              and (:cliente is null
                   or lower(coalesce(f.nomeCliente, '')) like lower(concat('%', :cliente, '%'))
                   or lower(coalesce(f.nifCliente, '')) like lower(concat('%', :cliente, '%')))
            order by f.dataEmissao desc
            """)
    Page<Fatura> pesquisarPorLojaECliente(@Param("lojaId") UUID lojaId,
                                          @Param("cliente") String cliente,
                                          Pageable pageable);
}
