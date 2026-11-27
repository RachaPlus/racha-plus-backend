package br.com.rachaplus.api.application.dto;

import java.util.List;

public record EquilibrarTimesResponseDTO(
        List<TimeGeradoDTO> times,
        TimeGeradoDTO deFora
) {
}
