package pt.miniFormiga.subsistemas.sincronizacao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.EntidadeBase;
import pt.miniFormiga.domain.EstadoSincronizacao;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Sincronizacao;
import pt.miniFormiga.exception.BusinessException;
import pt.miniFormiga.exception.RecursoNaoEncontradoException;
import pt.miniFormiga.repository.AjusteInventarioRepository;
import pt.miniFormiga.repository.EntradaMercadoriaRepository;
import pt.miniFormiga.repository.EstadoSincronizacaoRepository;
import pt.miniFormiga.repository.FaturaRepository;
import pt.miniFormiga.repository.FechoCaixaRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.SincronizacaoRepository;
import pt.miniFormiga.repository.StockRepository;
import pt.miniFormiga.repository.VendaRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.ConflitoSincronizacaoResponse;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.RegistoSincronizacao;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoPayload;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoResponse;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoTransporte.ResultadoTransmissao;

@Service
@Transactional
public class SincronizacaoFacade implements ISubSincronizacao {

    private static final String PENDENTE = "PENDENTE";
    private static final String EM_CURSO = "EM_CURSO";
    private static final String CONCLUIDA = "CONCLUIDA";
    private static final String COM_CONFLITOS = "COM_CONFLITOS";

    private final SincronizacaoRepository sincronizacaoRepository;
    private final EstadoSincronizacaoRepository estadoRepository;
    private final LojaRepository lojaRepository;
    private final VendaRepository vendaRepository;
    private final FaturaRepository faturaRepository;
    private final StockRepository stockRepository;
    private final AjusteInventarioRepository ajusteRepository;
    private final FechoCaixaRepository fechoRepository;
    private final EntradaMercadoriaRepository entradaRepository;
    private final SincronizacaoTransporte transporte;
    private final ObjectMapper objectMapper;

    @Value("${mini-formiga.audit.file:logs/audit-mini-formiga.jsonl}")
    private String auditFile;

    public SincronizacaoFacade(SincronizacaoRepository sincronizacaoRepository,
                               EstadoSincronizacaoRepository estadoRepository,
                               LojaRepository lojaRepository,
                               VendaRepository vendaRepository,
                               FaturaRepository faturaRepository,
                               StockRepository stockRepository,
                               AjusteInventarioRepository ajusteRepository,
                               FechoCaixaRepository fechoRepository,
                               EntradaMercadoriaRepository entradaRepository,
                               SincronizacaoTransporte transporte,
                               ObjectMapper objectMapper) {
        this.sincronizacaoRepository = sincronizacaoRepository;
        this.estadoRepository = estadoRepository;
        this.lojaRepository = lojaRepository;
        this.vendaRepository = vendaRepository;
        this.faturaRepository = faturaRepository;
        this.stockRepository = stockRepository;
        this.ajusteRepository = ajusteRepository;
        this.fechoRepository = fechoRepository;
        this.entradaRepository = entradaRepository;
        this.transporte = transporte;
        this.objectMapper = objectMapper;
    }

