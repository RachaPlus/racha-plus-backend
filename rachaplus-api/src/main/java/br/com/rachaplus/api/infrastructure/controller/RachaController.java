package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.*;
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

    @PostMapping("/{id}/members")
    public ResponseEntity<RachaMemberResponseDTO> addMember(
            @PathVariable UUID id,
            @RequestBody @Valid CreateRachaMemberDTO newMemberData,
            UriComponentsBuilder uriBuilder) {

        // Chama o service que criamos no passo anterior
        var savedMember = rachaService.adicionarJogador(id, newMemberData);

        // Constrói a URI de retorno padrão (201 Created)
        var uri = uriBuilder.path("/api/v1/rachas/{id}/members/{memberId}")
                .buildAndExpand(id, savedMember.getId()).toUri();

        return ResponseEntity.created(uri).body(new RachaMemberResponseDTO(savedMember));
    }

    @PutMapping("/{id}/members/{memberId}")
    public ResponseEntity<RachaMemberResponseDTO> updateMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @RequestBody @Valid UpdateRachaMemberDTO dto) {

        var updatedMember = rachaService.atualizarJogador(id, memberId, dto);
        return ResponseEntity.ok(new RachaMemberResponseDTO(updatedMember));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId) {

        rachaService.removerJogador(id, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/partidas")
    public ResponseEntity<PartidaResponseDTO> criarPartida(
            @PathVariable UUID id,
            UriComponentsBuilder uriBuilder) {

        var user = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var partida = rachaService.criarPartida(id, user);

        var uri = uriBuilder.path("/api/v1/rachas/{id}/partidas/{partidaId}")
                .buildAndExpand(id, partida.getId()).toUri();

        return ResponseEntity.created(uri).body(new PartidaResponseDTO(partida));
    }

    @GetMapping("/{id}/partidas")
    public ResponseEntity<List<PartidaResponseDTO>> listarPartidas(@PathVariable UUID id) {
        var user = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var partidas = rachaService.listarPartidas(id, user);

        var response = partidas.stream()
                .map(PartidaResponseDTO::new)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/partidas/{partidaId}")
    public ResponseEntity<PartidaResponseDTO> atualizarPlacar(
            @PathVariable UUID id,
            @PathVariable UUID partidaId,
            @RequestBody @Valid UpdatePlacarDTO dto) {

        var user = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var partida = rachaService.atualizarPlacar(id, partidaId, dto, user);

        return ResponseEntity.ok(new PartidaResponseDTO(partida));
    }
}
