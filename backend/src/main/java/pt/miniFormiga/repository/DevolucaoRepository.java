package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import pt.miniFormiga.domain.Devolucao;

import java.util.List;
import java.util.UUID;

public interface DevolucaoRepository extends JpaRepository<Devolucao, UUID> {
    int countByVendaLojaIdAndNumeroDocumentoStartingWith(UUID lojaId, String prefixo);

    @EntityGraph(attributePaths = {"venda", "produto"})
    List<Devolucao> findByVendaLojaIdOrderByDataHoraDesc(UUID lojaId);
}
