package pt.miniFormiga.subsistemas.encomendas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pt.miniFormiga.domain.CondicaoComercial;
import pt.miniFormiga.domain.Encomenda;
import pt.miniFormiga.domain.EntradaMercadoria;
import pt.miniFormiga.domain.Fornecedor;
import pt.miniFormiga.domain.LinhaEncomenda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class EncomendasDtos {
    private EncomendasDtos() {
    }

    public record CriarFornecedorRequest(@NotBlank String nome,
                                         @NotBlank String nif,
                                         @NotBlank String morada,
                                         @NotBlank String telefone,
                                         @NotBlank String email,
                                         @NotNull LocalTime horarioInicioArmazem,
                                         @NotNull LocalTime horarioFimArmazem) {
    }

    public record AtualizarFornecedorRequest(@NotBlank String nome,
                                             @NotBlank String nif,
                                             @NotBlank String morada,
                                             @NotBlank String telefone,
                                             @NotBlank String email,
                                             @NotNull LocalTime horarioInicioArmazem,
                                             @NotNull LocalTime horarioFimArmazem) {
    }

    public record CondicaoComercialRequest(@NotNull UUID produtoId,
                                           @NotNull @DecimalMin("0.00") BigDecimal precoUnitario,
                                           @Min(0) int prazoEntregaDias,
                                           @Min(1) int quantidadeMinima,
                                           LocalDate dataVigencia) {
    }

    public record CriarLinhaEncomendaRequest(@NotNull UUID produtoId,
                                             @Min(1) int quantidade,
                                             @NotNull @DecimalMin("0.00") BigDecimal precoUnitario) {
    }

    public record CriarEncomendaRequest(@NotNull UUID lojaId,
                                        @NotNull UUID fornecedorId,
                                        @Valid @NotEmpty List<CriarLinhaEncomendaRequest> linhas,
                                        LocalDateTime dataHoraSubmissao) {
        public CriarEncomendaRequest(UUID lojaId, UUID fornecedorId, List<CriarLinhaEncomendaRequest> linhas) {
            this(lojaId, fornecedorId, linhas, null);
        }
    }

    public record AtualizarEstadoEncomendaRequest(@NotBlank String estadoCodigo) {
    }

    public record RegistarEntradaMercadoriaRequest(@NotNull UUID encomendaId,
                                                   @NotNull UUID lojaId,
                                                   @NotNull UUID responsavelId,
                                                   @NotBlank String guiaNumero,
                                                   @NotNull LocalDate dataEmissao,
                                                   LocalDate dataRecepcao,
                                                   @Valid @NotEmpty List<RegistarEntradaMercadoriaLinhaRequest> linhas) {
    }

    public record RegistarEntradaMercadoriaLinhaRequest(@NotNull UUID produtoId,
                                                        @Min(0) int quantidadeRecebida,
                                                        @Min(0) int quantidadeEncomendada,
                                                        String observacoes) {
    }

    public record SugestaoEncomendaResponse(UUID fornecedorId,
                                            String fornecedor,
                                            UUID produtoId,
                                            String produto,
                                            UUID lojaId,
                                            int quantidadeAtual,
                                            Integer nivelMinimo,
                                            int quantidadeSugerida,
                                            BigDecimal precoUnitario) {
    }

    public record FornecedorResponse(UUID id,
                                     String nome,
                                     String nif,
                                     String morada,
                                     String telefone,
                                     String email,
                                     boolean ativo,
                                     LocalTime horarioInicioArmazem,
                                     LocalTime horarioFimArmazem) {
        public static FornecedorResponse from(Fornecedor fornecedor) {
            return new FornecedorResponse(
                    fornecedor.getId(),
                    fornecedor.getNome(),
                    fornecedor.getNif(),
                    fornecedor.getMorada(),
                    fornecedor.getTelefone(),
                    fornecedor.getEmail(),
                    fornecedor.isAtivo(),
                    fornecedor.getHorarioInicioArmazem(),
                    fornecedor.getHorarioFimArmazem()
            );
        }
    }

    public record CondicaoComercialResponse(UUID id,
                                            UUID fornecedorId,
                                            UUID produtoId,
                                            String produto,
                                            BigDecimal precoUnitario,
                                            int prazoEntregaDias,
                                            int quantidadeMinima,
                                            LocalDate dataVigencia) {
        public static CondicaoComercialResponse from(CondicaoComercial condicao) {
            return new CondicaoComercialResponse(
                    condicao.getId(),
                    condicao.getFornecedor().getId(),
                    condicao.getProduto().getId(),
                    condicao.getProduto().getNome(),
                    condicao.getPrecoUnitario(),
                    condicao.getPrazoEntregaDias(),
                    condicao.getQuantidadeMinima(),
                    condicao.getDataVigencia()
            );
        }
    }

    public record LinhaEncomendaResponse(UUID id,
                                         UUID produtoId,
                                         String produto,
                                         int quantidade,
                                         BigDecimal precoUnitario,
                                         BigDecimal totalLinha) {
        public static LinhaEncomendaResponse from(LinhaEncomenda linha) {
            return new LinhaEncomendaResponse(
                    linha.getId(),
                    linha.getProduto().getId(),
                    linha.getProduto().getNome(),
                    linha.getQuantidade(),
                    linha.getPrecoUnitario(),
                    linha.getTotalLinha()
            );
        }
    }

    public record EncomendaResponse(UUID id,
                                    String numeroDocumento,
                                    UUID lojaId,
                                    UUID fornecedorId,
                                    String fornecedor,
                                    String estado,
                                    LocalDateTime dataSubmissao,
                                    LocalDateTime dataProcessamento,
                                    BigDecimal totalEstimado,
                                    List<LinhaEncomendaResponse> linhas) {
        public static EncomendaResponse from(Encomenda encomenda) {
            return from(encomenda, null);
        }

        public static EncomendaResponse from(Encomenda encomenda, String numeroDocumento) {
            return new EncomendaResponse(
                    encomenda.getId(),
                    numeroDocumento,
                    encomenda.getLoja().getId(),
                    encomenda.getFornecedor().getId(),
                    encomenda.getFornecedor().getNome(),
                    encomenda.getEstado().getCodigo(),
                    encomenda.getDataSubmissao(),
                    encomenda.getDataProcessamento(),
                    encomenda.getTotalEstimado(),
                    encomenda.getLinhas().stream().map(LinhaEncomendaResponse::from).toList()
            );
        }
    }

    public record ProximaGuiaRemessaResponse(String numero) {
    }

    public record EntradaMercadoriaResponse(UUID id,
                                            UUID encomendaId,
                                            UUID lojaId,
                                            UUID responsavelId,
                                            UUID produtoId,
                                            String produto,
                                            String guiaNumero,
                                            LocalDateTime dataHora,
                                            int quantidadeRecebida,
                                            int quantidadeEncomendada,
                                            int discrepancia,
                                            String observacoes) {
        public static EntradaMercadoriaResponse from(EntradaMercadoria entrada) {
            UUID encomendaId = entrada.getGuiaRemessa().getEncomenda() == null
                    ? null
                    : entrada.getGuiaRemessa().getEncomenda().getId();
            return new EntradaMercadoriaResponse(
                    entrada.getId(),
                    encomendaId,
                    entrada.getLoja().getId(),
                    entrada.getResponsavel().getId(),
                    entrada.getProduto() == null ? null : entrada.getProduto().getId(),
                    entrada.getProduto() == null ? null : entrada.getProduto().getNome(),
                    entrada.getGuiaRemessa().getNumero(),
                    entrada.getDataHora(),
                    entrada.getQuantidadeRecebida(),
                    entrada.getQuantidadeEncomendada(),
                    entrada.getDiscrepancia(),
                    entrada.getObservacoes()
            );
        }
    }
}
