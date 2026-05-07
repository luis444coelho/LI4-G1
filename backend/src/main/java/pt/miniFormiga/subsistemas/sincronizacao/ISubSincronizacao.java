package pt.miniFormiga.subsistemas.sincronizacao;

import java.util.UUID;

public interface ISubSincronizacao {
    void agendarSincronizacao(UUID lojaId);
}
