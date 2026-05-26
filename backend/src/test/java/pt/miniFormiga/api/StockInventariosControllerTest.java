package pt.miniFormiga.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.miniFormiga.api.error.ApiExceptionHandler;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.MotivoAjusteCodigo;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.repository.InventarioFisicoRepository;
import pt.miniFormiga.repository.LinhaInventarioRepository;
import pt.miniFormiga.subsistemas.stock.ISubStock;
import pt.miniFormiga.subsistemas.stock.StockItem;
import pt.miniFormiga.subsistemas.stock.StockStore;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockInventariosControllerTest {

    private ISubStock stock;
    private AjusteInventarioRepository ajusteRepository;
    private StockStore stockStore;
    private InventarioFisicoRepository inventarioRepository;
    private LinhaInventarioRepository linhaRepository;
    private MockMvc stockMvc;
    private MockMvc inventariosMvc;

    @BeforeEach
    void setUp() {
        stock = mock(ISubStock.class);
        ajusteRepository = mock(AjusteInventarioRepository.class);
        stockStore = mock(StockStore.class);
        inventarioRepository = mock(InventarioFisicoRepository.class);
        linhaRepository = mock(LinhaInventarioRepository.class);
        stockMvc = MockMvcBuilders.standaloneSetup(new StockController(stock, ajusteRepository, stockStore))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        inventariosMvc = MockMvcBuilders.standaloneSetup(new InventariosController(stock, inventarioRepository, linhaRepository))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void stockEndpointsCobremConsultaAlertasAjustesEMotivos() throws Exception {
        Loja loja = loja();
        Produto produto = produto();
        Utilizador utilizador = utilizador(loja);
        StockItem item = new StockItem(produto, loja.getId(), loja.getNome(), 4, 10, "A1", "P2", null, null);
        AlertaStock alerta = new AlertaStock(produto, loja, 4);
        AlertaStock alertaLido = new AlertaStock(produto, loja, 4);
        alertaLido.marcarComoLido();
        AlertaStock alertaResolvido = new AlertaStock(produto, loja, 4);
        alertaResolvido.resolver();
        AjusteInventario ajuste = new AjusteInventario(produto, MotivoAjusteCodigo.QUEBRA, utilizador, -1, "Quebra");
        PageRequest pageable = PageRequest.of(0, 20);

        when(stockStore.listar(loja.getId())).thenReturn(List.of(item));
        when(stockStore.obter(produto.getId(), loja.getId())).thenReturn(item);
        when(stock.getAlertasAtivos(loja.getId())).thenReturn(List.of(alerta));
        when(stock.marcarAlertaLido(alerta.getId())).thenReturn(alertaLido);
        when(stock.resolverAlerta(alerta.getId())).thenReturn(alertaResolvido);
        when(stock.registarAjuste(produto.getId(), loja.getId(), -1, "QUEBRA", utilizador.getId())).thenReturn(ajuste);
        when(ajusteRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ajuste), pageable, 1));

        stockMvc.perform(get("/api/v1/stock")
                        .param("lojaId", loja.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produto").value("Agua 0.5L"))
                .andExpect(jsonPath("$[0].precisaReposicao").value(true));

        stockMvc.perform(get("/api/v1/stock/{produtoId}", produto.getId())
                        .param("lojaId", loja.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corredor").value("A1"));

        stockMvc.perform(put("/api/v1/stock/{produtoId}/nivel-minimo", produto.getId())
                        .contentType("application/json")
                        .content("""
                                {
                                  "lojaId": "%s",
                                  "quantidade": 5
                                }
                                """.formatted(loja.getId())))
                .andExpect(status().isOk());

        stockMvc.perform(get("/api/v1/stock/alertas")
                        .param("lojaId", loja.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantidadeNoMomento").value(4));

        stockMvc.perform(patch("/api/v1/stock/alertas/{alertaId}/lido", alerta.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lido").value(true));

        stockMvc.perform(patch("/api/v1/stock/alertas/{alertaId}/resolver", alerta.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvido").value(true));

        stockMvc.perform(post("/api/v1/stock/ajustes")
                        .contentType("application/json")
                        .content("""
                                {
                                  "produtoId": "%s",
                                  "lojaId": "%s",
                                  "quantidade": -1,
                                  "motivo": "QUEBRA",
                                  "utilizadorId": "%s"
                                }
                                """.formatted(produto.getId(), loja.getId(), utilizador.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivo").value("QUEBRA"));

        stockMvc.perform(get("/api/v1/stock/ajustes")
                        .param("lojaId", loja.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantidade").value(-1));

        stockMvc.perform(get("/api/v1/stock/motivos-ajuste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").exists());

        verify(stock).definirNivelMinimo(produto.getId(), loja.getId(), 5);
    }

    @Test
    void inventariosEndpointsCobremCriacaoContagemFechoConsultaEDiscrepancias() throws Exception {
        Loja loja = loja();
        Produto produto = produto();
        Utilizador responsavel = utilizador(loja);
        InventarioFisico inventario = new InventarioFisico(loja, responsavel);
        LinhaInventario linha = new LinhaInventario(inventario, produto, 8, 5);
        PageRequest pageable = PageRequest.of(0, 20);

        when(stock.iniciarInventarioFisico(loja.getId(), responsavel.getId())).thenReturn(inventario);
        when(stock.registarContagemLinha(inventario.getId(), produto.getId(), 8)).thenReturn(linha);
        when(linhaRepository.findByIdAndInventarioId(linha.getId(), inventario.getId())).thenReturn(Optional.of(linha));
        when(linhaRepository.save(linha)).thenReturn(linha);
        when(stock.listarDiscrepanciasInventario(inventario.getId())).thenReturn(List.of(linha));
        when(inventarioRepository.findByLojaId(loja.getId(), pageable)).thenReturn(new PageImpl<>(List.of(inventario), pageable, 1));
        when(inventarioRepository.findById(inventario.getId())).thenReturn(Optional.of(inventario));

        inventariosMvc.perform(post("/api/v1/inventarios")
                        .contentType("application/json")
                        .content("""
                                {
                                  "lojaId": "%s",
                                  "utilizadorId": "%s"
                                }
                                """.formatted(loja.getId(), responsavel.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lojaId").value(loja.getId().toString()));

        inventariosMvc.perform(post("/api/v1/inventarios/{id}/linhas", inventario.getId())
                        .contentType("application/json")
                        .content("""
                                {
                                  "produtoId": "%s",
                                  "quantidade": 8
                                }
                                """.formatted(produto.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discrepancia").value(3));

        inventariosMvc.perform(put("/api/v1/inventarios/{id}/linhas/{linhaId}", inventario.getId(), linha.getId())
                        .contentType("application/json")
                        .content("""
                                {
                                  "quantidade": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeContada").value(6));

        inventariosMvc.perform(post("/api/v1/inventarios/{id}/fechar", inventario.getId()))
                .andExpect(status().isOk());

        inventariosMvc.perform(get("/api/v1/inventarios/{id}/discrepancias", inventario.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produto").value("Agua 0.5L"));

        inventariosMvc.perform(get("/api/v1/inventarios")
                        .param("lojaId", loja.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(inventario.getId().toString()));

        inventariosMvc.perform(get("/api/v1/inventarios/{id}", inventario.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDiscrepancias").value(0));

        verify(stock).fecharInventario(inventario.getId());
    }

    @Test
    void atualizarLinhaFalhaParaInventarioFechadoOuLinhaInexistente() throws Exception {
        Loja loja = loja();
        Produto produto = produto();
        Utilizador responsavel = utilizador(loja);
        InventarioFisico inventario = new InventarioFisico(loja, responsavel);
        LinhaInventario linha = new LinhaInventario(inventario, produto, 8, 5);
        inventario.fechar();
        UUID linhaInexistente = UUID.randomUUID();

        when(linhaRepository.findByIdAndInventarioId(linha.getId(), inventario.getId())).thenReturn(Optional.of(linha));
        when(linhaRepository.findByIdAndInventarioId(linhaInexistente, inventario.getId())).thenReturn(Optional.empty());

        inventariosMvc.perform(put("/api/v1/inventarios/{id}/linhas/{linhaId}", inventario.getId(), linha.getId())
                        .contentType("application/json")
                        .content("""
                                {
                                  "quantidade": 6
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVENTARIO_FECHADO"));

        inventariosMvc.perform(put("/api/v1/inventarios/{id}/linhas/{linhaId}", inventario.getId(), linhaInexistente)
                        .contentType("application/json")
                        .content("""
                                {
                                  "quantidade": 6
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void obterInventarioInexistenteDevolve404() throws Exception {
        UUID id = UUID.randomUUID();
        when(inventarioRepository.findById(id)).thenReturn(Optional.empty());

        inventariosMvc.perform(get("/api/v1/inventarios/{id}", id))
                .andExpect(status().isNotFound());
    }

    private Loja loja() {
        return new Loja("Loja Braga", "Rua Central", "123456789");
    }

    private Utilizador utilizador(Loja loja) {
        return new Utilizador(
                "armazem.braga",
                "hash",
                "Armazem Braga",
                "armazem@mini-formiga.pt",
                new Perfil("ARMAZEM", List.of(Permissao.STOCK_WRITE)),
                loja
        );
    }

    private Produto produto() {
        return new Produto("5600000000011", "Agua 0.5L", new BigDecimal("1.00"),
                new BigDecimal("0.40"), new TaxaIVA("Normal", new BigDecimal("23")),
                new Categoria("Bebidas", "Bebidas frias"));
    }
}
