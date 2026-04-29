package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "fornecedores")
public class Fornecedor extends EntidadeBase {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 9)
    private String nif;

    @Column(nullable = false)
    private String morada;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false)
    private LocalTime horarioInicioArmazem;

    @Column(nullable = false)
    private LocalTime horarioFimArmazem;

    protected Fornecedor() {
    }

    public Fornecedor(String nome,
                      String nif,
                      String morada,
                      String telefone,
                      String email,
                      LocalTime horarioInicioArmazem,
                      LocalTime horarioFimArmazem) {
        this.nome = nome;
        this.nif = nif;
        this.morada = morada;
        this.telefone = telefone;
        this.email = email;
        this.horarioInicioArmazem = horarioInicioArmazem;
        this.horarioFimArmazem = horarioFimArmazem;
        this.ativo = true;
    }

    public boolean estaDisponivel(LocalDateTime dataHora) {
        if (!ativo || dataHora == null) {
            return false;
        }
        DayOfWeek dia = dataHora.getDayOfWeek();
        boolean diaUtil = dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
        LocalTime hora = dataHora.toLocalTime();
        return diaUtil && !hora.isBefore(horarioInicioArmazem) && hora.isBefore(horarioFimArmazem);
    }

    public LocalDateTime calcularDataProcessamento(LocalDateTime submissao) {
        if (estaDisponivel(submissao)) {
            return submissao;
        }

        LocalDate data = submissao == null ? LocalDate.now() : submissao.toLocalDate();
        LocalDateTime candidato = data.atTime(horarioInicioArmazem);
        if (submissao != null && !submissao.toLocalTime().isBefore(horarioInicioArmazem)) {
            candidato = candidato.plusDays(1);
        }

        while (candidato.getDayOfWeek() == DayOfWeek.SATURDAY
                || candidato.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidato = candidato.plusDays(1);
        }
        return candidato;
    }

    public String getNome() {
        return nome;
    }

    public String getNif() {
        return nif;
    }

    public String getMorada() {
        return morada;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public LocalTime getHorarioInicioArmazem() {
        return horarioInicioArmazem;
    }

    public LocalTime getHorarioFimArmazem() {
        return horarioFimArmazem;
    }
}
