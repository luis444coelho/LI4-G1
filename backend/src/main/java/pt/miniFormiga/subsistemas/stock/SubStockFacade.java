package pt.miniFormiga.subsistemas.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.subsistemas.auditoria.ISubAuditoria;
import pt.miniFormiga.domain.AjusteInventario;
import pt.miniFormiga.domain.AlertaStock;
import pt.miniFormiga.domain.InventarioFisico;
import pt.miniFormiga.domain.LinhaInventario;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.MotivoAjusteCodigo;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.StockProdutoLoja;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.exception.StockInsuficienteException;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.repository.AlertaStockRepository;
import pt.miniFormiga.repository.InventarioFisicoRepository;
import pt.miniFormiga.repository.LinhaInventarioRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.UtilizadorRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SubStockFacade implements ISubStock {

    private final AlertaStockRepository alertaStockRepository;
    private final AjusteInventarioRepository ajusteInventarioRepository;
    private final InventarioFisicoRepository inventarioFisicoRepository;
    private final LinhaInventarioRepository linhaInventarioRepository;
    private final LojaRepository lojaRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final ISubAuditoria auditoria;
    private final StockStore stockStore;

    public SubStockFacade(AlertaStockRepository alertaStockRepository,
                       AjusteInventarioRepository ajusteInventarioRepository,
                       InventarioFisicoRepository inventarioFisicoRepository,
                       LinhaInventarioRepository linhaInventarioRepository,
                       LojaRepository lojaRepository,
                       UtilizadorRepository utilizadorRepository,
                       ISubAuditoria auditoria,
                       StockStore stockStore) {
        this.alertaStockRepository = alertaStockRepository;
        this.ajusteInventarioRepository = ajusteInventarioRepository;
        this.inventarioFisicoRepository = inventarioFisicoRepository;
        this.linhaInventarioRepository = linhaInventarioRepository;
        this.lojaRepository = lojaRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.auditoria = auditoria;
        this.stockStore = stockStore;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockDTO> consultarStock(UUID lojaId) {
        return stockStore.listar(lojaId).stream()
                .map(item -> new StockDTO(
                        item.produtoId(),
                        item.lojaId(),
                        item.quantidade(),
                        item.nivelMinimo(),
                        item.precisaReposicao()
                ))
                .toList();
    }

    @Override
    public void atualizarStock(UUID produtoId, UUID lojaId, int delta) {
        StockItem item = stockStore.atualizarStock(produtoId, lojaId, delta);
        emitirAlertaSeNecessario(item);
    }

    @Override
    public void definirNivelMinimo(UUID produtoId, UUID lojaId, int quantidade) {
        if (quantidade < 0) {
            throw new BusinessException("NIVEL_MINIMO_INVALIDO", "Nivel minimo nao pode ser negativo");
        }
        StockItem item = stockStore.definirNivelMinimo(produtoId, lojaId, quantidade);
        emitirAlertaSeNecessario(item);
    }

    @Override
    public AjusteInventario registarAjuste(UUID produtoId, UUID lojaId, int quantidade, String motivo, UUID utilizadorId) {
        MotivoAjusteCodigo motivoAjuste = motivo(motivo);
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador", utilizadorId));

        atualizarStock(produtoId, lojaId, quantidade);
        StockItem item = stockStore.obter(produtoId, lojaId);
        AjusteInventario ajuste = ajusteInventarioRepository.save(criarAjuste(item, motivoAjuste, utilizador, quantidade, motivo));
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
        stockStore.listar(lojaId).forEach(item ->
                new LinhaInventario(inventario, item.produto(), 0, item.quantidade()));
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
        StockItem item = stockStore.obter(produtoId, inventario.getLoja().getId());
        LinhaInventario linha = linhaInventarioRepository.findByInventarioIdAndProdutoId(inventarioId, produtoId)
                .map(existente -> {
                    existente.atualizarQuantidadeContada(quantidade);
                    return existente;
                })
                .orElseGet(() -> new LinhaInventario(inventario, item.produto(), quantidade, item.quantidade()));
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
        UUID lojaId = inventario.getLoja().getId();
        inventario.getLinhas().forEach(linha -> {
            StockItem item = stockStore.obter(linha.getProduto().getId(), lojaId);
            int delta = linha.getQuantidadeContada() - item.quantidade();
            atualizarStock(linha.getProduto().getId(), lojaId, delta);
            linha.consolidarComStockAtualizado();
        });
        inventario.fechar();
        auditoria.registar(TipoOperacao.INVENTARIO_FECHADO,
                inventario.getResponsavel().getId(), "INVENTARIO_FISICO", "Inventario fisico fechado");
    }

    @Override
    public List<AlertaStock> getAlertasAtivos(UUID lojaId) {
        if (lojaId != null) {
            stockStore.listar(lojaId).forEach(this::emitirAlertaSeNecessarioAoListar);
            return alertaStockRepository.findAtivosByLojaId(lojaId);
        }
        return alertaStockRepository.findByResolvidoFalseOrderByDataHoraDesc();
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
        return linhaInventarioRepository.findByInventarioId(inventarioId).stream()
                .peek(LinhaInventario::calcularDiscrepancia)
                .filter(linha -> linha.getDiscrepancia() != 0)
                .toList();
    }

    private void emitirAlertaSeNecessario(StockItem item) {
        boolean alertaAberto = item.stockProdutoLoja() == null
                ? alertaStockRepository.existsByProdutoIdAndResolvidoFalse(item.produtoId())
                : alertaStockRepository.existsByStockProdutoLojaIdAndResolvidoFalse(item.stockProdutoLoja().getId());
        if (item.precisaReposicao() && !alertaAberto) {
            alertaStockRepository.save(criarAlerta(item));
        }
    }

    private void emitirAlertaSeNecessarioAoListar(StockItem item) {
        boolean alertaExistente = item.stockProdutoLoja() == null
                ? alertaStockRepository.existsByProdutoIdAndResolvidoFalse(item.produtoId())
                    || alertaStockRepository.existsByProdutoIdAndResolvidoTrue(item.produtoId())
                : alertaStockRepository.existsByStockProdutoLojaIdAndResolvidoFalse(item.stockProdutoLoja().getId())
                    || alertaStockRepository.existsByStockProdutoLojaIdAndResolvidoTrue(item.stockProdutoLoja().getId());
        if (item.precisaReposicao() && !alertaExistente) {
            alertaStockRepository.save(criarAlerta(item));
        }
    }

    private AlertaStock criarAlerta(StockItem item) {
        if (item.stockProdutoLoja() != null) {
            return new AlertaStock(item.stockProdutoLoja(), item.quantidade());
        }
        Loja loja = item.lojaId() == null
                ? null
                : lojaRepository.findById(item.lojaId()).orElse(null);
        return loja == null
                ? new AlertaStock(item.produto(), item.quantidade())
                : new AlertaStock(item.produto(), loja, item.quantidade());
    }

    private AjusteInventario criarAjuste(StockItem item,
                                         MotivoAjusteCodigo motivo,
                                         Utilizador utilizador,
                                         int quantidade,
                                         String observacoes) {
        StockProdutoLoja stockProdutoLoja = item.stockProdutoLoja();
        if (stockProdutoLoja != null) {
            return new AjusteInventario(stockProdutoLoja, motivo, utilizador, quantidade, observacoes);
        }
        Produto produto = item.produto();
        return new AjusteInventario(produto, motivo, utilizador, quantidade, observacoes);
    }

    private MotivoAjusteCodigo motivo(String codigo) {
        try {
            return MotivoAjusteCodigo.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("MOTIVO_AJUSTE_INVALIDO", "Motivo de ajuste invalido");
        }
    }

}
