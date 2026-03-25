package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.CadastroUsuarioDTO;
import br.com.rachaplus.api.application.dto.UsuarioResponseDTO;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponseDTO cadastrar(CadastroUsuarioDTO dadosNovoUsuario) {
        var usuarioExistente = usuarioRepository.findByEmail(dadosNovoUsuario.email());

        if (usuarioExistente.isPresent()) {
            throw new RuntimeException("Já existe um usuário com este email");
        }

        var novoUsuario = new Usuario();
        novoUsuario.setNome(dadosNovoUsuario.nome());
        novoUsuario.setEmail(dadosNovoUsuario.email());
        novoUsuario.setSenha(passwordEncoder.encode(dadosNovoUsuario.senha()));

        var usuarioResponseDTO = new UsuarioResponseDTO(usuarioRepository.save(novoUsuario));
        return usuarioResponseDTO;
    }
}
