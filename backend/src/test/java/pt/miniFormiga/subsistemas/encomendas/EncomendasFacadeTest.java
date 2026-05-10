package pt.miniFormiga.subsistemas.encomendas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.miniFormiga.auditoria.AuditoriaService;
import pt.miniFormiga.domain.Categoria;
import pt.miniFormiga.domain.Encomenda;
import pt.miniFormiga.domain.EntradaMercadoria;
import pt.miniFormiga.domain.EstadoEncomenda;
import pt.miniFormiga.domain.Fornecedor;
import pt.miniFormiga.domain.Loja;
import pt.miniFormiga.domain.Perfil;
import pt.miniFormiga.domain.Produto;
import pt.miniFormiga.domain.TaxaIVA;
import pt.miniFormiga.domain.TipoOperacao;
import pt.miniFormiga.domain.Utilizador;
import pt.miniFormiga.repository.CondicaoComercialRepository;
import pt.miniFormiga.repository.EncomendaRepository;
import pt.miniFormiga.repository.EntradaMercadoriaRepository;
import pt.miniFormiga.repository.EstadoEncomendaRepository;
import pt.miniFormiga.repository.FornecedorRepository;
import pt.miniFormiga.repository.GuiaRemessaRepository;
import pt.miniFormiga.repository.LojaRepository;
import pt.miniFormiga.repository.ProdutoRepository;
import pt.miniFormiga.repository.UtilizadorRepository;
import pt.miniFormiga.subsistemas.stock.ISubStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pt.miniFormiga.subsistemas.encomendas.EncomendasDtos.*;

@ExtendWith(MockitoExtension.class)
class EncomendasFacadeTest {

    @Mock FornecedorRepository fornecedorRepository;
    @Mock CondicaoComercialRepository condicaoComercialRepository;
    @Mock EncomendaRepository encomendaRepository;
    @Mock EstadoEncomendaRepository estadoEncomendaRepository;
    @Mock GuiaRemessaRepository guiaRemessaRepository;
    @Mock EntradaMercadoriaRepository entradaMercadoriaRepository;
    @Mock LojaRepository lojaRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock UtilizadorRepository utilizadorRepository;
    @Mock ISubStock stock;
    @Mock AuditoriaService auditoria;

    EncomendasFacade facade;
    Loja loja;
    Fornecedor fornecedor;
    Produto produto;
    Utilizador responsavel;

    @BeforeEach
    void setUp() {
        facade = new EncomendasFacade(
                fornecedorRepository,
                condicaoComercialRepository,
                encomendaRepository,
                estadoEncomendaRepository,
                guiaRemessaRepository,
                entradaMercadoriaRepository,
                lojaRepository,
                produtoRepository,
                utilizadorRepository,
                stock,
                auditoria
        );
        loja = new Loja("Loja Braga", "Rua Central", "123456789");
        fornecedor = new Fornecedor("Fornecedor Norte", "987654321", "Rua Norte", "229000000",
                "norte@mini-formiga.pt", LocalTime.of(8, 0), LocalTime.of(18, 0));
        produto = new Produto("5600000000110", "Agua", new BigDecimal("1.00"), new BigDecimal("0.40"),
                new TaxaIVA("Normal", new BigDecimal("23")), new Categoria("Bebidas", "Bebidas"));
        responsavel = new Utilizador("armazem", "hash", "Armazem",
                new Perfil("RESPONSAVEL_ARMAZEM", List.of("ENCOMENDAS_WRITE")), loja);
    }

    @Test
    void criarEncomendaDefineEstadoPendenteEDataProcessamentoDoFornecedor() {
        EstadoEncomenda pendente = new EstadoEncomenda("PENDENTE", "Pendente");
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(fornecedorRepository.findById(fornecedor.getId())).thenReturn(Optional.of(fornecedor));
        when(estadoEncomendaRepository.findByCodigo("PENDENTE")).thenReturn(Optional.of(pendente));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EncomendaResponse response = facade.criarEncomenda(new CriarEncomendaRequest(
                loja.getId(),
                fornecedor.getId(),
                List.of(new CriarLinhaEncomendaRequest(produto.getId(), 2, new BigDecimal("0.60")))
        ));

        assertEquals("PENDENTE", response.estado());
        assertEquals(new BigDecimal("1.20"), response.totalEstimado());
        assertEquals(1, response.linhas().size());
        verify(auditoria).registar(TipoOperacao.ENCOMENDA_CRIADA, null, "ENCOMENDA", "Encomenda criada");
    }

    @Test
    void registarEntradaAtualizaStockEMarcaEncomendaComoRecebida() {
        EstadoEncomenda pendente = new EstadoEncomenda("PENDENTE", "Pendente");
        EstadoEncomenda recebida = new EstadoEncomenda("RECEBIDA", "Recebida");
        Encomenda encomenda = new Encomenda(loja, fornecedor, pendente);
        when(encomendaRepository.findById(encomenda.getId())).thenReturn(Optional.of(encomenda));
        when(lojaRepository.findById(loja.getId())).thenReturn(Optional.of(loja));
        when(utilizadorRepository.findById(responsavel.getId())).thenReturn(Optional.of(responsavel));
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
        when(guiaRemessaRepository.findByNumero("GR-1")).thenReturn(Optional.empty());
        when(guiaRemessaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(estadoEncomendaRepository.findByCodigo("RECEBIDA")).thenReturn(Optional.of(recebida));
        when(entradaMercadoriaRepository.save(any(EntradaMercadoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EntradaMercadoriaResponse response = facade.registarEntradaMercadoria(new RegistarEntradaMercadoriaRequest(
                encomenda.getId(),
                loja.getId(),
                responsavel.getId(),
                produto.getId(),
                "GR-1",
                LocalDate.now(),
                LocalDate.now(),
                8,
                10,
                "Entrega parcial"
        ));

        assertEquals(-2, response.discrepancia());
        assertEquals("RECEBIDA", encomenda.getEstado().getCodigo());
        verify(stock).atualizarStock(produto.getId(), loja.getId(), 8);
        verify(auditoria).registar(TipoOperacao.ENTRADA_MERCADORIA_REGISTADA,
                responsavel.getId(), "ENTRADA_MERCADORIA", "Entrada de mercadoria registada");
    }
}
