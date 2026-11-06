package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.AutenticacaoDTO;
import br.com.rachaplus.api.application.dto.TokenDTO;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.infrastructure.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Endpoints para login e geração de Token JWT")
@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacaoController {

    private final AuthenticationManager manager;
    private final TokenService tokenService;

    public AutenticacaoController(AuthenticationManager manager, TokenService tokenService) {
        this.manager = manager;
        this.tokenService = tokenService;
    }

    @Operation(summary = "Efetuar Login", description = "Autentica um usuário com email e senha e retorna um Token JWT")
    @PostMapping("/login")
    public ResponseEntity<TokenDTO> efetuarLogin(@RequestBody @Valid AutenticacaoDTO dadosLogin) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(dadosLogin.email(), dadosLogin.senha());

        var authentication =  manager.authenticate(authenticationToken);

        var tokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        return ResponseEntity.ok(new TokenDTO(tokenJWT));
    }
}
