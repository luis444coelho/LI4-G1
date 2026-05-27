package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubEncomendasDomainTest {

    @Test
    void fornecedorCalculaProcessamentoSegundoHorarioDoRelatorio() {
        Fornecedor fornecedor = fornecedor();

        LocalDateTime dentroHorario = LocalDateTime.of(2026, 5, 11, 10, 0);
        LocalDateTime depoisHorario = LocalDateTime.of(2026, 5, 11, 19, 0);
        LocalDateTime sabado = LocalDateTime.of(2026, 5, 9, 12, 0);

        assertEquals(dentroHorario, fornecedor.calcularDataProcessamento(dentroHorario));
        assertEquals(LocalDateTime.of(2026, 5, 12, 8, 0), fornecedor.calcularDataProcessamento(depoisHorario));
        assertEquals(LocalDateTime.of(2026, 5, 11, 8, 0), fornecedor.calcularDataProcessamento(sabado));
    }

    @Test
    void fornecedorPermiteAtualizarEDesativar() {
        Fornecedor fornecedor = fornecedor();

        fornecedor.atualizarDados("Fornecedor Sul", "123456789", "Rua Sul", "210000000",
                "sul@mini-formiga.pt", LocalTime.of(8, 30), LocalTime.of(17, 30));

        assertTrue(fornecedor.isAtivo());
        assertEquals("Rua Sul", fornecedor.getMorada());
        assertEquals("210000000", fornecedor.getTelefone());
        assertEquals("sul@mini-formiga.pt", fornecedor.getEmail());
        assertEquals(LocalTime.of(8, 30), fornecedor.getHorarioInicioArmazem());
        assertEquals(LocalTime.of(17, 30), fornecedor.getHorarioFimArmazem());
        assertFalse(fornecedor.estaDisponivel(null));

        fornecedor.desativar();

        assertEquals("Fornecedor Sul", fornecedor.getNome());
        assertEquals("123456789", fornecedor.getNif());
        assertFalse(fornecedor.isAtivo());
    }

    @Test
    void encomendaMantemEstadoELinhasComTotalEstimado() {
        Encomenda encomenda = new Encomenda(loja(), fornecedor(), EstadoEncomendaCodigo.PENDENTE);
        Produto produto = produto();

        new LinhaEncomenda(encomenda, produto, 3, new BigDecimal("0.60"));
        encomenda.alterarEstado(EstadoEncomendaCodigo.ENVIADA);

        assertEquals("ENVIADA", encomenda.getEstado().getCodigo());
        assertEquals(new BigDecimal("1.80"), encomenda.calcularTotal());
        assertEquals(1, encomenda.getLinhas().size());
    }

    @Test
    void guiaRemessaFicaAssociadaAEncomendaEEntradaCalculaDiscrepancia() {
        Loja loja = loja();
        Encomenda encomenda = new Encomenda(loja, fornecedor(), EstadoEncomendaCodigo.PENDENTE);
        LinhaEncomenda linhaEncomenda = new LinhaEncomenda(encomenda, produto(), 10, new BigDecimal("0.60"));
        GuiaRemessa guia = new GuiaRemessa(encomenda, "GR-1", LocalDate.now(), LocalDate.now());
        Utilizador responsavel = new Utilizador("armazem", "hash", "Armazem",
                PerfilUtilizador.ARMAZEM, loja);

        EntradaMercadoria entrada = new EntradaMercadoria(guia, loja, responsavel, linhaEncomenda, 8, 10, "Entrega parcial");

        assertEquals(encomenda, guia.getEncomenda());
        assertEquals(encomenda.getFornecedor(), guia.getFornecedor());
        assertEquals(linhaEncomenda.getProduto(), entrada.getProduto());
        assertEquals(-2, entrada.getDiscrepancia());
        assertEquals("Entrega parcial", entrada.getObservacoes());
    }

    @Test
    void condicaoComercialValidaValores() {
        CondicaoComercial condicao = new CondicaoComercial(fornecedor(), produto(), new BigDecimal("0.60"), 2, 10, LocalDate.now());

        condicao.atualizar(new BigDecimal("0.55"), 3, 12, LocalDate.of(2026, 5, 10));

        assertEquals(new BigDecimal("0.55"), condicao.getPrecoUnitario());
        assertEquals(3, condicao.getPrazoEntregaDias());
        assertEquals(12, condicao.getQuantidadeMinima());
        assertThrows(IllegalArgumentException.class,
                () -> condicao.atualizar(new BigDecimal("-0.01"), 1, 1, LocalDate.now()));
    }

    private Fornecedor fornecedor() {
        return new Fornecedor("Fornecedor Norte", "987654321", "Rua Norte", "229000000",
                "norte@mini-formiga.pt", LocalTime.of(8, 0), LocalTime.of(18, 0));
    }

    private Produto produto() {
        return new Produto("5600000000110", "Agua 0.5L", new BigDecimal("1.00"), new BigDecimal("0.40"),
                new TaxaIVA("Normal", new BigDecimal("23")), new Categoria("Bebidas", "Bebidas"));
    }

    private Loja loja() {
        return new Loja("Loja Braga", "Rua Central", "123456789");
    }
}
