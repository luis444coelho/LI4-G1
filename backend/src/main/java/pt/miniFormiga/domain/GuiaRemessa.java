package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "guias_remessa")
public class GuiaRemessa extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Encomenda encomenda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

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
        this.fornecedor = Objects.requireNonNull(this.encomenda.getFornecedor(), "Fornecedor e obrigatorio");
        this.numero = Objects.requireNonNull(numero, "Numero da guia e obrigatorio");
        this.dataEmissao = Objects.requireNonNull(dataEmissao, "Data de emissao e obrigatoria");
        this.dataRecepcao = dataRecepcao;
    }

    @PrePersist
    @PreUpdate
    private void sincronizarFornecedor() {
        if (fornecedor == null && encomenda != null) {
            fornecedor = encomenda.getFornecedor();
        }
    }

    public Fornecedor getFornecedor() {
        return fornecedor == null ? encomenda.getFornecedor() : fornecedor;
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
