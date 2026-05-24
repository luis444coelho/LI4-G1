package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.miniFormiga.domain.AlertaStock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, UUID> {
    @Override
    @EntityGraph(attributePaths = {"produto", "produtoLoja", "produtoLoja.produto", "loja"})
    Optional<AlertaStock> findById(UUID id);

    boolean existsByProdutoIdAndResolvidoFalse(UUID produtoId);

    boolean existsByProdutoIdAndResolvidoTrue(UUID produtoId);

    boolean existsByProdutoLojaIdAndResolvidoFalse(UUID produtoLojaId);

    boolean existsByProdutoLojaIdAndResolvidoTrue(UUID produtoLojaId);

    @EntityGraph(attributePaths = {"produto", "produtoLoja", "produtoLoja.produto", "loja"})
    List<AlertaStock> findByResolvidoFalseOrderByDataHoraDesc();

    @EntityGraph(attributePaths = {"produto", "produtoLoja", "produtoLoja.produto", "loja"})
    @Query("""
            select alerta
            from AlertaStock alerta
            left join alerta.produtoLoja produtoLoja
            left join alerta.loja loja
            where alerta.resolvido = false
              and (
                loja.id = :lojaId
                or produtoLoja.loja.id = :lojaId
                or exists (
                    select 1 from ProdutoLoja produtoDaLoja
                    where produtoDaLoja.produto = alerta.produto
                      and produtoDaLoja.loja.id = :lojaId
                )
              )
            order by alerta.dataHora desc
            """)
    List<AlertaStock> findAtivosByLojaId(UUID lojaId);

}
