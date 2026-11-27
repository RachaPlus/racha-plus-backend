package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.EquilibrarTimesRequestDTO;
import br.com.rachaplus.api.application.dto.EquilibrarTimesResponseDTO;
import br.com.rachaplus.api.application.service.EquilibrioTimesService;
import br.com.rachaplus.api.domain.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Rachas", description = "Endpoints para criação, gestão e equilíbrio inteligente de times")
@RestController
@RequestMapping("/api/v1/rachas")
public class EquilibrioTimesController {

    private final EquilibrioTimesService equilibrioTimesService;

    public EquilibrioTimesController(EquilibrioTimesService equilibrioTimesService) {
        this.equilibrioTimesService = equilibrioTimesService;
    }

    @Operation(
            summary = "Equilibrar Times Inteligente",
            description = "Sorteia e equilibra os times por posição tática e OVR (com refinamento pairwise swap) e separa excedentes em 'De fora'"
    )
    @PostMapping("/{id}/equilibrar-times")
    public ResponseEntity<EquilibrarTimesResponseDTO> equilibrarTimes(
            @PathVariable UUID id,
            @RequestBody EquilibrarTimesRequestDTO dto) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario requester = (auth != null && auth.getPrincipal() instanceof Usuario u) ? u : null;
        return ResponseEntity.ok(equilibrioTimesService.equilibrarTimes(id, dto, requester));
    }
}
