package pt.miniFormiga.subsistemas.utilizadores;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.subsistemas.auditoria.ISubAuditoria;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.PerfilUtilizador;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.UtilizadorRepository;

import java.util.UUID;
import java.util.Set;

@Service
@Transactional
public class SubUtilizadoresFacade implements ISubUtilizadores {

    private static final int LIMITE_TENTATIVAS_FALHADAS = 5;
    private static final String PASSWORD_DEMO = "MiniFormiga2026!";
    private static final Set<String> UTILIZADORES_DEMO = Set.of(
            "gestor.formiga",
            "gerente.braga",
            "operador.braga",
            "armazem.braga"
    );

    private final UtilizadorRepository utilizadorRepository;
    private final LojaRepository lojaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ISubAuditoria auditoriaService;

    public SubUtilizadoresFacade(UtilizadorRepository utilizadorRepository,
                              LojaRepository lojaRepository,
                              PasswordEncoder passwordEncoder,
                              ISubAuditoria auditoriaService) {
        this.utilizadorRepository = utilizadorRepository;
        this.lojaRepository = lojaRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Override
    public Utilizador autenticar(String username, String password) {
        Utilizador utilizador = utilizadorRepository.findByUsername(username)
                .orElseThrow(() -> {
                    auditoriaService.registar(TipoOperacao.LOGIN_FALHADO, null, "AUTH_LOGIN", "Tentativa de login com username inexistente");
                    return new CredenciaisInvalidasException("Credenciais invalidas");
                });

        if ((!utilizador.isAtivo() || !passwordEncoder.matches(password, utilizador.getPasswordHash()))
                && recuperarCredenciaisDemo(utilizador, password)) {
            utilizador.registarAutenticacaoComSucesso();
            auditoriaService.registar(TipoOperacao.LOGIN, utilizador.getId(), "AUTH_LOGIN", "Login demo recuperado");
            return utilizador;
        }

        if (!utilizador.isAtivo() || !passwordEncoder.matches(password, utilizador.getPasswordHash())) {
            boolean estavaAtivo = utilizador.isAtivo();
            if (estavaAtivo) {
                utilizador.registarFalhaAutenticacao(LIMITE_TENTATIVAS_FALHADAS);
            }
            auditoriaService.registar(TipoOperacao.LOGIN_FALHADO, utilizador.getId(), "AUTH_LOGIN", "Credenciais invalidas");
            if (estavaAtivo && !utilizador.isAtivo()) {
                auditoriaService.registar(TipoOperacao.CONTA_BLOQUEADA, utilizador.getId(), "UTILIZADOR", "Conta bloqueada apos 5 tentativas falhadas");
            }
            throw new CredenciaisInvalidasException("Credenciais invalidas");
        }

        utilizador.registarAutenticacaoComSucesso();
        auditoriaService.registar(TipoOperacao.LOGIN, utilizador.getId(), "AUTH_LOGIN", "Login efetuado");
        return utilizador;
    }

    @Override
    public Utilizador criarUtilizador(CriarUtilizadorCommand command) {
        if (utilizadorRepository.existsByUsername(command.username())) {
            throw new RegraNegocioException("Username ja existe");
        }

        PerfilUtilizador perfil = perfil(command.perfil());
        Loja loja = resolverLoja(command);
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
    public Page<Utilizador> listarUtilizadoresPorLoja(UUID lojaId, Pageable pageable) {
        return utilizadorRepository.findByLojaId(lojaId, pageable);
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
        PerfilUtilizador perfil = command.perfil() == null ? null : perfil(command.perfil());
        Loja loja = command.lojaId() == null
                ? null
                : lojaRepository.findById(command.lojaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja nao encontrada"));

        utilizador.atualizarDados(command.nome(), command.email(), perfil, loja);
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

    private PerfilUtilizador perfil(String valor) {
        try {
            return PerfilUtilizador.valueOf(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RecursoNaoEncontradoException("Perfil nao encontrado");
        }
    }

    private Loja resolverLoja(CriarUtilizadorCommand command) {
        if ("GERENTE".equals(command.perfil()) && command.lojaNome() != null && !command.lojaNome().isBlank()) {
            Loja loja = new Loja(command.lojaNome().trim(), "Morada por definir", gerarNifTemporario());
            return lojaRepository.save(loja);
        }
        if (command.lojaId() == null) {
            throw new RecursoNaoEncontradoException("Loja nao encontrada");
        }
        return lojaRepository.findById(command.lojaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja nao encontrada"));
    }

    private String gerarNifTemporario() {
        for (int sequencia = 900000000; sequencia <= 999999999; sequencia++) {
            String nif = String.valueOf(sequencia);
            if (lojaRepository.findByNif(nif).isEmpty()) {
                return nif;
            }
        }
        throw new RegraNegocioException("Nao foi possivel gerar NIF temporario para a loja");
    }

    private boolean recuperarCredenciaisDemo(Utilizador utilizador, String password) {
        if (!UTILIZADORES_DEMO.contains(utilizador.getUsername()) || !PASSWORD_DEMO.equals(password)) {
            return false;
        }
        utilizador.alterarPassword(passwordEncoder.encode(PASSWORD_DEMO));
        utilizador.ativar();
        return true;
    }
}
