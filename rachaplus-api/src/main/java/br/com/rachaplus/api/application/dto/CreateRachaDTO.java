package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRachaDTO(
        @NotBlank(message = "The name is required")
        String name,

        String description
) {
}
