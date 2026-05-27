package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadosReferenciaTest {

    @Test
    void estadosLegacyExposicaoCodigoEDescricao() {
        EstadoEncomenda estadoEncomenda = new EstadoEncomenda("PENDENTE", "Pendente");
        EstadoSincronizacao estadoSincronizacao = new EstadoSincronizacao("CONCLUIDA", "Concluida");
        MotivoAjuste motivoAjuste = new MotivoAjuste("QUEBRA", "Produto danificado");
        FaturaSequencia sequencia = new FaturaSequencia("A");

        assertEquals("PENDENTE", estadoEncomenda.getCodigo());
        assertEquals("Pendente", estadoEncomenda.getDescricao());
        assertEquals("CONCLUIDA", estadoSincronizacao.getCodigo());
        assertEquals("Concluida", estadoSincronizacao.getDescricao());
        assertEquals("QUEBRA", motivoAjuste.getCodigo());
        assertEquals("Produto danificado", motivoAjuste.getDescricao());
        assertEquals("A", sequencia.getSerie());
        assertEquals(1, sequencia.proximoNumero());
        assertEquals(1, sequencia.getUltimoNumero());
    }
}
