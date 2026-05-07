package pt.miniFormiga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.AjusteInventario;

import java.util.UUID;

public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, UUID> {
    @EntityGraph(attributePaths = {"stock", "stock.produto", "stock.loja", "motivoAjuste", "utilizador"})
    Page<AjusteInventario> findByStockLojaId(UUID lojaId, Pageable pageable);
}