    @Override
    public void agendarSincronizacao(UUID lojaId) {
        Loja loja = lojaRepository.findById(lojaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja", lojaId));
        sincronizacaoRepository.findFirstByLojaIdAndEstadoCodigoOrderByDataHoraInicioDesc(lojaId, PENDENTE)
                .orElseGet(() -> sincronizacaoRepository.save(new Sincronizacao(loja, estado(PENDENTE, "Pendente"))));
    }

    @Override
    public SincronizacaoResponse iniciarSincronizacao(UUID lojaId) {
        Sincronizacao sincronizacao = sincronizacaoRepository
                .findFirstByLojaIdAndEstadoCodigoOrderByDataHoraInicioDesc(lojaId, PENDENTE)
                .orElseGet(() -> {
                    agendarSincronizacao(lojaId);
                    return sincronizacaoRepository.findFirstByLojaIdAndEstadoCodigoOrderByDataHoraInicioDesc(lojaId, PENDENTE)
                            .orElseThrow(() -> new BusinessException("SINCRONIZACAO_NAO_AGENDADA", "Nao foi possivel agendar sincronizacao"));
                });

        SincronizacaoPayload payload = construirPayload(lojaId);
        String payloadJson = toJson(payload);
        sincronizacao.iniciar(estado(EM_CURSO, "Em curso"), payloadJson, payload.quantidadeRegistos());

        ResultadoTransmissao resultado = transporte.transmitir(payload);
        if (!resultado.sucesso()) {
            sincronizacao.falharMantendoPendente(
                    estado(PENDENTE, "Pendente"),
                    payloadJson,
                    payload.quantidadeRegistos(),
                    resultado.mensagemErro() == null ? "Falha de comunicacao com servidor central" : resultado.mensagemErro(),
                    LocalDateTime.now().plusMinutes(15)
            );
            return SincronizacaoResponse.from(sincronizacaoRepository.save(sincronizacao));
        }

        int conflitos = resultado.conflitos() == null ? 0 : resultado.conflitos().size();
        sincronizacao.concluir(
                conflitos == 0 ? estado(CONCLUIDA, "Concluida") : estado(COM_CONFLITOS, "Concluida com conflitos"),
                payloadJson,
                payload.quantidadeRegistos(),
                toJson(resultado.conflitos() == null ? List.of() : resultado.conflitos()),
                conflitos
        );
        return SincronizacaoResponse.from(sincronizacaoRepository.save(sincronizacao));
    }

    @Override
    @Transactional(readOnly = true)
    public SincronizacaoResponse estadoAtual(UUID lojaId) {
        return sincronizacaoRepository.findFirstByLojaIdOrderByDataHoraInicioDesc(lojaId)
                .map(SincronizacaoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sincronizacao", lojaId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SincronizacaoResponse> historico(UUID lojaId, Pageable pageable) {
        return sincronizacaoRepository.findByLojaIdOrderByDataHoraInicioDesc(lojaId, pageable)
                .map(SincronizacaoResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflitoSincronizacaoResponse> conflitos(UUID lojaId) {
        return sincronizacaoRepository.findByLojaIdAndConflitosResolvidosGreaterThanOrderByDataHoraInicioDesc(lojaId, 0)
                .stream()
                .map(ConflitoSincronizacaoResponse::from)
                .toList();
    }

    private SincronizacaoPayload construirPayload(UUID lojaId) {
        LocalDateTime desde = sincronizacaoRepository
                .findFirstByLojaIdAndEstadoCodigoInOrderByDataHoraFimDesc(lojaId, List.of(CONCLUIDA, COM_CONFLITOS))
                .map(Sincronizacao::getDataHoraFim)
                .orElse(null);
        Predicate<EntidadeBase> pendente = entidade -> desde == null || entidade.getUpdatedAt() == null || entidade.getUpdatedAt().isAfter(desde);

        Map<String, List<RegistoSincronizacao>> registos = new LinkedHashMap<>();
        registos.put("vendas", vendaRepository.findByLojaIdAndDataHoraBetween(lojaId,
                        java.time.LocalDate.of(1970, 1, 1).atStartOfDay(), java.time.LocalDate.now().plusDays(1).atStartOfDay(), Pageable.unpaged())
                .getContent().stream().filter(pendente).map(entidade -> registo("VENDA", entidade)).toList());
        registos.put("faturas", faturaRepository.findAll().stream()
                .filter(fatura -> fatura.getVenda().getLoja().getId().equals(lojaId))
                .filter(pendente)
                .map(entidade -> registo("FATURA", entidade))
                .toList());
        registos.put("stock", stockRepository.findByLojaId(lojaId).stream().filter(pendente).map(entidade -> registo("STOCK", entidade)).toList());
        registos.put("ajustes", ajusteRepository.findByStockLojaId(lojaId, Pageable.unpaged()).getContent().stream()
                .filter(pendente).map(entidade -> registo("AJUSTE_STOCK", entidade)).toList());
        registos.put("fechos", fechoRepository.findByLojaId(lojaId, Pageable.unpaged()).getContent().stream()
                .filter(pendente).map(entidade -> registo("FECHO_CAIXA", entidade)).toList());
        registos.put("entradasMercadoria", entradaRepository.findByLojaId(lojaId, Pageable.unpaged()).getContent().stream()
                .filter(pendente).map(entidade -> registo("ENTRADA_MERCADORIA", entidade)).toList());

        return new SincronizacaoPayload(lojaId, LocalDateTime.now(), desde, registos, logsAuditoria());
    }

    private RegistoSincronizacao registo(String tipo, EntidadeBase entidade) {
        return new RegistoSincronizacao(tipo, entidade.getId(), entidade.getUpdatedAt(), entidade.getVersion());
    }

    private EstadoSincronizacao estado(String codigo, String descricao) {
        return estadoRepository.findByCodigo(codigo)
                .orElseGet(() -> estadoRepository.save(new EstadoSincronizacao(codigo, descricao)));
    }

    private List<String> logsAuditoria() {
        if (auditFile == null || auditFile.isBlank()) {
            return List.of();
        }
        Path path = Path.of(auditFile);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return List.of();
        }
        try {
            List<String> linhas = Files.readAllLines(path);
            int inicio = Math.max(0, linhas.size() - 500);
            return linhas.subList(inicio, linhas.size());
        } catch (IOException e) {
            return List.of();
        }
    }

    private String toJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new BusinessException("SINCRONIZACAO_JSON_INVALIDO", "Nao foi possivel serializar payload de sincronizacao");
        }
    }
}
