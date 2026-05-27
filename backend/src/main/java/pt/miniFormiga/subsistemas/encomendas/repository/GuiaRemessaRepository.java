package pt.miniFormiga.subsistemas.encomendas.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.miniFormiga.domain.GuiaRemessa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuiaRemessaRepository extends JpaRepository<GuiaRemessa, UUID> {
    @EntityGraph(attributePaths = {"encomenda", "encomenda.fornecedor"})
    Optional<GuiaRemessa> findByNumero(String numero);

    @Query("""
            select g.numero
            from GuiaRemessa g
            where g.encomenda.loja.id = :lojaId
              and g.numero like :prefixo
            order by g.numero desc
            """)
    List<String> findNumerosPorLojaEPrefixo(@Param("lojaId") UUID lojaId, @Param("prefixo") String prefixo);
}
