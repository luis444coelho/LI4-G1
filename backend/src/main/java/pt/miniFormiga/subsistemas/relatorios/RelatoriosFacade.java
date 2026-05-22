package pt.miniFormiga.subsistemas.relatorios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.LinhaVenda;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.Stock;
import pt.miniFormiga.domain.Venda;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.repository.AlertaStockRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.StockRepository;
import pt.miniFormiga.repository.VendaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.relatorios.RelatoriosDtos.*;

@Service
@Transactional(readOnly = true)
public class RelatoriosFacade implements ISubRelatorios {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final VendaRepository vendaRepository;
    private final StockRepository stockRepository;
    private final AlertaStockRepository alertaStockRepository;
    private final LojaRepository lojaRepository;

    public RelatoriosFacade(VendaRepository vendaRepository,
                            StockRepository stockRepository,
                            AlertaStockRepository alertaStockRepository,
                            LojaRepository lojaRepository) {
        this.vendaRepository = vendaRepository;
        this.stockRepository = stockRepository;
        this.alertaStockRepository = alertaStockRepository;
        this.lojaRepository = lojaRepository;
    }

    @Override
    public DashboardResponse obterDashboard(RelatorioFiltro filtro) {
        RelatorioFiltro normalizado = normalizar(filtro);
        List<LinhaRelatorio> linhas = linhasDeVendas(buscarVendas(normalizado), normalizado.categoriaId());
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

    @Override
    public RelatorioVendasResponse relatorioVendas(RelatorioFiltro filtro) {
        RelatorioFiltro normalizado = normalizar(filtro);
        List<LinhaRelatorio> linhas = linhasDeVendas(buscarVendas(normalizado), normalizado.categoriaId());
        Resumo resumo = resumir(linhas);

        return new RelatorioVendasResponse(
                periodo(normalizado),
                normalizado.lojaId(),
                normalizado.categoriaId(),
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
                .filter(stock -> pertenceCategoria(stock.getProduto(), normalizado.categoriaId()))
                .sorted(Comparator
                        .comparing((Stock stock) -> stock.getLoja().getNome())
                        .thenComparing(stock -> stock.getProduto().getNome()))
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
        List<LinhaRelatorio> linhas = linhasDeVendas(buscarVendas(normalizado), normalizado.categoriaId());
        Resumo resumo = resumir(linhas);

        return new RelatorioRentabilidadeResponse(
                periodo(normalizado),
                normalizado.lojaId(),
                normalizado.categoriaId(),
                dinheiro(resumo.semIva),
                dinheiro(resumo.custo),
                dinheiro(resumo.margem),
                percentagem(resumo.margem, resumo.semIva),
                rentabilidadePorProduto(linhas),
                rentabilidadePorCategoria(linhas)
        );
    }

    @Override
    public ExportacaoRelatorio exportar(ExportarRelatorioRequest request) {
        String tipo = normalizarTipo(request.tipo());
        String formato = normalizarFormato(request.formato());
        RelatorioFiltro filtro = normalizar(new RelatorioFiltro(request.lojaId(), request.inicio(), request.fim(), request.categoriaId()));
        byte[] conteudo = switch (formato) {
            case "CSV" -> csv(tipo, filtro);
            case "PDF" -> pdf(tipo, filtro);
            default -> throw new BusinessException("RELATORIO_FORMATO_INVALIDO", "Formato de exportacao invalido");
        };
        String extensao = formato.toLowerCase();
        String mediaType = "CSV".equals(formato) ? "text/csv;charset=UTF-8" : "application/pdf";
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
        return new RelatorioFiltro(seguro.lojaId(), inicio, fim, seguro.categoriaId());
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

    private List<Stock> buscarStocks(RelatorioFiltro filtro) {
        if (filtro.lojaId() != null) {
            return stockRepository.findByLojaId(filtro.lojaId());
        }
        return stockRepository.findAll();
    }

    private List<AlertaStock> alertasAtivos(RelatorioFiltro filtro) {
        List<AlertaStock> alertas = filtro.lojaId() == null
                ? alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc()
                : alertaStockRepository.findByStockLojaIdAndResolvidoFalseOrderByDataHoraDesc(filtro.lojaId());
        return alertas.stream()
                .filter(alerta -> pertenceCategoria(alerta.getStock().getProduto(), filtro.categoriaId()))
                .toList();
    }

    private List<LinhaRelatorio> linhasDeVendas(List<Venda> vendas, UUID categoriaId) {
        List<LinhaRelatorio> linhas = new ArrayList<>();
        for (Venda venda : vendas) {
            for (LinhaVenda linha : venda.getLinhas()) {
                if (linha.isAnulada() || !pertenceCategoria(linha.getProduto(), categoriaId)) {
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

    private PeriodoResponse periodo(RelatorioFiltro filtro) {
        return new PeriodoResponse(filtro.inicio(), filtro.fim());
    }

    private Resumo resumir(List<LinhaRelatorio> linhas) {
        Resumo resumo = new Resumo();
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

    private StockItemResponse stockResponse(Stock stock) {
        BigDecimal valor = stock.getProduto().getPrecoCusto()
                .multiply(BigDecimal.valueOf(stock.getQuantidade()));
        return new StockItemResponse(
                stock.getProduto().getId(),
                stock.getProduto().getNome(),
                stock.getProduto().getCategoria().getNome(),
                stock.getLoja().getId(),
                stock.getLoja().getNome(),
                stock.getQuantidade(),
                stock.getNivelMinimo() == null ? null : stock.getNivelMinimo().getQuantidade(),
                stock.precisaReposicao(),
                dinheiro(valor),
                stock.getDataUltimaAtualizacao()
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
        String texto = switch (tipo) {
            case "DASHBOARD" -> resumoDashboard(obterDashboard(filtro));
            case "VENDAS" -> resumoVendas(relatorioVendas(filtro));
            case "STOCK" -> resumoStock(relatorioStock(filtro));
            case "RENTABILIDADE" -> resumoRentabilidade(relatorioRentabilidade(filtro));
            default -> throw new BusinessException("RELATORIO_TIPO_INVALIDO", "Tipo de relatorio invalido");
        };
        String conteudo = "%PDF-1.4\n"
                + "% Mini-Formiga\n"
                + "Relatorio " + tipo + "\n"
                + texto
                + "\n%%EOF\n";
        return conteudo.getBytes(StandardCharsets.UTF_8);
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

    private String normalizarTipo(String tipo) {
        String valor = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!List.of("DASHBOARD", "VENDAS", "STOCK", "RENTABILIDADE").contains(valor)) {
            throw new BusinessException("RELATORIO_TIPO_INVALIDO", "Tipo de relatorio invalido");
        }
        return valor;
    }

    private String normalizarFormato(String formato) {
        String valor = formato == null ? "" : formato.trim().toUpperCase();
        if (!List.of("CSV", "PDF").contains(valor)) {
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
}
