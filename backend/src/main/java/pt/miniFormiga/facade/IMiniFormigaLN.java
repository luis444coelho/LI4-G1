package pt.miniFormiga.facade;

import pt.miniFormiga.subsistemas.pdv.ISubPDV;
import pt.miniFormiga.subsistemas.encomendas.ISubEncomendas;
import pt.miniFormiga.subsistemas.sincronizacao.ISubSincronizacao;
import pt.miniFormiga.subsistemas.stock.ISubStock;
import pt.miniFormiga.subsistemas.utilizadores.ISubUtilizadores;

public interface IMiniFormigaLN {
    ISubUtilizadores utilizadores();
    ISubPDV pdv();
    ISubStock stock();
    ISubEncomendas encomendas();
    ISubSincronizacao sincronizacao();
}
