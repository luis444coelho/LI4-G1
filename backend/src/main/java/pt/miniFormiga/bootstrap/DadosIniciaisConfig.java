package pt.miniFormiga.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.PerfilRepository;
import pt.miniFormiga.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;

import java.util.List;

@Configuration
public class DadosIniciaisConfig {

    @Bean
    @Transactional
    CommandLineRunner dadosIniciais(LojaRepository lojaRepository,
                                    PerfilRepository perfilRepository,
                                    UtilizadorRepository utilizadorRepository,
                                    PasswordEncoder passwordEncoder) {
        return args -> {
            Loja loja = lojaRepository.findAll().stream()
                    .findFirst()
                    .orElseGet(() -> lojaRepository.save(new Loja("Loja Braga", "Rua Central", "123456789", "253000000")));

            Perfil gestor = criarPerfilSeNecessario(perfilRepository, "GESTOR", List.of(
                    Permissao.GLOBAL_ADMIN,
                    Permissao.UTILIZADORES_READ,
                    Permissao.UTILIZADORES_WRITE,
                    Permissao.UTILIZADORES_DELETE,
                    Permissao.RELATORIOS_READ,
                    Permissao.SINCRONIZACAO_WRITE
            ));
            criarPerfilSeNecessario(perfilRepository, "GERENTE", List.of(
                    Permissao.UTILIZADORES_READ,
                    Permissao.UTILIZADORES_WRITE,
                    Permissao.STOCK_WRITE,
                    Permissao.ENCOMENDAS_WRITE,
                    Permissao.RELATORIOS_READ
            ));
            criarPerfilSeNecessario(perfilRepository, "FUNCIONARIO", List.of(Permissao.PDV_WRITE));
            criarPerfilSeNecessario(perfilRepository, "RESPONSAVEL_ARMAZEM", List.of(
                    Permissao.STOCK_READ,
                    Permissao.STOCK_WRITE,
                    Permissao.ENCOMENDAS_WRITE
            ));

            if (!utilizadorRepository.existsByUsername("gestor.formiga")) {
                utilizadorRepository.save(new Utilizador(
                        "gestor.formiga",
                        passwordEncoder.encode("MiniFormiga2026!"),
                        "Sr. Formiga",
                        "gestor@mini-formiga.pt",
                        gestor,
                        loja
                ));
            }
        };
    }

    private Perfil criarPerfilSeNecessario(PerfilRepository repository, String nome, List<String> permissoes) {
        return repository.findByNome(nome)
                .orElseGet(() -> repository.save(new Perfil(nome, permissoes)));
    }
}
