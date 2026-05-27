package pt.miniFormiga.subsistemas.sincronizacao.transport;

import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.sincronizacao.dto.SincronizacaoDtos.SincronizacaoPayload;

public interface SincronizacaoTransporte {
    ResultadoTransmissao transmitir(SincronizacaoPayload payload);

    record ResultadoTransmissao(boolean sucesso, List<Conflito> conflitos, String mensagemErro) {
        public static ResultadoTransmissao concluida() {
            return new ResultadoTransmissao(true, List.of(), null);
        }

        public static ResultadoTransmissao sucessoComConflitos(List<Conflito> conflitos) {
            return new ResultadoTransmissao(true, conflitos == null ? List.of() : conflitos, null);
        }

        public static ResultadoTransmissao falha(String mensagemErro) {
            return new ResultadoTransmissao(false, List.of(), mensagemErro);
        }
    }

    record Conflito(String tipo,
                    UUID registoId,
                    String campo,
                    String resolucao,
                    String valorLocal,
                    String valorCentral) {
    }
}
