package pt.miniFormiga.facade;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.CondicaoComercial;
import pt.miniFormiga.domain.Encomenda;
import pt.miniFormiga.domain.EntidadeBase;
import pt.miniFormiga.domain.EntradaMercadoria;
import pt.miniFormiga.domain.Fatura;
import pt.miniFormiga.domain.FechoCaixa;
import pt.miniFormiga.domain.Fornecedor;
import pt.miniFormiga.domain.GuiaRemessa;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaEncomenda;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.domain.LinhaVenda;
import pt.miniFormiga.domain.LocalizacaoProduto;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.MeioPagamento;
import pt.miniFormiga.domain.MotivoAjuste;
import pt.miniFormiga.domain.NivelMinimo;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.Sincronizacao;
import pt.miniFormiga.domain.Stock;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.domain.Venda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MiniFormigaFacade {

    private final Map<UUID, Loja> lojas = new LinkedHashMap<>();
    private final Map<UUID, Perfil> perfis = new LinkedHashMap<>();
    private final Map<UUID, Utilizador> utilizadores = new LinkedHashMap<>();
    private final Map<UUID, Categoria> categorias = new LinkedHashMap<>();
    private final Map<UUID, TaxaIVA> taxasIva = new LinkedHashMap<>();
    private final Map<UUID, Produto> produtos = new LinkedHashMap<>();
    private final Map<UUID, Fornecedor> fornecedores = new LinkedHashMap<>();
    private final Map<UUID, Stock> stocks = new LinkedHashMap<>();
    private final Map<UUID, MotivoAjuste> motivosAjuste = new LinkedHashMap<>();
    private final Map<UUID, Encomenda> encomendas = new LinkedHashMap<>();
    private final Map<UUID, Venda> vendas = new LinkedHashMap<>();
    private final Map<UUID, Fatura> faturas = new LinkedHashMap<>();
    private final Map<UUID, FechoCaixa> fechosCaixa = new LinkedHashMap<>();
    private final Map<UUID, Sincronizacao> sincronizacoes = new LinkedHashMap<>();
    private final Map<UUID, InventarioFisico> inventarios = new LinkedHashMap<>();
    private final Map<UUID, AjusteInventario> ajustes = new LinkedHashMap<>();
    private final Map<UUID, EntradaMercadoria> entradasMercadoria = new LinkedHashMap<>();
    private final Map<UUID, NivelMinimo> niveisMinimos = new LinkedHashMap<>();
    private final Map<UUID, AlertaStock> alertasStock = new LinkedHashMap<>();
    private final Map<UUID, GuiaRemessa> guiasRemessa = new LinkedHashMap<>();
    private final Map<UUID, CondicaoComercial> condicoesComerciais = new LinkedHashMap<>();
    private final Map<UUID, LocalizacaoProduto> localizacoesProduto = new LinkedHashMap<>();

    @PostConstruct
    public void inicializarDadosDemo() {
        if (!lojas.isEmpty()) {
            return;
        }

        Loja loja = registarLoja(new Loja("Loja Braga", "Rua Central", "123456789", "253000000"));
        Perfil gestor = registarPerfil(new Perfil("GESTOR", List.of("DASHBOARD_READ", "RELATORIOS_READ", "USERS_WRITE")));
        Perfil gerente = registarPerfil(new Perfil("GERENTE", List.of("PDV_WRITE", "STOCK_WRITE", "UTILIZADOR_WRITE")));
        Perfil funcionario = registarPerfil(new Perfil("FUNCIONARIO", List.of("PDV_WRITE")));
        Perfil armazem = registarPerfil(new Perfil("RESPONSAVEL_ARMAZEM", List.of("STOCK_READ", "ENCOMENDAS_WRITE")));

        registarUtilizador(new Utilizador("gestor.formiga", "hash-gestor", "Sr. Formiga", "gestor@mini.pt", gestor, loja));
        Utilizador gerenteLoja = registarUtilizador(new Utilizador("gerente.braga", "hash-gerente", "Gerente Braga", "gerente@mini.pt", gerente, loja));
        registarUtilizador(new Utilizador("operador.braga", "hash-operador", "Operador Braga", "operador@mini.pt", funcionario, loja));
        Utilizador responsavelArmazem = registarUtilizador(new Utilizador("armazem.braga", "hash-armazem", "Responsavel Armazem", "armazem@mini.pt", armazem, loja));

        Categoria bebidas = registarCategoria(new Categoria("Bebidas", "Bebidas frescas"));
        TaxaIVA taxaNormal = registarTaxaIVA(new TaxaIVA("Taxa Normal", new BigDecimal("23")));
        Produto agua = registarProduto(new Produto("5601234567890", "Agua 1.5L", new BigDecimal("1.20"), new BigDecimal("0.40"), taxaNormal, bebidas));

        Fornecedor fornecedor = registarFornecedor(new Fornecedor(
                "Fornecedor Norte",
                "987654321",
                "Rua do Armazem",
                "229000000",
                "fornecedor@mini.pt",
                LocalDateTime.now().toLocalTime().withHour(8).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().toLocalTime().withHour(18).withMinute(0).withSecond(0).withNano(0)
        ));

        Stock stock = registarStock(new Stock(agua, loja, 10));
        registarNivelMinimo(new NivelMinimo(stock, 5));

        registarMotivoAjuste(new MotivoAjuste("QUEBRA", "Quebra"));
        registarMotivoAjuste(new MotivoAjuste("DESPERDICIO", "Desperdicio"));

        registarLocalizacaoProduto(new LocalizacaoProduto(agua, "A", "3", "Perto da caixa"));
        registarCondicaoComercial(new CondicaoComercial(fornecedor, agua, new BigDecimal("0.60"), 2, 10, LocalDate.now()));

        InventarioFisico inventario = registarInventario(new InventarioFisico(loja, responsavelArmazem));
        registarLinhaInventario(inventario.getId(), agua.getId(), 8, 10);
    }

    public List<Loja> listarLojas() {
        return List.copyOf(lojas.values());
    }

    public List<Produto> listarProdutos() {
        return List.copyOf(produtos.values());
    }

    public List<Fornecedor> listarFornecedores() {
        return List.copyOf(fornecedores.values());
    }

    public List<Stock> listarStocksPorLoja(UUID lojaId) {
        return stocks.values().stream()
                .filter(stock -> stock.getLoja().getId().equals(lojaId))
                .toList();
    }

    public Utilizador criarUtilizador(String username, String passwordHash, String nome, String email, UUID perfilId, UUID lojaId) {
        Perfil perfil = require(perfis, perfilId, "Perfil");
        Loja loja = require(lojas, lojaId, "Loja");
        return registarUtilizador(new Utilizador(username, passwordHash, nome, email, perfil, loja));
    }

    public Venda registarVenda(UUID lojaId, UUID utilizadorId, String meioPagamentoTipo, String meioPagamentoDescricao, List<LinhaVendaRequest> linhasRequest) {
        Loja loja = require(lojas, lojaId, "Loja");
        Utilizador utilizador = require(utilizadores, utilizadorId, "Utilizador");
        MeioPagamento meioPagamento = new MeioPagamento(meioPagamentoTipo, meioPagamentoDescricao);
        Venda venda = new Venda(loja, utilizador, meioPagamento);

        List<LinhaVendaRequest> linhas = linhasRequest == null ? List.of() : linhasRequest;
        for (LinhaVendaRequest linhaRequest : linhas) {
            Produto produto = require(produtos, linhaRequest.produtoId(), "Produto");
            new LinhaVenda(venda, produto, linhaRequest.quantidade(), linhaRequest.precoUnitario());
            atualizarStockDaVenda(lojaId, produto.getId(), linhaRequest.quantidade());
        }

        venda.calcularTotais();
        return registarVenda(venda);
    }

    public Fatura emitirFatura(UUID vendaId, String numero, String serie, String tipo, String nifCliente, String nomeCliente) {
        Venda venda = require(vendas, vendaId, "Venda");
        Fatura fatura = new Fatura(venda, numero, serie, tipo, nifCliente, nomeCliente);
        fatura.emitir();
        return registarFatura(fatura);
    }

    public FechoCaixa registarFechoCaixa(UUID lojaId, UUID responsavelId, LocalDate data) {
        Loja loja = require(lojas, lojaId, "Loja");
        Utilizador responsavel = require(utilizadores, responsavelId, "Utilizador");
        List<Venda> vendasDaData = vendas.values().stream()
                .filter(venda -> venda.getLoja().getId().equals(lojaId))
                .filter(venda -> venda.getDataHora().toLocalDate().equals(data))
                .filter(venda -> !venda.isAnulada())
                .toList();
        FechoCaixa fechoCaixa = new FechoCaixa(loja, responsavel, data, vendasDaData);
        fechoCaixa.confirmar();
        return registarFechoCaixa(fechoCaixa);
    }

    public Encomenda criarEncomenda(UUID lojaId, UUID fornecedorId, List<LinhaEncomendaRequest> linhasRequest) {
        Loja loja = require(lojas, lojaId, "Loja");
        Fornecedor fornecedor = require(fornecedores, fornecedorId, "Fornecedor");
        Encomenda encomenda = new Encomenda(loja, fornecedor);

        List<LinhaEncomendaRequest> linhas = linhasRequest == null ? List.of() : linhasRequest;
        for (LinhaEncomendaRequest linhaRequest : linhas) {
            Produto produto = require(produtos, linhaRequest.produtoId(), "Produto");
            new LinhaEncomenda(encomenda, produto, linhaRequest.quantidade(), linhaRequest.precoUnitario());
        }

        encomenda.submeter();
        return registarEncomenda(encomenda);
    }

    public Sincronizacao iniciarSincronizacao(UUID lojaId) {
        Sincronizacao sincronizacao = new Sincronizacao(require(lojas, lojaId, "Loja"));
        return registarSincronizacao(sincronizacao);
    }

    public NivelMinimo definirNivelMinimo(UUID stockId, int quantidade) {
        Stock stock = require(stocks, stockId, "Stock");
        NivelMinimo nivelMinimo = new NivelMinimo(stock, quantidade);
        return registarNivelMinimo(nivelMinimo);
    }

    public EntradaMercadoria registarEntrada(UUID fornecedorId,
                                             UUID lojaId,
                                             UUID responsavelId,
                                             UUID produtoId,
                                             String guiaNumero,
                                             LocalDate dataEmissao,
                                             LocalDate dataRecepcao,
                                             int quantidadeRecebida,
                                             int quantidadeEncomendada,
                                             String observacoes) {
        Fornecedor fornecedor = require(fornecedores, fornecedorId, "Fornecedor");
        Loja loja = require(lojas, lojaId, "Loja");
        Utilizador responsavel = require(utilizadores, responsavelId, "Utilizador");
        Produto produto = require(produtos, produtoId, "Produto");
        GuiaRemessa guiaRemessa = registarGuiaRemessa(new GuiaRemessa(fornecedor, guiaNumero, dataEmissao, dataRecepcao));
        EntradaMercadoria entradaMercadoria = new EntradaMercadoria(guiaRemessa, loja, responsavel, quantidadeRecebida, quantidadeEncomendada, observacoes);
        atualizarStockDaEntrada(lojaId, produtoId, quantidadeRecebida);
        return registarEntradaMercadoria(entradaMercadoria);
    }

    public AjusteInventario registarAjuste(UUID stockId, UUID motivoId, UUID responsavelId, int quantidade, String observacoes) {
        Stock stock = require(stocks, stockId, "Stock");
        MotivoAjuste motivo = require(motivosAjuste, motivoId, "MotivoAjuste");
        Utilizador responsavel = require(utilizadores, responsavelId, "Utilizador");
        if (stock.getQuantidade() + quantidade < 0) {
            throw new IllegalArgumentException("Quantidade de stock nao pode ser negativa");
        }
        stock.atualizarQuantidade(quantidade);
        AjusteInventario ajuste = new AjusteInventario(stock, motivo, responsavel, quantidade, observacoes);
        return registarAjusteInventario(ajuste);
    }

    public InventarioFisico criarInventario(UUID lojaId, UUID responsavelId) {
        InventarioFisico inventarioFisico = new InventarioFisico(require(lojas, lojaId, "Loja"), require(utilizadores, responsavelId, "Utilizador"));
        return registarInventario(inventarioFisico);
    }

    public LinhaInventario calcularDiscrepancia(UUID inventarioId, UUID produtoId, int quantidadeContada, int quantidadeSistema) {
        InventarioFisico inventarioFisico = require(inventarios, inventarioId, "InventarioFisico");
        Produto produto = require(produtos, produtoId, "Produto");
        LinhaInventario linhaInventario = new LinhaInventario(inventarioFisico, produto, quantidadeContada, quantidadeSistema);
        inventarioFisico.calcularDiscrepancias();
        return linhaInventario;
    }

    public Fornecedor gerirFornecedor(String nome,
                                      String nif,
                                      String morada,
                                      String telefone,
                                      String email,
                                      LocalDateTime horarioInicio,
                                      LocalDateTime horarioFim) {
        Fornecedor fornecedor = new Fornecedor(nome, nif, morada, telefone, email, horarioInicio.toLocalTime(), horarioFim.toLocalTime());
        return registarFornecedor(fornecedor);
    }

    public List<EntidadeResumo> resumoLojas() {
        return lojas.values().stream().map(loja -> new EntidadeResumo(loja.getId(), loja.getNome())).toList();
    }

    public List<EntidadeResumo> resumoProdutos() {
        return produtos.values().stream().map(produto -> new EntidadeResumo(produto.getId(), produto.getNome())).toList();
    }

    public List<EntidadeResumo> resumoFornecedores() {
        return fornecedores.values().stream().map(fornecedor -> new EntidadeResumo(fornecedor.getId(), fornecedor.getNome())).toList();
    }

    public List<EntidadeResumo> resumoUtilizadores() {
        return utilizadores.values().stream().map(utilizador -> new EntidadeResumo(utilizador.getId(), utilizador.getNome())).toList();
    }

    public List<EntidadeResumo> resumoVendas() {
        return vendas.values().stream().map(venda -> new EntidadeResumo(venda.getId(), venda.getDataHora().toString())).toList();
    }

    public List<EntidadeResumo> resumoInventarios() {
        return inventarios.values().stream().map(inventario -> new EntidadeResumo(inventario.getId(), inventario.getDataInicio().toString())).toList();
    }

    private void atualizarStockDaVenda(UUID lojaId, UUID produtoId, int quantidade) {
        Stock stock = localizarStock(lojaId, produtoId);
        if (stock != null) {
            stock.atualizarQuantidade(-quantidade);
            if (stock.estaAbaixoMinimo()) {
                alertasStock.put(UUID.randomUUID(), new AlertaStock(stock.getNivelMinimo(), stock.getQuantidade()));
            }
        }
    }

    private void atualizarStockDaEntrada(UUID lojaId, UUID produtoId, int quantidadeRecebida) {
        Stock stock = localizarStock(lojaId, produtoId);
        if (stock != null) {
            stock.atualizarQuantidade(quantidadeRecebida);
        }
    }

    private Stock localizarStock(UUID lojaId, UUID produtoId) {
        return stocks.values().stream()
                .filter(stock -> stock.getLoja().getId().equals(lojaId))
                .filter(stock -> stock.getProduto().getId().equals(produtoId))
                .findFirst()
                .orElse(null);
    }

    private <T extends EntidadeBase> T require(Map<UUID, T> store, UUID id, String tipo) {
        T entity = store.get(id);
        if (entity == null) {
            throw new IllegalArgumentException(tipo + " nao encontrado");
        }
        return entity;
    }

    private Loja registarLoja(Loja loja) {
        lojas.put(loja.getId(), loja);
        return loja;
    }

    private Perfil registarPerfil(Perfil perfil) {
        perfis.put(perfil.getId(), perfil);
        return perfil;
    }

    private Utilizador registarUtilizador(Utilizador utilizador) {
        utilizadores.put(utilizador.getId(), utilizador);
        return utilizador;
    }

    private Categoria registarCategoria(Categoria categoria) {
        categorias.put(categoria.getId(), categoria);
        return categoria;
    }

    private TaxaIVA registarTaxaIVA(TaxaIVA taxaIVA) {
        taxasIva.put(taxaIVA.getId(), taxaIVA);
        return taxaIVA;
    }

    private Produto registarProduto(Produto produto) {
        produtos.put(produto.getId(), produto);
        return produto;
    }

    private Fornecedor registarFornecedor(Fornecedor fornecedor) {
        fornecedores.put(fornecedor.getId(), fornecedor);
        return fornecedor;
    }

    private Stock registarStock(Stock stock) {
        stocks.put(stock.getId(), stock);
        return stock;
    }

    private MotivoAjuste registarMotivoAjuste(MotivoAjuste motivoAjuste) {
        motivosAjuste.put(motivoAjuste.getId(), motivoAjuste);
        return motivoAjuste;
    }

    private Encomenda registarEncomenda(Encomenda encomenda) {
        encomendas.put(encomenda.getId(), encomenda);
        return encomenda;
    }

    private Venda registarVenda(Venda venda) {
        vendas.put(venda.getId(), venda);
        return venda;
    }

    private Fatura registarFatura(Fatura fatura) {
        faturas.put(fatura.getId(), fatura);
        return fatura;
    }

    private FechoCaixa registarFechoCaixa(FechoCaixa fechoCaixa) {
        fechosCaixa.put(fechoCaixa.getId(), fechoCaixa);
        return fechoCaixa;
    }

    private Sincronizacao registarSincronizacao(Sincronizacao sincronizacao) {
        sincronizacoes.put(sincronizacao.getId(), sincronizacao);
        return sincronizacao;
    }

    private InventarioFisico registarInventario(InventarioFisico inventarioFisico) {
        inventarios.put(inventarioFisico.getId(), inventarioFisico);
        return inventarioFisico;
    }

    private AjusteInventario registarAjusteInventario(AjusteInventario ajusteInventario) {
        ajustes.put(ajusteInventario.getId(), ajusteInventario);
        return ajusteInventario;
    }

    private EntradaMercadoria registarEntradaMercadoria(EntradaMercadoria entradaMercadoria) {
        entradasMercadoria.put(entradaMercadoria.getId(), entradaMercadoria);
        return entradaMercadoria;
    }

    private NivelMinimo registarNivelMinimo(NivelMinimo nivelMinimo) {
        niveisMinimos.put(nivelMinimo.getId(), nivelMinimo);
        return nivelMinimo;
    }

    private AlertaStock registarAlertaStock(AlertaStock alertaStock) {
        this.alertasStock.put(alertaStock.getId(), alertaStock);
        return alertaStock;
    }

    private GuiaRemessa registarGuiaRemessa(GuiaRemessa guiaRemessa) {
        guiasRemessa.put(guiaRemessa.getId(), guiaRemessa);
        return guiaRemessa;
    }

    private CondicaoComercial registarCondicaoComercial(CondicaoComercial condicaoComercial) {
        condicoesComerciais.put(condicaoComercial.getId(), condicaoComercial);
        return condicaoComercial;
    }

    private LocalizacaoProduto registarLocalizacaoProduto(LocalizacaoProduto localizacaoProduto) {
        localizacoesProduto.put(localizacaoProduto.getId(), localizacaoProduto);
        return localizacaoProduto;
    }

    private LinhaInventario registarLinhaInventario(UUID inventarioId, UUID produtoId, int quantidadeContada, int quantidadeSistema) {
        InventarioFisico inventarioFisico = require(inventarios, inventarioId, "InventarioFisico");
        Produto produto = require(produtos, produtoId, "Produto");
        LinhaInventario linhaInventario = new LinhaInventario(inventarioFisico, produto, quantidadeContada, quantidadeSistema);
        inventarioFisico.calcularDiscrepancias();
        return linhaInventario;
    }

    public record EntidadeResumo(UUID id, String descricao) { }

    public record LinhaVendaRequest(UUID produtoId, int quantidade, BigDecimal precoUnitario) { }

    public record LinhaEncomendaRequest(UUID produtoId, int quantidade, BigDecimal precoUnitario) { }

    public record LinhaInventarioRequest(UUID produtoId, int quantidadeContada, int quantidadeSistema) { }
}