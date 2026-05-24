package pt.miniFormiga.subsistemas.sincronizacao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.EntidadeBase;
import pt.miniFormiga.domain.EstadoSincronizacaoCodigo;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Sincronizacao;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.RegistoSincronizacao;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoDtos.SincronizacaoPayload;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoTransporte.Conflito;
import static pt.miniFormiga.subsistemas.sincronizacao.SincronizacaoTransporte.ResultadoTransmissao;

@Service
@Profile("central")
@Transactional
public class ServidorCentralSincronizacaoService {

    private static final String CONCLUIDA = "CONCLUIDA";
    private static final String COM_CONFLITOS = "COM_CONFLITOS";

    private final SincronizacaoRepository sincronizacaoRepository;
    private final LojaRepository lojaRepository;
    private final VendaRepository vendaRepository;
    private final FaturaRepository faturaRepository;
    private final ProdutoRepository produtoRepository;
    private final ProdutoLojaRepository produtoLojaRepository;
    private final AjusteInventarioRepository ajusteRepository;
    private final FechoCaixaRepository fechoRepository;
    private final EntradaMercadoriaRepository entradaRepository;
    private final ObjectMapper objectMapper;

    public ServidorCentralSincronizacaoService(SincronizacaoRepository sincronizacaoRepository,
                                               LojaRepository lojaRepository,
                                               VendaRepository vendaRepository,
                                               FaturaRepository faturaRepository,
                                               ProdutoRepository produtoRepository,
                                               ProdutoLojaRepository produtoLojaRepository,
                                               AjusteInventarioRepository ajusteRepository,
                                               FechoCaixaRepository fechoRepository,
                                               EntradaMercadoriaRepository entradaRepository,
                                               ObjectMapper objectMapper) {
        this.sincronizacaoRepository = sincronizacaoRepository;
        this.lojaRepository = lojaRepository;
        this.vendaRepository = vendaRepository;
        this.faturaRepository = faturaRepository;
        this.produtoRepository = produtoRepository;
        this.produtoLojaRepository = produtoLojaRepository;
        this.ajusteRepository = ajusteRepository;
        this.fechoRepository = fechoRepository;
        this.entradaRepository = entradaRepository;
        this.objectMapper = objectMapper;
    }

    public ResultadoTransmissao receber(SincronizacaoPayload payload) {
        Loja loja = lojaRepository.findById(payload.lojaId())
                .orElseGet(() -> lojaRepository.findAll().stream()
                        .findFirst()
                        .orElseGet(() -> lojaRepository.save(new Loja(
                                payload.lojaId(),
                                "Loja Braga",
                                "Rua Central",
                                nifTecnico(payload.lojaId()),
                                null
                        ))));

        List<Conflito> conflitos = detetarConflitos(payload);
        Sincronizacao sincronizacao = new Sincronizacao(loja, estado(conflitos.isEmpty() ? CONCLUIDA : COM_CONFLITOS));
        sincronizacao.concluir(
                estado(conflitos.isEmpty() ? CONCLUIDA : COM_CONFLITOS),
                toJson(payload),
                payload.quantidadeRegistos(),
                toJson(conflitos),
                conflitos.size()
        );
        sincronizacaoRepository.save(sincronizacao);

        return conflitos.isEmpty()
                ? ResultadoTransmissao.concluida()
                : ResultadoTransmissao.sucessoComConflitos(conflitos);
    }

    private List<Conflito> detetarConflitos(SincronizacaoPayload payload) {
        List<Conflito> conflitos = new ArrayList<>();
        payload.registos().values().forEach(registos -> registos.forEach(registo ->
                entidadeCentral(registo).ifPresent(entidade -> {
                    LocalDateTime centralUpdatedAt = entidade.getUpdatedAt();
                    LocalDateTime localUpdatedAt = registo.updatedAt();
                    if (centralUpdatedAt != null && localUpdatedAt != null && !centralUpdatedAt.equals(localUpdatedAt)) {
                        conflitos.add(new Conflito(
                                registo.tipo(),
                                registo.id(),
                                "updatedAt/version",
                                centralUpdatedAt.isAfter(localUpdatedAt) ? "last-write-wins:central" : "last-write-wins:loja",
                                localUpdatedAt + "/v" + registo.version(),
                                centralUpdatedAt + "/v" + entidade.getVersion()
                        ));
                    }
                })
        ));
        return conflitos;
    }

    private Optional<? extends EntidadeBase> entidadeCentral(RegistoSincronizacao registo) {
        return switch (registo.tipo()) {
            case "VENDA" -> vendaRepository.findById(registo.id());
            case "FATURA" -> faturaRepository.findById(registo.id());
            case "STOCK" -> {
                Optional<? extends EntidadeBase> produtoLoja = produtoLojaRepository.findById(registo.id());
                yield produtoLoja.isPresent() ? produtoLoja : produtoRepository.findById(registo.id());
            }
            case "AJUSTE_STOCK" -> ajusteRepository.findById(registo.id());
            case "FECHO_CAIXA" -> fechoRepository.findById(registo.id());
            case "ENTRADA_MERCADORIA" -> entradaRepository.findById(registo.id());
            default -> Optional.empty();
        };
    }

    private EstadoSincronizacaoCodigo estado(String codigo) {
        return EstadoSincronizacaoCodigo.valueOf(codigo);
    }

    private String nifTecnico(UUID lojaId) {
        return String.format("%09d", Math.floorMod(lojaId.hashCode(), 1_000_000_000));
    }

    private String toJson(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
