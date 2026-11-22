package br.com.rachaplus.api.domain;

import java.util.Arrays;
import java.util.List;

public enum Posicao {
    // FUTEBOL
    GOLEIRO(Esporte.FUTEBOL),
    ZAGUEIRO(Esporte.FUTEBOL),
    MEIO_CAMPISTA(Esporte.FUTEBOL),
    ATACANTE(Esporte.FUTEBOL),

    // BASQUETE
    ARMADOR(Esporte.BASQUETE),
    ALA(Esporte.BASQUETE),
    PIVO(Esporte.BASQUETE),

    // VOLEI
    LEVANTADOR(Esporte.VOLEI),
    PONTEIRO(Esporte.VOLEI),
    CENTRAL(Esporte.VOLEI),
    OPOSTO(Esporte.VOLEI),
    LIBERO(Esporte.VOLEI);

    private final Esporte esporte;

    Posicao(Esporte esporte) {
        this.esporte = esporte;
    }

    public Esporte getEsporte() {
        return esporte;
    }

    public boolean pertenceA(Esporte esporte) {
        return this.esporte == esporte;
    }

    public static List<Posicao> doEsporte(Esporte esporte) {
        return Arrays.stream(values())
                .filter(posicao -> posicao.pertenceA(esporte))
                .toList();
    }
}
