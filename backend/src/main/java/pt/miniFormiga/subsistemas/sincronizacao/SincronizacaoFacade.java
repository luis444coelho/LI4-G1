package pt.miniFormiga.subsistemas.sincronizacao;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SincronizacaoFacade implements ISubSincronizacao {
    @Override
    public void agendarSincronizacao(UUID lojaId) {
        // SubSincronizacao completo sera implementado na fase seguinte.
    }
}
