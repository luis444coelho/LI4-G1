package pt.miniFormiga.subsistemas.pdv;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.Fatura;
import pt.miniFormiga.domain.LinhaVenda;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.MeioPagamentoTipo;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.domain.Venda;
import pt.miniFormiga.repository.CategoriaRepository;
import pt.miniFormiga.repository.FaturaRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.ProdutoRepository;
import pt.miniFormiga.repository.TaxaIVARepository;
import pt.miniFormiga.repository.VendaRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "mini-formiga.demo-data.enabled=false",
        "spring.datasource.url=jdbc:sqlite:file:fatura-persistence-test?mode=memory&cache=shared",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FaturaPersistenceTest {

    @Autowired SubPDVFacade pdv;
    @Autowired LojaRepository lojaRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired TaxaIVARepository taxaIVARepository;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired VendaRepository vendaRepository;
    @Autowired FaturaRepository faturaRepository;

    @Test
    @Transactional
    void emitirFaturaPersisteComIdIgualAoDaVenda() {
        Loja loja = lojaRepository.save(new Loja("Loja Teste", "Rua Teste", "123456789"));
        Utilizador operador = new Utilizador(
                "operador.persistencia",
                "hash",
                "Operador",
                "operador.persistencia@mini-formiga.pt",
                PerfilUtilizador.FUNCIONARIO,
                loja
        );
        lojaRepository.save(loja);
        TaxaIVA taxaIVA = taxaIVARepository.save(new TaxaIVA("NORMAL", new BigDecimal("23")));
        Categoria categoria = categoriaRepository.save(new Categoria("Bebidas Teste", "Bebidas"));
        Produto produto = produtoRepository.save(new Produto(
                "5600000099999",
                "Agua Teste",
                new BigDecimal("1.00"),
                new BigDecimal("0.40"),
                taxaIVA,
                categoria
        ));
        Venda venda = new Venda(loja, operador);
        new LinhaVenda(venda, produto, 1);
        venda.finalizar(MeioPagamentoTipo.NUMERARIO);
        venda = vendaRepository.save(venda);

        Fatura fatura = pdv.emitirFatura(venda.getId(), null, null);

        assertEquals(venda.getId(), fatura.getId());
        assertTrue(faturaRepository.findById(venda.getId()).isPresent());
        assertEquals(venda.getId(), faturaRepository.findById(venda.getId()).orElseThrow().getVenda().getId());
    }
}
