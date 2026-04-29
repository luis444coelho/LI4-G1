package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "guias_remessa")
public class GuiaRemessa extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private LocalDate dataEmissao;

    @Column
    private LocalDate dataRecepcao;

    protected GuiaRemessa() {
    }

    public GuiaRemessa(Fornecedor fornecedor, String numero, LocalDate dataEmissao, LocalDate dataRecepcao) {
        this.fornecedor = fornecedor;
        this.numero = numero;
        this.dataEmissao = dataEmissao;
        this.dataRecepcao = dataRecepcao;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
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
