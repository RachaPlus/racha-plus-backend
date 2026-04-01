package br.com.rachaplus.api.domain;

import lombok.Getter;

@Getter
public enum RachaRole {
    ADMIN("admin"),
    PLAYER("jogador");

    private final String description;

    RachaRole(String description) {
        this.description = description;
    }
}
