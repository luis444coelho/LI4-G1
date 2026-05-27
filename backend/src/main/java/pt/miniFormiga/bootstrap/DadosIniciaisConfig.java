package pt.miniFormiga.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.*;
import pt.miniFormiga.subsistemas.catalogo.repository.CategoriaRepository;
import pt.miniFormiga.subsistemas.catalogo.repository.ProdutoRepository;
import pt.miniFormiga.subsistemas.catalogo.repository.TaxaIVARepository;
import pt.miniFormiga.subsistemas.encomendas.repository.CondicaoComercialRepository;
import pt.miniFormiga.subsistemas.encomendas.repository.FornecedorRepository;
import pt.miniFormiga.subsistemas.lojas.repository.LojaRepository;
import pt.miniFormiga.subsistemas.stock.repository.StockProdutoLojaRepository;
import pt.miniFormiga.subsistemas.utilizadores.repository.UtilizadorRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Configuration
public class DadosIniciaisConfig {
    private static final UUID LOJA_BRAGA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LOJA_PORTO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LOJA_LISBOA_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Bean
    @Transactional
    @ConditionalOnProperty(name = "mini-formiga.demo-data.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner dadosIniciais(LojaRepository lojaRepository,
                                    UtilizadorRepository utilizadorRepository,
                                    CategoriaRepository categoriaRepository,
                                    TaxaIVARepository taxaIVARepository,
                                    ProdutoRepository produtoRepository,
                                    StockProdutoLojaRepository stockProdutoLojaRepository,
                                    FornecedorRepository fornecedorRepository,
                                    CondicaoComercialRepository condicaoComercialRepository,
                                    PasswordEncoder passwordEncoder) {
        return args -> {
            Loja lojaBraga = criarLojaSeNecessaria(lojaRepository, LOJA_BRAGA_ID,
                    "Loja Braga", "Rua Central", "123456789", "253000000");
            Loja lojaPorto = criarLojaSeNecessaria(lojaRepository, LOJA_PORTO_ID,
                    "Loja Porto", "Rua de Santa Catarina", "223456789", "222000000");
            Loja lojaLisboa = criarLojaSeNecessaria(lojaRepository, LOJA_LISBOA_ID,
                    "Loja Lisboa", "Avenida da Liberdade", "323456789", "210000000");

            criarUtilizadorSeNecessario(utilizadorRepository, passwordEncoder,
                    "gestor.formiga", "Sr. Formiga", "gestor@mini-formiga.pt", PerfilUtilizador.GESTOR, lojaBraga);
            criarUtilizadorSeNecessario(utilizadorRepository, passwordEncoder,
                    "gerente.braga", "Gerente Braga", "gerente@mini-formiga.pt", PerfilUtilizador.GERENTE, lojaBraga);
            criarUtilizadorSeNecessario(utilizadorRepository, passwordEncoder,
                    "operador.braga", "Operador Braga", "operador@mini-formiga.pt", PerfilUtilizador.FUNCIONARIO, lojaBraga);
            criarUtilizadorSeNecessario(utilizadorRepository, passwordEncoder,
                    "armazem.braga", "Responsavel Armazem", "armazem@mini-formiga.pt", PerfilUtilizador.ARMAZEM, lojaBraga);
            criarUtilizadoresDemoDaLoja(utilizadorRepository, passwordEncoder, lojaPorto,
                    "porto", "Porto", "porto");
            criarUtilizadoresDemoDaLoja(utilizadorRepository, passwordEncoder, lojaLisboa,
                    "lisboa", "Lisboa", "lisboa");

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
            criarProdutosBaseDaLoja(stockProdutoLojaRepository, lojaBraga, agua, sandes, champo, acucar);
            criarProdutosBaseDaLoja(stockProdutoLojaRepository, lojaPorto, agua, sandes, champo, acucar);
            criarProdutosBaseDaLoja(stockProdutoLojaRepository, lojaLisboa, agua, sandes, champo, acucar);

            Fornecedor fornecedor = criarFornecedorSeNecessario(fornecedorRepository);
            criarCondicaoSeNecessaria(condicaoComercialRepository, fornecedor, agua);
            criarCondicaoSeNecessaria(condicaoComercialRepository, fornecedor, sandes);
            criarCondicaoSeNecessaria(condicaoComercialRepository, fornecedor, champo);
            criarCondicaoSeNecessaria(condicaoComercialRepository, fornecedor, acucar);
        };
    }

