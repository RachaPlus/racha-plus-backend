package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Partida;

import java.time.LocalDateTime;
import java.util.UUID;

public record PartidaResponseDTO(
        UUID id,
        UUID rachaId,
        Integer placarTimeA,
        Integer placarTimeB,
        LocalDateTime dataPartida
) {
    public PartidaResponseDTO(Partida partida) {
        this(
                partida.getId(),
                partida.getRacha().getId(),
                partida.getPlacarTimeA(),
                partida.getPlacarTimeB(),
                partida.getDataPartida()
        );
    }
}