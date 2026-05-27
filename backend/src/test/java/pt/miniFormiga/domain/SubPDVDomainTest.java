package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubPDVDomainTest {

    @Test
    void vendaDeveCalcularTotalComMultiplasTaxasIva() {
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador operador = new Utilizador("op", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, loja);
        Produto agua = produto("Agua", "1.00", "0.40", "6");
        Produto gel = produto("Gel", "2.00", "1.00", "23");
        Venda venda = new Venda(loja, operador);

        new LinhaVenda(venda, agua, 2);
        new LinhaVenda(venda, gel, 1);

        venda.calcularTotais();

        assertEquals(new BigDecimal("4.00"), venda.getTotalSemIVA());
        assertEquals(new BigDecimal("0.58"), venda.getTotalIVA());
        assertEquals(new BigDecimal("4.58"), venda.getTotalComIVA());
    }

    @Test
    void linhaVendaDeveCalcularTotalArmazenado() {
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador operador = new Utilizador("op-linha", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, loja);
        Venda venda = new Venda(loja, operador);
        LinhaVenda linha = new LinhaVenda(venda, produto("Agua", "1.25", "0.40", "23"), 3);

        linha.calcularTotal();

        assertEquals(new BigDecimal("3.75"), linha.getTotalLinha());
    }

    @Test
    void linhaVendaAnuladaDeveZerarTotalLinha() {
        Venda venda = vendaAberta();
        LinhaVenda linha = new LinhaVenda(venda, produto("Agua", "1.25", "0.40", "23"), 3);

        linha.anular();

        assertTrue(linha.isAnulada());
        assertEquals(new BigDecimal("0.00"), linha.getTotalLinha());
    }

    @Test
    void fechoCaixaDeveAgregarPorMeioPagamento() {
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador operador = new Utilizador("op2", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, loja);
        Venda numerario = vendaFinalizada(loja, operador, MeioPagamentoTipo.NUMERARIO, "1.00");
        Venda cartao = vendaFinalizada(loja, operador, MeioPagamentoTipo.CARTAO, "2.00");
        Venda mbway = vendaFinalizada(loja, operador, MeioPagamentoTipo.MBWAY, "3.00");
        FechoCaixa fecho = new FechoCaixa(loja, operador, LocalDate.now(), List.of(numerario, cartao, mbway));

        assertEquals(operador, fecho.getResponsavel());
        assertEquals(3, fecho.getVendas().size());
        assertFalse(fecho.isConfirmado());

        fecho.calcularTotais();

        assertEquals(new BigDecimal("1.23"), fecho.getTotalNumerario());
        assertEquals(new BigDecimal("2.46"), fecho.getTotalCartao());
        assertEquals(new BigDecimal("3.69"), fecho.getTotalMBWay());
        assertEquals(new BigDecimal("7.38"), fecho.getTotalGeral());

        fecho.setObservacaoDiscrepancia("Sem discrepancias");
        assertEquals("Sem discrepancias", fecho.getObservacoesDiscrepancia());
    }

    @Test
    void fechoCaixaDeveIgnorarVendasAnuladasEConfirmar() {
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador operador = new Utilizador("op-fecho", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, loja);
        Venda vendaAtiva = vendaFinalizada(loja, operador, MeioPagamentoTipo.NUMERARIO, "2.00");
        Venda vendaAnulada = vendaFinalizada(loja, operador, MeioPagamentoTipo.CARTAO, "5.00");
        vendaAnulada.anular();
        FechoCaixa fecho = new FechoCaixa(loja, operador, LocalDate.now(), List.of(vendaAtiva, vendaAnulada));

        fecho.confirmar();

        assertTrue(fecho.isConfirmado());
        assertEquals(new BigDecimal("2.46"), fecho.getTotalNumerario());
        assertEquals(new BigDecimal("0"), fecho.getTotalCartao());
        assertEquals(new BigDecimal("2.46"), fecho.getTotalGeral());
    }

    @Test
    void fechoCaixaCalculaTotaisRecebendoListaERecalculaVendasAbertas() {
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador operador = new Utilizador("op-lista", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, loja);
        Venda venda = new Venda(loja, operador, MeioPagamentoTipo.NUMERARIO);
        new LinhaVenda(venda, produto("Agua", "2.00", "0.40", "23"), 1);
        FechoCaixa fecho = new FechoCaixa(loja, operador, LocalDate.now(), List.of());

        fecho.calcularTotais(List.of(venda));

        assertEquals(List.of(venda), fecho.getVendas());
        assertEquals(new BigDecimal("2.46"), fecho.getTotalNumerario());
        assertEquals(new BigDecimal("2.46"), fecho.getTotalGeral());

        fecho.calcularTotais(null);

        assertTrue(fecho.getVendas().isEmpty());
        assertEquals(new BigDecimal("0"), fecho.getTotalGeral());
    }

    @Test
    void faturaDeveDecidirTipoPelaRegraRd02() {
        assertEquals("SIMPLIFICADA", Fatura.decidirTipo(new BigDecimal("1000.00"), null));
        assertEquals("COMPLETA", Fatura.decidirTipo(new BigDecimal("1000.01"), null));
        assertEquals("COMPLETA", Fatura.decidirTipo(new BigDecimal("10.00"), "123456789"));
    }

    @Test
    void faturaDeveEmitirEGerarPdfComTotaisDaVenda() {
        Venda venda = vendaAberta();
        new LinhaVenda(venda, produto("Agua", "2.00", "0.40", "23"), 2);
        Fatura fatura = new Fatura(venda, "00001", "A/2026", "SIMPLIFICADA", null, null);

        fatura.emitir();
        byte[] pdfEmitido = fatura.gerarPDF();

        assertTrue(fatura.isEmitida());
        assertEquals("A/2026/00001", fatura.getNumeroFatura());
        assertEquals(new BigDecimal("4.00"), fatura.getTotalSemIVA());
        assertEquals(new BigDecimal("0.92"), fatura.getTotalIVA());
        assertEquals(new BigDecimal("4.92"), fatura.getTotalComIVA());
        assertArrayEquals(pdfEmitido, fatura.gerarPDF());
    }

    @Test
    void faturaDeveGerarPdfMesmoAntesDeEmitirEGuardarDadosCliente() {
        Venda venda = vendaAberta();
        new LinhaVenda(venda, produto("Servico", "10.00", "4.00", "6"), 1);
        Fatura fatura = new Fatura(venda, "00002", "A/2026", "COMPLETA", "123456789", "Cliente Teste");

        byte[] pdf = fatura.gerarPDF();

        assertTrue(fatura.isEmitida());
        assertTrue(pdf.length > 0);
        assertEquals(venda, fatura.getVenda());
        assertEquals("00002", fatura.getNumero());
        assertEquals("A/2026", fatura.getSerie());
        assertEquals("COMPLETA", fatura.getTipo());
        assertEquals("123456789", fatura.getNifCliente());
        assertEquals("Cliente Teste", fatura.getNomeCliente());
    }

    @Test
    void faturaDeveValidarCamposObrigatorios() {
        Venda venda = vendaAberta();

        assertThrows(NullPointerException.class, () -> new Fatura(null, "00001", "A/2026", "SIMPLIFICADA", null, null));
        assertThrows(NullPointerException.class, () -> new Fatura(venda, null, "A/2026", "SIMPLIFICADA", null, null));
        assertThrows(NullPointerException.class, () -> new Fatura(venda, "00001", null, "SIMPLIFICADA", null, null));
        assertThrows(NullPointerException.class, () -> new Fatura(venda, "00001", "A/2026", null, null, null));
    }

    @Test
    void vendaDeveAnularLinhaRecalcularEImpedirFinalizacaoSemLinhasAtivas() {
        Venda venda = vendaAberta();
        LinhaVenda linha = new LinhaVenda(venda, produto("Agua", "1.00", "0.40", "23"), 1);

        venda.anularLinha(linha.getId());

        assertTrue(linha.isAnulada());
        assertEquals(new BigDecimal("0.00"), venda.getTotalComIVA());
        assertThrows(IllegalStateException.class, () -> venda.finalizar(MeioPagamentoTipo.NUMERARIO));
    }

    @Test
    void vendaAnuladaNaoPodeSerFinalizada() {
        Venda venda = vendaAberta();
        new LinhaVenda(venda, produto("Agua", "1.00", "0.40", "23"), 1);

        venda.anular();

        assertTrue(venda.isAnulada());
        assertThrows(IllegalStateException.class, () -> venda.finalizar(MeioPagamentoTipo.NUMERARIO));
    }

    private Venda vendaFinalizada(Loja loja, Utilizador operador, MeioPagamentoTipo meioPagamento, String preco) {
        Venda venda = new Venda(loja, operador);
        new LinhaVenda(venda, produto("Produto " + preco, preco, "0.50", "23"), 1);
        venda.finalizar(meioPagamento);
        return venda;
    }

    private Venda vendaAberta() {
        Loja loja = new Loja("Loja Braga", "Rua Central", "123456789");
        Utilizador operador = new Utilizador("op-aberta", "hash", "Operador", PerfilUtilizador.FUNCIONARIO, loja);
        return new Venda(loja, operador);
    }

    private Produto produto(String nome, String precoVenda, String precoCusto, String taxa) {
        return new Produto(nome + "-cod", nome, new BigDecimal(precoVenda), new BigDecimal(precoCusto),
                new TaxaIVA("IVA " + taxa, new BigDecimal(taxa)), new Categoria("Cat " + nome, "Categoria"));
    }
}
