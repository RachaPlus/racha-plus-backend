package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarRachaDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O esporte é obirgatório")
        String esporte
) {}
