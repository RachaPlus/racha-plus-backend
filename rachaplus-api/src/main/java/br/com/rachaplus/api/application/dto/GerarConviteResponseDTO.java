package br.com.rachaplus.api.application.dto;

import java.util.UUID;

public record GerarConviteResponseDTO(
        String codigo,
        UUID rachaId,
        String nomeRacha,
        String esporte
) {}
