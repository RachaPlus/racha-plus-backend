package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Racha;

import java.time.LocalDateTime;
import java.util.UUID;

public record RachaResponseDTO(
        UUID id,
        String nome,
        String descricao,
        String esporte,
        long confirmados,
        LocalDateTime dataCriacao
) {
    public RachaResponseDTO(Racha racha, long confirmados) {
        this(
                racha.getId(),
                racha.getNome(),
                racha.getDescricao(),
                racha.getEsporte(),
                confirmados,
                racha.getDataCriacao()
        );
    }
}
