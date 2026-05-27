package pt.miniFormiga.subsistemas.utilizadores.facade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.subsistemas.utilizadores.dto.AtualizarUtilizadorCommand;
import pt.miniFormiga.subsistemas.utilizadores.dto.CriarUtilizadorCommand;

import java.util.List;
import java.util.UUID;

public interface ISubUtilizadores {
    Utilizador autenticar(String username, String password);

    Utilizador criarUtilizador(CriarUtilizadorCommand command);

    Page<Utilizador> listarUtilizadores(Pageable pageable);

    Page<Utilizador> listarUtilizadoresPorLoja(UUID lojaId, Pageable pageable);

    Page<Utilizador> listarUtilizadoresPorLojaEPerfis(UUID lojaId, List<PerfilUtilizador> perfis, Pageable pageable);

    Utilizador obterUtilizador(UUID id);

    Utilizador atualizarUtilizador(UUID id, AtualizarUtilizadorCommand command);

    void desativarUtilizador(UUID id);
}
