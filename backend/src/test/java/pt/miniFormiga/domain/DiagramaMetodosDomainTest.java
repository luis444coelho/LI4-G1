package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagramaMetodosDomainTest {

    @Test
    void deveAutenticarEAlterarPassword() {
        Utilizador utilizador = criarUtilizador();

        assertTrue(utilizador.autenticar("hash"));
        assertFalse(utilizador.autenticar("errada"));

        utilizador.alterarPassword("nova");

        assertTrue(utilizador.autenticar("nova"));
        assertFalse(utilizador.autenticar("hash"));
    }

    @Test
    void lojaDeveCriarFechoCaixaESincronizacao() {
        Loja loja = criarLoja();
        LocalDate data = LocalDate.of(2026, 4, 29);

        FechoCaixa fechoCaixa = loja.efetuarFechoCaixa(data);
        Sincronizacao sincronizacao = loja.iniciarSincronizacao();

        assertEquals(loja, fechoCaixa.getLoja());
        assertEquals(data, fechoCaixa.getData());
        assertEquals(loja, sincronizacao.getLoja());
        assertNotNull(sincronizacao.getDataHoraInicio());
    }

    @Test
    void stockDeveAtualizarQuantidadeEValidarNivelMinimo() {
        StockProdutoLoja stock = new StockProdutoLoja(criarProduto(), criarLoja(), 10, 5);

        assertEquals(5, stock.getNivelMinimo());
        assertFalse(stock.precisaReposicao());

        stock.atualizarStock(-6);

        assertEquals(4, stock.getQuantidadeStock());
        assertTrue(stock.precisaReposicao());
        assertThrows(IllegalArgumentException.class, () -> stock.atualizarStock(-5));
    }

    @Test
    void vendaFaturaEFechoCaixaDevemCalcularTotais() {
        Loja loja = criarLoja();
        Utilizador utilizador = criarUtilizador(loja);
        Produto produto = criarProduto();
        MeioPagamentoTipo numerario = MeioPagamentoTipo.NUMERARIO;
        Venda venda = new Venda(loja, utilizador);
        LinhaVenda linhaVenda = new LinhaVenda(venda, produto, 2);
        venda.finalizar(numerario);

        venda.calcularTotais();

        assertEquals(new BigDecimal("2.40"), linhaVenda.getTotalLinha());
        assertEquals(new BigDecimal("2.40"), venda.getTotalSemIVA());
        assertEquals(new BigDecimal("0.55"), venda.getTotalIVA());
        assertEquals(new BigDecimal("2.95"), venda.getTotalComIVA());

        venda.finalizar();
        Fatura fatura = new Fatura(venda, "0001", "A/2026", "SIMPLIFICADA", null, null);
        fatura.emitir();

        assertTrue(fatura.isEmitida());
        assertEquals(venda.getTotalComIVA(), fatura.getTotalComIVA());
        assertTrue(new String(fatura.gerarPDF(), StandardCharsets.UTF_8).contains("Fatura A/2026/1"));

        FechoCaixa fechoCaixa = new FechoCaixa(loja, utilizador, LocalDate.of(2026, 4, 29), List.of(venda));
        fechoCaixa.confirmar();

        assertTrue(fechoCaixa.isConfirmado());
        assertEquals(new BigDecimal("2.95"), fechoCaixa.getTotalNumerario());
        assertEquals(new BigDecimal("2.95"), fechoCaixa.getTotalGeral());

        fechoCaixa.setObservacaoDiscrepancia("Sem discrepancias");
        fechoCaixa.calcularTotais(List.of(venda));

        assertEquals("Sem discrepancias", fechoCaixa.getObservacoesDiscrepancia());

        venda.anular();
        assertTrue(venda.isAnulada());
    }

    @Test
    void fornecedorDeveCalcularDisponibilidadeEProcessamento() {
        Fornecedor fornecedor = criarFornecedor();
        LocalDateTime segundaAsDez = LocalDateTime.of(2026, 4, 27, 10, 0);
        LocalDateTime segundaAntesAbertura = LocalDateTime.of(2026, 4, 27, 7, 30);
        LocalDateTime sextaDepoisFecho = LocalDateTime.of(2026, 5, 1, 19, 0);

        assertTrue(fornecedor.estaDisponivel(segundaAsDez));
        assertFalse(fornecedor.estaDisponivel(LocalDateTime.of(2026, 5, 2, 10, 0)));
        assertEquals(LocalDateTime.of(2026, 4, 27, 8, 0),
                fornecedor.calcularDataProcessamento(segundaAntesAbertura));
        assertEquals(LocalDateTime.of(2026, 5, 4, 8, 0),
                fornecedor.calcularDataProcessamento(sextaDepoisFecho));
    }

    @Test
    void encomendaELinhaDevemCalcularTotalESubmeter() {
        Encomenda encomenda = new Encomenda(criarLoja(), criarFornecedor());
        LinhaEncomenda linha = new LinhaEncomenda(encomenda, criarProduto(), 3, new BigDecimal("0.50"));

        assertEquals(new BigDecimal("1.50"), linha.calcularTotal());
        assertEquals(new BigDecimal("1.50"), encomenda.calcularTotal());

        encomenda.submeter();

        assertEquals(new BigDecimal("1.50"), encomenda.getTotalEstimado());
        assertNotNull(encomenda.getDataProcessamento());
    }

    @Test
    void entradaMercadoriaEInventarioDevemCalcularDiscrepancias() {
        Loja loja = criarLoja();
        Utilizador utilizador = criarUtilizador(loja);
        Produto produto = criarProduto();
        GuiaRemessa guia = new GuiaRemessa(criarFornecedor(), "GR-1", LocalDate.of(2026, 4, 29), null);
        EntradaMercadoria entrada = new EntradaMercadoria(guia, loja, utilizador, 8, 10, "faltam unidades");

        assertEquals(-2, entrada.getDiscrepancia());
        assertNotNull(entrada.getDataHora());

        InventarioFisico inventario = new InventarioFisico(loja, utilizador);
        LinhaInventario linhaComFalha = new LinhaInventario(inventario, produto, 8, 10);
        new LinhaInventario(inventario, produto, 7, 7);

        linhaComFalha.calcularDiscrepancia();
        inventario.calcularDiscrepancias();

        assertEquals(-2, linhaComFalha.getDiscrepancia());
        assertEquals(1, inventario.getTotalDiscrepancias());

        inventario.fechar();
        assertTrue(inventario.isFechado());
        assertNotNull(inventario.getDataFecho());
    }

    @Test
    void deveCriarEntidadesDeApoioDoDiagrama() {
        Produto produto = criarProduto();
        Fornecedor fornecedor = criarFornecedor();
        StockProdutoLoja stock = new StockProdutoLoja(produto, criarLoja(), 6, 7);
        Utilizador utilizador = criarUtilizador();

        AjusteInventario ajuste = new AjusteInventario(stock, MotivoAjusteCodigo.QUEBRA, utilizador, -1, "produto danificado");
        AlertaStock alerta = new AlertaStock(stock, stock.getQuantidadeStock());
        CondicaoComercial condicao = new CondicaoComercial(
                fornecedor,
                produto,
                new BigDecimal("0.60"),
                2,
                10,
                LocalDate.of(2026, 4, 29)
        );
        EstadoEncomendaCodigo estadoEncomenda = EstadoEncomendaCodigo.PENDENTE;
        MeioPagamentoTipo cartao = MeioPagamentoTipo.CARTAO;

        assertEquals(MotivoAjusteCodigo.QUEBRA, ajuste.getMotivo());
        assertEquals(-1, ajuste.getQuantidade());
        assertEquals(stock.getQuantidadeStock(), alerta.getQuantidadeNoMomento());
        assertFalse(alerta.isLido());
        alerta.marcarComoLido();
        assertTrue(alerta.isLido());
        assertEquals(new BigDecimal("0.60"), condicao.getPrecoUnitario());
        assertEquals("PENDENTE", estadoEncomenda.getCodigo());
        assertEquals("CONCLUIDA", EstadoSincronizacaoCodigo.CONCLUIDA.getCodigo());
        assertEquals("CARTAO", cartao.name());
    }

    private Loja criarLoja() {
        return new Loja("Loja Braga", "Rua Central", "123456789", "253000000");
    }

    private Utilizador criarUtilizador() {
        return criarUtilizador(criarLoja());
    }

    private Utilizador criarUtilizador(Loja loja) {
        return new Utilizador("operador", "hash", "Operador", "operador@mini.pt", PerfilUtilizador.FUNCIONARIO, loja);
    }

    private Produto criarProduto() {
        Categoria categoria = new Categoria("Bebidas", "Bebidas frescas");
        TaxaIVA taxaIVA = new TaxaIVA("Taxa Normal", new BigDecimal("23"));
        return new Produto("5601234567890", "Agua 1.5L", new BigDecimal("1.20"), new BigDecimal("0.40"), taxaIVA, categoria);
    }

    private Fornecedor criarFornecedor() {
        return new Fornecedor(
                "Fornecedor Norte",
                "987654321",
                "Rua do Armazem",
                "229000000",
                "fornecedor@mini.pt",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );
    }
}
