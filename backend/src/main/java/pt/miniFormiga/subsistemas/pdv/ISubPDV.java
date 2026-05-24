package pt.miniFormiga.subsistemas.pdv;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.miniFormiga.domain.Fatura;
import pt.miniFormiga.domain.FechoCaixa;
import pt.miniFormiga.domain.LinhaVenda;
import pt.miniFormiga.domain.Venda;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.pdv.PdvDtos.*;

public interface ISubPDV {
    ProdutoDTO criarProduto(CriarProdutoRequest request);
    ProdutoDTO atualizarProduto(UUID id, AtualizarProdutoRequest request);
    ProdutoDTO obterProdutoPorId(UUID id);
    ProdutoDTO obterProdutoPorCodigoBarras(String codigoBarras);
    Page<ProdutoDTO> listarProdutos(UUID lojaId, Pageable pageable);
    Venda registarVenda(UUID lojaId, UUID utilizadorId);
    LinhaVenda adicionarLinhaVenda(UUID vendaId, UUID produtoId, Integer quantidade);
    Venda finalizarVenda(UUID vendaId, String meioPagamento);
    void anularVenda(UUID vendaId);
    void anularLinhaVenda(UUID vendaId, UUID linhaId);
    Fatura emitirFatura(UUID vendaId, String nifCliente, String nomeCliente);
    FechoCaixa registarFechoCaixa(UUID lojaId, UUID utilizadorId);
    FechoCaixa confirmarFechoCaixa(UUID fechoCaixaId, String observacoesDiscrepancia);
    List<Venda> getVendasPorLoja(UUID lojaId, LocalDate inicio, LocalDate fim);
    List<FechoCaixa> getFechoCaixaByLoja(UUID lojaId);
    VendaDTO obterVenda(UUID vendaId);
    Page<VendaDTO> listarVendas(UUID lojaId, LocalDate inicio, LocalDate fim, Pageable pageable);
    Page<VendaDTO> listarVendasPorFechar(UUID lojaId, LocalDate inicio, LocalDate fim, Pageable pageable);
    FaturaDTO obterFatura(UUID faturaId);
    Page<FechoCaixaDTO> listarFechosCaixa(UUID lojaId, Pageable pageable);
    FechoCaixaDTO obterFechoCaixa(UUID fechoCaixaId);
}
