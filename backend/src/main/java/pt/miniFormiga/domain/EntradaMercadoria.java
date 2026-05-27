package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.Objects;

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
    @JoinColumn(name = "linha_encomenda_id", nullable = false)
    private LinhaEncomenda linhaEncomenda;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private int quantidadeRecebida;

    @Column(nullable = false)
    private int quantidadeEncomendadaSnapshot;

    @Transient
    private int discrepancia;

    @Column(length = 2000)
    private String observacoes;

    protected EntradaMercadoria() {
    }

    public EntradaMercadoria(GuiaRemessa guiaRemessa,
                             Loja loja,
                             Utilizador responsavel,
                             LinhaEncomenda linhaEncomenda,
                             int quantidadeRecebida,
                             int quantidadeEncomendada,
                             String observacoes) {
        this.guiaRemessa = Objects.requireNonNull(guiaRemessa, "Guia de remessa e obrigatoria");
        this.loja = Objects.requireNonNull(loja, "Loja e obrigatoria");
        this.responsavel = Objects.requireNonNull(responsavel, "Responsavel e obrigatorio");
        this.linhaEncomenda = Objects.requireNonNull(linhaEncomenda, "Linha de encomenda e obrigatoria");
        if (!this.linhaEncomenda.getEncomenda().getId().equals(this.guiaRemessa.getEncomenda().getId())) {
            throw new IllegalArgumentException("Linha de encomenda nao pertence a encomenda da guia de remessa");
        }
        this.quantidadeRecebida = quantidadeRecebida;
        this.quantidadeEncomendadaSnapshot = quantidadeEncomendada;
        this.observacoes = observacoes;
        registar();
    }

    public void registar() {
        this.dataHora = LocalDateTime.now();
        this.discrepancia = quantidadeRecebida - quantidadeEncomendadaSnapshot;
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

    public LinhaEncomenda getLinhaEncomenda() {
        return linhaEncomenda;
    }

    public Produto getProduto() {
        return linhaEncomenda.getProduto();
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public int getQuantidadeRecebida() {
        return quantidadeRecebida;
    }

    public int getQuantidadeEncomendada() {
        return quantidadeEncomendadaSnapshot;
    }

    public int getDiscrepancia() {
        return discrepancia;
    }

    public String getObservacoes() {
        return observacoes;
    }
}
