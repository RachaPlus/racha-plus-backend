package br.com.rachaplus.api.application.dto;

public record IniciarPartidaDTO(
        Integer timeAId,
        Integer timeBId,
        Integer duracaoMinutos,
        Integer intervaloMinutos,
        Integer metaPontos,
        Integer melhorDeSets,
        Boolean usarCronometro
) {
}
