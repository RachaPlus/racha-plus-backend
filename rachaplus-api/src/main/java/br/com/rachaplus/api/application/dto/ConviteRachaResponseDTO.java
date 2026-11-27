package br.com.rachaplus.api.application.dto;

import java.util.List;

public record ConviteRachaResponseDTO(
        RachaResponseDTO racha,
        List<RachaMemberResponseDTO> membros
) {
}
