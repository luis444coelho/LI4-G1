package pt.miniFormiga.subsistemas.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.TaxaIVA;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TaxaIVARepository extends JpaRepository<TaxaIVA, UUID> {
    Optional<TaxaIVA> findByPercentagem(BigDecimal percentagem);
}
