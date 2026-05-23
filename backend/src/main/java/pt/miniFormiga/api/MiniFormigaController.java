package pt.miniFormiga.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.Encomenda;
import pt.miniFormiga.domain.EntradaMercadoria;
import pt.miniFormiga.domain.Fatura;
import pt.miniFormiga.domain.FechoCaixa;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.Sincronizacao;
import pt.miniFormiga.domain.NivelMinimo;
import pt.miniFormiga.domain.Stock;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.domain.Venda;
import pt.miniFormiga.facade.MiniFormigaFacade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Mini-Formiga Demo API", description = "Operacoes de demonstracao baseadas no diagrama de classes")
public class MiniFormigaController {

    private final MiniFormigaFacade facade;

    public MiniFormigaController(MiniFormigaFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/lojas")
    @Operation(summary = "Listar lojas de demonstração")
    public List<MiniFormigaFacade.EntidadeResumo> listarLojas() {
        return facade.resumoLojas();
    }

    @GetMapping("/produtos")
    @Operation(summary = "Listar produtos de demonstração")
    public List<MiniFormigaFacade.EntidadeResumo> listarProdutos() {
        return facade.resumoProdutos();
    }

    @GetMapping("/fornecedores")
    @Operation(summary = "Listar fornecedores de demonstração")
    public List<MiniFormigaFacade.EntidadeResumo> listarFornecedores() {
        return facade.resumoFornecedores();
    }

    @GetMapping("/utilizadores")
    @Operation(summary = "Listar utilizadores de demonstração")
    public List<MiniFormigaFacade.EntidadeResumo> listarUtilizadores() {
        return facade.resumoUtilizadores();
    }

    @GetMapping("/stocks/{lojaId}")
    @Operation(summary = "Listar stock por loja")
    public List<StockResponse> listarStocksPorLoja(@PathVariable UUID lojaId) {
        return facade.listarStocksPorLoja(lojaId).stream().map(StockResponse::from).toList();
    }

    @PostMapping("/utilizadores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar utilizador")
    @ApiResponse(responseCode = "201", description = "Utilizador criado")
    public UtilizadorResponse criarUtilizador(@RequestBody CreateUtilizadorRequest request) {
        Utilizador utilizador = facade.criarUtilizador(
                request.username(),
                request.passwordHash(),
                request.nome(),
                request.email(),
                request.perfilId(),
                request.lojaId()
        );
        return UtilizadorResponse.from(utilizador);
    }

    @PostMapping("/vendas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registar venda")
    @ApiResponse(responseCode = "201", description = "Venda registada")
    public VendaResponse registarVenda(@RequestBody VendaRequest request) {
        Venda venda = facade.registarVenda(
                request.lojaId(),
                request.utilizadorId(),
                request.meioPagamentoTipo(),
                request.meioPagamentoDescricao(),
                request.linhas()
        );
        return VendaResponse.from(venda);
    }

    @PostMapping("/vendas/{vendaId}/fatura")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Emitir fatura")
    @ApiResponse(responseCode = "201", description = "Fatura emitida")
    public FaturaResponse emitirFatura(@PathVariable UUID vendaId, @RequestBody FaturaRequest request) {
        Fatura fatura = facade.emitirFatura(vendaId, request.numero(), request.serie(), request.tipo(), request.nifCliente(), request.nomeCliente());
        return FaturaResponse.from(fatura);
    }

    @PostMapping("/fechos-caixa")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registar fecho de caixa")
    @ApiResponse(responseCode = "201", description = "Fecho de caixa criado")
    public FechoCaixaResponse registarFechoCaixa(@RequestBody FechoCaixaRequest request) {
        FechoCaixa fechoCaixa = facade.registarFechoCaixa(request.lojaId(), request.responsavelId(), request.data());
        return FechoCaixaResponse.from(fechoCaixa);
    }

    @PostMapping("/encomendas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar encomenda")
    @ApiResponse(responseCode = "201", description = "Encomenda criada")
    public EncomendaResponse criarEncomenda(@RequestBody EncomendaRequest request) {
        Encomenda encomenda = facade.criarEncomenda(request.lojaId(), request.fornecedorId(), request.linhas());
        return EncomendaResponse.from(encomenda);
    }

    @PostMapping("/sincronizacoes/{lojaId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar sincronizacao")
    @ApiResponse(responseCode = "201", description = "Sincronizacao iniciada")
    public SincronizacaoResponse iniciarSincronizacao(@PathVariable UUID lojaId) {
        Sincronizacao sincronizacao = facade.iniciarSincronizacao(lojaId);
        return SincronizacaoResponse.from(sincronizacao);
    }

    @PostMapping("/stocks/{stockId}/nivel-minimo")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Definir nivel minimo de stock")
    @ApiResponse(responseCode = "201", description = "Nivel minimo definido")
    public NivelMinimoResponse definirNivelMinimo(@PathVariable UUID stockId, @RequestBody NivelMinimoRequest request) {
        return NivelMinimoResponse.from(facade.definirNivelMinimo(stockId, request.quantidade()));
    }

    @PostMapping("/stocks/{stockId}/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registar ajuste de inventario")
    @ApiResponse(responseCode = "201", description = "Ajuste registado")
    public AjusteInventarioResponse registarAjuste(@PathVariable UUID stockId, @RequestBody AjusteInventarioRequest request) {
        AjusteInventario ajuste = facade.registarAjuste(stockId, request.motivoId(), request.responsavelId(), request.quantidade(), request.observacoes());
        return AjusteInventarioResponse.from(ajuste);
    }

    @PostMapping("/inventarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar inventario fisico")
    @ApiResponse(responseCode = "201", description = "Inventario criado")
    public InventarioResponse criarInventario(@RequestBody InventarioRequest request) {
        InventarioFisico inventario = facade.criarInventario(request.lojaId(), request.responsavelId());
        return InventarioResponse.from(inventario);
    }

    @PostMapping("/inventarios/{inventarioId}/linhas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Calcular discrepancia de inventario")
    @ApiResponse(responseCode = "201", description = "Linha de inventario criada")
    public LinhaInventarioResponse calcularDiscrepancia(@PathVariable UUID inventarioId, @RequestBody LinhaInventarioRequest request) {
        LinhaInventario linha = facade.calcularDiscrepancia(inventarioId, request.produtoId(), request.quantidadeContada(), request.quantidadeSistema());
        return LinhaInventarioResponse.from(linha);
    }

    @PostMapping("/entradas-mercadoria")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registar entrada de mercadoria")
    @ApiResponse(responseCode = "201", description = "Entrada de mercadoria registada")
    public EntradaMercadoriaResponse registarEntrada(@RequestBody EntradaMercadoriaRequest request) {
        EntradaMercadoria entradaMercadoria = facade.registarEntrada(
                request.fornecedorId(),
                request.lojaId(),
                request.responsavelId(),
                request.produtoId(),
                request.guiaNumero(),
                request.dataEmissao(),
                request.dataRecepcao(),
                request.quantidadeRecebida(),
                request.quantidadeEncomendada(),
                request.observacoes()
        );
        return EntradaMercadoriaResponse.from(entradaMercadoria);
    }

    public record CreateUtilizadorRequest(UUID perfilId, UUID lojaId, String username, String passwordHash, String nome, String email) { }

    public record LinhaVendaRequest(UUID produtoId, int quantidade, BigDecimal precoUnitario) { }

    public record VendaRequest(UUID lojaId, UUID utilizadorId, String meioPagamentoTipo, String meioPagamentoDescricao, List<MiniFormigaFacade.LinhaVendaRequest> linhas) { }

    public record FaturaRequest(String numero, String serie, String tipo, String nifCliente, String nomeCliente) { }

    public record FechoCaixaRequest(UUID lojaId, UUID responsavelId, LocalDate data) { }

    public record LinhaEncomendaRequest(UUID produtoId, int quantidade, BigDecimal precoUnitario) { }

    public record EncomendaRequest(UUID lojaId, UUID fornecedorId, List<MiniFormigaFacade.LinhaEncomendaRequest> linhas) { }

    public record NivelMinimoRequest(int quantidade) { }

    public record AjusteInventarioRequest(UUID responsavelId, UUID motivoId, int quantidade, String observacoes) { }

    public record InventarioRequest(UUID lojaId, UUID responsavelId) { }

    public record LinhaInventarioRequest(UUID produtoId, int quantidadeContada, int quantidadeSistema) { }

    public record EntradaMercadoriaRequest(UUID fornecedorId,
                                           UUID lojaId,
                                           UUID responsavelId,
                                           UUID produtoId,
                                           String guiaNumero,
                                           LocalDate dataEmissao,
                                           LocalDate dataRecepcao,
                                           int quantidadeRecebida,
                                           int quantidadeEncomendada,
                                           String observacoes) { }

    public record UtilizadorResponse(UUID id, String nome, String username, String perfil, UUID lojaId) {
        static UtilizadorResponse from(Utilizador utilizador) {
            return new UtilizadorResponse(utilizador.getId(), utilizador.getNome(), utilizador.getUsername(), utilizador.getPerfil().getNome(), utilizador.getLoja().getId());
        }
    }

    public record VendaResponse(UUID id, BigDecimal totalSemIVA, BigDecimal totalIVA, BigDecimal totalComIVA, boolean anulada, int linhas) {
        static VendaResponse from(Venda venda) {
            return new VendaResponse(venda.getId(), venda.getTotalSemIVA(), venda.getTotalIVA(), venda.getTotalComIVA(), venda.isAnulada(), venda.getLinhas().size());
        }
    }

    public record FaturaResponse(UUID id, String numero, String serie, String tipo, BigDecimal totalComIVA, boolean emitida) {
        static FaturaResponse from(Fatura fatura) {
            return new FaturaResponse(fatura.getId(), fatura.getNumero(), fatura.getSerie(), fatura.getTipo(), fatura.getTotalComIVA(), fatura.isEmitida());
        }
    }

    public record FechoCaixaResponse(UUID id, BigDecimal totalNumerario, BigDecimal totalCartao, BigDecimal totalMBWay, BigDecimal totalGeral, boolean confirmado) {
        static FechoCaixaResponse from(FechoCaixa fechoCaixa) {
            return new FechoCaixaResponse(fechoCaixa.getId(), fechoCaixa.getTotalNumerario(), fechoCaixa.getTotalCartao(), fechoCaixa.getTotalMBWay(), fechoCaixa.getTotalGeral(), fechoCaixa.isConfirmado());
        }
    }

    public record EncomendaResponse(UUID id, LocalDateTime dataSubmissao, LocalDateTime dataProcessamento, BigDecimal totalEstimado, int linhas) {
        static EncomendaResponse from(Encomenda encomenda) {
            return new EncomendaResponse(encomenda.getId(), encomenda.getDataSubmissao(), encomenda.getDataProcessamento(), encomenda.getTotalEstimado(), encomenda.getLinhas().size());
        }
    }

    public record SincronizacaoResponse(UUID id, LocalDateTime inicio, LocalDateTime fim, int registos) {
        static SincronizacaoResponse from(Sincronizacao sincronizacao) {
            return new SincronizacaoResponse(sincronizacao.getId(), sincronizacao.getDataHoraInicio(), sincronizacao.getDataHoraFim(), sincronizacao.getQuantidadeRegistos());
        }
    }

    public record NivelMinimoResponse(UUID id, int quantidade, UUID stockId) {
        static NivelMinimoResponse from(NivelMinimo nivelMinimo) {
            return new NivelMinimoResponse(nivelMinimo.getId(), nivelMinimo.getQuantidade(), nivelMinimo.getStock().getId());
        }
    }

    public record AjusteInventarioResponse(UUID id, int quantidade, String observacoes, UUID stockId) {
        static AjusteInventarioResponse from(AjusteInventario ajusteInventario) {
            return new AjusteInventarioResponse(ajusteInventario.getId(), ajusteInventario.getQuantidade(), ajusteInventario.getObservacoes(), ajusteInventario.getProduto().getId());
        }
    }

    public record InventarioResponse(UUID id, LocalDateTime dataInicio, int totalDiscrepancias, boolean fechado) {
        static InventarioResponse from(InventarioFisico inventarioFisico) {
            return new InventarioResponse(inventarioFisico.getId(), inventarioFisico.getDataInicio(), inventarioFisico.getTotalDiscrepancias(), inventarioFisico.isFechado());
        }
    }

    public record LinhaInventarioResponse(UUID id, UUID produtoId, int quantidadeContada, int quantidadeSistema, int discrepancia) {
        static LinhaInventarioResponse from(LinhaInventario linhaInventario) {
            return new LinhaInventarioResponse(linhaInventario.getId(), linhaInventario.getProduto().getId(), linhaInventario.getQuantidadeContada(), linhaInventario.getQuantidadeSistema(), linhaInventario.getDiscrepancia());
        }
    }

    public record StockResponse(UUID id, UUID produtoId, String produto, int quantidade, boolean abaixoMinimo) {
        static StockResponse from(Stock stock) {
            return new StockResponse(stock.getId(), stock.getProduto().getId(), stock.getProduto().getNome(), stock.getQuantidade(), stock.estaAbaixoMinimo());
        }
    }

    public record EntradaMercadoriaResponse(UUID id, int quantidadeRecebida, int quantidadeEncomendada, int discrepancia) {
        static EntradaMercadoriaResponse from(EntradaMercadoria entradaMercadoria) {
            return new EntradaMercadoriaResponse(entradaMercadoria.getId(), entradaMercadoria.getQuantidadeRecebida(), entradaMercadoria.getQuantidadeEncomendada(), entradaMercadoria.getDiscrepancia());
        }
    }
}
