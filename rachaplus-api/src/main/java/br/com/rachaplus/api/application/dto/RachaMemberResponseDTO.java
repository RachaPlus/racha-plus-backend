package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Posicao;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;

import java.util.UUID;

public record RachaMemberResponseDTO(
        UUID id,
        UUID rachaId,
        String nomeRacha,
        UUID usuarioId,
        String nomeJogador,
        Posicao posicao,
        float nota,
        Float minhaNota,
        long totalAvaliacoes,
        RachaRole papel
) {
    // minhaNota = nota dada pelo usuário logado (null se ele ainda não avaliou)
    public RachaMemberResponseDTO(RachaMember member, Float minhaNota, long totalAvaliacoes) {
        this(
                member.getId(),
                member.getRacha().getId(),
                member.getRacha().getNome(),
                member.getUser() != null ? member.getUser().getId() : null,
                member.getUser() != null ? member.getUser().getNomeUsuario() : member.getNome(),
                member.getPosicao(),
                member.getRachaRating(),
                minhaNota,
                totalAvaliacoes,
                member.getRole());
    }
}
