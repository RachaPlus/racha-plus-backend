package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void cadastrar(Usuario novoUsuario) {
        var usuarioExistente = usuarioRepository.findByEmail(novoUsuario.getEmail());

        if (usuarioExistente.isPresent()) {
            throw new RuntimeException("Já existe um usuário com este email");
        }

        usuarioRepository.save(novoUsuario);
    }
}
