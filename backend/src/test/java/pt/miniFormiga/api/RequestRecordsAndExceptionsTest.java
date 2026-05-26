package pt.miniFormiga.api;

import org.junit.jupiter.api.Test;
import pt.miniFormiga.exception.FaturaNaoEmitidaException;
import pt.miniFormiga.exception.VendaNaoEncontradaException;
import pt.miniFormiga.subsistemas.pdv.PdvDtos.RegistarFechoCaixaRequest;
import pt.miniFormiga.subsistemas.utilizadores.RecursoNaoEncontradoException;
import pt.miniFormiga.subsistemas.utilizadores.RegraNegocioException;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestRecordsAndExceptionsTest {

    @Test
    void recordsDeRequestExpõemCamposSerializadosPelaApi() {
        UUID lojaId = UUID.randomUUID();
        UUID operadorId = UUID.randomUUID();
        LocalDate data = LocalDate.of(2026, 5, 26);

        var iniciarVenda = new VendasController.IniciarVendaRequest(lojaId, operadorId);
        var fechoCaixa = new RegistarFechoCaixaRequest(data);

        assertEquals(lojaId, iniciarVenda.lojaId());
        assertEquals(operadorId, iniciarVenda.operadorId());
        assertEquals(data, fechoCaixa.data());
    }

    @Test
    void excecoesDeDominioMantêmCodigoMensagemEDetalhes() {
        UUID vendaId = UUID.randomUUID();

        var fatura = new FaturaNaoEmitidaException("Venda anulada");
        var venda = new VendaNaoEncontradaException(vendaId);
        var recurso = new RecursoNaoEncontradoException("Utilizador nao encontrado");
        var regra = new RegraNegocioException("Perfil invalido");

        assertEquals("FATURA_NAO_EMITIDA", fatura.getCode());
        assertEquals("Venda anulada", fatura.getMessage());
        assertEquals("VENDA_NAO_ENCONTRADA", venda.getCode());
        assertEquals(vendaId, venda.getDetails().get("vendaId"));
        assertEquals("Utilizador nao encontrado", recurso.getMessage());
        assertEquals("Perfil invalido", regra.getMessage());
    }
}
