package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxaIVATest {

    @Test
    void devePermitirTaxasValidas() {
        assertDoesNotThrow(() -> new TaxaIVA("Taxa Reduzida", new BigDecimal("6")));
        assertDoesNotThrow(() -> new TaxaIVA("Taxa Intermedia", new BigDecimal("13")));
        assertDoesNotThrow(() -> new TaxaIVA("Taxa Normal", new BigDecimal("23")));
    }

    @Test
    void deveRejeitarTaxaInvalida() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new TaxaIVA("Taxa Invalida", new BigDecimal("7")));

        assertTrue(ex.getMessage().contains("Taxa de IVA invalida"));
    }

    @Test
    void deveAssociarProdutosATaxaIva() {
        TaxaIVA taxaIVA = new TaxaIVA("Taxa Intermedia", new BigDecimal("13"));
        Categoria categoria = new Categoria("Padaria", "Pao e bolos");
        Produto produto = new Produto("5600000000002", "Bolo de arroz", new BigDecimal("1.00"), new BigDecimal("0.55"), taxaIVA, categoria);

        assertEquals("Taxa Intermedia", taxaIVA.getDescricao());
        assertTrue(taxaIVA.getProdutos().contains(produto));
    }
}
