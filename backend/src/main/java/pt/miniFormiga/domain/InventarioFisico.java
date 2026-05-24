package pt.miniFormiga.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "inventarios_fisicos")
public class InventarioFisico extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private Loja loja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Utilizador responsavel;

    @Column(nullable = false)
    private LocalDateTime dataInicio;

    @Column
    private LocalDateTime dataFecho;

    @Transient
    private int totalDiscrepancias;

    @Column(nullable = false)
    private boolean fechado;

    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinhaInventario> linhas = new ArrayList<>();

    protected InventarioFisico() {
    }

    public InventarioFisico(Loja loja, Utilizador responsavel) {
        this.loja = loja;
        this.responsavel = responsavel;
        this.dataInicio = LocalDateTime.now();
        this.fechado = false;
    }

    public void calcularDiscrepancias() {
        int total = 0;
        for (LinhaInventario linha : linhas) {
            linha.calcularDiscrepancia();
            if (linha.getDiscrepancia() != 0) {
                total++;
            }
        }
        this.totalDiscrepancias = total;
    }

    public void fechar() {
        if (fechado) {
            throw new IllegalStateException("Inventario fisico ja fechado");
        }
        calcularDiscrepancias();
        this.dataFecho = LocalDateTime.now();
        this.fechado = true;
    }

    void adicionarLinha(LinhaInventario linhaInventario) {
        if (fechado) {
            throw new IllegalStateException("Inventario fisico fechado nao aceita contagens");
        }
        if (linhaInventario != null && !linhas.contains(linhaInventario)) {
            linhas.add(linhaInventario);
        }
    }

    public Loja getLoja() {
        return loja;
    }

    public Utilizador getResponsavel() {
        return responsavel;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public LocalDateTime getDataFecho() {
        return dataFecho;
    }

    public int getTotalDiscrepancias() {
        return totalDiscrepancias;
    }

    public boolean isFechado() {
        return fechado;
    }

    public List<LinhaInventario> getLinhas() {
        return Collections.unmodifiableList(linhas);
    }
}
