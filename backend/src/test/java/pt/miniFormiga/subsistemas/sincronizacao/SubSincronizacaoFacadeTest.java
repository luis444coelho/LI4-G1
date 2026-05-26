package pt.miniFormiga.subsistemas.sincronizacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import pt.miniFormiga.domain.EstadoSincronizacao;
import pt.miniFormiga.domain.EstadoSincronizacaoCodigo;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Sincronizacao;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.repository.EntradaMercadoriaRepository;
import pt.miniFormiga.repository.FaturaRepository;
import pt.miniFormiga.repository.FechoCaixaRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.SincronizacaoRepository;
import pt.miniFormiga.repository.VendaRepository;
import pt.miniFormiga.subsistemas.relatorios.ISubRelatorios;
import pt.miniFormiga.subsistemas.relatorios.RelatoriosDtos.DashboardResponse;
import pt.miniFormiga.subsistemas.relatorios.RelatoriosDtos.PeriodoResponse;
import pt.miniFormiga.subsistemas.relatorios.RelatoriosDtos.VendasPorLojaResponse;
import pt.miniFormiga.subsistemas.stock.StockStore;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.ConflitoSincronizacaoResponse;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoResponse;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoTransporte.Conflito;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoTransporte.ResultadoTransmissao;

class SubSincronizacaoFacadeTest {

    private SincronizacaoRepository sincronizacaoRepository;
    private LojaRepository lojaRepository;
    private VendaRepository vendaRepository;
    private FaturaRepository faturaRepository;
    private StockStore stockStore;
    private AjusteInventarioRepository ajusteRepository;
    private FechoCaixaRepository fechoRepository;
    private EntradaMercadoriaRepository entradaRepository;
    private ISubRelatorios relatorios;
    private SincronizacaoTransporte transporte;
    private SubSincronizacaoFacade facade;
    private Loja loja;

    @BeforeEach
    void setUp() {
        sincronizacaoRepository = mock(SincronizacaoRepository.class);
        lojaRepository = mock(LojaRepository.class);
        vendaRepository = mock(VendaRepository.class);
        faturaRepository = mock(FaturaRepository.class);
        stockStore = mock(StockStore.class);
        ajusteRepository = mock(AjusteInventarioRepository.class);
        fechoRepository = mock(FechoCaixaRepository.class);
        entradaRepository = mock(EntradaMercadoriaRepository.class);
        relatorios = mock(ISubRelatorios.class);
        transporte = mock(SincronizacaoTransporte.class);
        facade = new SubSincronizacaoFacade(
                sincronizacaoRepository,
                lojaRepository,
                vendaRepository,
                faturaRepository,
                stockStore,
                ajusteRepository,
                fechoRepository,
                entradaRepository,
                relatorios,
                transporte,
                new ObjectMapper().findAndRegisterModules()
        );
        loja = new Loja("Loja Sync", "Rua Sync", "123456789");
    }

