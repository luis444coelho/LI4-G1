package pt.miniFormiga.subsistemas.encomendas.facade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static pt.miniFormiga.subsistemas.encomendas.dto.EncomendasDtos.*;

public interface ISubEncomendas {
    Page<FornecedorResponse> listarFornecedores(Pageable pageable);

    FornecedorResponse obterFornecedor(UUID id);

    FornecedorResponse criarFornecedor(CriarFornecedorRequest request);

    FornecedorResponse atualizarFornecedor(UUID id, AtualizarFornecedorRequest request);

    void desativarFornecedor(UUID id);

    CondicaoComercialResponse definirCondicaoComercial(UUID fornecedorId, CondicaoComercialRequest request);

    List<CondicaoComercialResponse> listarCondicoesComerciais(UUID fornecedorId);

    Page<EncomendaResponse> listarEncomendas(UUID lojaId, Pageable pageable);

    EncomendaResponse obterEncomenda(UUID id);

    EncomendaResponse criarEncomenda(CriarEncomendaRequest request);

    List<EncomendaResponse> criarEncomendaConsolidada(CriarEncomendaConsolidadaRequest request);

    EncomendaResponse atualizarEstado(UUID id, AtualizarEstadoEncomendaRequest request);

    List<SugestaoEncomendaResponse> sugerirEncomendas(UUID lojaId, UUID fornecedorId);

    List<EntradaMercadoriaResponse> registarEntradaMercadoria(RegistarEntradaMercadoriaRequest request);

    Page<EntradaMercadoriaResponse> listarEntradasMercadoria(UUID lojaId, Pageable pageable);

    ProximaGuiaRemessaResponse obterProximaGuiaRemessa(UUID lojaId);
}
