package br.com.rachaplus.api.application.dto;

public record AuthResponseDTO(
        String token,
        UsuarioResumoDTO usuario
) {}
