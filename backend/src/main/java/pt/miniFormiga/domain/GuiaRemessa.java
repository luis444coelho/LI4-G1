package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "guias_remessa")
public class GuiaRemessa extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Encomenda encomenda;

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private LocalDate dataEmissao;

    @Column
    private LocalDate dataRecepcao;

    protected GuiaRemessa() {
    }

    public GuiaRemessa(Encomenda encomenda, String numero, LocalDate dataEmissao, LocalDate dataRecepcao) {
        this.encomenda = Objects.requireNonNull(encomenda, "Encomenda e obrigatoria");
        this.numero = Objects.requireNonNull(numero, "Numero da guia e obrigatorio");
        this.dataEmissao = Objects.requireNonNull(dataEmissao, "Data de emissao e obrigatoria");
        this.dataRecepcao = dataRecepcao;
    }

    public Fornecedor getFornecedor() {
        return encomenda.getFornecedor();
    }

    public Encomenda getEncomenda() {
        return encomenda;
    }

    public String getNumero() {
        return numero;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public LocalDate getDataRecepcao() {
        return dataRecepcao;
    }
}
