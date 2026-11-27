package br.com.rachaplus.api.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EstadoJogoDTO(
        String formato,
        List<TimeSorteadoDTO> times,
        List<JogadorSorteadoDTO> deFora,
        PartidaAtivaDTO partidaAtual
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JogadorSorteadoDTO(
            String id,
            String usuarioId,
            String nome,
            String posicao,
            float nota
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeSorteadoDTO(
            int id,
            String nome,
            float mediaOvr,
            List<JogadorSorteadoDTO> jogadores
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartidaAtivaDTO(
            boolean ativa,
            String fase, // EM_JOGO, INTERVALO, PAUSADO
            int timeAId,
            String timeANome,
            int placarA,
            int timeBId,
            String timeBNome,
            int placarB,
            int duracaoMinutos,
            int intervaloMinutos,
            long segundosRestantes,
            boolean rodando,
            long atualizadoEmEpochMs,
            Integer metaPontos,
            Integer melhorDeSets,
            Integer setsA,
            Integer setsB,
            Boolean usarCronometro
    ) {}
}
