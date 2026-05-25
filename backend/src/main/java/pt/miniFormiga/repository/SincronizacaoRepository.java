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
    @EntityGraph(attributePaths = {"loja"})
    Optional<Sincronizacao> findFirstByLojaIdAndEstadoOrderByDataHoraInicioDesc(UUID lojaId, pt.miniFormiga.domain.EstadoSincronizacaoCodigo estado);

    @EntityGraph(attributePaths = {"loja"})
    Optional<Sincronizacao> findFirstByLojaIdOrderByDataHoraInicioDesc(UUID lojaId);

    @EntityGraph(attributePaths = {"loja"})
    Optional<Sincronizacao> findFirstByLojaIdAndEstadoInOrderByDataHoraFimDesc(UUID lojaId, List<pt.miniFormiga.domain.EstadoSincronizacaoCodigo> estados);

    @EntityGraph(attributePaths = {"loja"})
    Optional<Sincronizacao> findFirstByEstadoInOrderByDataHoraFimDesc(List<pt.miniFormiga.domain.EstadoSincronizacaoCodigo> estados);

    @EntityGraph(attributePaths = {"loja"})
    List<Sincronizacao> findByEstadoInOrderByDataHoraFimDesc(List<pt.miniFormiga.domain.EstadoSincronizacaoCodigo> estados);

    @EntityGraph(attributePaths = {"loja"})
    Page<Sincronizacao> findByLojaIdOrderByDataHoraInicioDesc(UUID lojaId, Pageable pageable);

    @EntityGraph(attributePaths = {"loja"})
    List<Sincronizacao> findByLojaIdAndConflitosResolvidosGreaterThanOrderByDataHoraInicioDesc(UUID lojaId, int conflitosResolvidos);
}
