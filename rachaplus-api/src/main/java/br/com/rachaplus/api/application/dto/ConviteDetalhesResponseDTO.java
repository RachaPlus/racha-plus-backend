package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Posicao;

import java.util.List;
import java.util.UUID;

public record ConviteDetalhesResponseDTO(
        UUID rachaId,
        String nomeRacha,
        String esporte,
        boolean jaEMembro,
        boolean solicitacaoPendente,
        List<PerfilDisponivelDTO> perfisDisponiveis
) {
    public record PerfilDisponivelDTO(
            UUID id,
            String nomeJogador,
            Posicao posicao,
            float nota
    ) {}
}
