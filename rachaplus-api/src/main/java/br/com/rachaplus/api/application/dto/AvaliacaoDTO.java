package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.NotNull;

public record AvaliacaoDTO(
        @NotNull(message = "A nota é obrigatória")
        Float nota
) {}
