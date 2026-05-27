package pt.miniFormiga.subsistemas.sincronizacao.facade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.ConflitoSincronizacaoResponse;
import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.SincronizacaoPayload;
import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.SincronizacaoResponse;
import static pt.miniFormiga.subsistemas.sincronizacao.transport.SincronizacaoTransporte.ResultadoTransmissao;

public interface ISubSincronizacao {
    void agendarSincronizacao(UUID lojaId);

    SincronizacaoResponse iniciarSincronizacao(UUID lojaId);

    SincronizacaoResponse estadoAtual(UUID lojaId);

    Page<SincronizacaoResponse> historico(UUID lojaId, Pageable pageable);

    List<ConflitoSincronizacaoResponse> conflitos(UUID lojaId);

    ResultadoTransmissao receber(SincronizacaoPayload payload);
}
