package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.CadastroUsuarioDTO;
import br.com.rachaplus.api.application.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid CadastroUsuarioDTO dadosNovoUsuario) {
        usuarioService.cadastrar(dadosNovoUsuario);

        return ResponseEntity.status(201).build();
    }
}
