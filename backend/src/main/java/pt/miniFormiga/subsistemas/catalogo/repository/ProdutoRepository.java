package pt.miniFormiga.subsistemas.catalogo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Produto;

import java.util.Optional;
import java.util.UUID;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {
    @EntityGraph(attributePaths = {"categoria", "taxaIVA", "fornecedorPrincipal"})
    Optional<Produto> findByCodigoBarras(String codigoBarras);

    @EntityGraph(attributePaths = {"categoria", "taxaIVA", "fornecedorPrincipal"})
    Optional<Produto> findByCodigo(String codigo);

    @Override
    @EntityGraph(attributePaths = {"categoria", "taxaIVA", "fornecedorPrincipal"})
    Optional<Produto> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"categoria", "taxaIVA", "fornecedorPrincipal"})
    Page<Produto> findAll(Pageable pageable);
}
