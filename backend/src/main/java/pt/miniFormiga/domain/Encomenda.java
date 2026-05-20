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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "encomendas")
public class Encomenda extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoEncomenda estado;

    @Column(nullable = false)
    private LocalDateTime dataSubmissao;

    @Column(nullable = false)
    private LocalDateTime dataProcessamento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEstimado = BigDecimal.ZERO;

    @Column(length = 2000)
    private String observacoes;

    @OneToMany(mappedBy = "encomenda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinhaEncomenda> linhas = new ArrayList<>();

    protected Encomenda() {
    }

    public Encomenda(Loja loja, Fornecedor fornecedor) {
        this(loja, fornecedor, new EstadoEncomenda("PENDENTE", "Pendente"));
    }

    public Encomenda(Loja loja, Fornecedor fornecedor, EstadoEncomenda estado) {
        this.loja = loja;
        this.fornecedor = fornecedor;
        this.estado = estado;
        this.dataSubmissao = LocalDateTime.now();
        this.dataProcessamento = fornecedor == null
                ? this.dataSubmissao
                : fornecedor.calcularDataProcessamento(this.dataSubmissao);
    }

    public BigDecimal calcularTotal() {
        totalEstimado = linhas.stream()
                .map(LinhaEncomenda::calcularTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalEstimado;
    }

    public void submeter() {
        submeter(LocalDateTime.now());
    }

    public void submeter(LocalDateTime dataSubmissao) {
        if (linhas.isEmpty()) {
            throw new IllegalStateException("Encomenda deve ter pelo menos uma linha");
        }
        this.dataSubmissao = dataSubmissao == null ? LocalDateTime.now() : dataSubmissao;
        this.dataProcessamento = fornecedor == null
                ? this.dataSubmissao
                : fornecedor.calcularDataProcessamento(this.dataSubmissao);
        calcularTotal();
    }

    public void alterarEstado(EstadoEncomenda estado) {
        if (estado == null) {
            throw new IllegalArgumentException("Estado da encomenda e obrigatorio");
        }
        this.estado = estado;
    }

    void adicionarLinha(LinhaEncomenda linhaEncomenda) {
        if (linhaEncomenda != null && !linhas.contains(linhaEncomenda)) {
            linhas.add(linhaEncomenda);
        }
    }

    public Loja getLoja() {
        return loja;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public EstadoEncomenda getEstado() {
        return estado;
    }

    public LocalDateTime getDataSubmissao() {
        return dataSubmissao;
    }

    public LocalDateTime getDataProcessamento() {
        return dataProcessamento;
    }

    public BigDecimal getTotalEstimado() {
        return totalEstimado;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public List<LinhaEncomenda> getLinhas() {
        return Collections.unmodifiableList(linhas);
    }
}
