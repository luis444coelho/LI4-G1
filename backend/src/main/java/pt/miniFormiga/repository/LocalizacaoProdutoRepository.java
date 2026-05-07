package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.LocalizacaoProduto;

import java.util.Optional;
import java.util.UUID;

public interface LocalizacaoProdutoRepository extends JpaRepository<LocalizacaoProduto, UUID> {
    @EntityGraph(attributePaths = {"produto"})
    Optional<LocalizacaoProduto> findByProdutoId(UUID produtoId);
}
