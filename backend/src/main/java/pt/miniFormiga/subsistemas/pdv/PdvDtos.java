package pt.miniFormiga.subsistemas.pdv;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.miniFormiga.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class PdvDtos {
    private PdvDtos() {
    }

    public record CriarProdutoRequest(@NotBlank String codigoBarras,
                                      @NotBlank String nome,
                                      String descricao,
                                      @NotNull @DecimalMin("0.01") BigDecimal precoVenda,
                                      @NotNull @DecimalMin("0.00") BigDecimal precoCusto,
                                      @NotNull UUID categoriaId,
                                      @NotNull UUID taxaIvaId,
                                      UUID fornecedorPrincipalId) { }
    public record AtualizarProdutoRequest(String nome, String descricao, @DecimalMin("0.01") BigDecimal precoVenda, @DecimalMin("0.00") BigDecimal precoCusto,
                                          UUID categoriaId, UUID taxaIvaId, UUID fornecedorPrincipalId, Boolean ativo) { }
    public record AdicionarLinhaRequest(@NotNull UUID produtoId, @Min(1) int quantidade) { }
    public record FinalizarVendaRequest(@NotNull UUID meioPagamentoId) { }
    public record EmitirFaturaRequest(String nifCliente, String nomeCliente) { }
    public record ProcessarDevolucaoRequest(@NotNull UUID produtoId, @Min(1) int quantidade) { }
    public record RegistarFechoCaixaRequest(LocalDate data) { }

    public record ProdutoDTO(UUID id, String codigoBarras, String nome, String descricao, BigDecimal precoVenda,
                             BigDecimal precoCusto, BigDecimal margem, String categoria, BigDecimal taxaIva,
                             boolean ativo) {
        public static ProdutoDTO from(Produto produto) {
            return new ProdutoDTO(produto.getId(), produto.getCodigoBarras(), produto.getNome(), produto.getDescricao(),
                    produto.getPrecoVenda(), produto.getPrecoCusto(), produto.calcularMargem(),
                    produto.getCategoria().getNome(), produto.getTaxaIVA().getPercentagem(), produto.isAtivo());
        }
    }

    public record LinhaVendaDTO(UUID id, UUID produtoId, String produto, int quantidade, BigDecimal precoUnitario,
                                BigDecimal totalLinha, boolean anulada) {
        public static LinhaVendaDTO from(LinhaVenda linha) {
            return new LinhaVendaDTO(linha.getId(), linha.getProduto().getId(), linha.getProduto().getNome(),
                    linha.getQuantidade(), linha.getPrecoUnitario(), linha.getTotalLinha(), linha.isAnulada());
        }
    }

    public record VendaDTO(UUID id, UUID lojaId, UUID operadorId, LocalDateTime dataHora, boolean anulada,
                           String meioPagamento, BigDecimal subtotal, BigDecimal iva, BigDecimal total,
                           List<LinhaVendaDTO> linhas) {
        public static VendaDTO from(Venda venda) {
            return new VendaDTO(venda.getId(), venda.getLoja().getId(), venda.getUtilizador().getId(),
                    venda.getDataHora(), venda.isAnulada(),
                    venda.getMeioPagamento() == null ? null : venda.getMeioPagamento().getTipo(),
                    venda.getTotalSemIVA(), venda.getTotalIVA(), venda.getTotalComIVA(),
                    venda.getLinhas().stream().map(LinhaVendaDTO::from).toList());
        }
    }

    public record FaturaDTO(UUID id, UUID vendaId, String numeroFatura, String serie, String tipo,
                            String nifCliente, String nomeCliente, LocalDateTime dataEmissao,
                            BigDecimal totalSemIva, BigDecimal totalIva, BigDecimal totalComIva) {
        public static FaturaDTO from(Fatura fatura) {
            return new FaturaDTO(fatura.getId(), fatura.getVenda().getId(), fatura.getNumeroFatura(),
                    fatura.getSerie(), fatura.getTipo(), fatura.getNifCliente(), fatura.getNomeCliente(),
                    fatura.getDataEmissao(), fatura.getTotalSemIVA(), fatura.getTotalIVA(),
                    fatura.getTotalComIVA());
        }
    }

    public record FechoCaixaDTO(UUID id, UUID lojaId, UUID gerenteId, LocalDate data, BigDecimal totalNumerario,
                                BigDecimal totalCartao, BigDecimal totalMbway, BigDecimal totalGeral,
                                String observacoesDiscrepancia, boolean confirmado) {
        public static FechoCaixaDTO from(FechoCaixa fecho) {
            return new FechoCaixaDTO(fecho.getId(), fecho.getLoja().getId(),
                    fecho.getResponsavel() == null ? null : fecho.getResponsavel().getId(),
                    fecho.getData(), fecho.getTotalNumerario(), fecho.getTotalCartao(), fecho.getTotalMBWay(),
                    fecho.getTotalGeral(), fecho.getObservacoesDiscrepancia(), fecho.isConfirmado());
        }
    }

    public record MeioPagamentoDTO(UUID id, String tipo, String descricao) {
        public static MeioPagamentoDTO from(MeioPagamento meioPagamento) {
            return new MeioPagamentoDTO(meioPagamento.getId(), meioPagamento.getTipo(), meioPagamento.getDescricao());
        }
    }
}
