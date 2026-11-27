package br.com.rachaplus.api.application.dto;

public record EntrarRachaDTO(
        String nome,
        String posicao,
        String avatarJson
) {
    public EntrarRachaDTO(String nome, String posicao) {
        this(nome, posicao, null);
    }
}
