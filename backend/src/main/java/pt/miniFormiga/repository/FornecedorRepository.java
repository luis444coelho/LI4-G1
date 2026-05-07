package pt.miniFormiga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.miniFormiga.domain.Fornecedor;

import java.util.UUID;

public interface FornecedorRepository extends JpaRepository<Fornecedor, UUID> {
}
