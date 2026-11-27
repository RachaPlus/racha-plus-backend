package br.com.rachaplus.api.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum Posicao {
    // FUTEBOL
    GOLEIRO(Esporte.FUTEBOL, Map.of(
            AtributoEsporte.FINALIZACAO, 0.05f,
            AtributoEsporte.PASSE_VISAO, 0.10f,
            AtributoEsporte.CONTROLE_BOLA, 0.10f,
            AtributoEsporte.DEFESA, 0.35f,
            AtributoEsporte.FISICO_FORCA, 0.15f,
            AtributoEsporte.AGILIDADE, 0.25f
    )),
    ZAGUEIRO(Esporte.FUTEBOL, Map.of(
            AtributoEsporte.FINALIZACAO, 0.05f,
            AtributoEsporte.PASSE_VISAO, 0.15f,
            AtributoEsporte.CONTROLE_BOLA, 0.10f,
            AtributoEsporte.DEFESA, 0.35f,
            AtributoEsporte.FISICO_FORCA, 0.25f,
            AtributoEsporte.AGILIDADE, 0.10f
    )),
    MEIO_CAMPISTA(Esporte.FUTEBOL, Map.of(
            AtributoEsporte.FINALIZACAO, 0.15f,
            AtributoEsporte.PASSE_VISAO, 0.30f,
            AtributoEsporte.CONTROLE_BOLA, 0.25f,
            AtributoEsporte.DEFESA, 0.10f,
            AtributoEsporte.FISICO_FORCA, 0.10f,
            AtributoEsporte.AGILIDADE, 0.10f
    )),
    ATACANTE(Esporte.FUTEBOL, Map.of(
            AtributoEsporte.FINALIZACAO, 0.35f,
            AtributoEsporte.PASSE_VISAO, 0.10f,
            AtributoEsporte.CONTROLE_BOLA, 0.20f,
            AtributoEsporte.DEFESA, 0.05f,
            AtributoEsporte.FISICO_FORCA, 0.10f,
            AtributoEsporte.AGILIDADE, 0.20f
    )),

    // BASQUETE
    ARMADOR(Esporte.BASQUETE, Map.of(
            AtributoEsporte.FINALIZACAO, 0.15f,
            AtributoEsporte.ARREMESSO, 0.20f,
            AtributoEsporte.CONTROLE_BOLA, 0.25f,
            AtributoEsporte.PASSE, 0.25f,
            AtributoEsporte.DEFESA, 0.10f,
            AtributoEsporte.REBOTE, 0.05f
    )),
    ALA(Esporte.BASQUETE, Map.of(
            AtributoEsporte.FINALIZACAO, 0.20f,
            AtributoEsporte.ARREMESSO, 0.25f,
            AtributoEsporte.CONTROLE_BOLA, 0.15f,
            AtributoEsporte.PASSE, 0.10f,
            AtributoEsporte.DEFESA, 0.20f,
            AtributoEsporte.REBOTE, 0.10f
    )),
    PIVO(Esporte.BASQUETE, Map.of(
            AtributoEsporte.FINALIZACAO, 0.25f,
            AtributoEsporte.ARREMESSO, 0.05f,
            AtributoEsporte.CONTROLE_BOLA, 0.05f,
            AtributoEsporte.PASSE, 0.10f,
            AtributoEsporte.DEFESA, 0.25f,
            AtributoEsporte.REBOTE, 0.30f
    )),

    // VOLEI
    LEVANTADOR(Esporte.VOLEI, Map.of(
            AtributoEsporte.SAQUE, 0.15f,
            AtributoEsporte.RECEPCAO, 0.10f,
            AtributoEsporte.LEVANTAMENTO, 0.40f,
            AtributoEsporte.ATAQUE, 0.05f,
            AtributoEsporte.BLOQUEIO, 0.10f,
            AtributoEsporte.DEFESA_FUNDO, 0.20f
    )),
    PONTEIRO(Esporte.VOLEI, Map.of(
            AtributoEsporte.SAQUE, 0.15f,
            AtributoEsporte.RECEPCAO, 0.25f,
            AtributoEsporte.LEVANTAMENTO, 0.05f,
            AtributoEsporte.ATAQUE, 0.25f,
            AtributoEsporte.BLOQUEIO, 0.10f,
            AtributoEsporte.DEFESA_FUNDO, 0.20f
    )),
    CENTRAL(Esporte.VOLEI, Map.of(
            AtributoEsporte.SAQUE, 0.10f,
            AtributoEsporte.RECEPCAO, 0.05f,
            AtributoEsporte.LEVANTAMENTO, 0.05f,
            AtributoEsporte.ATAQUE, 0.35f,
            AtributoEsporte.BLOQUEIO, 0.40f,
            AtributoEsporte.DEFESA_FUNDO, 0.05f
    )),
    OPOSTO(Esporte.VOLEI, Map.of(
            AtributoEsporte.SAQUE, 0.20f,
            AtributoEsporte.RECEPCAO, 0.05f,
            AtributoEsporte.LEVANTAMENTO, 0.05f,
            AtributoEsporte.ATAQUE, 0.40f,
            AtributoEsporte.BLOQUEIO, 0.20f,
            AtributoEsporte.DEFESA_FUNDO, 0.10f
    )),
    LIBERO(Esporte.VOLEI, Map.of(
            AtributoEsporte.SAQUE, 0.05f,
            AtributoEsporte.RECEPCAO, 0.45f,
            AtributoEsporte.LEVANTAMENTO, 0.10f,
            AtributoEsporte.ATAQUE, 0.00f,
            AtributoEsporte.BLOQUEIO, 0.00f,
            AtributoEsporte.DEFESA_FUNDO, 0.40f
    ));

    private final Esporte esporte;
    private final Map<AtributoEsporte, Float> pesos;

    Posicao(Esporte esporte, Map<AtributoEsporte, Float> pesos) {
        this.esporte = esporte;
        EnumMap<AtributoEsporte, Float> ordenado = new EnumMap<>(AtributoEsporte.class);
        ordenado.putAll(pesos);
        this.pesos = Collections.unmodifiableMap(ordenado);
    }

    public Esporte getEsporte() {
        return esporte;
    }

    public Map<AtributoEsporte, Float> getPesos() {
        return pesos;
    }

    public float pesoDe(AtributoEsporte atributo) {
        return pesos.getOrDefault(atributo, 0f);
    }

    public float calcularNotaPonderada(Map<AtributoEsporte, Float> atributos) {
        if (atributos == null || atributos.isEmpty()) {
            return 0f;
        }
        BigDecimal somaPonderada = BigDecimal.ZERO;
        BigDecimal somaPesosPresentes = BigDecimal.ZERO;
        for (var entry : pesos.entrySet()) {
            float peso = entry.getValue();
            if (peso <= 0f) {
                continue;
            }
            Float notaAtributo = atributos.get(entry.getKey());
            if (notaAtributo != null) {
                BigDecimal pesoExato = BigDecimal.valueOf(Math.round(peso * 100f), 2);
                BigDecimal notaExata = BigDecimal.valueOf(Math.round(notaAtributo * 10f), 1);
                somaPonderada = somaPonderada.add(notaExata.multiply(pesoExato));
                somaPesosPresentes = somaPesosPresentes.add(pesoExato);
            }
        }
        if (somaPesosPresentes.compareTo(BigDecimal.ZERO) <= 0) {
            return 0f;
        }
        return somaPonderada
                .divide(somaPesosPresentes, 1, RoundingMode.HALF_UP)
                .floatValue();
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
