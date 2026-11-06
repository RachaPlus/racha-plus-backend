package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;

import java.util.UUID;

public record RachaMemberResponseDTO(
        UUID id,
        UUID rachaId,
        String nomeRacha,
        UUID usuarioId,
        String nomeJogador,
        String posicao,
        float nota,
        RachaRole papel
) {
    public RachaMemberResponseDTO(RachaMember member) {
        this(
                member.getId(),
                member.getRacha().getId(),
                member.getRacha().getNome(),
                member.getUser() != null ? member.getUser().getId() : null,
                member.getUser() != null ? member.getUser().getNomeUsuario() : member.getNome(),
                member.getPosicao(),
                member.getRachaRating(),
                member.getRole()
        );
    }
}
