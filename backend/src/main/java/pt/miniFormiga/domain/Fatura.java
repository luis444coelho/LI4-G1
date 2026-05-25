package pt.miniFormiga.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "faturas")
public class Fatura extends EntidadeBase implements Persistable<UUID> {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id", nullable = false)
    @MapsId
    private Venda venda;

    @Column(name = "loja_id")
    private java.util.UUID lojaId;

    @Column(nullable = false)
    private int numero;

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

    @Transient
    private BigDecimal totalSemIVA = BigDecimal.ZERO;

    @Transient
    private BigDecimal totalIVA = BigDecimal.ZERO;

    @Transient
    private BigDecimal totalComIVA = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean emitida;

    protected Fatura() {
    }

    public Fatura(Venda venda, String numero, String serie, String tipo, String nifCliente, String nomeCliente) {
        this.venda = Objects.requireNonNull(venda, "Venda e obrigatoria");
        definirId(venda.getId());
        this.lojaId = venda.getLoja() == null ? null : venda.getLoja().getId();
        this.numero = Integer.parseInt(Objects.requireNonNull(numero, "Numero da fatura e obrigatorio"));
        this.serie = Objects.requireNonNull(serie, "Serie da fatura e obrigatoria");
        this.tipo = Objects.requireNonNull(tipo, "Tipo da fatura e obrigatorio");
        this.nifCliente = nifCliente;
        this.nomeCliente = nomeCliente;
        this.dataEmissao = LocalDateTime.now();
        this.emitida = false;
    }

    public void emitir() {
        atualizarTotaisDocumento();
        this.dataEmissao = LocalDateTime.now();
        this.emitida = true;
    }

    private void atualizarTotaisDocumento() {
        venda.calcularTotais();
        this.totalSemIVA = venda.getTotalSemIVA();
        this.totalIVA = venda.getTotalIVA();
        this.totalComIVA = venda.getTotalComIVA();
    }

    public static String decidirTipo(BigDecimal totalComIva, String nifCliente) {
        boolean temNif = nifCliente != null && !nifCliente.isBlank();
        if (temNif || totalComIva.compareTo(new BigDecimal("1000.00")) > 0) {
            return "COMPLETA";
        }
        return "SIMPLIFICADA";
    }

    public byte[] gerarPDF() {
        if (!emitida) {
            emitir();
        } else {
            atualizarTotaisDocumento();
        }
        return gerarDocumentoPdf(List.of(
                "Mini-Formiga",
                "Fatura " + serie + "/" + numero,
                "Tipo: " + tipo,
                "NIF: " + texto(nifCliente),
                "Cliente: " + texto(nomeCliente),
                "Total sem IVA: " + totalSemIVA,
                "Total IVA: " + totalIVA,
                "Total com IVA: " + totalComIVA
        ));
    }

    public byte[] gerarReciboPDF() {
        if (!emitida) {
            emitir();
        } else {
            atualizarTotaisDocumento();
        }
        return gerarDocumentoPdf(List.of(
                "Mini-Formiga",
                "Recibo " + serie + "/" + numero,
                "Fatura associada: " + getNumeroFatura(),
                "Meio pagamento: " + (venda.getMeioPagamento() == null ? "" : venda.getMeioPagamento().name()),
                "Total recebido: " + totalComIVA,
                "Data: " + dataEmissao
        ));
    }

    private byte[] gerarDocumentoPdf(List<String> linhas) {
        StringBuilder stream = new StringBuilder("BT\n/F1 12 Tf\n50 790 Td\n");
        for (String linha : linhas) {
            stream.append("(").append(escaparPdf(linha)).append(") Tj\n0 -18 Td\n");
        }
        stream.append("ET\n");

        byte[] streamBytes = stream.toString().getBytes(StandardCharsets.UTF_8);
        List<String> objetos = new ArrayList<>();
        objetos.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        objetos.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        objetos.add("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");
        objetos.add("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        objetos.add("5 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n" + stream + "endstream\nendobj\n");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        escreverAscii(output, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (String objeto : objetos) {
            offsets.add(output.size());
            escreverAscii(output, objeto);
        }
        int xrefOffset = output.size();
        escreverAscii(output, "xref\n0 " + offsets.size() + "\n");
        escreverAscii(output, "0000000000 65535 f \n");
        for (int i = 1; i < offsets.size(); i++) {
            escreverAscii(output, String.format("%010d 00000 n \n", offsets.get(i)));
        }
        escreverAscii(output, "trailer\n<< /Size " + offsets.size() + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n");
        return output.toByteArray();
    }

    private static void escreverAscii(ByteArrayOutputStream output, String texto) {
        output.writeBytes(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String escaparPdf(String valor) {
        return texto(valor)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    public Venda getVenda() {
        return venda;
    }

    public String getNumero() {
        return String.format("%05d", numero);
    }

    public int getNumeroSequencial() {
        return numero;
    }

    public String getNumeroFatura() {
        return serie + "/" + getNumero();
    }

    public java.util.UUID getLojaId() {
        return lojaId;
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

    @Override
    @Transient
    public boolean isNew() {
        return getCreatedAt() == null;
    }

}
