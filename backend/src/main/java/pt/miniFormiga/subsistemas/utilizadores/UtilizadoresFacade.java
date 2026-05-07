package pt.miniFormiga.subsistemas.utilizadores;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.PerfilRepository;
import pt.miniFormiga.repository.UtilizadorRepository;

import java.util.UUID;

@Service
@Transactional
public class UtilizadoresFacade implements ISubUtilizadores {

    private final UtilizadorRepository utilizadorRepository;
    private final PerfilRepository perfilRepository;
    private final LojaRepository lojaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UtilizadoresFacade(UtilizadorRepository utilizadorRepository,
                              PerfilRepository perfilRepository,
                              LojaRepository lojaRepository,
                              PasswordEncoder passwordEncoder,
                              AuditoriaService auditoriaService) {
        this.utilizadorRepository = utilizadorRepository;
        this.perfilRepository = perfilRepository;
        this.lojaRepository = lojaRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional(readOnly = true)
    public Utilizador autenticar(String username, String password) {
        Utilizador utilizador = utilizadorRepository.findByUsername(username)
                .orElseThrow(() -> new CredenciaisInvalidasException("Credenciais invalidas"));

        if (!utilizador.isAtivo() || !passwordEncoder.matches(password, utilizador.getPasswordHash())) {
            throw new CredenciaisInvalidasException("Credenciais invalidas");
        }

        auditoriaService.registar(TipoOperacao.LOGIN, utilizador.getId(), "AUTH_LOGIN", "Login efetuado");
        return utilizador;
    }

    @Override
    public Utilizador criarUtilizador(CriarUtilizadorCommand command) {
        if (utilizadorRepository.existsByUsername(command.username())) {
            throw new RegraNegocioException("Username ja existe");
        }

        Perfil perfil = perfilRepository.findById(command.perfilId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil nao encontrado"));
        Loja loja = lojaRepository.findById(command.lojaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja nao encontrada"));
        Utilizador utilizador = new Utilizador(
                command.username(),
                passwordEncoder.encode(command.password()),
                command.nome(),
                command.email(),
                perfil,
                loja
        );

        Utilizador guardado = utilizadorRepository.save(utilizador);
        auditoriaService.registar(TipoOperacao.UTILIZADOR_CRIADO, guardado.getId(), "UTILIZADOR_CRIADO", "Utilizador criado");
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Utilizador> listarUtilizadores(Pageable pageable) {
        return utilizadorRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Utilizador obterUtilizador(UUID id) {
        return utilizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
    }

    @Override
    public Utilizador atualizarUtilizador(UUID id, AtualizarUtilizadorCommand command) {
        Utilizador utilizador = obterUtilizador(id);
        if (command.password() != null && !command.password().isBlank()) {
            utilizador.alterarPassword(passwordEncoder.encode(command.password()));
        }
        if (command.ativo() != null) {
            if (command.ativo()) {
                utilizador.ativar();
            } else {
                utilizador.desativar();
            }
        }

        auditoriaService.registar(TipoOperacao.UTILIZADOR_EDITADO, utilizador.getId(), "UTILIZADOR_ATUALIZADO", "Utilizador atualizado");
        return utilizador;
    }

    @Override
    public void desativarUtilizador(UUID id) {
        Utilizador utilizador = obterUtilizador(id);
        utilizador.desativar();
        auditoriaService.registar(TipoOperacao.UTILIZADOR_DESATIVADO, utilizador.getId(), "UTILIZADOR_DESATIVADO", "Utilizador desativado");
    }
}
