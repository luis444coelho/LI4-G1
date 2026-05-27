package pt.miniFormiga.subsistemas.stock.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.miniFormiga.domain.AlertaStock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, UUID> {
    @Override
    @EntityGraph(attributePaths = {"produto", "stockProdutoLoja", "stockProdutoLoja.produto", "loja"})
    Optional<AlertaStock> findById(UUID id);

    boolean existsByProdutoIdAndResolvidoFalse(UUID produtoId);

    boolean existsByProdutoIdAndResolvidoTrue(UUID produtoId);

    boolean existsByStockProdutoLojaIdAndResolvidoFalse(UUID stockProdutoLojaId);

    boolean existsByStockProdutoLojaIdAndResolvidoTrue(UUID stockProdutoLojaId);

    @EntityGraph(attributePaths = {"produto", "stockProdutoLoja", "stockProdutoLoja.produto", "loja"})
    List<AlertaStock> findByResolvidoFalseOrderByDataHoraDesc();

    @EntityGraph(attributePaths = {"produto", "stockProdutoLoja", "stockProdutoLoja.produto", "loja"})
    @Query("""
            select alerta
            from AlertaStock alerta
            left join alerta.stockProdutoLoja stockProdutoLoja
            left join alerta.loja loja
            where alerta.resolvido = false
              and (
                loja.id = :lojaId
                or stockProdutoLoja.loja.id = :lojaId
                or exists (
                    select 1 from StockProdutoLoja produtoDaLoja
                    where produtoDaLoja.produto = alerta.produto
                      and produtoDaLoja.loja.id = :lojaId
                )
              )
            order by alerta.dataHora desc
            """)
    List<AlertaStock> findAtivosByLojaId(UUID lojaId);

}
