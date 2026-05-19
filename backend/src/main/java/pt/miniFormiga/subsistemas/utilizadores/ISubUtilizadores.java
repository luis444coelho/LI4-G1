package pt.miniFormiga.subsistemas.utilizadores;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.miniFormiga.domain.Utilizador;

import java.util.UUID;

public interface ISubUtilizadores {
    Utilizador autenticar(String username, String password);

    Utilizador criarUtilizador(CriarUtilizadorCommand command);

    Page<Utilizador> listarUtilizadores(Pageable pageable);

    Page<Utilizador> listarUtilizadoresPorLoja(UUID lojaId, Pageable pageable);

    Utilizador obterUtilizador(UUID id);

    Utilizador atualizarUtilizador(UUID id, AtualizarUtilizadorCommand command);

    void desativarUtilizador(UUID id);
}
