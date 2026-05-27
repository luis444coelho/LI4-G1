package pt.miniFormiga.subsistemas.encomendas.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.EntradaMercadoria;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface EntradaMercadoriaRepository extends JpaRepository<EntradaMercadoria, UUID> {
    @EntityGraph(attributePaths = {"guiaRemessa", "guiaRemessa.encomenda", "loja", "responsavel", "produto"})
    Page<EntradaMercadoria> findByLojaId(UUID lojaId, Pageable pageable);

    @EntityGraph(attributePaths = {"guiaRemessa", "guiaRemessa.encomenda", "loja", "responsavel", "produto"})
    List<EntradaMercadoria> findByGuiaRemessaEncomendaId(UUID encomendaId);

    @Override
    @EntityGraph(attributePaths = {"guiaRemessa", "guiaRemessa.encomenda", "loja", "responsavel", "produto"})
    Optional<EntradaMercadoria> findById(UUID id);
}
