package pt.miniFormiga.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import pt.miniFormiga.domain.*;
import pt.miniFormiga.repository.*;
import pt.miniFormiga.subsistemas.utilizadores.Permissao;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DadosIniciaisConfig {

    @Bean
    @Transactional
    CommandLineRunner dadosIniciais(LojaRepository lojaRepository,
                                    PerfilRepository perfilRepository,
                                    UtilizadorRepository utilizadorRepository,
                                    CategoriaRepository categoriaRepository,
                                    TaxaIVARepository taxaIVARepository,
                                    ProdutoRepository produtoRepository,
                                    StockRepository stockRepository,
                                    NivelMinimoRepository nivelMinimoRepository,
                                    MotivoAjusteRepository motivoAjusteRepository,
                                    MeioPagamentoRepository meioPagamentoRepository,
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
            criarPerfilSeNecessario(perfilRepository, "RESP_ARMAZEM", List.of(
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

            TaxaIVA reduzida = criarTaxaSeNecessaria(taxaIVARepository, "Taxa Reduzida", new BigDecimal("6"));
            TaxaIVA intermedia = criarTaxaSeNecessaria(taxaIVARepository, "Taxa Intermedia", new BigDecimal("13"));
            TaxaIVA normal = criarTaxaSeNecessaria(taxaIVARepository, "Taxa Normal", new BigDecimal("23"));
            Categoria bebidas = criarCategoriaSeNecessaria(categoriaRepository, "Bebidas", "Bebidas frias e sumos");
            Categoria snacks = criarCategoriaSeNecessaria(categoriaRepository, "Snacks", "Snacks e produtos prontos");
            Categoria higiene = criarCategoriaSeNecessaria(categoriaRepository, "Higiene", "Produtos de higiene");
            Categoria mercearia = criarCategoriaSeNecessaria(categoriaRepository, "Mercearia", "Artigos essenciais de mercearia");

            criarProdutoSeNecessario(produtoRepository, stockRepository, nivelMinimoRepository, loja, "5600000000011", "Agua 0.5L", "Garrafa de agua 0.5L", new BigDecimal("1.00"), new BigDecimal("0.40"), bebidas, normal, 50);
            criarProdutoSeNecessario(produtoRepository, stockRepository, nivelMinimoRepository, loja, "5600000000028", "Sandes Mista", "Sandes pronta", new BigDecimal("2.50"), new BigDecimal("1.20"), snacks, reduzida, 50);
            criarProdutoSeNecessario(produtoRepository, stockRepository, nivelMinimoRepository, loja, "5600000000035", "Champo 200ml", "Champo de higiene pessoal", new BigDecimal("3.50"), new BigDecimal("1.80"), higiene, normal, 50);
            criarProdutoSeNecessario(produtoRepository, stockRepository, nivelMinimoRepository, loja, "5600000000042", "Acucar 1kg", "Acucar branco 1kg", new BigDecimal("1.80"), new BigDecimal("0.90"), mercearia, reduzida, 50);

            criarMeioPagamentoSeNecessario(meioPagamentoRepository, "NUMERARIO", "Pagamento em numerario");
            criarMeioPagamentoSeNecessario(meioPagamentoRepository, "CARTAO", "Pagamento por cartao bancario");
            criarMeioPagamentoSeNecessario(meioPagamentoRepository, "MBWAY", "Pagamento por MB Way");

            criarMotivoAjusteSeNecessario(motivoAjusteRepository, "QUEBRA", "Produto danificado ou partido");
            criarMotivoAjusteSeNecessario(motivoAjusteRepository, "DESPERDICIO", "Produto fora de prazo ou deteriorado");
            criarMotivoAjusteSeNecessario(motivoAjusteRepository, "CORRECAO_ERRO", "Correcao de erro de registo");
        };
    }

    private Perfil criarPerfilSeNecessario(PerfilRepository repository, String nome, List<String> permissoes) {
        return repository.findByNome(nome)
                .orElseGet(() -> repository.save(new Perfil(nome, permissoes)));
    }

    private Categoria criarCategoriaSeNecessaria(CategoriaRepository repository, String nome, String descricao) {
        return repository.findByNome(nome).orElseGet(() -> repository.save(new Categoria(nome, descricao)));
    }

    private TaxaIVA criarTaxaSeNecessaria(TaxaIVARepository repository, String descricao, BigDecimal percentagem) {
        return repository.findByPercentagem(percentagem).orElseGet(() -> repository.save(new TaxaIVA(descricao, percentagem)));
    }

    private void criarProdutoSeNecessario(ProdutoRepository produtoRepository,
                                          StockRepository stockRepository,
                                          NivelMinimoRepository nivelMinimoRepository,
                                          Loja loja,
                                          String codigo,
                                          String nome,
                                          String descricao,
                                          BigDecimal precoVenda,
                                          BigDecimal precoCusto,
                                          Categoria categoria,
                                          TaxaIVA taxaIVA,
                                          int quantidade) {
        Produto produto = produtoRepository.findByCodigo(codigo).orElseGet(() -> {
            Produto novo = new Produto(codigo, nome, precoVenda, precoCusto, taxaIVA, categoria);
            novo.atualizar(null, descricao, null, null, null, null, null, null);
            return produtoRepository.save(novo);
        });
        Stock stock = stockRepository.findByProdutoIdAndLojaId(produto.getId(), loja.getId())
                .orElseGet(() -> stockRepository.save(new Stock(produto, loja, quantidade)));
        nivelMinimoRepository.findByStockId(stock.getId())
                .orElseGet(() -> nivelMinimoRepository.save(new NivelMinimo(stock, 10)));
    }

    private void criarMeioPagamentoSeNecessario(MeioPagamentoRepository repository, String tipo, String descricao) {
        repository.findByTipo(tipo).orElseGet(() -> repository.save(new MeioPagamento(tipo, descricao)));
    }

    private void criarMotivoAjusteSeNecessario(MotivoAjusteRepository repository, String codigo, String descricao) {
        repository.findByCodigo(codigo).orElseGet(() -> repository.save(new MotivoAjuste(codigo, descricao)));
    }
}