    private Loja criarLojaSeNecessaria(LojaRepository repository,
                                       UUID id,
                                       String nome,
                                       String morada,
                                       String nif,
                                       String telefone) {
        return repository.findById(id)
                .or(() -> repository.findByNif(nif))
                .orElseGet(() -> repository.save(new Loja(id, nome, morada, nif, telefone)));
    }

    private void criarUtilizadoresDemoDaLoja(UtilizadorRepository repository,
                                             PasswordEncoder passwordEncoder,
                                             Loja loja,
                                             String sufixoUsername,
                                             String sufixoNome,
                                             String dominioEmail) {
        criarUtilizadorSeNecessario(repository, passwordEncoder,
                "gestor." + sufixoUsername, "Gestor " + sufixoNome,
                "gestor@" + dominioEmail + ".mini-formiga.pt", PerfilUtilizador.GESTOR, loja);
        criarUtilizadorSeNecessario(repository, passwordEncoder,
                "gerente." + sufixoUsername, "Gerente " + sufixoNome,
                "gerente@" + dominioEmail + ".mini-formiga.pt", PerfilUtilizador.GERENTE, loja);
        criarUtilizadorSeNecessario(repository, passwordEncoder,
                "operador." + sufixoUsername, "Operador " + sufixoNome,
                "operador@" + dominioEmail + ".mini-formiga.pt", PerfilUtilizador.FUNCIONARIO, loja);
        criarUtilizadorSeNecessario(repository, passwordEncoder,
                "armazem." + sufixoUsername, "Responsavel Armazem " + sufixoNome,
                "armazem@" + dominioEmail + ".mini-formiga.pt", PerfilUtilizador.ARMAZEM, loja);
    }

    private void criarUtilizadorSeNecessario(UtilizadorRepository repository,
                                             PasswordEncoder passwordEncoder,
                                             String username,
                                             String nome,
                                             String email,
                                             PerfilUtilizador perfil,
                                             Loja loja) {
        repository.findByUsername(username).ifPresentOrElse(utilizador -> {
            utilizador.alterarPassword(passwordEncoder.encode("MiniFormiga2026!"));
            utilizador.ativar();
        }, () -> repository.save(new Utilizador(
                    username,
                    passwordEncoder.encode("MiniFormiga2026!"),
                    nome,
                    email,
                    perfil,
                    loja
            )));
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

    private void criarStockProdutoLojaSeNecessario(StockProdutoLojaRepository repository,
                                              Produto produto,
                                              Loja loja,
                                              int quantidade,
                                              int nivelMinimo) {
        StockProdutoLoja stockProdutoLoja = repository.findByProdutoIdAndLojaId(produto.getId(), loja.getId())
                .orElseGet(() -> repository.save(new StockProdutoLoja(produto, loja, quantidade, nivelMinimo)));
        stockProdutoLoja.definirNivelMinimo(nivelMinimo);
        if ("5600000000011".equals(produto.getCodigo()) && stockProdutoLoja.getQuantidadeStock() >= nivelMinimo) {
            stockProdutoLoja.definirStockInicial(quantidade);
        }
    }

    private void criarProdutosBaseDaLoja(StockProdutoLojaRepository repository,
                                         Loja loja,
                                         Produto agua,
                                         Produto sandes,
                                         Produto champo,
                                         Produto acucar) {
        criarStockProdutoLojaSeNecessario(repository, agua, loja, 5, 10);
        criarStockProdutoLojaSeNecessario(repository, sandes, loja, 50, 10);
        criarStockProdutoLojaSeNecessario(repository, champo, loja, 50, 10);
        criarStockProdutoLojaSeNecessario(repository, acucar, loja, 50, 10);
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
