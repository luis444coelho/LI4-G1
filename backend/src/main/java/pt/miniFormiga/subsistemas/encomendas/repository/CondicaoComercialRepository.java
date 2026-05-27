package pt.miniFormiga.subsistemas.encomendas.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.CondicaoComercial;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CondicaoComercialRepository extends JpaRepository<CondicaoComercial, UUID> {
    @EntityGraph(attributePaths = {"fornecedor", "produto"})
    List<CondicaoComercial> findByFornecedorId(UUID fornecedorId);

    @EntityGraph(attributePaths = {"fornecedor", "produto"})
    Optional<CondicaoComercial> findByFornecedorIdAndProdutoId(UUID fornecedorId, UUID produtoId);
}
