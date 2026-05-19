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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vendas")
public class Venda extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id", nullable = false)
    private Utilizador utilizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meio_pagamento_id")
    private MeioPagamento meioPagamento;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSemIVA = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIVA = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalComIVA = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean anulada;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinhaVenda> linhas = new ArrayList<>();

    protected Venda() {
    }

    public Venda(Loja loja, Utilizador utilizador) {
        this(loja, utilizador, null);
    }

    public Venda(Loja loja, Utilizador utilizador, MeioPagamento meioPagamento) {
        this.loja = loja;
        this.utilizador = utilizador;
        this.meioPagamento = meioPagamento;
        this.dataHora = LocalDateTime.now();
        this.anulada = false;
    }

    public void calcularTotais() {
        BigDecimal novoTotalSemIVA = BigDecimal.ZERO;
        BigDecimal novoTotalIVA = BigDecimal.ZERO;

        for (LinhaVenda linha : linhas) {
            if (linha.isAnulada()) {
                continue;
            }
            linha.calcularTotal();
            novoTotalSemIVA = novoTotalSemIVA.add(linha.getTotalLinha());
            BigDecimal percentagem = linha.getProduto().getTaxaIVA().getPercentagem();
            BigDecimal ivaLinha = linha.getTotalLinha()
                    .multiply(percentagem)
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            novoTotalIVA = novoTotalIVA.add(ivaLinha);
        }

        this.totalSemIVA = novoTotalSemIVA.setScale(2, RoundingMode.HALF_UP);
        this.totalIVA = novoTotalIVA.setScale(2, RoundingMode.HALF_UP);
        this.totalComIVA = this.totalSemIVA.add(this.totalIVA).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularSubtotal() {
        calcularTotais();
        return totalSemIVA;
    }

    public BigDecimal calcularIVA() {
        calcularTotais();
        return totalIVA;
    }

    public BigDecimal calcularTotal() {
        calcularTotais();
        return totalComIVA;
    }

    public void finalizar(MeioPagamento meioPagamento) {
        if (anulada) {
            throw new IllegalStateException("Venda anulada nao pode ser finalizada");
        }
        if (linhas.stream().noneMatch(linha -> !linha.isAnulada())) {
            throw new IllegalStateException("Venda deve ter pelo menos uma linha");
        }
        this.meioPagamento = meioPagamento;
        calcularTotais();
    }

    public void finalizar() {
        finalizar(this.meioPagamento);
    }

    public void anular() {
        this.anulada = true;
    }

    void adicionarLinha(LinhaVenda linhaVenda) {
        if (linhaVenda != null && !linhas.contains(linhaVenda)) {
            linhas.add(linhaVenda);
        }
    }

    public void anularLinha(UUID linhaId) {
        LinhaVenda linhaVenda = linhas.stream()
                .filter(linha -> linha.getId().equals(linhaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Linha de venda nao encontrada"));
        linhaVenda.anular();
        calcularTotais();
    }

    public Loja getLoja() {
        return loja;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    public MeioPagamento getMeioPagamento() {
        return meioPagamento;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public BigDecimal getTotalSemIVA() {
        return totalSemIVA;
    }

    public BigDecimal getTotalIVA() {
        return totalIVA;
    }

    public BigDecimal getTotalComIVA() {
        return totalComIVA;
    }

    public boolean isAnulada() {
        return anulada;
    }

    public List<LinhaVenda> getLinhas() {
        return Collections.unmodifiableList(linhas);
    }

}
