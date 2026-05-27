package pt.miniFormiga.subsistemas.sincronizacao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.subsistemas.stock.repository.AjusteInventarioRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.EntradaMercadoriaRepository;
import pt.miniFormiga.subsistemas.pdv.repository.FaturaRepository;
import pt.miniFormiga.subsistemas.pdv.repository.FechoCaixaRepository;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;
import pt.miniFormiga.subsistemas.stock.repository.StockProdutoLojaRepository;
import pt.miniFormiga.subsistemas.catalogo.repository.ProdutoRepository;
import pt.miniFormiga.subsistemas.sincronizacao.repository.SincronizacaoRepository;
import pt.miniFormiga.subsistemas.pdv.repository.VendaRepository;

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
import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.RegistoSincronizacao;
import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.SincronizacaoPayload;

class ServidorCentralSincronizacaoServiceTest {

    private SincronizacaoRepository sincronizacaoRepository;
    private LojaRepository lojaRepository;
    private ProdutoRepository produtoRepository;
    private StockProdutoLojaRepository stockProdutoLojaRepository;
    private ServidorCentralSincronizacaoService service;
    private Loja loja;

    @BeforeEach
    void setUp() {
        sincronizacaoRepository = mock(SincronizacaoRepository.class);
        lojaRepository = mock(LojaRepository.class);
        VendaRepository vendaRepository = mock(VendaRepository.class);
        FaturaRepository faturaRepository = mock(FaturaRepository.class);
        produtoRepository = mock(ProdutoRepository.class);
        stockProdutoLojaRepository = mock(StockProdutoLojaRepository.class);
        AjusteInventarioRepository ajusteRepository = mock(AjusteInventarioRepository.class);
        FechoCaixaRepository fechoRepository = mock(FechoCaixaRepository.class);
        EntradaMercadoriaRepository entradaRepository = mock(EntradaMercadoriaRepository.class);
        service = new ServidorCentralSincronizacaoService(
                sincronizacaoRepository,
                lojaRepository,
                vendaRepository,
                faturaRepository,
                produtoRepository,
                stockProdutoLojaRepository,
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
                null,
                List.of()
        );

        var resultado = service.receber(payload);

        assertTrue(resultado.sucesso());
        assertTrue(resultado.conflitos().isEmpty());
        verify(sincronizacaoRepository).save(any());
    }

    @Test
    void receberPayloadDeLojaAindaNaoRegistadaCriaLojaERegistaSincronizacao() {
        UUID lojaId = UUID.randomUUID();
        when(lojaRepository.findById(lojaId)).thenReturn(Optional.empty());
        when(lojaRepository.save(any(Loja.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SincronizacaoPayload payload = new SincronizacaoPayload(
                lojaId,
                LocalDateTime.now(),
                null,
                Map.of("fechos", List.of()),
                null,
                List.of()
        );

        var resultado = service.receber(payload);

        assertTrue(resultado.sucesso());
        verify(lojaRepository).save(any(Loja.class));
        verify(sincronizacaoRepository).save(any());
    }

    @Test
    void receberPayloadComRegistoMaisAntigoRegistaConflitoLastWriteWinsCentral() {
        UUID stockId = UUID.randomUUID();
        Produto stockCentral = mock(Produto.class);
        when(stockCentral.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 5, 23, 12, 0));
        when(stockCentral.getVersion()).thenReturn(2L);
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(stockProdutoLojaRepository.findById(stockId)).thenReturn(Optional.empty());
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
                null,
                List.of()
        );

        var resultado = service.receber(payload);

        assertTrue(resultado.sucesso());
        assertFalse(resultado.conflitos().isEmpty());
        assertEquals("last-write-wins:central", resultado.conflitos().get(0).resolucao());
    }
}
