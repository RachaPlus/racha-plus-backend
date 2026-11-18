package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateRachaDTO(
        @NotBlank(message = "O nome do racha é obrigatório")
        String nome,

        String descricao,

        @NotBlank(message = "O esporte é obrigatório")
        @Pattern(
                regexp = "^(FUTEBOL|BASQUETE|VOLEI)$",
                message = "O esporte deve ser FUTEBOL, BASQUETE ou VOLEI"
        )
        String esporte
) {
}
