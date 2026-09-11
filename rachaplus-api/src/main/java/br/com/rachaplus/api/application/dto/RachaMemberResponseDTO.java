package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;

import java.util.UUID;

public record RachaMemberResponseDTO(
        UUID rachaId,
        String rachaName,
        UUID userId,
        String userName,
        float rachaRating,
        RachaRole role
) {
    public RachaMemberResponseDTO(RachaMember member) {
        this(
                member.getRacha().getId(),
                member.getRacha().getName(),
                member.getUser().getId(),
                member.getUser().getNome(),
                member.getRachaRating(),
                member.getRole()
        );
    }
}
