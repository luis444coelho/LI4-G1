package pt.miniFormiga.subsistemas.sincronizacao.dto;

import pt.miniFormiga.domain.Sincronizacao;
import pt.miniFormiga.subsistemas.relatorios.dto.RelatoriosDtos.DashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SincronizacaoDtos {
    private SincronizacaoDtos() {
    }

    public record IniciarSincronizacaoRequest(UUID lojaId) {
    }

    public record SincronizacaoResponse(UUID id,
                                        UUID lojaId,
                                        String estado,
                                        LocalDateTime inicio,
                                        LocalDateTime fim,
                                        LocalDateTime proximaTentativa,
                                        int quantidadeRegistos,
                                        int conflitosResolvidos,
                                        String mensagemErro) {
        public static SincronizacaoResponse from(Sincronizacao sincronizacao) {
            return new SincronizacaoResponse(
                    sincronizacao.getId(),
                    sincronizacao.getLoja().getId(),
                    sincronizacao.getEstado().getCodigo(),
                    sincronizacao.getDataHoraInicio(),
                    sincronizacao.getDataHoraFim(),
                    sincronizacao.getProximaTentativa(),
                    sincronizacao.getQuantidadeRegistos(),
                    sincronizacao.getConflitosResolvidos(),
                    sincronizacao.getMensagemErro()
            );
        }
    }

    public record SincronizacaoPayload(UUID lojaId,
                                       LocalDateTime geradoEm,
                                       LocalDateTime desde,
                                       Map<String, List<RegistoSincronizacao>> registos,
                                       DashboardResponse dashboard,
                                       List<VendaRelatorioSync> vendasRelatorio,
                                       List<String> logsAuditoria) {
        public SincronizacaoPayload(UUID lojaId,
                                    LocalDateTime geradoEm,
                                    LocalDateTime desde,
                                    Map<String, List<RegistoSincronizacao>> registos,
                                    DashboardResponse dashboard,
                                    List<String> logsAuditoria) {
            this(lojaId, geradoEm, desde, registos, dashboard, List.of(), logsAuditoria);
        }

        public int quantidadeRegistos() {
            int registosDominio = registos.values().stream().mapToInt(List::size).sum();
            int vendasParaRelatorio = vendasRelatorio == null ? 0 : vendasRelatorio.size();
            int logs = logsAuditoria == null ? 0 : logsAuditoria.size();
            return registosDominio + vendasParaRelatorio + logs;
        }
    }

    public record RegistoSincronizacao(String tipo,
                                       UUID id,
                                       LocalDateTime updatedAt,
                                       long version) {
    }

    public record VendaRelatorioSync(UUID vendaId,
                                     LocalDateTime dataHora,
                                     UUID lojaId,
                                     String loja,
                                     UUID produtoId,
                                     String produto,
                                     UUID categoriaId,
                                     String categoria,
                                     int quantidade,
                                     BigDecimal valorSemIva,
                                     BigDecimal iva,
                                     BigDecimal valorComIva,
                                     BigDecimal custo,
                                     BigDecimal margem) {
    }

    public record ConflitoSincronizacaoResponse(UUID sincronizacaoId,
                                                UUID lojaId,
                                                LocalDateTime dataHora,
                                                String estado,
                                                int conflitosResolvidos,
                                                String conflitosJson) {
        public static ConflitoSincronizacaoResponse from(Sincronizacao sincronizacao) {
            return new ConflitoSincronizacaoResponse(
                    sincronizacao.getId(),
                    sincronizacao.getLoja().getId(),
                    sincronizacao.getDataHoraFim() == null ? sincronizacao.getDataHoraInicio() : sincronizacao.getDataHoraFim(),
                    sincronizacao.getEstado().getCodigo(),
                    sincronizacao.getConflitosResolvidos(),
                    sincronizacao.getConflitosJson()
            );
        }
    }
}
