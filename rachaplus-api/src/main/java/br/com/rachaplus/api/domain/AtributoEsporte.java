package br.com.rachaplus.api.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum AtributoEsporte {
    // Atributos de Futebol (alguns compartilhados com Basquete)
    FINALIZACAO(Esporte.FUTEBOL, Esporte.BASQUETE),
    PASSE_VISAO(Esporte.FUTEBOL),
    CONTROLE_BOLA(Esporte.FUTEBOL, Esporte.BASQUETE),
    DEFESA(Esporte.FUTEBOL, Esporte.BASQUETE),
    FISICO_FORCA(Esporte.FUTEBOL),
    AGILIDADE(Esporte.FUTEBOL),

    // Atributos exclusivos de Basquete
    ARREMESSO(Esporte.BASQUETE),
    PASSE(Esporte.BASQUETE),
    REBOTE(Esporte.BASQUETE),

    // Atributos de Vôlei
    SAQUE(Esporte.VOLEI),
    RECEPCAO(Esporte.VOLEI),
    LEVANTAMENTO(Esporte.VOLEI),
    ATAQUE(Esporte.VOLEI),
    BLOQUEIO(Esporte.VOLEI),
    DEFESA_FUNDO(Esporte.VOLEI);

    private static final List<AtributoEsporte> ORDEM_FUTEBOL = List.of(
            FINALIZACAO, PASSE_VISAO, CONTROLE_BOLA, DEFESA, FISICO_FORCA, AGILIDADE
    );

    private static final List<AtributoEsporte> ORDEM_BASQUETE = List.of(
            FINALIZACAO, ARREMESSO, CONTROLE_BOLA, PASSE, DEFESA, REBOTE
    );

    private static final List<AtributoEsporte> ORDEM_VOLEI = List.of(
            SAQUE, RECEPCAO, LEVANTAMENTO, ATAQUE, BLOQUEIO, DEFESA_FUNDO
    );

    private final Set<Esporte> esportes;

    AtributoEsporte(Esporte primeiro, Esporte... outros) {
        this.esportes = Collections.unmodifiableSet(EnumSet.of(primeiro, outros));
    }

    public Set<Esporte> getEsportes() {
        return esportes;
    }

    public boolean pertenceA(Esporte esporte) {
        return esporte != null && esportes.contains(esporte);
    }

    public static List<AtributoEsporte> doEsporte(Esporte esporte) {
        if (esporte == null) {
            return List.of();
        }
        return switch (esporte) {
            case FUTEBOL -> ORDEM_FUTEBOL;
            case BASQUETE -> ORDEM_BASQUETE;
            case VOLEI -> ORDEM_VOLEI;
        };
    }
}
