package pt.miniFormiga.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.miniFormiga.api.error.ApiExceptionHandler;
import pt.miniFormiga.subsistemas.auditoria.ISubAuditoria;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.Devolucao;
import pt.miniFormiga.domain.Fatura;
import pt.miniFormiga.domain.LinhaVenda;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.MeioPagamentoTipo;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.domain.Venda;
import pt.miniFormiga.repository.DevolucaoRepository;
import pt.miniFormiga.repository.FaturaRepository;
import pt.miniFormiga.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.pdv.ISubPDV;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.miniFormiga.subsistemas.pdv.PdvDtos.FaturaDTO;
import static pt.miniFormiga.subsistemas.pdv.PdvDtos.VendaDTO;

class VendasControllerTest {

    private ISubPDV pdv;
    private FaturaRepository faturaRepository;
    private DevolucaoRepository devolucaoRepository;
    private UtilizadorRepository utilizadorRepository;
    private ISubAuditoria auditoria;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        pdv = mock(ISubPDV.class);
        faturaRepository = mock(FaturaRepository.class);
        devolucaoRepository = mock(DevolucaoRepository.class);
        utilizadorRepository = mock(UtilizadorRepository.class);
        auditoria = mock(ISubAuditoria.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new VendasController(
                        pdv,
                        faturaRepository,
                        devolucaoRepository,
                        utilizadorRepository,
                        auditoria
                ))
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void fluxoBaseDeVendaExpõeContratoRestDoPdv() throws Exception {
        Loja loja = loja();
        Utilizador operador = operador(loja);
        Produto produto = produto();
        Venda vendaAberta = new Venda(loja, operador);
        Venda vendaComLinha = new Venda(loja, operador);
        LinhaVenda linha = new LinhaVenda(vendaComLinha, produto, 2);
        Venda vendaFinalizada = new Venda(loja, operador);
        new LinhaVenda(vendaFinalizada, produto, 1);
        vendaFinalizada.finalizar(MeioPagamentoTipo.NUMERARIO);
        PageRequest pageable = PageRequest.of(0, 20);

        when(pdv.registarVenda(loja.getId(), operador.getId())).thenReturn(vendaAberta);
        when(pdv.obterVenda(vendaAberta.getId())).thenReturn(VendaDTO.from(vendaComLinha));
        when(pdv.finalizarVenda(vendaAberta.getId(), "NUMERARIO")).thenReturn(vendaFinalizada);
        when(pdv.listarVendas(eq(loja.getId()), any(), any(), any())).thenReturn(new PageImpl<>(List.of(VendaDTO.from(vendaFinalizada)), pageable, 1));
        when(pdv.listarVendasPorFechar(eq(loja.getId()), any(), any(), any())).thenReturn(new PageImpl<>(List.of(VendaDTO.from(vendaAberta)), pageable, 1));

        mockMvc.perform(post("/api/v1/vendas")
                        .contentType("application/json")
                        .content("""
                                {
                                  "lojaId": "%s",
                                  "utilizadorId": "%s"
                                }
                                """.formatted(loja.getId(), operador.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendaAberta.getId().toString()));

        mockMvc.perform(post("/api/v1/vendas/{id}/linhas", vendaAberta.getId())
                        .contentType("application/json")
                        .content("""
                                {
                                  "produtoId": "%s",
                                  "quantidade": 2
                                }
                                """.formatted(produto.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas[0].id").value(linha.getId().toString()));

        mockMvc.perform(delete("/api/v1/vendas/{id}/linhas/{linhaId}", vendaAberta.getId(), linha.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas[0].produto").value("Agua 0.5L"));

        mockMvc.perform(post("/api/v1/vendas/{id}/finalizar", vendaAberta.getId())
                        .contentType("application/json")
                        .content("""
                                {
                                  "meioPagamento": "NUMERARIO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meioPagamento").value("NUMERARIO"));

        mockMvc.perform(post("/api/v1/vendas/{id}/anular", vendaAberta.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/vendas/{id}", vendaAberta.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vendaComLinha.getId().toString()));

        mockMvc.perform(get("/api/v1/vendas")
                        .param("lojaId", loja.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].meioPagamento").value("NUMERARIO"));

        mockMvc.perform(get("/api/v1/vendas")
                        .param("lojaId", loja.getId().toString())
                        .param("porFechar", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(vendaAberta.getId().toString()));

        verify(pdv).adicionarLinhaVenda(vendaAberta.getId(), produto.getId(), 2);
        verify(pdv).anularLinhaVenda(vendaAberta.getId(), linha.getId());
        verify(pdv).anularVenda(vendaAberta.getId());
    }

    @Test
    void endpointsDeFaturasDevolvemDocumentoPesquisaEAuditoria() throws Exception {
        Loja loja = loja();
        Utilizador operador = operador(loja);
        Produto produto = produto();
        Venda venda = new Venda(loja, operador);
        new LinhaVenda(venda, produto, 2);
        venda.finalizar(MeioPagamentoTipo.CARTAO);
        Fatura fatura = new Fatura(venda, "12", "A/2026", "SIMPLIFICADA", "123456789", "Cliente Teste");
        fatura.emitir();
        byte[] pdf = "%PDF-1.4\n".getBytes();
        var authentication = new UsernamePasswordAuthenticationToken(
                User.withUsername("operador.braga").password("hash").authorities(Permissao.PDV_WRITE).build(),
                null,
                List.of()
        );

        when(pdv.emitirFatura(venda.getId(), "123456789", "Cliente Teste")).thenReturn(fatura);
        when(faturaRepository.pesquisarPorLojaECliente(eq(loja.getId()), eq("Cliente"), any()))
                .thenReturn(new PageImpl<>(List.of(fatura), PageRequest.of(0, 20), 1));
        when(pdv.obterFatura(fatura.getId())).thenReturn(FaturaDTO.from(fatura));
        when(pdv.gerarDocumentoFiscal(fatura.getId(), "RECIBO")).thenReturn(pdf);
        when(faturaRepository.findBySerieAndNumero("A/2026", 12)).thenReturn(Optional.of(fatura));
        when(utilizadorRepository.findByUsername("operador.braga")).thenReturn(Optional.of(operador));

        mockMvc.perform(post("/api/v1/vendas/{id}/fatura", venda.getId())
                        .contentType("application/json")
                        .content("""
                                {
                                  "nifCliente": "123456789",
                                  "nomeCliente": "Cliente Teste"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroFatura").value("A/2026/00012"));

        mockMvc.perform(get("/api/v1/vendas/faturas")
                        .param("lojaId", loja.getId().toString())
                        .param("cliente", " Cliente "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nomeCliente").value("Cliente Teste"));

        mockMvc.perform(get("/api/v1/vendas/faturas/{id}", fatura.getId())
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fatura.getId().toString()));

        mockMvc.perform(get("/api/v1/vendas/faturas/{id}/documento", fatura.getId())
                        .param("tipo", "RECIBO")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"mini-formiga-recibo-" + fatura.getId() + ".pdf\""))
                .andExpect(content().bytes(pdf));

        mockMvc.perform(get("/api/v1/vendas/faturas/numero")
                        .param("numeroFatura", " A/2026/12 ")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie").value("A/2026"));

        mockMvc.perform(get("/api/v1/vendas/faturas/numero")
                        .param("numeroFatura", "A-2026")
                        .principal(authentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NUMERO_FATURA_INVALIDO"));
    }

    @Test
    void listarDevolucoesMapeiaHistoricoDaLoja() throws Exception {
        Loja loja = loja();
        Utilizador operador = operador(loja);
        Produto produto = produto();
        Venda venda = new Venda(loja, operador);
        new LinhaVenda(venda, produto, 1);
        venda.finalizar(MeioPagamentoTipo.MBWAY);
        Devolucao devolucao = new Devolucao(venda, produto, 1, new BigDecimal("1.23"), "DEV-1");
        when(devolucaoRepository.findByVendaLojaIdOrderByDataHoraDesc(loja.getId())).thenReturn(List.of(devolucao));

        mockMvc.perform(get("/api/v1/vendas/devolucoes")
                        .param("lojaId", loja.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produto").value("Agua 0.5L"))
                .andExpect(jsonPath("$[0].numeroDocumento").value("DEV-1"));
    }

    private Loja loja() {
        return new Loja("Loja Braga", "Rua Central", "123456789");
    }

    private Utilizador operador(Loja loja) {
        return new Utilizador(
                "operador.braga",
                "hash",
                "Operador Braga",
                "operador@mini-formiga.pt",
                new Perfil("FUNCIONARIO", List.of(Permissao.PDV_WRITE)),
                loja
        );
    }

    private Produto produto() {
        return new Produto("5600000000011", "Agua 0.5L", new BigDecimal("1.00"),
                new BigDecimal("0.40"), new TaxaIVA("Normal", new BigDecimal("23")),
                new Categoria("Bebidas", "Bebidas frias"));
    }
}
