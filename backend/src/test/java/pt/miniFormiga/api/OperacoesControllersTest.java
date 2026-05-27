package pt.miniFormiga.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.miniFormiga.api.error.ApiExceptionHandler;
import pt.miniFormiga.subsistemas.encomendas.facade.ISubEncomendas;
import pt.miniFormiga.subsistemas.pdv.facade.ISubPDV;
import pt.miniFormiga.subsistemas.sincronizacao.facade.ISubSincronizacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.CondicaoComercialResponse;
import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.EncomendaResponse;
import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.EntradaMercadoriaResponse;
import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.FornecedorResponse;
import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.LinhaEncomendaResponse;
import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.ProximaGuiaRemessaResponse;
import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.SugestaoEncomendaResponse;
import static pt.miniFormiga.subsistemas.pdv.dto.PdvDtos.FechoCaixaDTO;
import static pt.miniFormiga.subsistemas.pdv.dto.PdvDtos.ProdutoDTO;
import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.ConflitoSincronizacaoResponse;
import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.SincronizacaoResponse;
import static pt.miniFormiga.subsistemas.sincronizacao.transport.SincronizacaoTransporte.ResultadoTransmissao;

class OperacoesControllersTest {

    @Test
    void produtosControllerExpoeCatalogoCompleto() throws Exception {
        ISubPDV pdv = mock(ISubPDV.class);
        MockMvc mockMvc = standalone(new ProdutosController(pdv));
        UUID lojaId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        ProdutoDTO produto = produtoDto(produtoId);
        when(pdv.listarProdutos(eq(lojaId), any())).thenReturn(new PageImpl<>(List.of(produto), PageRequest.of(0, 20), 1));
        when(pdv.criarProduto(any())).thenReturn(produto);
        when(pdv.obterProdutoPorId(produtoId)).thenReturn(produto);
        when(pdv.obterProdutoPorCodigoBarras("5600000000011")).thenReturn(produto);
        when(pdv.atualizarProduto(eq(produtoId), any())).thenReturn(produto);

        mockMvc.perform(get("/api/v1/produtos").param("lojaId", lojaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Agua 0.5L"));

        mockMvc.perform(post("/api/v1/produtos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "codigoBarras": "5600000000011",
                                  "nome": "Agua 0.5L",
                                  "descricao": "Garrafa",
                                  "precoVenda": 1.00,
                                  "precoCusto": 0.40,
                                  "categoriaId": "%s",
                                  "taxaIvaId": "%s"
                                }
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoBarras").value("5600000000011"));

        mockMvc.perform(get("/api/v1/produtos/{id}", produtoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produtoId.toString()));

        mockMvc.perform(get("/api/v1/produtos/barcode/{codigo}", "5600000000011"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Agua 0.5L"));

        mockMvc.perform(put("/api/v1/produtos/{id}", produtoId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "nome": "Agua 0.5L",
                                  "ativo": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void fornecedoresControllerExpoeCrudECondicoesComerciais() throws Exception {
        ISubEncomendas encomendas = mock(ISubEncomendas.class);
        MockMvc mockMvc = standalone(new FornecedoresController(encomendas));
        UUID fornecedorId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        FornecedorResponse fornecedor = new FornecedorResponse(
                fornecedorId,
                "Fornecedor Norte",
                "987654321",
                "Rua do Armazem",
                "229000000",
                "fornecedor@mini.pt",
                true,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        );
        CondicaoComercialResponse condicao = new CondicaoComercialResponse(
                UUID.randomUUID(),
                fornecedorId,
                produtoId,
                "Agua 0.5L",
                new BigDecimal("0.60"),
                2,
                10,
                LocalDate.of(2026, 5, 1)
        );
        when(encomendas.listarFornecedores(any())).thenReturn(new PageImpl<>(List.of(fornecedor), PageRequest.of(0, 20), 1));
        when(encomendas.obterFornecedor(fornecedorId)).thenReturn(fornecedor);
        when(encomendas.criarFornecedor(any())).thenReturn(fornecedor);
        when(encomendas.atualizarFornecedor(eq(fornecedorId), any())).thenReturn(fornecedor);
        when(encomendas.listarCondicoesComerciais(fornecedorId)).thenReturn(List.of(condicao));
        when(encomendas.definirCondicaoComercial(eq(fornecedorId), any())).thenReturn(condicao);

        mockMvc.perform(get("/api/v1/fornecedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Fornecedor Norte"));

        mockMvc.perform(get("/api/v1/fornecedores/{id}", fornecedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nif").value("987654321"));

        mockMvc.perform(post("/api/v1/fornecedores")
                        .contentType("application/json")
                        .content(fornecedorPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("fornecedor@mini.pt"));

        mockMvc.perform(put("/api/v1/fornecedores/{id}", fornecedorId)
                        .contentType("application/json")
                        .content(fornecedorPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        mockMvc.perform(delete("/api/v1/fornecedores/{id}", fornecedorId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/fornecedores/{id}/condicoes", fornecedorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produto").value("Agua 0.5L"));

        mockMvc.perform(post("/api/v1/fornecedores/{id}/condicoes", fornecedorId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "produtoId": "%s",
                                  "precoUnitario": 0.60,
                                  "prazoEntregaDias": 2,
                                  "quantidadeMinima": 10,
                                  "dataVigencia": "2026-05-01"
                                }
                                """.formatted(produtoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precoUnitario").value(0.60));

        verify(encomendas).desativarFornecedor(fornecedorId);
    }

    @Test
    void encomendasEEntradasControllerExpoemFluxoDeCompras() throws Exception {
        ISubEncomendas encomendas = mock(ISubEncomendas.class);
        MockMvc encomendasMvc = standalone(new EncomendasController(encomendas));
        MockMvc entradasMvc = standalone(new EntradasMercadoriaController(encomendas));
        UUID lojaId = UUID.randomUUID();
        UUID fornecedorId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        UUID encomendaId = UUID.randomUUID();
        UUID responsavelId = UUID.randomUUID();
        EncomendaResponse encomenda = encomendaResponse(encomendaId, lojaId, fornecedorId, produtoId);
        SugestaoEncomendaResponse sugestao = new SugestaoEncomendaResponse(
                fornecedorId,
                "Fornecedor Norte",
                produtoId,
                "Agua 0.5L",
                lojaId,
                4,
                10,
                16,
                new BigDecimal("0.60")
        );
        EntradaMercadoriaResponse entrada = new EntradaMercadoriaResponse(
                UUID.randomUUID(),
                encomendaId,
                lojaId,
                responsavelId,
                produtoId,
                "Agua 0.5L",
                "GR-2026-001",
                LocalDateTime.of(2026, 5, 26, 10, 0),
                10,
                10,
                0,
                "OK"
        );

        when(encomendas.listarEncomendas(eq(lojaId), any())).thenReturn(new PageImpl<>(List.of(encomenda), PageRequest.of(0, 20), 1));
        when(encomendas.criarEncomenda(any())).thenReturn(encomenda);
        when(encomendas.criarEncomendaConsolidada(any())).thenReturn(List.of(encomenda));
        when(encomendas.sugerirEncomendas(lojaId, fornecedorId)).thenReturn(List.of(sugestao));
        when(encomendas.obterEncomenda(encomendaId)).thenReturn(encomenda);
        when(encomendas.atualizarEstado(eq(encomendaId), any())).thenReturn(encomenda);
        when(encomendas.registarEntradaMercadoria(any())).thenReturn(List.of(entrada));
        when(encomendas.listarEntradasMercadoria(eq(lojaId), any())).thenReturn(new PageImpl<>(List.of(entrada), PageRequest.of(0, 20), 1));
        when(encomendas.obterProximaGuiaRemessa(lojaId)).thenReturn(new ProximaGuiaRemessaResponse("GR-2026-002"));

        encomendasMvc.perform(get("/api/v1/encomendas").param("lojaId", lojaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].estado").value("PENDENTE"));

        encomendasMvc.perform(post("/api/v1/encomendas")
                        .contentType("application/json")
                        .content(encomendaPayload(lojaId, fornecedorId, produtoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(encomendaId.toString()));

        encomendasMvc.perform(post("/api/v1/encomendas/consolidada")
                        .contentType("application/json")
                        .content("""
                                {
                                  "lojaIds": ["%s", "%s"],
                                  "fornecedorId": "%s",
                                  "linhas": [
                                    {
                                      "produtoId": "%s",
                                      "quantidade": 10,
                                      "precoUnitario": 0.60
                                    }
                                  ]
                                }
                                """.formatted(lojaId, UUID.randomUUID(), fornecedorId, produtoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].fornecedor").value("Fornecedor Norte"));

        encomendasMvc.perform(get("/api/v1/encomendas/sugestoes")
                        .param("lojaId", lojaId.toString())
                        .param("fornecedorId", fornecedorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantidadeSugerida").value(16));

        encomendasMvc.perform(get("/api/v1/encomendas/{id}", encomendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas[0].produto").value("Agua 0.5L"));

        encomendasMvc.perform(patch("/api/v1/encomendas/{id}/estado", encomendaId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "estadoCodigo": "ENVIADA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDENTE"));

        entradasMvc.perform(post("/api/v1/entradas-mercadoria")
                        .contentType("application/json")
                        .content("""
                                {
                                  "encomendaId": "%s",
                                  "lojaId": "%s",
                                  "responsavelId": "%s",
                                  "guiaNumero": "GR-2026-001",
                                  "dataEmissao": "2026-05-26",
                                  "dataRecepcao": "2026-05-26",
                                  "linhas": [
                                    {
                                      "produtoId": "%s",
                                      "quantidadeRecebida": 10,
                                      "quantidadeEncomendada": 10,
                                      "observacoes": "OK"
                                    }
                                  ]
                                }
                                """.formatted(encomendaId, lojaId, responsavelId, produtoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].guiaNumero").value("GR-2026-001"));

        entradasMvc.perform(get("/api/v1/entradas-mercadoria").param("lojaId", lojaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantidadeRecebida").value(10));

        entradasMvc.perform(get("/api/v1/entradas-mercadoria/proxima-guia").param("lojaId", lojaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("GR-2026-002"));
    }

    @Test
    void fechosSincronizacaoCentralEMeiosPagamentoCobremContratosRest() throws Exception {
        ISubPDV pdv = mock(ISubPDV.class);
        ISubSincronizacao sincronizacao = mock(ISubSincronizacao.class);
        MockMvc fechosMvc = standalone(new FechosCaixaController(pdv));
        MockMvc sincronizacaoMvc = standalone(new SincronizacaoController(sincronizacao));
        MockMvc centralMvc = standalone(new CentralSincronizacaoController(sincronizacao, "sync-token"));
        MockMvc meiosMvc = standalone(new MeiosPagamentoController());
        UUID lojaId = UUID.randomUUID();
        UUID gerenteId = UUID.randomUUID();
        UUID fechoId = UUID.randomUUID();
        UUID sincronizacaoId = UUID.randomUUID();
        FechoCaixaDTO fecho = new FechoCaixaDTO(
                fechoId,
                lojaId,
                gerenteId,
                LocalDate.of(2026, 5, 26),
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                new BigDecimal("5.00"),
                new BigDecimal("35.00"),
                null,
                false
        );
        SincronizacaoResponse sync = new SincronizacaoResponse(
                sincronizacaoId,
                lojaId,
                "CONCLUIDA",
                LocalDateTime.of(2026, 5, 26, 10, 0),
                LocalDateTime.of(2026, 5, 26, 10, 1),
                null,
                4,
                0,
                null
        );
        ConflitoSincronizacaoResponse conflito = new ConflitoSincronizacaoResponse(
                sincronizacaoId,
                lojaId,
                LocalDateTime.of(2026, 5, 26, 10, 1),
                "COM_CONFLITOS",
                1,
                "[]"
        );

        when(pdv.listarFechosCaixa(eq(lojaId), any())).thenReturn(new PageImpl<>(List.of(fecho), PageRequest.of(0, 20), 1));
        when(pdv.obterFechoCaixa(fechoId)).thenReturn(fecho);
        when(sincronizacao.iniciarSincronizacao(lojaId)).thenReturn(sync);
        when(sincronizacao.estadoAtual(lojaId)).thenReturn(sync);
        when(sincronizacao.historico(eq(lojaId), any())).thenReturn(new PageImpl<>(List.of(sync), PageRequest.of(0, 20), 1));
        when(sincronizacao.conflitos(lojaId)).thenReturn(List.of(conflito));
        when(sincronizacao.receber(any())).thenReturn(ResultadoTransmissao.concluida());
        when(pdv.registarFechoCaixa(lojaId, gerenteId)).thenReturn(new pt.miniFormiga.domain.FechoCaixa(
                new pt.miniFormiga.domain.Loja(lojaId, "Loja Braga", "Rua Central", "123456789", "253000000"),
                new pt.miniFormiga.domain.Utilizador(
                        "gerente.braga",
                        "hash",
                        "Gerente Braga",
                        "gerente@mini-formiga.pt",
                        pt.miniFormiga.domain.PerfilUtilizador.GERENTE,
                        new pt.miniFormiga.domain.Loja(lojaId, "Loja Braga", "Rua Central", "123456789", "253000000")
                ),
                LocalDate.of(2026, 5, 26),
                List.of()
        ));
        when(pdv.confirmarFechoCaixa(eq(fechoId), eq("OK"))).thenAnswer(invocation -> {
            pt.miniFormiga.domain.Loja loja = new pt.miniFormiga.domain.Loja(lojaId, "Loja Braga", "Rua Central", "123456789", "253000000");
            pt.miniFormiga.domain.Utilizador gerente = new pt.miniFormiga.domain.Utilizador(
                    "gerente.braga",
                    "hash",
                    "Gerente Braga",
                    "gerente@mini-formiga.pt",
                    pt.miniFormiga.domain.PerfilUtilizador.GERENTE,
                    loja
            );
            pt.miniFormiga.domain.FechoCaixa entidade = new pt.miniFormiga.domain.FechoCaixa(loja, gerente, LocalDate.of(2026, 5, 26), List.of());
            entidade.setObservacaoDiscrepancia("OK");
            entidade.confirmar();
            return entidade;
        });

        fechosMvc.perform(post("/api/v1/fechos-caixa")
                        .contentType("application/json")
                        .content("""
                                {
                                  "lojaId": "%s",
                                  "utilizadorId": "%s"
                                }
                                """.formatted(lojaId, gerenteId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lojaId").exists());

        fechosMvc.perform(post("/api/v1/fechos-caixa/{id}/confirmar", fechoId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "observacoesDiscrepancia": "OK"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(true));

        fechosMvc.perform(get("/api/v1/fechos-caixa").param("lojaId", lojaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].totalGeral").value(35.00));

        fechosMvc.perform(get("/api/v1/fechos-caixa/{id}", fechoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fechoId.toString()));

        sincronizacaoMvc.perform(post("/api/v1/sincronizacao/iniciar")
                        .contentType("application/json")
                        .content("""
                                {
                                  "lojaId": "%s"
                                }
                                """.formatted(lojaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONCLUIDA"));

        sincronizacaoMvc.perform(get("/api/v1/sincronizacao/estado").param("lojaId", lojaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeRegistos").value(4));

        sincronizacaoMvc.perform(get("/api/v1/sincronizacao/historico").param("lojaId", lojaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sincronizacaoId.toString()));

        sincronizacaoMvc.perform(get("/api/v1/sincronizacao/conflitos").param("lojaId", lojaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conflitosResolvidos").value(1));

        centralMvc.perform(post("/api/v1/central/sincronizacao/receber")
                        .header("X-Sync-Token", "sync-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "lojaId": "%s",
                                  "registos": {},
                                  "vendasRelatorio": [],
                                  "logsAuditoria": []
                                }
                                """.formatted(lojaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true));

        centralMvc.perform(post("/api/v1/central/sincronizacao/receber")
                        .header("X-Sync-Token", "errado")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SYNC_TOKEN_INVALIDO"));

        meiosMvc.perform(get("/api/v1/meios-pagamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").exists());

        verify(sincronizacao).receber(any());
        verify(pdv).listarFechosCaixa(eq(lojaId), any());
    }

    private MockMvc standalone(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private ProdutoDTO produtoDto(UUID produtoId) {
        return new ProdutoDTO(
                produtoId,
                "5600000000011",
                "Agua 0.5L",
                "Garrafa",
                new BigDecimal("1.00"),
                new BigDecimal("0.40"),
                new BigDecimal("0.60"),
                "Bebidas",
                new BigDecimal("23"),
                true
        );
    }

    private String fornecedorPayload() {
        return """
                {
                  "nome": "Fornecedor Norte",
                  "nif": "987654321",
                  "morada": "Rua do Armazem",
                  "telefone": "229000000",
                  "email": "fornecedor@mini.pt",
                  "horarioInicioArmazem": "08:00:00",
                  "horarioFimArmazem": "18:00:00"
                }
                """;
    }

    private EncomendaResponse encomendaResponse(UUID encomendaId, UUID lojaId, UUID fornecedorId, UUID produtoId) {
        return new EncomendaResponse(
                encomendaId,
                "ENC-1",
                lojaId,
                fornecedorId,
                "Fornecedor Norte",
                "PENDENTE",
                LocalDateTime.of(2026, 5, 26, 10, 0),
                LocalDateTime.of(2026, 5, 26, 10, 0),
                new BigDecimal("6.00"),
                List.of(new LinhaEncomendaResponse(
                        UUID.randomUUID(),
                        produtoId,
                        "Agua 0.5L",
                        10,
                        new BigDecimal("0.60"),
                        new BigDecimal("6.00")
                ))
        );
    }

    private String encomendaPayload(UUID lojaId, UUID fornecedorId, UUID produtoId) {
        return """
                {
                  "lojaId": "%s",
                  "fornecedorId": "%s",
                  "linhas": [
                    {
                      "produtoId": "%s",
                      "quantidade": 10,
                      "precoUnitario": 0.60
                    }
                  ]
                }
                """.formatted(lojaId, fornecedorId, produtoId);
    }
}
