package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Usuario;

public record UsuarioResponseDTO(
        java.util.UUID id,
        String username,
        String email
) {
    public UsuarioResponseDTO(Usuario usuario) {
        this(usuario.getId(), usuario.getUsername(), usuario.getEmail());
    }
}
