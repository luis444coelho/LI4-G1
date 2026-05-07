package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "fatura_sequencias")
public class FaturaSequencia extends EntidadeBase {

    @Column(nullable = false, unique = true)
    private String serie;

    @Column(nullable = false)
    private int ultimoNumero;

    protected FaturaSequencia() {
    }

    public FaturaSequencia(String serie) {
        this.serie = serie;
        this.ultimoNumero = 0;
    }

    public int proximoNumero() {
        ultimoNumero++;
        return ultimoNumero;
    }

    public String getSerie() {
        return serie;
    }

    public int getUltimoNumero() {
        return ultimoNumero;
    }
}
