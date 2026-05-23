package pt.miniFormiga.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "fechos_caixa")
public class FechoCaixa extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Utilizador responsavel;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalNumerario = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCartao = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalMBWay = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGeral = BigDecimal.ZERO;

    @Column(length = 2000)
    private String observacoesDiscrepancia;

    @Column(nullable = false)
    private boolean confirmado;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Venda> vendas = new ArrayList<>();

    protected FechoCaixa() {
    }

    public FechoCaixa(Loja loja, LocalDate data) {
        this(loja, null, data, List.of());
    }

    public FechoCaixa(Loja loja, Utilizador responsavel, LocalDate data, List<Venda> vendas) {
        this.loja = loja;
        this.responsavel = responsavel;
        this.data = data;
        this.vendas = vendas == null ? new ArrayList<>() : new ArrayList<>(vendas);
        this.confirmado = false;
    }

    public void calcularTotais() {
        totalNumerario = BigDecimal.ZERO;
        totalCartao = BigDecimal.ZERO;
        totalMBWay = BigDecimal.ZERO;
        totalGeral = BigDecimal.ZERO;

        for (Venda venda : vendas) {
            if (venda == null || venda.isAnulada()) {
                continue;
            }
            venda.calcularTotais();
            BigDecimal totalVenda = venda.getTotalComIVA();
            totalGeral = totalGeral.add(totalVenda);

            String tipo = venda.getMeioPagamento() == null ? "" : venda.getMeioPagamento().name();
            String tipoNormalizado = tipo.toUpperCase(Locale.ROOT).replace("_", "").replace(" ", "");
            if ("NUMERARIO".equals(tipoNormalizado)) {
                totalNumerario = totalNumerario.add(totalVenda);
            } else if ("CARTAO".equals(tipoNormalizado)) {
                totalCartao = totalCartao.add(totalVenda);
            } else if ("MBWAY".equals(tipoNormalizado)) {
                totalMBWay = totalMBWay.add(totalVenda);
            }
        }
    }

    public void calcularTotais(List<Venda> vendas) {
        this.vendas = vendas == null ? new ArrayList<>() : new ArrayList<>(vendas);
        calcularTotais();
    }

    public void setObservacaoDiscrepancia(String texto) {
        this.observacoesDiscrepancia = texto;
    }

    public void confirmar() {
        calcularTotais();
        this.confirmado = true;
    }

    public Loja getLoja() {
        return loja;
    }

    public Utilizador getResponsavel() {
        return responsavel;
    }

    public LocalDate getData() {
        return data;
    }

    public BigDecimal getTotalNumerario() {
        return totalNumerario;
    }

    public BigDecimal getTotalCartao() {
        return totalCartao;
    }

    public BigDecimal getTotalMBWay() {
        return totalMBWay;
    }

    public BigDecimal getTotalGeral() {
        return totalGeral;
    }

    public String getObservacoesDiscrepancia() {
        return observacoesDiscrepancia;
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public List<Venda> getVendas() {
        return Collections.unmodifiableList(vendas);
    }
}
