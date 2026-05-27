package pt.miniFormiga.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.miniFormiga.api.error.ApiExceptionHandler;
import pt.miniFormiga.subsistemas.relatorios.facade.ISubRelatorios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.miniFormiga.subsistemas.relatorios.dto.RelatoriosDtos.*;

class RelatoriosControllerTest {

    private ISubRelatorios relatorios;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        relatorios = mock(ISubRelatorios.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RelatoriosController(relatorios))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void dashboardEndpointDevolveKpis() throws Exception {
        DashboardResponse response = new DashboardResponse(
                new PeriodoResponse(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new BigDecimal("10.50"),
                new BigDecimal("1.96"),
                new BigDecimal("4.00"),
                2,
                1,
                1,
                3,
                new BigDecimal("5.25"),
                List.of()
        );
        when(relatorios.obterDashboard(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("inicio", "2026-05-01")
                        .param("fim", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroVendas").value(2))
                .andExpect(jsonPath("$.alertasAtivos").value(3))
                .andExpect(jsonPath("$.totalVendas").value(10.50));
    }

    @Test
    void exportarEndpointDevolveFicheiroCsv() throws Exception {
        byte[] conteudo = "data,descricao,valor,iva,loja\n".getBytes();
        when(relatorios.exportar(any())).thenReturn(new ExportacaoRelatorio(
                "mini-formiga-vendas.csv",
                "text/csv;charset=UTF-8",
                conteudo
        ));

        mockMvc.perform(post("/api/v1/relatorios/exportar")
                        .contentType("application/json")
                        .content("""
                                {
                                  "tipo": "VENDAS",
                                  "formato": "CSV",
                                  "inicio": "2026-05-01",
                                  "fim": "2026-05-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"mini-formiga-vendas.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().bytes(conteudo));
    }
}
