package pt.miniFormiga.subsistemas.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.MotivoAjuste;
import pt.miniFormiga.domain.NivelMinimo;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.Stock;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.repository.AlertaStockRepository;
import pt.miniFormiga.repository.InventarioFisicoRepository;
import pt.miniFormiga.repository.LinhaInventarioRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.MotivoAjusteRepository;
import pt.miniFormiga.repository.NivelMinimoRepository;
import pt.miniFormiga.repository.ProdutoRepository;
import pt.miniFormiga.repository.StockRepository;
import pt.miniFormiga.repository.UtilizadorRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StockFacade implements ISubStock {

    private static final List<String> PERFIS_GESTORES_ALERTA = List.of("GESTOR");
    private static final List<String> PERFIS_LOJA_ALERTA = List.of("GERENTE");

    private final StockRepository stockRepository;
    private final NivelMinimoRepository nivelMinimoRepository;
    private final AlertaStockRepository alertaStockRepository;
    private final AjusteInventarioRepository ajusteInventarioRepository;
    private final MotivoAjusteRepository motivoAjusteRepository;
    private final InventarioFisicoRepository inventarioFisicoRepository;
    private final LinhaInventarioRepository linhaInventarioRepository;
    private final LojaRepository lojaRepository;
    private final ProdutoRepository produtoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuditoriaService auditoria;

    public StockFacade(StockRepository stockRepository,
                       NivelMinimoRepository nivelMinimoRepository,
                       AlertaStockRepository alertaStockRepository,
                       AjusteInventarioRepository ajusteInventarioRepository,
                       MotivoAjusteRepository motivoAjusteRepository,
                       InventarioFisicoRepository inventarioFisicoRepository,
                       LinhaInventarioRepository linhaInventarioRepository,
                       LojaRepository lojaRepository,
                       ProdutoRepository produtoRepository,
                       UtilizadorRepository utilizadorRepository,
                       AuditoriaService auditoria) {
        this.stockRepository = stockRepository;
        this.nivelMinimoRepository = nivelMinimoRepository;
        this.alertaStockRepository = alertaStockRepository;
        this.ajusteInventarioRepository = ajusteInventarioRepository;
        this.motivoAjusteRepository = motivoAjusteRepository;
        this.inventarioFisicoRepository = inventarioFisicoRepository;
        this.linhaInventarioRepository = linhaInventarioRepository;
        this.lojaRepository = lojaRepository;
        this.produtoRepository = produtoRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockDTO> consultarStock(UUID lojaId) {
        return stockRepository.findByLojaId(lojaId).stream()
                .map(stock -> new StockDTO(
                        stock.getProduto().getId(),
                        stock.getLoja().getId(),
                        stock.getQuantidade(),
                        stock.getNivelMinimo() == null ? null : stock.getNivelMinimo().getQuantidade(),
                        stock.precisaReposicao()
                ))
                .toList();
    }

    @Override
    public void atualizarStock(UUID produtoId, UUID lojaId, int delta) {
        Stock stock = obterStock(produtoId, lojaId);
        int novaQuantidade = stock.getQuantidade() + delta;
        if (novaQuantidade < 0) {
            throw new StockInsuficienteException(produtoId, stock.getQuantidade(), Math.abs(delta));
        }
        stock.atualizarQuantidade(delta);
        emitirAlertaSeNecessario(stock);
    }

    @Override
    public void definirNivelMinimo(UUID produtoId, UUID lojaId, int quantidade) {
        if (quantidade < 0) {
            throw new BusinessException("NIVEL_MINIMO_INVALIDO", "Nivel minimo nao pode ser negativo");
        }
        Stock stock = obterStock(produtoId, lojaId);
        NivelMinimo nivelMinimo = nivelMinimoRepository.findByStockId(stock.getId())
                .orElseGet(() -> new NivelMinimo(stock, quantidade));
        nivelMinimo.atualizarQuantidade(quantidade);
        nivelMinimoRepository.save(nivelMinimo);
        emitirAlertaSeNecessario(stock);
    }

    @Override
    public AjusteInventario registarAjuste(UUID produtoId, UUID lojaId, int quantidade, String motivo, UUID utilizadorId) {
        Stock stock = obterStock(produtoId, lojaId);
        MotivoAjuste motivoAjuste = motivoAjusteRepository.findByCodigo(motivo)
                .orElseThrow(() -> new BusinessException("MOTIVO_AJUSTE_INVALIDO", "Motivo de ajuste invalido"));
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador", utilizadorId));

        atualizarStock(produtoId, lojaId, quantidade);
        AjusteInventario ajuste = ajusteInventarioRepository.save(
                new AjusteInventario(stock, motivoAjuste, utilizador, quantidade, motivo)
        );
        auditoria.registar(TipoOperacao.AJUSTE_STOCK, utilizadorId, "STOCK", "Ajuste de stock registado");
        return ajuste;
    }

    @Override
    public InventarioFisico iniciarInventarioFisico(UUID lojaId, UUID utilizadorId) {
        if (inventarioFisicoRepository.existsByLojaIdAndFechadoFalse(lojaId)) {
            throw new BusinessException("INVENTARIO_JA_ABERTO", "Ja existe um inventario fisico aberto para esta loja");
        }
        Loja loja = lojaRepository.findById(lojaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja", lojaId));
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador", utilizadorId));
        InventarioFisico inventario = new InventarioFisico(loja, utilizador);
        stockRepository.findByLojaId(lojaId).forEach(stock ->
                new LinhaInventario(inventario, stock.getProduto(), 0, stock.getQuantidade()));
        inventario.calcularDiscrepancias();
        return inventarioFisicoRepository.save(inventario);
    }

    @Override
    public LinhaInventario registarContagemLinha(UUID inventarioId, UUID produtoId, int quantidade) {
        if (quantidade < 0) {
            throw new BusinessException("QUANTIDADE_CONTAGEM_INVALIDA", "Quantidade contada nao pode ser negativa");
        }
        InventarioFisico inventario = inventarioFisicoRepository.findById(inventarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InventarioFisico", inventarioId));
        if (inventario.isFechado()) {
            throw new BusinessException("INVENTARIO_FECHADO", "Inventario fisico ja esta fechado");
        }
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
        Stock stock = obterStock(produtoId, inventario.getLoja().getId());
        LinhaInventario linha = linhaInventarioRepository.findByInventarioIdAndProdutoId(inventarioId, produtoId)
                .map(existente -> {
                    existente.atualizarQuantidadeContada(quantidade);
                    return existente;
                })
                .orElseGet(() -> new LinhaInventario(inventario, produto, quantidade, stock.getQuantidade()));
        inventario.calcularDiscrepancias();
        return linhaInventarioRepository.save(linha);
    }

    @Override
    public void fecharInventario(UUID inventarioId) {
        InventarioFisico inventario = inventarioFisicoRepository.findById(inventarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InventarioFisico", inventarioId));
        if (inventario.isFechado()) {
            throw new BusinessException("INVENTARIO_FECHADO", "Inventario fisico ja esta fechado");
        }
        inventario.fechar();
        auditoria.registar(TipoOperacao.INVENTARIO_FECHADO,
                inventario.getResponsavel().getId(), "INVENTARIO_FISICO", "Inventario fisico fechado");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaStock> getAlertasAtivos(UUID lojaId) {
        return alertaStockRepository.findByStockLojaIdAndResolvidoFalseOrderByDataHoraDesc(lojaId);
    }

    @Override
    public AlertaStock marcarAlertaLido(UUID alertaId) {
        AlertaStock alerta = alertaStockRepository.findById(alertaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("AlertaStock", alertaId));
        alerta.marcarComoLido();
        return alertaStockRepository.save(alerta);
    }

    @Override
    public AlertaStock resolverAlerta(UUID alertaId) {
        AlertaStock alerta = alertaStockRepository.findById(alertaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("AlertaStock", alertaId));
        alerta.resolver();
        return alertaStockRepository.save(alerta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinhaInventario> listarDiscrepanciasInventario(UUID inventarioId) {
        return linhaInventarioRepository.findByInventarioIdAndDiscrepanciaNot(inventarioId, 0);
    }

    private Stock obterStock(UUID produtoId, UUID lojaId) {
        return stockRepository.findByProdutoIdAndLojaId(produtoId, lojaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Stock", produtoId));
    }

    private void emitirAlertaSeNecessario(Stock stock) {
        if (stock.precisaReposicao() && !alertaStockRepository.existsByStockIdAndResolvidoFalse(stock.getId())) {
            AlertaStock alerta = new AlertaStock(stock, stock.getQuantidade());
            destinatariosAlerta(stock).forEach(alerta::adicionarDestinatario);
            alertaStockRepository.save(alerta);
        }
    }

    private List<Utilizador> destinatariosAlerta(Stock stock) {
        List<Utilizador> destinatarios = new java.util.ArrayList<>();
        List<Utilizador> gestores = utilizadorRepository.findByAtivoTrueAndPerfilNomeIn(PERFIS_GESTORES_ALERTA);
        if (gestores != null) {
            destinatarios.addAll(gestores);
        }
        List<Utilizador> gerentes = utilizadorRepository.findByAtivoTrueAndLojaIdAndPerfilNomeIn(
                stock.getLoja().getId(), PERFIS_LOJA_ALERTA);
        if (gerentes != null) {
            destinatarios.addAll(gerentes);
        }
        return destinatarios.stream().distinct().toList();
    }
}
