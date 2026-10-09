package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.CriarRachaDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.application.service.RachaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rachas")
public class RachaController {

    private final RachaService rachaService;

    public RachaController(RachaService rachaService) {
        this.rachaService = rachaService;
    }

    @PostMapping
    public ResponseEntity<RachaResponseDTO> criar(@RequestBody @Valid CriarRachaDTO dto) {
        return ResponseEntity.status(201).body(rachaService.criar(dto));
    }
}