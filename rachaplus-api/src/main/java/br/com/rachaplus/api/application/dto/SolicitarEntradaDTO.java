package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.TipoSolicitacao;

import java.util.UUID;

public record SolicitarEntradaDTO(
        TipoSolicitacao tipo,
        UUID memberId,
        String nome,
        String posicao
) {}
