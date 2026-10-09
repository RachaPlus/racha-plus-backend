package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRachaMemberDTO(
        @NotBlank(message = "O nome do jogador é obrigatório")
        String nome
) {}