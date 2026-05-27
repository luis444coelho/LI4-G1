package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadosReferenciaTest {

    @Test
    void enumsDeReferenciaExpoemCodigoPersistido() {
        assertEquals("PENDENTE", EstadoEncomendaCodigo.PENDENTE.getCodigo());
        assertEquals("ENVIADA", EstadoEncomendaCodigo.ENVIADA.getCodigo());
        assertEquals("RECEBIDA", EstadoEncomendaCodigo.RECEBIDA.getCodigo());
        assertEquals("CANCELADA", EstadoEncomendaCodigo.CANCELADA.getCodigo());

        assertEquals("PENDENTE", EstadoSincronizacaoCodigo.PENDENTE.getCodigo());
        assertEquals("EM_CURSO", EstadoSincronizacaoCodigo.EM_CURSO.getCodigo());
        assertEquals("CONCLUIDA", EstadoSincronizacaoCodigo.CONCLUIDA.getCodigo());
        assertEquals("FALHADA", EstadoSincronizacaoCodigo.FALHADA.getCodigo());
        assertEquals("COM_CONFLITOS", EstadoSincronizacaoCodigo.COM_CONFLITOS.getCodigo());

        assertEquals("QUEBRA", MotivoAjusteCodigo.QUEBRA.getCodigo());
        assertEquals("DESPERDICIO", MotivoAjusteCodigo.DESPERDICIO.getCodigo());
        assertEquals("CORRECAO_ERRO", MotivoAjusteCodigo.CORRECAO_ERRO.getCodigo());
        assertEquals("DEVOLUCAO", MotivoAjusteCodigo.DEVOLUCAO.getCodigo());
        assertEquals("ENTRADA_MERCADORIA", MotivoAjusteCodigo.ENTRADA_MERCADORIA.getCodigo());
    }
}
