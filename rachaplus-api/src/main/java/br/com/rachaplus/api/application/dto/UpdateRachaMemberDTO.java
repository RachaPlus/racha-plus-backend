package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRachaMemberDTO(
        @NotBlank(message = "A posição é obrigatória")
        String posicao
) {}