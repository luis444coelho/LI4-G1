package pt.miniFormiga.subsistemas.sincronizacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.repository.EntradaMercadoriaRepository;
import pt.miniFormiga.repository.FaturaRepository;
import pt.miniFormiga.repository.FechoCaixaRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.ProdutoLojaRepository;
import pt.miniFormiga.repository.ProdutoRepository;
import pt.miniFormiga.repository.SincronizacaoRepository;
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
    private LojaRepository lojaRepository;
    private ProdutoRepository produtoRepository;
    private ProdutoLojaRepository produtoLojaRepository;
    private ServidorCentralSincronizacaoService service;
    private Loja loja;

    @BeforeEach
    void setUp() {
        sincronizacaoRepository = mock(SincronizacaoRepository.class);
        lojaRepository = mock(LojaRepository.class);
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FaturaRepository faturaRepository = mock(FaturaRepository.class);
        produtoRepository = mock(ProdutoRepository.class);
        produtoLojaRepository = mock(ProdutoLojaRepository.class);
        AjusteInventarioRepository ajusteRepository = mock(AjusteInventarioRepository.class);
        FechoCaixaRepository fechoRepository = mock(FechoCaixaRepository.class);
        EntradaMercadoriaRepository entradaRepository = mock(EntradaMercadoriaRepository.class);
        service = new ServidorCentralSincronizacaoService(
                sincronizacaoRepository,
                lojaRepository,
                vendaRepository,
                faturaRepository,
                produtoRepository,
                produtoLojaRepository,
                ajusteRepository,
                fechoRepository,
                entradaRepository,
                new ObjectMapper().findAndRegisterModules()
        );
        loja = new Loja("Central", "Rua Central", "123456789");
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
        Produto stockCentral = mock(Produto.class);
        when(stockCentral.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 5, 23, 12, 0));
        when(stockCentral.getVersion()).thenReturn(2L);
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(produtoLojaRepository.findById(stockId)).thenReturn(Optional.empty());
        when(produtoRepository.findById(stockId)).thenReturn(Optional.of(stockCentral));
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
