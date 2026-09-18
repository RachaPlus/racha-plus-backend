package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Racha;

import java.time.LocalDateTime;
import java.util.UUID;

public record RachaResponseDTO(
        UUID id,
        String name,
        String description,
        LocalDateTime createdAt
) {
    public RachaResponseDTO(Racha racha) {
        this(racha.getId(), racha.getName(), racha.getDescription(), racha.getCreatedAt());
    }
}
