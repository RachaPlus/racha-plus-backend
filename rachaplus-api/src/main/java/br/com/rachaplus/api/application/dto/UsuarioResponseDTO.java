package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.Usuario;

public record UsuarioResponseDTO(
        java.util.UUID id,
        String username,
        String email,
        String avatarJson
) {
    public UsuarioResponseDTO(java.util.UUID id, String username, String email) {
        this(id, username, email, null);
    }

    public UsuarioResponseDTO(Usuario usuario) {
        this(usuario.getId(), usuario.getNomeUsuario(), usuario.getEmail(), usuario.getAvatarJson());
    }
}
