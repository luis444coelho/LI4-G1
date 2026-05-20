package pt.miniFormiga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Sincronizacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SincronizacaoRepository extends JpaRepository<Sincronizacao, UUID> {
    @EntityGraph(attributePaths = {"loja", "estado"})
    Optional<Sincronizacao> findFirstByLojaIdAndEstadoCodigoOrderByDataHoraInicioDesc(UUID lojaId, String estadoCodigo);

    @EntityGraph(attributePaths = {"loja", "estado"})
    Optional<Sincronizacao> findFirstByLojaIdOrderByDataHoraInicioDesc(UUID lojaId);

    @EntityGraph(attributePaths = {"loja", "estado"})
    Optional<Sincronizacao> findFirstByLojaIdAndEstadoCodigoInOrderByDataHoraFimDesc(UUID lojaId, List<String> estados);

    @EntityGraph(attributePaths = {"loja", "estado"})
    Page<Sincronizacao> findByLojaIdOrderByDataHoraInicioDesc(UUID lojaId, Pageable pageable);

    @EntityGraph(attributePaths = {"loja", "estado"})
    List<Sincronizacao> findByLojaIdAndConflitosResolvidosGreaterThanOrderByDataHoraInicioDesc(UUID lojaId, int conflitosResolvidos);
}
