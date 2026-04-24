package pt.miniFormiga.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TipoOperacaoTest {

    @Test
    void deveConterOperacoesFinanceirasPrincipais() {
        assertTrue(Arrays.asList(TipoOperacao.values()).contains(TipoOperacao.VENDA_REGISTADA));
        assertTrue(Arrays.asList(TipoOperacao.values()).contains(TipoOperacao.FATURA_EMITIDA));
        assertTrue(Arrays.asList(TipoOperacao.values()).contains(TipoOperacao.FECHO_CAIXA));
    }
}
