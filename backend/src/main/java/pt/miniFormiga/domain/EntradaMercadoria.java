package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "entradas_mercadoria")
public class EntradaMercadoria extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guia_remessa_id", nullable = false)
    private GuiaRemessa guiaRemessa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Utilizador responsavel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private int quantidadeRecebida;

    @Column(nullable = false)
    private int quantidadeEncomendada;

    @Column(nullable = false)
    private int discrepancia;

    @Column(length = 2000)
    private String observacoes;

    protected EntradaMercadoria() {
    }

    public EntradaMercadoria(GuiaRemessa guiaRemessa,
                             Loja loja,
                             Utilizador responsavel,
                             int quantidadeRecebida,
                             int quantidadeEncomendada,
                             String observacoes) {
        this(guiaRemessa, loja, responsavel, null, quantidadeRecebida, quantidadeEncomendada, observacoes);
    }

    public EntradaMercadoria(GuiaRemessa guiaRemessa,
                             Loja loja,
                             Utilizador responsavel,
                             Produto produto,
                             int quantidadeRecebida,
                             int quantidadeEncomendada,
                             String observacoes) {
        this.guiaRemessa = guiaRemessa;
        this.loja = loja;
        this.responsavel = responsavel;
        this.produto = produto;
        this.quantidadeRecebida = quantidadeRecebida;
        this.quantidadeEncomendada = quantidadeEncomendada;
        this.observacoes = observacoes;
        registar();
    }

    public void registar() {
        this.dataHora = LocalDateTime.now();
        this.discrepancia = quantidadeRecebida - quantidadeEncomendada;
    }

    public GuiaRemessa getGuiaRemessa() {
        return guiaRemessa;
    }

    public Loja getLoja() {
        return loja;
    }

    public Utilizador getResponsavel() {
        return responsavel;
    }

    public Produto getProduto() {
        return produto;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public int getQuantidadeRecebida() {
        return quantidadeRecebida;
    }

    public int getQuantidadeEncomendada() {
        return quantidadeEncomendada;
    }

    public int getDiscrepancia() {
        return discrepancia;
    }

    public String getObservacoes() {
        return observacoes;
    }
}