    @Test
    void agendarSincronizacaoCriaPendente() {
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(sincronizacaoRepository.findFirstByLojaIdAndEstadoOrderByDataHoraInicioDesc(loja.getId(), EstadoSincronizacaoCodigo.PENDENTE))
                .thenReturn(Optional.empty());
        when(sincronizacaoRepository.save(any(Sincronizacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.agendarSincronizacao(loja.getId());

        verify(sincronizacaoRepository).save(any(Sincronizacao.class));
    }

    @Test
    void iniciarSincronizacaoSemRedeMantemPendente() {
        Sincronizacao pendente = new Sincronizacao(loja, new EstadoSincronizacao("PENDENTE", "Pendente"));
        prepararPendentesVazios(pendente);
        when(transporte.transmitir(any())).thenReturn(ResultadoTransmissao.falha("sem rede"));
        when(sincronizacaoRepository.save(any(Sincronizacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SincronizacaoResponse response = facade.iniciarSincronizacao(loja.getId());

        assertEquals("PENDENTE", response.estado());
        assertEquals("sem rede", response.mensagemErro());
        assertNotNull(response.proximaTentativa());
    }

    @Test
    void iniciarSincronizacaoComSucessoMarcaConcluida() {
        Sincronizacao pendente = new Sincronizacao(loja, new EstadoSincronizacao("PENDENTE", "Pendente"));
        prepararPendentesVazios(pendente);
        when(transporte.transmitir(any())).thenReturn(ResultadoTransmissao.concluida());
        when(sincronizacaoRepository.save(any(Sincronizacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SincronizacaoResponse response = facade.iniciarSincronizacao(loja.getId());

        assertEquals("CONCLUIDA", response.estado());
        assertEquals(0, response.conflitosResolvidos());
        assertNotNull(response.fim());
    }

    @Test
    void conflitoAplicaLastWriteWinsERegistaConsulta() {
        Sincronizacao pendente = new Sincronizacao(loja, new EstadoSincronizacao("PENDENTE", "Pendente"));
        prepararPendentesVazios(pendente);
        when(transporte.transmitir(any())).thenReturn(ResultadoTransmissao.sucessoComConflitos(List.of(
                new Conflito("STOCK", UUID.randomUUID(), "quantidade", "last-write-wins", "5", "4")
        )));
        when(sincronizacaoRepository.save(any(Sincronizacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SincronizacaoResponse response = facade.iniciarSincronizacao(loja.getId());

        assertEquals("COM_CONFLITOS", response.estado());
        assertEquals(1, response.conflitosResolvidos());
    }

    @Test
    void payloadIncluiLogsDeAuditoriaRelevantes() throws Exception {
        Path auditFile = Files.createTempFile("audit-mini-formiga", ".jsonl");
        Files.write(auditFile, List.of("{\"tipoOperacao\":\"FECHO_CAIXA\"}"));
        ReflectionTestUtils.setField(facade, "auditFile", auditFile.toString());
        Sincronizacao pendente = new Sincronizacao(loja, new EstadoSincronizacao("PENDENTE", "Pendente"));
        prepararPendentesVazios(pendente);
        when(transporte.transmitir(any())).thenReturn(ResultadoTransmissao.concluida());
        when(sincronizacaoRepository.save(any(Sincronizacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facade.iniciarSincronizacao(loja.getId());

        verify(transporte).transmitir(argThat(payload ->
                payload.logsAuditoria().size() == 1 && payload.logsAuditoria().get(0).contains("FECHO_CAIXA")));
    }

    @Test
    void historicoListaSincronizacoesAnteriores() {
        Sincronizacao sync = new Sincronizacao(loja, new EstadoSincronizacao("CONCLUIDA", "Concluida"));
        when(sincronizacaoRepository.findByLojaIdOrderByDataHoraInicioDesc(eq(loja.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sync)));

        assertEquals(1, facade.historico(loja.getId(), Pageable.unpaged()).getTotalElements());
    }

    @Test
    void endpointConflitosListaConflitosDetetados() {
        Sincronizacao sync = new Sincronizacao(loja, new EstadoSincronizacao("PENDENTE", "Pendente"));
        sync.concluir(new EstadoSincronizacao("COM_CONFLITOS", "Com conflitos"), "{}", 0, "[{}]", 1);
        when(sincronizacaoRepository.findByLojaIdAndConflitosResolvidosGreaterThanOrderByDataHoraInicioDesc(loja.getId(), 0))
                .thenReturn(List.of(sync));

        List<ConflitoSincronizacaoResponse> conflitos = facade.conflitos(loja.getId());

        assertEquals(1, conflitos.size());
        assertEquals(1, conflitos.get(0).conflitosResolvidos());
    }

    private void prepararPendentesVazios(Sincronizacao pendente) {
        when(sincronizacaoRepository.findFirstByLojaIdAndEstadoOrderByDataHoraInicioDesc(loja.getId(), EstadoSincronizacaoCodigo.PENDENTE))
                .thenReturn(Optional.of(pendente));
        when(sincronizacaoRepository.findFirstByLojaIdAndEstadoInOrderByDataHoraFimDesc(eq(loja.getId()), any()))
                .thenReturn(Optional.empty());
        when(vendaRepository.findByLojaIdAndDataHoraBetween(eq(loja.getId()), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(faturaRepository.findAll()).thenReturn(List.of());
        when(stockStore.listar(loja.getId())).thenReturn(List.of());
        when(ajusteRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(fechoRepository.findByLojaId(eq(loja.getId()), any())).thenReturn(new PageImpl<>(List.of()));
        when(fechoRepository.findByLojaIdAndConfirmadoTrueAndDataBetween(eq(loja.getId()), any(), any()))
                .thenReturn(List.of());
        when(entradaRepository.findByLojaId(eq(loja.getId()), any())).thenReturn(new PageImpl<>(List.of()));
        when(relatorios.obterDashboard(any())).thenReturn(new DashboardResponse(
                new PeriodoResponse(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                1,
                0,
                BigDecimal.ZERO,
                List.<VendasPorLojaResponse>of()
        ));
    }
}
