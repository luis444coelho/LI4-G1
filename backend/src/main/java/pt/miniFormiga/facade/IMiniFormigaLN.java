package pt.miniFormiga.facade;

import pt.miniFormiga.subsistemas.pdv.ISubPDV;
import pt.miniFormiga.subsistemas.utilizadores.ISubUtilizadores;

public interface IMiniFormigaLN {
    ISubUtilizadores utilizadores();
    ISubPDV pdv();
}
