package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Partida;

import java.time.LocalDateTime;
import java.util.UUID;

public record PartidaResponseDTO(
        UUID id,
        UUID rachaId,
        String timeANome,
        String timeBNome,
        Integer placarTimeA,
        Integer placarTimeB,
        String detalhePlacar,
        LocalDateTime dataPartida
) {
    public PartidaResponseDTO(Partida partida) {
        this(
                partida.getId(),
                partida.getRacha().getId(),
                partida.getTimeANome() != null ? partida.getTimeANome() : "Time 1",
                partida.getTimeBNome() != null ? partida.getTimeBNome() : "Time 2",
                partida.getPlacarTimeA(),
                partida.getPlacarTimeB(),
                partida.getDetalhePlacar(),
                partida.getDataPartida()
        );
    }
}