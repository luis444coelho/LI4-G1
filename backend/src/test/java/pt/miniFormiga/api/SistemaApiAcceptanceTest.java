package pt.miniFormiga.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pt.miniFormiga.domain.Fornecedor;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.subsistemas.encomendas.repository.FornecedorRepository;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;
import pt.miniFormiga.subsistemas.catalogo.repository.ProdutoRepository;
import pt.miniFormiga.subsistemas.utilizadores.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.sincronizacao.transport.SincronizacaoTransporte;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.miniFormiga.subsistemas.sincronizacao.transport.SincronizacaoTransporte.ResultadoTransmissao;

@SpringBootTest(properties = {
        "mini-formiga.demo-data.enabled=true",
        "mini-formiga.audit.file=target/test-audit-system.jsonl",
        "spring.datasource.url=jdbc:sqlite:file:sistema-api-acceptance-test?mode=memory&cache=shared",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class SistemaApiAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private UtilizadorRepository utilizadorRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @MockBean
    private SincronizacaoTransporte transporte;

    @Test
    void fluxosAceitacaoPdvComprasInventarioESincronizacaoFuncionamViaApi() throws Exception {
        when(transporte.transmitir(any())).thenReturn(ResultadoTransmissao.concluida());
        Loja loja = lojaRepository.findByNif("123456789").orElseThrow();
        Utilizador operador = utilizadorRepository.findByUsername("operador.braga").orElseThrow();
        Utilizador gerente = utilizadorRepository.findByUsername("gerente.braga").orElseThrow();
        Produto produto = produtoRepository.findByCodigo("5600000000011").orElseThrow();
        Fornecedor fornecedor = fornecedorRepository.findByNif("987654321").orElseThrow();
        String tokenOperador = login("operador.braga");
        String tokenGerente = login("gerente.braga");

        UUID vendaId = postAndReadId("/api/v1/vendas", tokenOperador, """
                {
                  "lojaId": "%s",
                  "utilizadorId": "%s"
                }
                """.formatted(loja.getId(), operador.getId()));

        mockMvc.perform(post("/api/v1/vendas/{id}/linhas", vendaId)
                        .header("Authorization", bearer(tokenOperador))
                        .contentType("application/json")
                        .content("""
                                {
                                  "produtoId": "%s",
                                  "quantidade": 41
                                }
                                """.formatted(produto.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas[0].quantidade").value(41));

        mockMvc.perform(post("/api/v1/vendas/{id}/finalizar", vendaId)
                        .header("Authorization", bearer(tokenOperador))
                        .contentType("application/json")
                        .content("""
                                {
                                  "meioPagamento": "NUMERARIO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meioPagamento").value("NUMERARIO"))
                .andExpect(jsonPath("$.total", greaterThan(0.0)));

        mockMvc.perform(post("/api/v1/vendas/{id}/fatura", vendaId)
                        .header("Authorization", bearer(tokenOperador))
                        .contentType("application/json")
                        .content("""
                                {
                                  "nifCliente": "123456789",
                                  "nomeCliente": "Cliente Aceitacao"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroFatura").exists());

        mockMvc.perform(post("/api/v1/fechos-caixa")
                        .header("Authorization", bearer(tokenGerente))
                        .contentType("application/json")
                        .content("""
                                {
                                  "lojaId": "%s",
                                  "utilizadorId": "%s"
                                }
                                """.formatted(loja.getId(), gerente.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGeral", greaterThan(0.0)));

        mockMvc.perform(post("/api/v1/sincronizacao/iniciar")
                        .header("Authorization", bearer(tokenGerente))
                        .contentType("application/json")
                        .content("""
                                {
                                  "lojaId": "%s"
                                }
                                """.formatted(loja.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONCLUIDA"))
                .andExpect(jsonPath("$.quantidadeRegistos", greaterThan(0)));

        mockMvc.perform(get("/api/v1/stock/alertas")
                        .header("Authorization", bearer(tokenGerente))
                        .param("lojaId", loja.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produto").value("Agua 0.5L"));

        UUID encomendaId = postAndReadId("/api/v1/encomendas", tokenGerente, """
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
                """.formatted(loja.getId(), fornecedor.getId(), produto.getId()));

        mockMvc.perform(post("/api/v1/entradas-mercadoria")
                        .header("Authorization", bearer(tokenGerente))
                        .contentType("application/json")
                        .content("""
                                {
                                  "encomendaId": "%s",
                                  "lojaId": "%s",
                                  "responsavelId": "%s",
                                  "guiaNumero": "GR-ACEITACAO-001",
                                  "dataEmissao": "2026-05-26",
                                  "dataRecepcao": "2026-05-26",
                                  "linhas": [
                                    {
                                      "produtoId": "%s",
                                      "quantidadeRecebida": 10,
                                      "quantidadeEncomendada": 10,
                                      "observacoes": "Rececao aceite"
                                    }
                                  ]
                                }
                                """.formatted(encomendaId, loja.getId(), gerente.getId(), produto.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].quantidadeRecebida").value(10));

        mockMvc.perform(get("/api/v1/stock/{produtoId}", produto.getId())
                        .header("Authorization", bearer(tokenGerente))
                        .param("lojaId", loja.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(19));

        UUID inventarioId = postAndReadId("/api/v1/inventarios", tokenGerente, """
                {
                  "lojaId": "%s",
                  "utilizadorId": "%s"
                }
                """.formatted(loja.getId(), gerente.getId()));

        mockMvc.perform(post("/api/v1/inventarios/{id}/linhas", inventarioId)
                        .header("Authorization", bearer(tokenGerente))
                        .contentType("application/json")
                        .content("""
                                {
                                  "produtoId": "%s",
                                  "quantidade": 17
                                }
                                """.formatted(produto.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discrepancia").value(-2));

        mockMvc.perform(get("/api/v1/inventarios/{id}/discrepancias", inventarioId)
                        .header("Authorization", bearer(tokenGerente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produto").value("Agua 0.5L"));

        mockMvc.perform(post("/api/v1/stock/ajustes")
                        .header("Authorization", bearer(tokenGerente))
                        .contentType("application/json")
                        .content("""
                                {
                                  "produtoId": "%s",
                                  "lojaId": "%s",
                                  "quantidade": -2,
                                  "motivo": "CORRECAO_ERRO",
                                  "utilizadorId": "%s"
                                }
                                """.formatted(produto.getId(), loja.getId(), gerente.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(-2));
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "MiniFormiga2026!"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private UUID postAndReadId(String path, String token, String json) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
