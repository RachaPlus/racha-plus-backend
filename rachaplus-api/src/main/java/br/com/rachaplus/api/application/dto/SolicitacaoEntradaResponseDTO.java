package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Posicao;
import br.com.rachaplus.api.domain.SolicitacaoEntrada;
import br.com.rachaplus.api.domain.StatusSolicitacao;
import br.com.rachaplus.api.domain.TipoSolicitacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoEntradaResponseDTO(
        UUID id,
        TipoSolicitacao tipo,
        StatusSolicitacao status,
        UUID usuarioId,
        String usernameSolicitante,
        UUID memberId,
        String nomePerfilAlvo,
        Posicao posicao,
        LocalDateTime criadoEm
) {
    public SolicitacaoEntradaResponseDTO(SolicitacaoEntrada s) {
        this(
                s.getId(),
                s.getTipo(),
                s.getStatus(),
                s.getSolicitante().getId(),
                s.getSolicitante().getNomeUsuario(),
                s.getMemberAlvo() != null ? s.getMemberAlvo().getId() : null,
                s.getMemberAlvo() != null ? s.getMemberAlvo().getNome() : s.getNomeNovoPerfil(),
                s.getMemberAlvo() != null ? s.getMemberAlvo().getPosicao() : s.getPosicaoNovoPerfil(),
                s.getCriadoEm()
        );
    }
}
