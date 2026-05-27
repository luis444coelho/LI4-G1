package pt.miniFormiga.subsistemas.sincronizacao.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.EstadoSincronizacaoCodigo;
import pt.miniFormiga.domain.Sincronizacao;
import pt.miniFormiga.subsistemas.sincronizacao.repository.SincronizacaoRepository;
import pt.miniFormiga.subsistemas.sincronizacao.service.IConsultaSincronizacao.DadosRelatorioSincronizado;
import pt.miniFormiga.subsistemas.sincronizacao.service.IConsultaSincronizacao.VendaRelatorioSincronizada;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.SincronizacaoPayload;

@Service
@Transactional(readOnly = true)
public class ConsultaSincronizacaoService implements IConsultaSincronizacao {

    private final SincronizacaoRepository sincronizacaoRepository;
    private final ObjectMapper objectMapper;

    public ConsultaSincronizacaoService(SincronizacaoRepository sincronizacaoRepository,
                                        ObjectMapper objectMapper) {
        this.sincronizacaoRepository = sincronizacaoRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<DadosRelatorioSincronizado> dadosRelatorio(UUID lojaId) {
        List<EstadoSincronizacaoCodigo> estados = List.of(
                EstadoSincronizacaoCodigo.CONCLUIDA,
                EstadoSincronizacaoCodigo.COM_CONFLITOS
        );
        if (lojaId != null) {
            return sincronizacaoRepository.findFirstByLojaIdAndEstadoInOrderByDataHoraFimDesc(lojaId, estados)
                    .map(this::dadosRelatorio)
                    .stream()
                    .filter(dados -> dados != null)
                    .toList();
        }

        Set<UUID> lojasIncluidas = new LinkedHashSet<>();
        List<DadosRelatorioSincronizado> dados = new ArrayList<>();
        List<Sincronizacao> sincronizacoes = sincronizacaoRepository.findByEstadoInOrderByDataHoraFimDesc(estados);
        if (sincronizacoes == null) {
            return List.of();
        }
        for (Sincronizacao sincronizacao : sincronizacoes) {
            DadosRelatorioSincronizado item = dadosRelatorio(sincronizacao);
            if (item == null) {
                continue;
            }
            UUID idLoja = item.lojaId();
            if (idLoja == null || !lojasIncluidas.add(idLoja)) {
                continue;
            }
            dados.add(item);
        }
        return dados;
    }

    private DadosRelatorioSincronizado dadosRelatorio(Sincronizacao sincronizacao) {
        SincronizacaoPayload payload = payload(sincronizacao);
        if (payload == null) {
            return null;
        }
        return new DadosRelatorioSincronizado(
                payload.lojaId(),
                payload.dashboard(),
                vendasRelatorio(payload)
        );
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

    private List<VendaRelatorioSincronizada> vendasRelatorio(SincronizacaoPayload payload) {
        if (payload.vendasRelatorio() == null) {
            return List.of();
        }
        return payload.vendasRelatorio().stream()
                .map(linha -> new VendaRelatorioSincronizada(
                        linha.vendaId(),
                        linha.dataHora(),
                        linha.lojaId(),
                        linha.loja(),
                        linha.produtoId(),
                        linha.produto(),
                        linha.categoriaId(),
                        linha.categoria(),
                        linha.quantidade(),
                        linha.valorSemIva(),
                        linha.iva(),
                        linha.valorComIva(),
                        linha.custo(),
                        linha.margem()
                ))
                .toList();
    }
}
