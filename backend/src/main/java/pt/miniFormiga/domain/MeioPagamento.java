package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "meios_pagamento")
public class MeioPagamento extends EntidadeBase {

    @Column(nullable = false, unique = true)
    private String tipo;

    @Column(nullable = false)
    private String descricao;

    protected MeioPagamento() {
    }

    public MeioPagamento(String tipo, String descricao) {
        this.tipo = tipo;
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }
}
