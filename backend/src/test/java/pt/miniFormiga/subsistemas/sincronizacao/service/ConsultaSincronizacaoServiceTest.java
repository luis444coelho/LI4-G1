package pt.miniFormiga.subsistemas.sincronizacao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.miniFormiga.domain.EstadoSincronizacaoCodigo;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Sincronizacao;
import pt.miniFormiga.subsistemas.sincronizacao.repository.SincronizacaoRepository;
import pt.miniFormiga.subsistemas.relatorios.dto.RelatoriosDtos.DashboardResponse;
import pt.miniFormiga.subsistemas.relatorios.dto.RelatoriosDtos.PeriodoResponse;
import pt.miniFormiga.subsistemas.sincronizacao.service.IConsultaSincronizacao.DadosRelatorioSincronizado;
import pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.SincronizacaoPayload;
import pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.VendaRelatorioSync;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultaSincronizacaoServiceTest {

    private SincronizacaoRepository sincronizacaoRepository;
    private ConsultaSincronizacaoService service;
    private ObjectMapper objectMapper;
    private Loja loja;

    @BeforeEach
    void setUp() {
        sincronizacaoRepository = mock(SincronizacaoRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new ConsultaSincronizacaoService(sincronizacaoRepository, objectMapper);
        loja = new Loja("Loja Braga", "Rua Central", "123456789");
    }

    @Test
    void dadosRelatorioConvertePayloadParaModeloDeLeitura() throws Exception {
        VendaRelatorioSync linha = vendaSync(loja.getId(), "Loja Braga", new BigDecimal("4.92"));
        DashboardResponse dashboard = new DashboardResponse(
                new PeriodoResponse(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new BigDecimal("4.92"),
                new BigDecimal("0.92"),
                new BigDecimal("2.50"),
                1,
                1,
                1,
                0,
                new BigDecimal("4.92"),
                List.of()
        );
        Sincronizacao sync = syncComPayload(loja, new SincronizacaoPayload(
                loja.getId(),
                LocalDateTime.of(2026, 5, 10, 13, 0),
                null,
                Map.of(),
                dashboard,
                List.of(linha),
                List.of()
        ));
        when(sincronizacaoRepository.findFirstByLojaIdAndEstadoInOrderByDataHoraFimDesc(any(), any()))
                .thenReturn(Optional.of(sync));

        List<DadosRelatorioSincronizado> dados = service.dadosRelatorio(loja.getId());

        assertEquals(1, dados.size());
        assertEquals(loja.getId(), dados.get(0).lojaId());
        assertEquals(dashboard, dados.get(0).dashboard());
        assertEquals(new BigDecimal("4.92"), dados.get(0).vendasRelatorio().get(0).valorComIva());
    }

    @Test
    void sincronizacoesInvalidasOuDuplicadasSaoIgnoradas() throws Exception {
        Loja lojaPorto = new Loja(UUID.randomUUID(), "Loja Porto", "Rua Porto", "223456789", "222000000");
        Sincronizacao invalida = new Sincronizacao(loja, EstadoSincronizacaoCodigo.CONCLUIDA);
        invalida.concluir(EstadoSincronizacaoCodigo.CONCLUIDA, "{json", 1, "[]", 0);
        Sincronizacao semPayload = new Sincronizacao(lojaPorto, EstadoSincronizacaoCodigo.CONCLUIDA);
        semPayload.concluir(EstadoSincronizacaoCodigo.CONCLUIDA, "", 0, "[]", 0);
        Sincronizacao valido = syncComPayload(loja, new SincronizacaoPayload(
                loja.getId(),
                LocalDateTime.of(2026, 5, 10, 13, 0),
                null,
                Map.of(),
                null,
                List.of(vendaSync(loja.getId(), "Loja Braga", new BigDecimal("4.92"))),
                List.of()
        ));
        Sincronizacao duplicadoMaisAntigo = syncComPayload(loja, new SincronizacaoPayload(
                loja.getId(),
                LocalDateTime.of(2026, 5, 9, 13, 0),
                null,
                Map.of(),
                null,
                List.of(vendaSync(loja.getId(), "Loja Braga", new BigDecimal("2.46"))),
                List.of()
        ));
        when(sincronizacaoRepository.findByEstadoInOrderByDataHoraFimDesc(any()))
                .thenReturn(List.of(invalida, semPayload, valido, duplicadoMaisAntigo));

        List<DadosRelatorioSincronizado> dados = service.dadosRelatorio(null);

        assertEquals(1, dados.size());
        assertEquals(loja.getId(), dados.get(0).lojaId());
        assertEquals(new BigDecimal("4.92"), dados.get(0).vendasRelatorio().get(0).valorComIva());
    }

    @Test
    void listaVaziaQuandoRepositorioNaoDevolveSincronizacoes() {
        when(sincronizacaoRepository.findByEstadoInOrderByDataHoraFimDesc(any())).thenReturn(null);

        assertTrue(service.dadosRelatorio(null).isEmpty());
    }

    private Sincronizacao syncComPayload(Loja lojaSync, SincronizacaoPayload payload) throws Exception {
        Sincronizacao sync = new Sincronizacao(lojaSync, EstadoSincronizacaoCodigo.CONCLUIDA);
        sync.concluir(
                EstadoSincronizacaoCodigo.CONCLUIDA,
                objectMapper.writeValueAsString(payload),
                payload.quantidadeRegistos(),
                "[]",
                0
        );
        return sync;
    }

    private VendaRelatorioSync vendaSync(UUID lojaId, String lojaNome, BigDecimal valorComIva) {
        BigDecimal valorSemIva = valorComIva.divide(new BigDecimal("1.23"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal iva = valorComIva.subtract(valorSemIva);
        BigDecimal custo = new BigDecimal("0.75");
        return new VendaRelatorioSync(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 5, 10, 12, 0),
                lojaId,
                lojaNome,
                UUID.randomUUID(),
                "Agua",
                UUID.randomUUID(),
                "Bebidas",
                1,
                valorSemIva,
                iva,
                valorComIva,
                custo,
                valorSemIva.subtract(custo)
        );
    }
}
