package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.CreateRachaDTO;
import br.com.rachaplus.api.application.dto.RachaMemberResponseDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.application.service.RachaService;
import br.com.rachaplus.api.domain.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Tag(name = "Rachas", description = "Endpoints para criação, gestão e consulta de grupos de partida (Rachas)")
@RestController
@RequestMapping("/api/v1/rachas")
public class RachaController {

    private final RachaService rachaService;

    public RachaController(RachaService rachaService) {
        this.rachaService = rachaService;
    }

    @Operation(summary = "Criar Racha", description = "Cria um novo grupo de racha vinculando o usuário autenticado como organizador")
    @PostMapping
    public ResponseEntity<RachaResponseDTO> create(@RequestBody @Valid CreateRachaDTO newRachaData, UriComponentsBuilder uriBuilder) {
        var user = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        var racha = rachaService.create(newRachaData.name(), newRachaData.description(), user);

        var uri = uriBuilder.path("/api/v1/rachas/{id}").buildAndExpand(racha.getId()).toUri();
        return ResponseEntity.created(uri).body(new RachaResponseDTO(racha));
    }

    @Operation(summary = "Listar meus Rachas", description = "Lista todos os rachas em que o usuário autenticado é membro ou organizador")
    @GetMapping
    public ResponseEntity<List<RachaMemberResponseDTO>> listMyRachas() {
        var user = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var members = rachaService.listByUser(user);

        var response = members.stream()
                .map(RachaMemberResponseDTO::new)
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar membros do Racha", description = "Lista os membros participantes de um racha específico por ID")
    @GetMapping("/{id}/members")
    public ResponseEntity<List<RachaMemberResponseDTO>> listMembers(@PathVariable UUID id) {
        var user = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var members = rachaService.listMembers(id, user);

        var response = members.stream()
                .map(RachaMemberResponseDTO::new)
                .toList();

        return ResponseEntity.ok(response);
    }
}
