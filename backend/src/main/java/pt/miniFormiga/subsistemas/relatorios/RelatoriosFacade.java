package pt.miniFormiga.subsistemas.relatorios;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.EstadoSincronizacaoCodigo;
import pt.miniFormiga.domain.LinhaVenda;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.Sincronizacao;
import pt.miniFormiga.domain.Venda;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.repository.AlertaStockRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.SincronizacaoRepository;
import pt.miniFormiga.repository.VendaRepository;
import pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoPayload;
import pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.VendaRelatorioSync;
import pt.miniFormiga.subsistemas.stock.StockItem;
import pt.miniFormiga.subsistemas.stock.StockStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static pt.miniFormiga.subsistemas.relatorios.RelatoriosDtos.*;

@Service
@Transactional(readOnly = true)
public class RelatoriosFacade implements ISubRelatorios {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final VendaRepository vendaRepository;
    private final AlertaStockRepository alertaStockRepository;
    private final LojaRepository lojaRepository;
    private final StockStore stockStore;
    private final SincronizacaoRepository sincronizacaoRepository;
    private final ObjectMapper objectMapper;

    public RelatoriosFacade(VendaRepository vendaRepository,
                            AlertaStockRepository alertaStockRepository,
                            LojaRepository lojaRepository,
                            StockStore stockStore,
                            SincronizacaoRepository sincronizacaoRepository,
                            ObjectMapper objectMapper) {
        this.vendaRepository = vendaRepository;
        this.alertaStockRepository = alertaStockRepository;
        this.lojaRepository = lojaRepository;
        this.stockStore = stockStore;
        this.sincronizacaoRepository = sincronizacaoRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public DashboardResponse obterDashboard(RelatorioFiltro filtro) {
        RelatorioFiltro normalizado = normalizar(filtro);
        List<LinhaRelatorio> linhas = linhasDeVendas(buscarVendas(normalizado), normalizado);
        if (linhas.isEmpty()) {
            DashboardResponse sincronizado = dashboardSincronizado(normalizado);
            if (sincronizado != null) {
                return sincronizado;
            }
        }
        Resumo resumo = resumir(linhas);
        long totalLojas = normalizado.lojaId() == null ? lojaRepository.findAll().size() : 1;
        List<VendasPorLojaResponse> vendasPorLoja = vendasPorLoja(linhas);

        return new DashboardResponse(
                periodo(normalizado),
                dinheiro(resumo.comIva),
                dinheiro(resumo.iva),
                dinheiro(resumo.margem),
                resumo.numeroVendas(),
                vendasPorLoja.size(),
                totalLojas,
                alertasAtivos(normalizado).size(),
                media(resumo.comIva, resumo.numeroVendas()),
                vendasPorLoja
        );
    }

    private DashboardResponse dashboardSincronizado(RelatorioFiltro filtro) {
        List<SincronizacaoPayload> payloads = payloadsSincronizados(filtro);
        if (payloads.isEmpty()) {
            return null;
        }

        List<VendaRelatorioSync> linhasSync = filtrarVendasSincronizadas(payloads, filtro);
        if (!linhasSync.isEmpty()) {
            ResumoSync resumo = resumirSync(linhasSync);
            List<VendasPorLojaResponse> vendasPorLoja = vendasPorLojaSync(linhasSync);
            return new DashboardResponse(
                    periodo(filtro),
                    dinheiro(resumo.comIva),
                    dinheiro(resumo.iva),
                    dinheiro(resumo.margem),
                    resumo.numeroVendas(),
                    vendasPorLoja.size(),
                    totalLojasSincronizadas(filtro, payloads),
                    alertasAtivos(filtro).size(),
                    media(resumo.comIva, resumo.numeroVendas()),
                    vendasPorLoja
            );
        }

        if (filtro.categoriaId() != null || filtro.produtoId() != null || filtro.turno() != null) {
            return null;
        }

        List<DashboardResponse> dashboards = payloads.stream()
                .map(SincronizacaoPayload::dashboard)
                .filter(dashboard -> dashboard != null && periodoCompativel(filtro, dashboard.periodo()))
                .toList();
        if (dashboards.isEmpty()) {
            return null;
        }

        BigDecimal totalVendas = dashboards.stream().map(DashboardResponse::totalVendas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIva = dashboards.stream().map(DashboardResponse::totalIva).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal margem = dashboards.stream().map(DashboardResponse::margem).reduce(BigDecimal.ZERO, BigDecimal::add);
        long numeroVendas = dashboards.stream().mapToLong(DashboardResponse::numeroVendas).sum();
        List<VendasPorLojaResponse> vendasPorLoja = dashboards.stream()
                .flatMap(dashboard -> dashboard.vendasPorLoja().stream())
                .toList();

        return new DashboardResponse(
                periodo(filtro),
                dinheiro(totalVendas),
                dinheiro(totalIva),
                dinheiro(margem),
                numeroVendas,
                vendasPorLoja.size(),
                totalLojasSincronizadas(filtro, payloads),
                alertasAtivos(filtro).size(),
                media(totalVendas, numeroVendas),
                vendasPorLoja
        );
    }

    private long totalLojasSincronizadas(RelatorioFiltro filtro, List<SincronizacaoPayload> payloads) {
        if (filtro.lojaId() != null) {
            return 1;
        }
        long totalLojas = lojaRepository.findAll().size();
        long lojasPayload = payloads.stream()
                .map(SincronizacaoPayload::lojaId)
                .filter(id -> id != null)
                .distinct()
                .count();
        return lojasPayload > 0 ? lojasPayload : totalLojas;
    }

    private SincronizacaoPayload payload(Sincronizacao sincronizacao) {
        if (sincronizacao.getPayloadJson() == null || sincronizacao.getPayloadJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(sincronizacao.getPayloadJson(), SincronizacaoPayload.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public RelatorioVendasResponse relatorioVendas(RelatorioFiltro filtro) {
        RelatorioFiltro normalizado = normalizar(filtro);
        List<LinhaRelatorio> linhas = linhasDeVendas(buscarVendas(normalizado), normalizado);
        if (linhas.isEmpty()) {
            RelatorioVendasResponse sincronizado = relatorioVendasSincronizado(normalizado);
            if (sincronizado != null) {
                return sincronizado;
            }
        }
        Resumo resumo = resumir(linhas);

        return new RelatorioVendasResponse(
                periodo(normalizado),
                normalizado.lojaId(),
                normalizado.categoriaId(),
                normalizado.produtoId(),
                normalizado.turno(),
                dinheiro(resumo.semIva),
                dinheiro(resumo.iva),
                dinheiro(resumo.comIva),
                dinheiro(resumo.margem),
                resumo.numeroVendas(),
                vendasPorLoja(linhas),
                vendasPorDia(linhas),
                linhas.stream().map(this::linhaResponse).toList()
        );
    }

    @Override
    public RelatorioStockResponse relatorioStock(RelatorioFiltro filtro) {
        RelatorioFiltro normalizado = normalizar(filtro);
        List<StockItemResponse> itens = buscarStocks(normalizado).stream()
                .filter(item -> pertenceCategoria(item.produto(), normalizado.categoriaId()))
                .filter(item -> pertenceProduto(item.produto(), normalizado.produtoId()))
                .sorted(Comparator
                        .comparing((StockItem item) -> item.produto().getNome()))
                .map(this::stockResponse)
                .toList();

        long totalUnidades = itens.stream().mapToLong(StockItemResponse::quantidade).sum();
        long produtosReposicao = itens.stream().filter(StockItemResponse::precisaReposicao).count();
        BigDecimal valorStock = itens.stream()
                .map(StockItemResponse::valorPrecoCusto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RelatorioStockResponse(
                normalizado.lojaId(),
                normalizado.categoriaId(),
                itens.size(),
                totalUnidades,
                dinheiro(valorStock),
                produtosReposicao,
                alertasAtivos(normalizado).size(),
                itens
        );
    }

    @Override
    public RelatorioRentabilidadeResponse relatorioRentabilidade(RelatorioFiltro filtro) {
        RelatorioFiltro normalizado = normalizar(filtro);
        List<LinhaRelatorio> linhas = linhasDeVendas(buscarVendas(normalizado), normalizado);
        if (linhas.isEmpty()) {
            RelatorioRentabilidadeResponse sincronizado = relatorioRentabilidadeSincronizado(normalizado);
            if (sincronizado != null) {
                return sincronizado;
            }
        }
        Resumo resumo = resumir(linhas);

        return new RelatorioRentabilidadeResponse(
                periodo(normalizado),
                normalizado.lojaId(),
                normalizado.categoriaId(),
                normalizado.produtoId(),
                normalizado.turno(),
                dinheiro(resumo.semIva),
                dinheiro(resumo.custo),
                dinheiro(resumo.margem),
                percentagem(resumo.margem, resumo.semIva),
                rentabilidadePorProduto(linhas),
                rentabilidadePorCategoria(linhas)
        );
    }

    private RelatorioVendasResponse relatorioVendasSincronizado(RelatorioFiltro filtro) {
        List<VendaRelatorioSync> linhasSync = vendasSincronizadas(filtro);
        if (!linhasSync.isEmpty()) {
            List<LinhaVendaRelatorioResponse> linhas = linhasSync.stream()
                    .map(this::linhaResponse)
                    .toList();
            ResumoSync resumo = resumirSync(linhasSync);
            return new RelatorioVendasResponse(
                    periodo(filtro),
                    filtro.lojaId(),
                    filtro.categoriaId(),
                    filtro.produtoId(),
                    filtro.turno(),
                    dinheiro(resumo.semIva),
                    dinheiro(resumo.iva),
                    dinheiro(resumo.comIva),
                    dinheiro(resumo.margem),
                    resumo.numeroVendas(),
                    vendasPorLojaSync(linhasSync),
                    vendasPorDiaSync(linhasSync),
                    linhas
            );
        }
        if (filtro.categoriaId() != null || filtro.produtoId() != null || filtro.turno() != null) {
            return null;
        }
        DashboardResponse dashboard = dashboardSincronizado(filtro);
        if (dashboard == null || !periodoCompativel(filtro, dashboard.periodo())) {
            return null;
        }
        return new RelatorioVendasResponse(
                periodo(filtro),
                filtro.lojaId(),
                filtro.categoriaId(),
                filtro.produtoId(),
                filtro.turno(),
                dinheiro(dashboard.totalVendas().subtract(dashboard.totalIva())),
                dashboard.totalIva(),
                dashboard.totalVendas(),
                dashboard.margem(),
                dashboard.numeroVendas(),
                dashboard.vendasPorLoja(),
                List.of(),
                List.of()
        );
    }

    private RelatorioRentabilidadeResponse relatorioRentabilidadeSincronizado(RelatorioFiltro filtro) {
        List<VendaRelatorioSync> linhasSync = vendasSincronizadas(filtro);
        if (!linhasSync.isEmpty()) {
            ResumoSync resumo = resumirSync(linhasSync);
            return new RelatorioRentabilidadeResponse(
                    periodo(filtro),
                    filtro.lojaId(),
                    filtro.categoriaId(),
                    filtro.produtoId(),
                    filtro.turno(),
                    dinheiro(resumo.semIva),
                    dinheiro(resumo.custo),
                    dinheiro(resumo.margem),
                    percentagem(resumo.margem, resumo.semIva),
                    rentabilidadePorProdutoSync(linhasSync),
                    rentabilidadePorCategoriaSync(linhasSync)
            );
        }
        if (filtro.categoriaId() != null || filtro.produtoId() != null || filtro.turno() != null) {
            return null;
        }
        DashboardResponse dashboard = dashboardSincronizado(filtro);
        if (dashboard == null || !periodoCompativel(filtro, dashboard.periodo())) {
            return null;
        }
        BigDecimal receitaSemIva = dinheiro(dashboard.totalVendas().subtract(dashboard.totalIva()));
        BigDecimal custo = dinheiro(receitaSemIva.subtract(dashboard.margem()));
        return new RelatorioRentabilidadeResponse(
                periodo(filtro),
                filtro.lojaId(),
                filtro.categoriaId(),
                filtro.produtoId(),
                filtro.turno(),
                receitaSemIva,
                custo,
                dashboard.margem(),
                percentagem(dashboard.margem(), receitaSemIva),
                List.of(),
                dashboard.vendasPorLoja().stream()
                        .map(loja -> new RentabilidadeCategoriaResponse(
                                loja.loja(),
                                (int) loja.numeroVendas(),
                                dinheiro(loja.total().subtract(loja.iva())),
                                dinheiro(loja.total().subtract(loja.iva()).subtract(loja.margem())),
                                loja.margem(),
                                percentagem(loja.margem(), dinheiro(loja.total().subtract(loja.iva())))
                        ))
                        .toList()
        );
    }

    private boolean periodoCompativel(RelatorioFiltro filtro, PeriodoResponse periodoSincronizado) {
        if (periodoSincronizado == null || periodoSincronizado.inicio() == null || periodoSincronizado.fim() == null) {
            return false;
        }
        return !periodoSincronizado.fim().isBefore(filtro.inicio())
                && !periodoSincronizado.inicio().isAfter(filtro.fim());
    }

    private List<VendaRelatorioSync> vendasSincronizadas(RelatorioFiltro filtro) {
        return filtrarVendasSincronizadas(payloadsSincronizados(filtro), filtro);
    }

    private List<SincronizacaoPayload> payloadsSincronizados(RelatorioFiltro filtro) {
        List<EstadoSincronizacaoCodigo> estados = List.of(
                EstadoSincronizacaoCodigo.CONCLUIDA,
                EstadoSincronizacaoCodigo.COM_CONFLITOS
        );
        if (filtro.lojaId() != null) {
            return sincronizacaoRepository.findFirstByLojaIdAndEstadoInOrderByDataHoraFimDesc(filtro.lojaId(), estados)
                    .map(this::payload)
                    .stream()
                    .filter(payload -> payload != null)
                    .toList();
        }
        Set<UUID> lojasIncluidas = new LinkedHashSet<>();
        List<SincronizacaoPayload> payloads = new ArrayList<>();
        List<Sincronizacao> sincronizacoes = sincronizacaoRepository.findByEstadoInOrderByDataHoraFimDesc(estados);
        if (sincronizacoes == null) {
            return List.of();
        }
        for (Sincronizacao sincronizacao : sincronizacoes) {
            SincronizacaoPayload payload = payload(sincronizacao);
            UUID lojaId = payload != null ? payload.lojaId() : sincronizacao.getLoja() == null ? null : sincronizacao.getLoja().getId();
            if (lojaId == null || !lojasIncluidas.add(lojaId)) {
                continue;
            }
            if (payload != null) {
                payloads.add(payload);
            }
        }
        return payloads;
    }

    private List<VendaRelatorioSync> filtrarVendasSincronizadas(List<SincronizacaoPayload> payloads, RelatorioFiltro filtro) {
        return payloads.stream()
                .flatMap(payload -> (payload.vendasRelatorio() == null ? List.<VendaRelatorioSync>of() : payload.vendasRelatorio()).stream())
                .filter(linha -> linha != null
                        && !linha.dataHora().toLocalDate().isBefore(filtro.inicio())
                        && !linha.dataHora().toLocalDate().isAfter(filtro.fim())
                        && (filtro.lojaId() == null || filtro.lojaId().equals(linha.lojaId()))
                        && (filtro.categoriaId() == null || filtro.categoriaId().equals(linha.categoriaId()))
                        && (filtro.produtoId() == null || filtro.produtoId().equals(linha.produtoId()))
                        && pertenceTurno(linha.dataHora(), filtro.turno()))
                .sorted(Comparator.comparing(VendaRelatorioSync::dataHora).thenComparing(VendaRelatorioSync::produto))
                .toList();
    }

    @Override
    public ExportacaoRelatorio exportar(ExportarRelatorioRequest request) {
        String tipo = normalizarTipo(request.tipo());
        String formato = normalizarFormato(request.formato());
        RelatorioFiltro filtro = normalizar(new RelatorioFiltro(
                request.lojaId(),
                request.inicio(),
                request.fim(),
                request.categoriaId(),
                request.produtoId(),
                request.turno()
        ));
        byte[] conteudo = switch (formato) {
            case "CSV" -> csv(tipo, filtro);
            case "PDF" -> pdf(tipo, filtro);
            case "XLSX" -> xlsx(tipo, filtro);
            default -> throw new BusinessException("RELATORIO_FORMATO_INVALIDO", "Formato de exportacao invalido");
        };
        String extensao = formato.toLowerCase();
        String mediaType = switch (formato) {
            case "CSV" -> "text/csv;charset=UTF-8";
            case "PDF" -> "application/pdf";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
        return new ExportacaoRelatorio("mini-formiga-" + tipo.toLowerCase() + "." + extensao, mediaType, conteudo);
    }

    private RelatorioFiltro normalizar(RelatorioFiltro filtro) {
        RelatorioFiltro seguro = filtro == null ? new RelatorioFiltro(null, null, null, null) : filtro;
        LocalDate hoje = LocalDate.now();
        LocalDate fim = seguro.fim() == null ? hoje : seguro.fim();
        LocalDate inicio = seguro.inicio() == null ? fim.withDayOfMonth(1) : seguro.inicio();
        if (fim.isBefore(inicio)) {
            throw new BusinessException("PERIODO_INVALIDO", "Data final nao pode ser anterior a data inicial");
        }
        return new RelatorioFiltro(
                seguro.lojaId(),
                inicio,
                fim,
                seguro.categoriaId(),
                seguro.produtoId(),
                normalizarTurno(seguro.turno())
        );
    }

    private List<Venda> buscarVendas(RelatorioFiltro filtro) {
        LocalDateTime inicio = filtro.inicio().atStartOfDay();
        LocalDateTime fimExclusivo = filtro.fim().plusDays(1).atStartOfDay();
        if (filtro.lojaId() != null) {
            return vendaRepository.findByLojaIdAndAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(
                    filtro.lojaId(), inicio, fimExclusivo);
        }
        return vendaRepository.findByAnuladaFalseAndMeioPagamentoIsNotNullAndDataHoraBetween(inicio, fimExclusivo);
    }

    private List<StockItem> buscarStocks(RelatorioFiltro filtro) {
        return stockStore.listar(filtro.lojaId());
    }

    private List<AlertaStock> alertasAtivos(RelatorioFiltro filtro) {
        List<AlertaStock> alertas = alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc();
        return alertas.stream()
                .filter(alerta -> pertenceCategoria(alerta.getProduto(), filtro.categoriaId()))
                .filter(alerta -> pertenceProduto(alerta.getProduto(), filtro.produtoId()))
                .toList();
    }

    private List<LinhaRelatorio> linhasDeVendas(List<Venda> vendas, RelatorioFiltro filtro) {
        List<LinhaRelatorio> linhas = new ArrayList<>();
        for (Venda venda : vendas) {
            for (LinhaVenda linha : venda.getLinhas()) {
                if (linha.isAnulada()
                        || !pertenceCategoria(linha.getProduto(), filtro.categoriaId())
                        || !pertenceProduto(linha.getProduto(), filtro.produtoId())
                        || !pertenceTurno(venda.getDataHora(), filtro.turno())) {
                    continue;
                }
                linhas.add(linhaRelatorio(venda, linha));
            }
        }
        return linhas.stream()
                .sorted(Comparator.comparing(LinhaRelatorio::dataHora).thenComparing(linha -> linha.produto().getNome()))
                .toList();
    }

    private LinhaRelatorio linhaRelatorio(Venda venda, LinhaVenda linha) {
        Produto produto = linha.getProduto();
        BigDecimal valorSemIva = linha.getTotalLinha();
        BigDecimal iva = valorSemIva
                .multiply(produto.getTaxaIVA().getPercentagem())
                .divide(CEM, 4, RoundingMode.HALF_UP);
        BigDecimal custo = produto.getPrecoCusto()
                .multiply(BigDecimal.valueOf(linha.getQuantidade()))
                .setScale(2, RoundingMode.HALF_UP);
        return new LinhaRelatorio(
                venda.getId(),
                venda.getDataHora(),
                venda.getLoja(),
                produto,
                linha.getQuantidade(),
                valorSemIva,
                iva,
                valorSemIva.add(iva),
                custo,
                valorSemIva.subtract(custo)
        );
    }

    private boolean pertenceCategoria(Produto produto, UUID categoriaId) {
        return categoriaId == null || produto.getCategoria().getId().equals(categoriaId);
    }

    private boolean pertenceProduto(Produto produto, UUID produtoId) {
        return produtoId == null || produto.getId().equals(produtoId);
    }

    private String normalizarTurno(String turno) {
        if (turno == null || turno.isBlank()) {
            return null;
        }
        String valor = turno.trim().toUpperCase()
                .replace('Ã', 'A')
                .replace('Á', 'A')
                .replace('Â', 'A');
        if (!List.of("MANHA", "TARDE", "NOITE").contains(valor)) {
            throw new BusinessException("RELATORIO_TURNO_INVALIDO", "Turno de relatorio invalido");
        }
        return valor;
    }

    private boolean pertenceTurno(LocalDateTime dataHora, String turno) {
        if (turno == null || turno.isBlank()) {
            return true;
        }
        LocalTime hora = dataHora.toLocalTime();
        return switch (turno) {
            case "MANHA" -> !hora.isBefore(LocalTime.of(7, 30)) && hora.isBefore(LocalTime.of(14, 0));
            case "TARDE" -> !hora.isBefore(LocalTime.of(14, 0)) && !hora.isAfter(LocalTime.of(20, 0));
            case "NOITE" -> hora.isBefore(LocalTime.of(7, 30)) || hora.isAfter(LocalTime.of(20, 0));
            default -> true;
        };
    }

    private PeriodoResponse periodo(RelatorioFiltro filtro) {
        return new PeriodoResponse(filtro.inicio(), filtro.fim());
    }

    private Resumo resumir(List<LinhaRelatorio> linhas) {
        Resumo resumo = new Resumo();
        linhas.forEach(resumo::adicionar);
        return resumo;
    }

    private ResumoSync resumirSync(List<VendaRelatorioSync> linhas) {
        ResumoSync resumo = new ResumoSync();
        linhas.forEach(resumo::adicionar);
        return resumo;
    }

    private List<VendasPorLojaResponse> vendasPorLoja(List<LinhaRelatorio> linhas) {
        Map<UUID, Resumo> porLoja = new LinkedHashMap<>();
        Map<UUID, String> nomes = new LinkedHashMap<>();
        for (LinhaRelatorio linha : linhas) {
            UUID lojaId = linha.loja().getId();
            porLoja.computeIfAbsent(lojaId, id -> new Resumo()).adicionar(linha);
            nomes.putIfAbsent(lojaId, linha.loja().getNome());
        }
        return porLoja.entrySet().stream()
                .map(entry -> {
                    Resumo resumo = entry.getValue();
                    return new VendasPorLojaResponse(
                            entry.getKey(),
                            nomes.get(entry.getKey()),
                            dinheiro(resumo.comIva),
                            dinheiro(resumo.iva),
                            dinheiro(resumo.margem),
                            resumo.numeroVendas()
                    );
                })
                .toList();
    }

    private List<VendasPorDiaResponse> vendasPorDia(List<LinhaRelatorio> linhas) {
        Map<LocalDate, Resumo> porDia = new LinkedHashMap<>();
        for (LinhaRelatorio linha : linhas) {
            porDia.computeIfAbsent(linha.dataHora().toLocalDate(), data -> new Resumo()).adicionar(linha);
        }
        return porDia.entrySet().stream()
                .map(entry -> {
                    Resumo resumo = entry.getValue();
                    return new VendasPorDiaResponse(
                            entry.getKey(),
                            dinheiro(resumo.comIva),
                            dinheiro(resumo.iva),
                            dinheiro(resumo.margem),
                            resumo.numeroVendas()
                    );
                })
                .toList();
    }

    private List<VendasPorLojaResponse> vendasPorLojaSync(List<VendaRelatorioSync> linhas) {
        Map<UUID, ResumoSync> porLoja = new LinkedHashMap<>();
        Map<UUID, String> nomes = new LinkedHashMap<>();
        for (VendaRelatorioSync linha : linhas) {
            porLoja.computeIfAbsent(linha.lojaId(), id -> new ResumoSync()).adicionar(linha);
            nomes.putIfAbsent(linha.lojaId(), linha.loja());
        }
        return porLoja.entrySet().stream()
                .map(entry -> {
                    ResumoSync resumo = entry.getValue();
                    return new VendasPorLojaResponse(
                            entry.getKey(),
                            nomes.get(entry.getKey()),
                            dinheiro(resumo.comIva),
                            dinheiro(resumo.iva),
                            dinheiro(resumo.margem),
                            resumo.numeroVendas()
                    );
                })
                .toList();
    }

    private List<VendasPorDiaResponse> vendasPorDiaSync(List<VendaRelatorioSync> linhas) {
        Map<LocalDate, ResumoSync> porDia = new LinkedHashMap<>();
        for (VendaRelatorioSync linha : linhas) {
            porDia.computeIfAbsent(linha.dataHora().toLocalDate(), data -> new ResumoSync()).adicionar(linha);
        }
        return porDia.entrySet().stream()
                .map(entry -> {
                    ResumoSync resumo = entry.getValue();
                    return new VendasPorDiaResponse(
                            entry.getKey(),
                            dinheiro(resumo.comIva),
                            dinheiro(resumo.iva),
                            dinheiro(resumo.margem),
                            resumo.numeroVendas()
                    );
                })
                .toList();
    }

    private LinhaVendaRelatorioResponse linhaResponse(LinhaRelatorio linha) {
        return new LinhaVendaRelatorioResponse(
                linha.vendaId(),
                linha.dataHora(),
                linha.loja().getId(),
                linha.loja().getNome(),
                linha.produto().getId(),
                linha.produto().getNome(),
                linha.produto().getCategoria().getNome(),
                linha.quantidade(),
                dinheiro(linha.valorSemIva()),
                dinheiro(linha.iva()),
                dinheiro(linha.valorComIva()),
                dinheiro(linha.margem())
        );
    }

    private LinhaVendaRelatorioResponse linhaResponse(VendaRelatorioSync linha) {
        return new LinhaVendaRelatorioResponse(
                linha.vendaId(),
                linha.dataHora(),
                linha.lojaId(),
                linha.loja(),
                linha.produtoId(),
                linha.produto(),
                linha.categoria(),
                linha.quantidade(),
                dinheiro(linha.valorSemIva()),
                dinheiro(linha.iva()),
                dinheiro(linha.valorComIva()),
                dinheiro(linha.margem())
        );
    }

    private StockItemResponse stockResponse(StockItem item) {
        Produto produto = item.produto();
        BigDecimal valor = produto.getPrecoCusto()
                .multiply(BigDecimal.valueOf(item.quantidade()));
        UUID lojaId = item.lojaId();
        String loja = item.lojaNome() != null ? item.lojaNome() : lojaId == null ? null : lojaRepository.findById(lojaId).map(Loja::getNome).orElse(null);
        return new StockItemResponse(
                produto.getId(),
                produto.getNome(),
                produto.getCategoria().getNome(),
                lojaId,
                loja,
                item.quantidade(),
                item.nivelMinimo(),
                item.precisaReposicao(),
                dinheiro(valor),
                item.atualizadoEm()
        );
    }

    private List<RentabilidadeProdutoResponse> rentabilidadePorProduto(List<LinhaRelatorio> linhas) {
        Map<UUID, Resumo> porProduto = new LinkedHashMap<>();
        Map<UUID, Produto> produtos = new LinkedHashMap<>();
        for (LinhaRelatorio linha : linhas) {
            UUID produtoId = linha.produto().getId();
            porProduto.computeIfAbsent(produtoId, id -> new Resumo()).adicionar(linha);
            produtos.putIfAbsent(produtoId, linha.produto());
        }
        return porProduto.entrySet().stream()
                .map(entry -> {
                    Produto produto = produtos.get(entry.getKey());
                    Resumo resumo = entry.getValue();
                    return new RentabilidadeProdutoResponse(
                            entry.getKey(),
                            produto.getNome(),
                            produto.getCategoria().getNome(),
                            resumo.quantidade,
                            dinheiro(resumo.semIva),
                            dinheiro(resumo.custo),
                            dinheiro(resumo.margem),
                            percentagem(resumo.margem, resumo.semIva)
                    );
                })
                .sorted(Comparator.comparing(RentabilidadeProdutoResponse::margem).reversed())
                .toList();
    }

    private List<RentabilidadeCategoriaResponse> rentabilidadePorCategoria(List<LinhaRelatorio> linhas) {
        Map<String, Resumo> porCategoria = new LinkedHashMap<>();
        for (LinhaRelatorio linha : linhas) {
            porCategoria.computeIfAbsent(linha.produto().getCategoria().getNome(), categoria -> new Resumo()).adicionar(linha);
        }
        return porCategoria.entrySet().stream()
                .map(entry -> {
                    Resumo resumo = entry.getValue();
                    return new RentabilidadeCategoriaResponse(
                            entry.getKey(),
                            resumo.quantidade,
                            dinheiro(resumo.semIva),
                            dinheiro(resumo.custo),
                            dinheiro(resumo.margem),
                            percentagem(resumo.margem, resumo.semIva)
                    );
                })
                .sorted(Comparator.comparing(RentabilidadeCategoriaResponse::margem).reversed())
                .toList();
    }

    private List<RentabilidadeProdutoResponse> rentabilidadePorProdutoSync(List<VendaRelatorioSync> linhas) {
        Map<UUID, ResumoSync> porProduto = new LinkedHashMap<>();
        Map<UUID, VendaRelatorioSync> produtos = new LinkedHashMap<>();
        for (VendaRelatorioSync linha : linhas) {
            porProduto.computeIfAbsent(linha.produtoId(), id -> new ResumoSync()).adicionar(linha);
            produtos.putIfAbsent(linha.produtoId(), linha);
        }
        return porProduto.entrySet().stream()
                .map(entry -> {
                    VendaRelatorioSync produto = produtos.get(entry.getKey());
                    ResumoSync resumo = entry.getValue();
                    return new RentabilidadeProdutoResponse(
                            entry.getKey(),
                            produto.produto(),
                            produto.categoria(),
                            resumo.quantidade,
                            dinheiro(resumo.semIva),
                            dinheiro(resumo.custo),
                            dinheiro(resumo.margem),
                            percentagem(resumo.margem, resumo.semIva)
                    );
                })
                .sorted(Comparator.comparing(RentabilidadeProdutoResponse::margem).reversed())
                .toList();
    }

    private List<RentabilidadeCategoriaResponse> rentabilidadePorCategoriaSync(List<VendaRelatorioSync> linhas) {
        Map<String, ResumoSync> porCategoria = new LinkedHashMap<>();
        for (VendaRelatorioSync linha : linhas) {
            porCategoria.computeIfAbsent(linha.categoria(), categoria -> new ResumoSync()).adicionar(linha);
        }
        return porCategoria.entrySet().stream()
                .map(entry -> {
                    ResumoSync resumo = entry.getValue();
                    return new RentabilidadeCategoriaResponse(
                            entry.getKey(),
                            resumo.quantidade,
                            dinheiro(resumo.semIva),
                            dinheiro(resumo.custo),
                            dinheiro(resumo.margem),
                            percentagem(resumo.margem, resumo.semIva)
                    );
                })
                .sorted(Comparator.comparing(RentabilidadeCategoriaResponse::margem).reversed())
                .toList();
    }

    private byte[] csv(String tipo, RelatorioFiltro filtro) {
        StringBuilder csv = new StringBuilder();
        switch (tipo) {
            case "DASHBOARD" -> csvDashboard(csv, obterDashboard(filtro));
            case "VENDAS" -> csvVendas(csv, relatorioVendas(filtro));
            case "STOCK" -> csvStock(csv, relatorioStock(filtro));
            case "RENTABILIDADE" -> csvRentabilidade(csv, relatorioRentabilidade(filtro));
            default -> throw new BusinessException("RELATORIO_TIPO_INVALIDO", "Tipo de relatorio invalido");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(String tipo, RelatorioFiltro filtro) {
        List<String> linhas = switch (tipo) {
            case "DASHBOARD" -> linhasPdfDashboard(obterDashboard(filtro));
            case "VENDAS" -> linhasPdfVendas(relatorioVendas(filtro));
            case "STOCK" -> linhasPdfStock(relatorioStock(filtro));
            case "RENTABILIDADE" -> linhasPdfRentabilidade(relatorioRentabilidade(filtro));
            default -> throw new BusinessException("RELATORIO_TIPO_INVALIDO", "Tipo de relatorio invalido");
        };
        return pdfTexto(linhas);
    }

    private byte[] xlsx(String tipo, RelatorioFiltro filtro) {
        List<List<Object>> linhas = switch (tipo) {
            case "DASHBOARD" -> linhasXlsxDashboard(obterDashboard(filtro));
            case "VENDAS" -> linhasXlsxVendas(relatorioVendas(filtro));
            case "STOCK" -> linhasXlsxStock(relatorioStock(filtro));
            case "RENTABILIDADE" -> linhasXlsxRentabilidade(relatorioRentabilidade(filtro));
            default -> throw new BusinessException("RELATORIO_TIPO_INVALIDO", "Tipo de relatorio invalido");
        };
        return xlsxSimples(tipo.toLowerCase(), linhas);
    }

    private List<List<Object>> linhasXlsxDashboard(DashboardResponse response) {
        List<List<Object>> linhas = new ArrayList<>();
        linhas.add(List.of("indicador", "valor"));
        linhas.add(List.of("total_vendas", response.totalVendas()));
        linhas.add(List.of("total_iva", response.totalIva()));
        linhas.add(List.of("margem", response.margem()));
        linhas.add(List.of("numero_vendas", response.numeroVendas()));
        linhas.add(List.of("total_lojas", response.totalLojas()));
        linhas.add(List.of("alertas_ativos", response.alertasAtivos()));
        return linhas;
    }

    private List<List<Object>> linhasXlsxVendas(RelatorioVendasResponse response) {
        List<List<Object>> linhas = new ArrayList<>();
        linhas.add(List.of("data", "descricao", "valor", "iva", "loja"));
        for (LinhaVendaRelatorioResponse linha : response.linhas()) {
            linhas.add(List.of(
                    linha.dataHora(),
                    "Venda " + linha.vendaId() + " - " + linha.produto(),
                    linha.valorComIva(),
                    linha.iva(),
                    linha.loja()
            ));
        }
        return linhas;
    }

    private List<List<Object>> linhasXlsxStock(RelatorioStockResponse response) {
        List<List<Object>> linhas = new ArrayList<>();
        linhas.add(List.of("produto", "categoria", "loja", "quantidade", "nivel_minimo", "precisa_reposicao", "valor_stock"));
        for (StockItemResponse item : response.itens()) {
            linhas.add(List.of(
                    item.produto(),
                    item.categoria(),
                    item.loja() == null ? "" : item.loja(),
                    item.quantidade(),
                    item.nivelMinimo() == null ? "" : item.nivelMinimo(),
                    item.precisaReposicao(),
                    item.valorPrecoCusto()
            ));
        }
        return linhas;
    }

    private List<List<Object>> linhasXlsxRentabilidade(RelatorioRentabilidadeResponse response) {
        List<List<Object>> linhas = new ArrayList<>();
        linhas.add(List.of("tipo", "nome", "receita", "custo", "margem", "margem_percentagem"));
        for (RentabilidadeProdutoResponse produto : response.produtos()) {
            linhas.add(List.of("PRODUTO", produto.produto(), produto.receitaSemIva(), produto.custo(),
                    produto.margem(), produto.margemPercentagem()));
        }
        for (RentabilidadeCategoriaResponse categoria : response.categorias()) {
            linhas.add(List.of("CATEGORIA", categoria.categoria(), categoria.receitaSemIva(), categoria.custo(),
                    categoria.margem(), categoria.margemPercentagem()));
        }
        return linhas;
    }

    private void csvDashboard(StringBuilder csv, DashboardResponse response) {
        csv.append("indicador,valor\n");
        linhaCsv(csv, "total_vendas", response.totalVendas());
        linhaCsv(csv, "total_iva", response.totalIva());
        linhaCsv(csv, "margem", response.margem());
        linhaCsv(csv, "numero_vendas", response.numeroVendas());
        linhaCsv(csv, "total_lojas", response.totalLojas());
        linhaCsv(csv, "alertas_ativos", response.alertasAtivos());
    }

    private void csvVendas(StringBuilder csv, RelatorioVendasResponse response) {
        csv.append("data,descricao,valor,iva,loja\n");
        for (LinhaVendaRelatorioResponse linha : response.linhas()) {
            linhaCsv(csv,
                    linha.dataHora(),
                    "Venda " + linha.vendaId() + " - " + linha.produto(),
                    linha.valorComIva(),
                    linha.iva(),
                    linha.loja());
        }
    }

    private void csvStock(StringBuilder csv, RelatorioStockResponse response) {
        csv.append("produto,categoria,loja,quantidade,nivel_minimo,precisa_reposicao,valor_stock\n");
        for (StockItemResponse item : response.itens()) {
            linhaCsv(csv, item.produto(), item.categoria(), item.loja(), item.quantidade(),
                    item.nivelMinimo() == null ? "" : item.nivelMinimo(), item.precisaReposicao(), item.valorPrecoCusto());
        }
    }

    private void csvRentabilidade(StringBuilder csv, RelatorioRentabilidadeResponse response) {
        csv.append("tipo,nome,receita,custo,margem,margem_percentagem\n");
        for (RentabilidadeProdutoResponse produto : response.produtos()) {
            linhaCsv(csv, "PRODUTO", produto.produto(), produto.receitaSemIva(), produto.custo(),
                    produto.margem(), produto.margemPercentagem());
        }
        for (RentabilidadeCategoriaResponse categoria : response.categorias()) {
            linhaCsv(csv, "CATEGORIA", categoria.categoria(), categoria.receitaSemIva(), categoria.custo(),
                    categoria.margem(), categoria.margemPercentagem());
        }
    }

    private String resumoDashboard(DashboardResponse response) {
        return "Total vendas: " + response.totalVendas()
                + "\nIVA: " + response.totalIva()
                + "\nMargem: " + response.margem()
                + "\nVendas: " + response.numeroVendas()
                + "\nAlertas ativos: " + response.alertasAtivos();
    }

    private String resumoVendas(RelatorioVendasResponse response) {
        return "Periodo: " + response.periodo().inicio() + " a " + response.periodo().fim()
                + "\nTotal: " + response.totalComIva()
                + "\nIVA: " + response.totalIva()
                + "\nVendas: " + response.numeroVendas();
    }

    private String resumoStock(RelatorioStockResponse response) {
        return "Produtos: " + response.totalProdutos()
                + "\nUnidades: " + response.totalUnidades()
                + "\nValor stock: " + response.valorStockPrecoCusto()
                + "\nAlertas ativos: " + response.alertasAtivos();
    }

    private String resumoRentabilidade(RelatorioRentabilidadeResponse response) {
        return "Receita: " + response.receitaSemIva()
                + "\nCusto: " + response.custoTotal()
                + "\nMargem: " + response.margemTotal()
                + "\nMargem %: " + response.margemPercentagem();
    }

    private List<String> linhasPdfDashboard(DashboardResponse response) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Mini-Formiga - Dashboard");
        linhas.add("Periodo: " + response.periodo().inicio() + " a " + response.periodo().fim());
        linhas.add("Total vendas: " + response.totalVendas());
        linhas.add("IVA: " + response.totalIva());
        linhas.add("Margem: " + response.margem());
        linhas.add("Vendas: " + response.numeroVendas());
        linhas.add("Alertas ativos: " + response.alertasAtivos());
        response.vendasPorLoja().stream()
                .limit(30)
                .forEach(loja -> linhas.add(loja.loja() + " | vendas " + loja.numeroVendas() + " | total " + loja.total()));
        return linhas;
    }

    private List<String> linhasPdfVendas(RelatorioVendasResponse response) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Mini-Formiga - Relatorio de vendas");
        linhas.add("Periodo: " + response.periodo().inicio() + " a " + response.periodo().fim());
        linhas.add("Loja: " + (response.lojaId() == null ? "Todas" : response.lojaId()));
        linhas.add("Categoria: " + (response.categoriaId() == null ? "Todas" : response.categoriaId()));
        linhas.add("Produto: " + (response.produtoId() == null ? "Todos" : response.produtoId()));
        linhas.add("Turno: " + (response.turno() == null ? "Todos" : response.turno()));
        linhas.add("Total: " + response.totalComIva() + " | IVA: " + response.totalIva() + " | Vendas: " + response.numeroVendas());
        linhas.add("Data | Loja | Produto | Qtd | Total | Margem");
        if (response.linhas().isEmpty() && response.vendasPorLoja().isEmpty()) {
            linhas.add("Sem vendas no periodo selecionado.");
        }
        if (response.linhas().isEmpty() && !response.vendasPorLoja().isEmpty()) {
            response.vendasPorLoja().stream()
                    .limit(45)
                    .forEach(loja -> linhas.add(loja.loja()
                            + " | vendas " + loja.numeroVendas()
                            + " | total " + loja.total()
                            + " | iva " + loja.iva()
                            + " | margem " + loja.margem()));
        }
        response.linhas().stream()
                .limit(45)
                .forEach(linha -> linhas.add(linha.dataHora().toLocalDate()
                        + " | " + linha.loja()
                        + " | " + linha.produto()
                        + " | " + linha.quantidade()
                        + " | " + linha.valorComIva()
                        + " | " + linha.margem()));
        return linhas;
    }

    private List<String> linhasPdfStock(RelatorioStockResponse response) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Mini-Formiga - Relatorio de stock");
        linhas.add("Loja: " + (response.lojaId() == null ? "Todas" : response.lojaId()));
        linhas.add("Categoria: " + (response.categoriaId() == null ? "Todas" : response.categoriaId()));
        linhas.add("Produtos: " + response.totalProdutos() + " | Unidades: " + response.totalUnidades() + " | Valor: " + response.valorStockPrecoCusto());
        linhas.add("Produto | Categoria | Loja | Unidades | Valor | Estado");
        if (response.itens().isEmpty()) {
            linhas.add("Sem stock para apresentar.");
        }
        response.itens().stream()
                .limit(45)
                .forEach(item -> linhas.add(item.produto()
                        + " | " + item.categoria()
                        + " | " + item.loja()
                        + " | " + item.quantidade()
                        + " | " + item.valorPrecoCusto()
                        + " | " + (item.precisaReposicao() ? "Reposicao" : "OK")));
        return linhas;
    }

    private List<String> linhasPdfRentabilidade(RelatorioRentabilidadeResponse response) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Mini-Formiga - Relatorio de rentabilidade");
        linhas.add("Periodo: " + response.periodo().inicio() + " a " + response.periodo().fim());
        linhas.add("Loja: " + (response.lojaId() == null ? "Todas" : response.lojaId()));
        linhas.add("Categoria: " + (response.categoriaId() == null ? "Todas" : response.categoriaId()));
        linhas.add("Produto: " + (response.produtoId() == null ? "Todos" : response.produtoId()));
        linhas.add("Turno: " + (response.turno() == null ? "Todos" : response.turno()));
        linhas.add("Receita: " + response.receitaSemIva() + " | Custo: " + response.custoTotal() + " | Margem: " + response.margemTotal());
        linhas.add("Produto | Categoria | Qtd | Receita | Custo | Margem");
        if (response.produtos().isEmpty()) {
            linhas.add("Sem rentabilidade para apresentar.");
        }
        response.produtos().stream()
                .limit(45)
                .forEach(produto -> linhas.add(produto.produto()
                        + " | " + produto.categoria()
                        + " | " + produto.quantidadeVendida()
                        + " | " + produto.receitaSemIva()
                        + " | " + produto.custo()
                        + " | " + produto.margem()));
        return linhas;
    }

    private byte[] xlsxSimples(String nomeFolha, List<List<Object>> linhas) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            entradaZip(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """);
            entradaZip(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            entradaZip(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="%s" sheetId="1" r:id="rId1"/>
                      </sheets>
                    </workbook>
                    """.formatted(escaparXml(nomeFolha)));
            entradaZip(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """);
            entradaZip(zip, "xl/worksheets/sheet1.xml", folhaXlsx(linhas));
            zip.finish();
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("RELATORIO_XLSX_INVALIDO", "Nao foi possivel gerar XLSX");
        }
    }

    private void entradaZip(ZipOutputStream zip, String nome, String conteudo) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        zip.write(conteudo.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String folhaXlsx(List<List<Object>> linhas) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);
        for (int rowIndex = 0; rowIndex < linhas.size(); rowIndex++) {
            int excelRow = rowIndex + 1;
            xml.append("<row r=\"").append(excelRow).append("\">");
            List<Object> linha = linhas.get(rowIndex);
            for (int colIndex = 0; colIndex < linha.size(); colIndex++) {
                xml.append("<c r=\"")
                        .append(colunaExcel(colIndex))
                        .append(excelRow)
                        .append("\" t=\"inlineStr\"><is><t>")
                        .append(escaparXml(linha.get(colIndex)))
                        .append("</t></is></c>");
            }
            xml.append("</row>");
        }
        xml.append("""
                  </sheetData>
                </worksheet>
                """);
        return xml.toString();
    }

    private String colunaExcel(int indice) {
        StringBuilder coluna = new StringBuilder();
        int valor = indice;
        do {
            coluna.insert(0, (char) ('A' + (valor % 26)));
            valor = valor / 26 - 1;
        } while (valor >= 0);
        return coluna.toString();
    }

    private String escaparXml(Object valor) {
        return valor == null ? "" : valor.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private byte[] pdfTexto(List<String> linhas) {
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 10 Tf\n50 790 Td\n14 TL\n");
        linhas.stream()
                .limit(55)
                .forEach(linha -> stream.append('(').append(escaparPdf(linha)).append(") Tj\nT*\n"));
        stream.append("ET\n");

        byte[] streamBytes = stream.toString().getBytes(StandardCharsets.UTF_8);
        List<String> objetos = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + streamBytes.length + " >>\nstream\n" + stream + "endstream"
        );

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.UTF_8).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objetos.get(i)).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.UTF_8).length;
        pdf.append("xref\n0 ").append(objetos.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        offsets.forEach(offset -> pdf.append(String.format("%010d 00000 n \n", offset)));
        pdf.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escaparPdf(String valor) {
        return valor == null ? "" : valor
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String normalizarTipo(String tipo) {
        String valor = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!List.of("DASHBOARD", "VENDAS", "STOCK", "RENTABILIDADE").contains(valor)) {
            throw new BusinessException("RELATORIO_TIPO_INVALIDO", "Tipo de relatorio invalido");
        }
        return valor;
    }

    private String normalizarFormato(String formato) {
        String valor = formato == null ? "" : formato.trim().toUpperCase();
        if (!List.of("CSV", "PDF", "XLSX").contains(valor)) {
            throw new BusinessException("RELATORIO_FORMATO_INVALIDO", "Formato de exportacao invalido");
        }
        return valor;
    }

    private void linhaCsv(StringBuilder csv, Object... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escaparCsv(valores[i]));
        }
        csv.append('\n');
    }

    private String escaparCsv(Object valor) {
        String texto = valor == null ? "" : valor.toString();
        if (texto.contains(",") || texto.contains("\"") || texto.contains("\n")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }

    private BigDecimal dinheiro(BigDecimal valor) {
        return valor == null ? ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal media(BigDecimal total, long quantidade) {
        if (quantidade == 0) {
            return ZERO;
        }
        return total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentagem(BigDecimal margem, BigDecimal receita) {
        if (receita == null || receita.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO;
        }
        return margem.divide(receita, 4, RoundingMode.HALF_UP)
                .multiply(CEM)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private record LinhaRelatorio(UUID vendaId,
                                  LocalDateTime dataHora,
                                  Loja loja,
                                  Produto produto,
                                  int quantidade,
                                  BigDecimal valorSemIva,
                                  BigDecimal iva,
                                  BigDecimal valorComIva,
                                  BigDecimal custo,
                                  BigDecimal margem) {
    }

    private static final class Resumo {
        private BigDecimal semIva = BigDecimal.ZERO;
        private BigDecimal iva = BigDecimal.ZERO;
        private BigDecimal comIva = BigDecimal.ZERO;
        private BigDecimal custo = BigDecimal.ZERO;
        private BigDecimal margem = BigDecimal.ZERO;
        private int quantidade;
        private final Set<UUID> vendas = new LinkedHashSet<>();

        private void adicionar(LinhaRelatorio linha) {
            semIva = semIva.add(linha.valorSemIva());
            iva = iva.add(linha.iva());
            comIva = comIva.add(linha.valorComIva());
            custo = custo.add(linha.custo());
            margem = margem.add(linha.margem());
            quantidade += linha.quantidade();
            vendas.add(linha.vendaId());
        }

        private long numeroVendas() {
            return vendas.size();
        }
    }

    private static final class ResumoSync {
        private BigDecimal semIva = BigDecimal.ZERO;
        private BigDecimal iva = BigDecimal.ZERO;
        private BigDecimal comIva = BigDecimal.ZERO;
        private BigDecimal custo = BigDecimal.ZERO;
        private BigDecimal margem = BigDecimal.ZERO;
        private int quantidade;
        private final Set<UUID> vendas = new LinkedHashSet<>();

        private void adicionar(VendaRelatorioSync linha) {
            semIva = semIva.add(linha.valorSemIva());
            iva = iva.add(linha.iva());
            comIva = comIva.add(linha.valorComIva());
            custo = custo.add(linha.custo());
            margem = margem.add(linha.margem());
            quantidade += linha.quantidade();
            vendas.add(linha.vendaId());
        }

        private long numeroVendas() {
            return vendas.size();
        }
    }
}
