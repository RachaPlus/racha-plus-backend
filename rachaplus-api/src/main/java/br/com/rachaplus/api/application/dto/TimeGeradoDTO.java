package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Posicao;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;

import java.util.List;
import java.util.UUID;

public record TimeGeradoDTO(
        String nome,
        float mediaNota,
        List<JogadorTimeDTO> jogadores
) {
    public record JogadorTimeDTO(
            UUID id,
            String nomeJogador,
            Posicao posicao,
            float nota,
            RachaRole papel
    ) {
        public JogadorTimeDTO(RachaMember member) {
            this(
                    member.getId(),
                    member.getUser() != null ? member.getUser().getNomeUsuario() : member.getNome(),
                    member.getPosicao(),
                    Math.round(member.getRachaRating() * 10f) / 10f,
                    member.getRole()
            );
        }
    }
}
