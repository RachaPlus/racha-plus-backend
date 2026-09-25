package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.CreateRachaDTO;
import br.com.rachaplus.api.application.dto.RachaMemberResponseDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.application.service.RachaService;
import br.com.rachaplus.api.domain.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rachas")
public class RachaController {

    private final RachaService rachaService;

    public RachaController(RachaService rachaService) {
        this.rachaService = rachaService;
    }

    @PostMapping
    public ResponseEntity<RachaResponseDTO> create(@RequestBody @Valid CreateRachaDTO newRachaData, UriComponentsBuilder uriBuilder) {
        var user = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        var racha = rachaService.create(newRachaData.name(), newRachaData.description(), user);

        var uri = uriBuilder.path("/api/v1/rachas/{id}").buildAndExpand(racha.getId()).toUri();
        return ResponseEntity.created(uri).body(new RachaResponseDTO(racha));
    }

    @GetMapping
    public ResponseEntity<List<RachaMemberResponseDTO>> listMyRachas() {
        var user = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var members = rachaService.listByUser(user);

        var response = members.stream()
                .map(RachaMemberResponseDTO::new)
                .toList();

        return ResponseEntity.ok(response);
    }

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
