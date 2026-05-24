package pt.miniFormiga.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.*;
import pt.miniFormiga.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Configuration
public class DadosIniciaisConfig {
    private static final UUID LOJA_BRAGA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Bean
    @Transactional
    @ConditionalOnProperty(name = "mini-formiga.demo-data.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner dadosIniciais(LojaRepository lojaRepository,
                                    UtilizadorRepository utilizadorRepository,
                                    CategoriaRepository categoriaRepository,
                                    TaxaIVARepository taxaIVARepository,
                                    ProdutoRepository produtoRepository,
                                    ProdutoLojaRepository produtoLojaRepository,
                                    FornecedorRepository fornecedorRepository,
                                    CondicaoComercialRepository condicaoComercialRepository,
                                    PasswordEncoder passwordEncoder) {
        return args -> {
            Loja loja = lojaRepository.findById(LOJA_BRAGA_ID)
                    .or(() -> lojaRepository.findByNif("123456789"))
                    .orElseGet(() -> lojaRepository.save(new Loja(LOJA_BRAGA_ID, "Loja Braga", "Rua Central", "123456789", "253000000")));

            criarUtilizadorSeNecessario(utilizadorRepository, passwordEncoder,
                    "gestor.formiga", "Sr. Formiga", "gestor@mini-formiga.pt", PerfilUtilizador.GESTOR, loja);
            criarUtilizadorSeNecessario(utilizadorRepository, passwordEncoder,
                    "gerente.braga", "Gerente Braga", "gerente@mini-formiga.pt", PerfilUtilizador.GERENTE, loja);
            criarUtilizadorSeNecessario(utilizadorRepository, passwordEncoder,
                    "operador.braga", "Operador Braga", "operador@mini-formiga.pt", PerfilUtilizador.FUNCIONARIO, loja);
            criarUtilizadorSeNecessario(utilizadorRepository, passwordEncoder,
                    "armazem.braga", "Responsavel Armazem", "armazem@mini-formiga.pt", PerfilUtilizador.ARMAZEM, loja);

            TaxaIVA reduzida = criarTaxaSeNecessaria(taxaIVARepository, "Taxa Reduzida", new BigDecimal("6"));
            TaxaIVA normal = criarTaxaSeNecessaria(taxaIVARepository, "Taxa Normal", new BigDecimal("23"));
            Categoria bebidas = criarCategoriaSeNecessaria(categoriaRepository, "Bebidas", "Bebidas frias e sumos");
            Categoria snacks = criarCategoriaSeNecessaria(categoriaRepository, "Snacks", "Snacks e produtos prontos");
            Categoria higiene = criarCategoriaSeNecessaria(categoriaRepository, "Higiene", "Produtos de higiene");
            Categoria mercearia = criarCategoriaSeNecessaria(categoriaRepository, "Mercearia", "Artigos essenciais de mercearia");

            Produto agua = criarProdutoSeNecessario(produtoRepository, "5600000000011", "Agua 0.5L", "Garrafa de agua 0.5L", new BigDecimal("1.00"), new BigDecimal("0.40"), bebidas, normal, 50);
            Produto sandes = criarProdutoSeNecessario(produtoRepository, "5600000000028", "Sandes Mista", "Sandes pronta", new BigDecimal("2.50"), new BigDecimal("1.20"), snacks, reduzida, 50);
            Produto champo = criarProdutoSeNecessario(produtoRepository, "5600000000035", "Champo 200ml", "Champo de higiene pessoal", new BigDecimal("3.50"), new BigDecimal("1.80"), higiene, normal, 50);
            Produto acucar = criarProdutoSeNecessario(produtoRepository, "5600000000042", "Acucar 1kg", "Acucar branco 1kg", new BigDecimal("1.80"), new BigDecimal("0.90"), mercearia, reduzida, 50);
            criarProdutoLojaSeNecessario(produtoLojaRepository, agua, loja, 50, 10);
            criarProdutoLojaSeNecessario(produtoLojaRepository, sandes, loja, 50, 10);
            criarProdutoLojaSeNecessario(produtoLojaRepository, champo, loja, 50, 10);
            criarProdutoLojaSeNecessario(produtoLojaRepository, acucar, loja, 50, 10);

            Fornecedor fornecedor = criarFornecedorSeNecessario(fornecedorRepository);
            criarCondicaoSeNecessaria(condicaoComercialRepository, fornecedor, agua);
        };
    }

    private void criarUtilizadorSeNecessario(UtilizadorRepository repository,
                                             PasswordEncoder passwordEncoder,
                                             String username,
                                             String nome,
                                             String email,
                                             PerfilUtilizador perfil,
                                             Loja loja) {
        if (!repository.existsByUsername(username)) {
            repository.save(new Utilizador(
                    username,
                    passwordEncoder.encode("MiniFormiga2026!"),
                    nome,
                    email,
                    perfil,
                    loja
            ));
        }
    }

    private Categoria criarCategoriaSeNecessaria(CategoriaRepository repository, String nome, String descricao) {
        return repository.findByNome(nome).orElseGet(() -> repository.save(new Categoria(nome, descricao)));
    }

    private TaxaIVA criarTaxaSeNecessaria(TaxaIVARepository repository, String descricao, BigDecimal percentagem) {
        return repository.findByPercentagem(percentagem).orElseGet(() -> repository.save(new TaxaIVA(descricao, percentagem)));
    }

    private Produto criarProdutoSeNecessario(ProdutoRepository produtoRepository,
                                             String codigo,
                                             String nome,
                                             String descricao,
                                             BigDecimal precoVenda,
                                             BigDecimal precoCusto,
                                             Categoria categoria,
                                             TaxaIVA taxaIVA,
                                             int quantidade) {
        return produtoRepository.findByCodigo(codigo).orElseGet(() -> {
            Produto novo = new Produto(codigo, nome, precoVenda, precoCusto, taxaIVA, categoria);
            novo.atualizar(null, descricao, null, null, null, null, null, null);
            novo.definirStockInicial(quantidade);
            novo.definirNivelMinimo(10);
            return produtoRepository.save(novo);
        });
    }

    private Fornecedor criarFornecedorSeNecessario(FornecedorRepository repository) {
        return repository.findByNif("987654321").orElseGet(() -> repository.save(new Fornecedor(
                "Fornecedor Norte",
                "987654321",
                "Rua do Armazem",
                "229000000",
                "fornecedor@mini-formiga.pt",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0)
        )));
    }

    private void criarProdutoLojaSeNecessario(ProdutoLojaRepository repository,
                                              Produto produto,
                                              Loja loja,
                                              int quantidade,
                                              int nivelMinimo) {
        repository.findByProdutoIdAndLojaId(produto.getId(), loja.getId())
                .orElseGet(() -> repository.save(new ProdutoLoja(produto, loja, quantidade, nivelMinimo)));
    }

    private void criarCondicaoSeNecessaria(CondicaoComercialRepository repository, Fornecedor fornecedor, Produto produto) {
        repository.findByFornecedorIdAndProdutoId(fornecedor.getId(), produto.getId())
                .orElseGet(() -> repository.save(new CondicaoComercial(
                        fornecedor,
                        produto,
                        new BigDecimal("0.60"),
                        2,
                        10,
                        LocalDate.now()
                )));
    }
}
