package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.CadastroUsuarioDTO;
import br.com.rachaplus.api.application.dto.UsuarioResponseDTO;
import br.com.rachaplus.api.application.service.UsuarioService;
import br.com.rachaplus.api.domain.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(
            @RequestBody @Valid CadastroUsuarioDTO dadosNovoUsuario,
            org.springframework.web.util.UriComponentsBuilder uriBuilder) {
        var usuarioResponseDTO = usuarioService.cadastrar(dadosNovoUsuario);

        var uri = uriBuilder.path("/api/v1/users/{id}").buildAndExpand(usuarioResponseDTO.id()).toUri();

        return ResponseEntity.created(uri).body(usuarioResponseDTO);
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me() {
        var usuario = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(new UsuarioResponseDTO(usuario));
    }
}
