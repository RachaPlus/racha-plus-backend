package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroUsuarioDTO(
        @NotBlank(message = "O username é obrigatório")
        @Size(min = 4, max = 16, message = "O username deve ter entre 4 e 16 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9_.]+$", message = "O username deve conter apenas letras, números, underscores (_) ou pontos (.)")
        String username,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String senha
) {}
