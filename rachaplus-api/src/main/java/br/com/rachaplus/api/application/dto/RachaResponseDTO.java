package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Racha;

import java.time.LocalDateTime;
import java.util.UUID;

public record RachaResponseDTO (
        UUID id,
        String nome,
        String esporte,
        String token,
        UUID criadorId,
        LocalDateTime dataCriacao
) {
    public RachaResponseDTO(Racha racha) {
        this(
                racha.getId(),
                racha.getNome(),
                racha.getEsporte(),
                racha.getToken(),
                racha.getCriador().getId(),
                racha.getDataCriacao()
        );
    }
}