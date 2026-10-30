package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.AutenticacaoDTO;
import br.com.rachaplus.api.application.dto.AuthResponseDTO;
import br.com.rachaplus.api.application.dto.UsuarioResumoDTO;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.infrastructure.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacaoController {

    private final AuthenticationManager manager;
    private final TokenService tokenService;

    public AutenticacaoController(AuthenticationManager manager, TokenService tokenService) {
        this.manager = manager;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> efetuarLogin(@RequestBody @Valid AutenticacaoDTO dadosLogin) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(dadosLogin.email(), dadosLogin.senha());
        var authentication =  manager.authenticate(authenticationToken);

        var usuario = (Usuario) authentication.getPrincipal();

        var tokenJWT = tokenService.gerarToken(usuario);

        var usuarioResumo = new UsuarioResumoDTO(usuario.getUsername());

        return ResponseEntity.ok(new AuthResponseDTO(tokenJWT, usuarioResumo));
    }
}
