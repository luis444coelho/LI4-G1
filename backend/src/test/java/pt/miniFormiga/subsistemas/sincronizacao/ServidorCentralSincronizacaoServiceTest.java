package pt.miniFormiga.subsistemas.sincronizacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.miniFormiga.domain.EstadoSincronizacao;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Stock;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.repository.EntradaMercadoriaRepository;
import pt.miniFormiga.repository.EstadoSincronizacaoRepository;
import pt.miniFormiga.repository.FaturaRepository;
import pt.miniFormiga.repository.FechoCaixaRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.SincronizacaoRepository;
import pt.miniFormiga.repository.StockRepository;
import pt.miniFormiga.repository.VendaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.RegistoSincronizacao;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoPayload;

class ServidorCentralSincronizacaoServiceTest {

    private SincronizacaoRepository sincronizacaoRepository;
    private EstadoSincronizacaoRepository estadoRepository;
    private LojaRepository lojaRepository;
    private StockRepository stockRepository;
    private ServidorCentralSincronizacaoService service;
    private Loja loja;

    @BeforeEach
    void setUp() {
        sincronizacaoRepository = mock(SincronizacaoRepository.class);
        estadoRepository = mock(EstadoSincronizacaoRepository.class);
        lojaRepository = mock(LojaRepository.class);
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FaturaRepository faturaRepository = mock(FaturaRepository.class);
        stockRepository = mock(StockRepository.class);
        AjusteInventarioRepository ajusteRepository = mock(AjusteInventarioRepository.class);
        FechoCaixaRepository fechoRepository = mock(FechoCaixaRepository.class);
        EntradaMercadoriaRepository entradaRepository = mock(EntradaMercadoriaRepository.class);
        service = new ServidorCentralSincronizacaoService(
                sincronizacaoRepository,
                estadoRepository,
                lojaRepository,
                vendaRepository,
                faturaRepository,
                stockRepository,
                ajusteRepository,
                fechoRepository,
                entradaRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        loja = new Loja("Central", "Rua Central", "123456789");
        when(estadoRepository.findByCodigo(any())).thenAnswer(invocation ->
                Optional.of(new EstadoSincronizacao(invocation.getArgument(0), invocation.getArgument(0))));
        when(sincronizacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void receberPayloadDeLojaRegistaSincronizacaoConcluida() {
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        SincronizacaoPayload payload = new SincronizacaoPayload(
                loja.getId(),
                LocalDateTime.now(),
                null,
                Map.of("stock", List.of()),
                List.of()
        );

        var resultado = service.receber(payload);

        assertTrue(resultado.sucesso());
        assertTrue(resultado.conflitos().isEmpty());
        verify(sincronizacaoRepository).save(any());
    }

    @Test
    void receberPayloadComRegistoMaisAntigoRegistaConflitoLastWriteWinsCentral() {
        UUID stockId = UUID.randomUUID();
        Stock stockCentral = mock(Stock.class);
        when(stockCentral.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 5, 23, 12, 0));
        when(stockCentral.getVersion()).thenReturn(2L);
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stockCentral));
        SincronizacaoPayload payload = new SincronizacaoPayload(
                loja.getId(),
                LocalDateTime.now(),
                null,
                Map.of("stock", List.of(new RegistoSincronizacao(
                        "STOCK",
                        stockId,
                        LocalDateTime.of(2026, 5, 23, 10, 0),
                        1
                ))),
                List.of()
        );

        var resultado = service.receber(payload);

        assertTrue(resultado.sucesso());
        assertFalse(resultado.conflitos().isEmpty());
        assertEquals("last-write-wins:central", resultado.conflitos().get(0).resolucao());
    }
}
