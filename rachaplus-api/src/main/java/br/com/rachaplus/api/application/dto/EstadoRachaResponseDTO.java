package br.com.rachaplus.api.application.dto;

import java.util.List;

public record EstadoRachaResponseDTO(
        String status,
        long servidorAgoraEpochMs,
        EstadoJogoDTO estadoJogo,
        List<PartidaResponseDTO> partidas
) {
}
