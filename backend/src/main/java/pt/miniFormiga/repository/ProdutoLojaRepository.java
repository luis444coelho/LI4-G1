package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.ProdutoLoja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProdutoLojaRepository extends JpaRepository<ProdutoLoja, UUID> {

    @EntityGraph(attributePaths = {"produto", "produto.categoria", "produto.taxaIVA", "loja"})
    Optional<ProdutoLoja> findByProdutoIdAndLojaId(UUID produtoId, UUID lojaId);

    @EntityGraph(attributePaths = {"produto", "produto.categoria", "produto.taxaIVA", "loja"})
    List<ProdutoLoja> findByLojaIdAndAtivoNaLojaTrue(UUID lojaId);

    @Override
    @EntityGraph(attributePaths = {"produto", "produto.categoria", "produto.taxaIVA", "loja"})
    List<ProdutoLoja> findAll();
}
