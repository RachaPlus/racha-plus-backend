package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Racha;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record RachaResponseDTO(
        UUID id,
        String nome,
        String descricao,
        String esporte,
        LocalDate data,
        LocalTime horario,
        long confirmados,
        LocalDateTime dataCriacao
) {
    public RachaResponseDTO(
            UUID id,
            String nome,
            String descricao,
            String esporte,
            long confirmados,
            LocalDateTime dataCriacao
    ) {
        this(id, nome, descricao, esporte, null, null, confirmados, dataCriacao);
    }

    public RachaResponseDTO(Racha racha, long confirmados) {
        this(
                racha.getId(),
                racha.getNome(),
                racha.getDescricao(),
                racha.getEsporte(),
                racha.getData(),
                racha.getHorario(),
                confirmados,
                racha.getDataCriacao()
        );
    }
}
