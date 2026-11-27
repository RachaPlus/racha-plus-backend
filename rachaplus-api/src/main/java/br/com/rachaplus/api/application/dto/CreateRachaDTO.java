package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateRachaDTO(
        @NotBlank(message = "O nome do racha é obrigatório")
        String nome,

        String descricao,

        @NotBlank(message = "O esporte é obrigatório")
        @Pattern(
                regexp = "^(FUTEBOL|BASQUETE|VOLEI)$",
                message = "O esporte deve ser FUTEBOL, BASQUETE ou VOLEI"
        )
        String esporte,

        LocalDate data,

        LocalTime horario
) {
    public CreateRachaDTO(String nome, String descricao, String esporte) {
        this(nome, descricao, esporte, null, null);
    }
}
