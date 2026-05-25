package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "linhas_inventario")
public class LinhaInventario extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventario_id", nullable = false)
    private InventarioFisico inventario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidadeContada;

    @Column(nullable = false)
    private int quantidadeSistema;

    @Transient
    private int discrepancia;

    protected LinhaInventario() {
    }

    public LinhaInventario(InventarioFisico inventario,
                           Produto produto,
                           int quantidadeContada,
                           int quantidadeSistema) {
        this.inventario = inventario;
        this.produto = produto;
        this.quantidadeContada = quantidadeContada;
        this.quantidadeSistema = quantidadeSistema;
        calcularDiscrepancia();
        if (this.inventario != null) {
            this.inventario.adicionarLinha(this);
        }
    }

    public void calcularDiscrepancia() {
        this.discrepancia = quantidadeContada - quantidadeSistema;
    }

    public void atualizarQuantidadeContada(int quantidadeContada) {
        if (quantidadeContada < 0) {
            throw new IllegalArgumentException("Quantidade contada nao pode ser negativa");
        }
        this.quantidadeContada = quantidadeContada;
        calcularDiscrepancia();
    }

    public void consolidarComStockAtualizado() {
        this.quantidadeSistema = this.quantidadeContada;
        calcularDiscrepancia();
    }

    public InventarioFisico getInventario() {
        return inventario;
    }

    public InventarioFisico getInventarioFisico() {
        return inventario;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidadeContada() {
        return quantidadeContada;
    }

    public int getQuantidadeSistema() {
        return quantidadeSistema;
    }

    public int getDiscrepancia() {
        return discrepancia;
    }
}
