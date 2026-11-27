package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.AtributoEsporte;
import br.com.rachaplus.api.domain.Posicao;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;
import br.com.rachaplus.api.domain.TipoAvaliacao;

import java.util.Map;
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
        TipoAvaliacao meuTipoAvaliacao,
        Map<AtributoEsporte, Float> meusAtributos,
        Map<AtributoEsporte, Float> mediaAtributos,
        long totalAvaliacoes,
        RachaRole papel,
        String avatarJson
) {
    public RachaMemberResponseDTO(
            RachaMember member,
            Float minhaNota,
            TipoAvaliacao meuTipoAvaliacao,
            Map<AtributoEsporte, Float> meusAtributos,
            Map<AtributoEsporte, Float> mediaAtributos,
            long totalAvaliacoes
    ) {
        this(
                member.getId(),
                member.getRacha().getId(),
                member.getRacha().getNome(),
                member.getUser() != null ? member.getUser().getId() : null,
                member.getNome() != null && !member.getNome().isBlank()
                        ? member.getNome()
                        : (member.getUser() != null ? member.getUser().getNomeUsuario() : "Jogador"),
                member.getPosicao(),
                member.getRachaRating(),
                minhaNota,
                meuTipoAvaliacao,
                meusAtributos,
                mediaAtributos != null ? mediaAtributos : Map.of(),
                totalAvaliacoes,
                member.getRole(),
                member.getAvatarJson() != null
                        ? member.getAvatarJson()
                        : (member.getUser() != null ? member.getUser().getAvatarJson() : null)
        );
    }

    // Construtor retrocompatível
    public RachaMemberResponseDTO(RachaMember member, Float minhaNota, long totalAvaliacoes) {
        this(
                member,
                minhaNota,
                minhaNota != null ? TipoAvaliacao.GERAL : null,
                null,
                Map.of(),
                totalAvaliacoes
        );
    }
}
