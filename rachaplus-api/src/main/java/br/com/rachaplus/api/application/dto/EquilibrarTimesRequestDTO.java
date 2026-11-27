package br.com.rachaplus.api.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EquilibrarTimesRequestDTO(
        @JsonAlias({"memberIds", "membrosIds", "jogadoresIds", "playerIds"})
        List<UUID> jogadoresConfirmadosIds,

        @JsonAlias({"tamanhoTime", "playersPerTeam"})
        Integer jogadoresPorTime,

        String formato
) {
    public EquilibrarTimesRequestDTO(List<UUID> jogadoresConfirmadosIds, Integer jogadoresPorTime) {
        this(jogadoresConfirmadosIds, jogadoresPorTime, null);
    }
}
