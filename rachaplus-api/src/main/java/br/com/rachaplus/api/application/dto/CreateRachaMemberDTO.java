package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRachaMemberDTO(
        @NotBlank(message = "O nome do jogador é obrigatório")
        String nome,

        // Validada no service, pois depende do esporte do racha
        @NotBlank(message = "A posição é obrigatória")
        String posicao,

        // Opcional: de 0.5 a 5.0 em passos de 0.5 (validada no service)
        Float notaInicial
) {}
