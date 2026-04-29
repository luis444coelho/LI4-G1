package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "faturas")
public class Fatura extends EntidadeBase {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private String serie;

    @Column(nullable = false)
    private LocalDateTime dataEmissao;

    @Column(nullable = false)
    private String tipo;

    @Column
    private String nifCliente;

    @Column
    private String nomeCliente;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSemIVA = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIVA = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalComIVA = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean emitida;

    protected Fatura() {
    }

    public Fatura(Venda venda, String numero, String serie, String tipo, String nifCliente, String nomeCliente) {
        this.venda = Objects.requireNonNull(venda, "Venda e obrigatoria");
        this.numero = Objects.requireNonNull(numero, "Numero da fatura e obrigatorio");
        this.serie = Objects.requireNonNull(serie, "Serie da fatura e obrigatoria");
        this.tipo = Objects.requireNonNull(tipo, "Tipo da fatura e obrigatorio");
        this.nifCliente = nifCliente;
        this.nomeCliente = nomeCliente;
        this.dataEmissao = LocalDateTime.now();
        this.emitida = false;
    }

    public void emitir() {
        venda.calcularTotais();
        this.totalSemIVA = venda.getTotalSemIVA();
        this.totalIVA = venda.getTotalIVA();
        this.totalComIVA = venda.getTotalComIVA();
        this.dataEmissao = LocalDateTime.now();
        this.emitida = true;
    }

    public byte[] gerarPDF() {
        if (!emitida) {
            emitir();
        }
        String conteudo = "%PDF-1.4\n"
                + "% Mini-Formiga\n"
                + "Fatura " + serie + "/" + numero + "\n"
                + "Tipo: " + tipo + "\n"
                + "NIF: " + (nifCliente == null ? "" : nifCliente) + "\n"
                + "Cliente: " + (nomeCliente == null ? "" : nomeCliente) + "\n"
                + "Total sem IVA: " + totalSemIVA + "\n"
                + "Total IVA: " + totalIVA + "\n"
                + "Total com IVA: " + totalComIVA + "\n"
                + "%%EOF\n";
        return conteudo.getBytes(StandardCharsets.UTF_8);
    }

    public Venda getVenda() {
        return venda;
    }

    public String getNumero() {
        return numero;
    }

    public String getSerie() {
        return serie;
    }

    public LocalDateTime getDataEmissao() {
        return dataEmissao;
    }

    public String getTipo() {
        return tipo;
    }

    public String getNifCliente() {
        return nifCliente;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public BigDecimal getTotalSemIVA() {
        return totalSemIVA;
    }

    public BigDecimal getTotalIVA() {
        return totalIVA;
    }

    public BigDecimal getTotalComIVA() {
        return totalComIVA;
    }

    public boolean isEmitida() {
        return emitida;
    }
}
