package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.ConviteDetalhesResponseDTO;
import br.com.rachaplus.api.application.dto.GerarConviteResponseDTO;
import br.com.rachaplus.api.application.dto.SolicitacaoEntradaResponseDTO;
import br.com.rachaplus.api.application.dto.SolicitarEntradaDTO;
import br.com.rachaplus.api.application.service.ConviteService;
import br.com.rachaplus.api.domain.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Convites e Solicitações", description = "Links de convite por código, reivindicação de perfil virtual e aprovação do Admin")
@RestController
@RequestMapping("/api/v1")
public class ConviteController {

    private final ConviteService conviteService;

    public ConviteController(ConviteService conviteService) {
        this.conviteService = conviteService;
    }

    @Operation(summary = "Gerar link de convite", description = "Gera ou retorna o código de convite único do racha (apenas Admin)")
    @PostMapping("/rachas/{id}/convite")
    public ResponseEntity<GerarConviteResponseDTO> gerarConvite(@PathVariable UUID id) {
        return ResponseEntity.ok(conviteService.gerarConvite(id, usuarioLogado()));
    }

    @Operation(summary = "Consultar convite por código", description = "Retorna dados do racha, status do usuário e perfis virtuais disponíveis")
    @GetMapping("/convites/{codigo}")
    public ResponseEntity<ConviteDetalhesResponseDTO> obterConvite(@PathVariable String codigo) {
        return ResponseEntity.ok(conviteService.obterConvite(codigo, usuarioLogado()));
    }

    @Operation(summary = "Solicitar entrada via convite", description = "Solicita reivindicação de perfil virtual ou criação de novo perfil")
    @PostMapping("/convites/{codigo}/solicitar")
    public ResponseEntity<SolicitacaoEntradaResponseDTO> solicitarEntrada(
            @PathVariable String codigo,
            @RequestBody SolicitarEntradaDTO dto) {
        var criada = conviteService.solicitarEntrada(codigo, dto, usuarioLogado());
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @Operation(summary = "Listar solicitações pendentes", description = "Lista as solicitações PENDENTE do racha (apenas Admin)")
    @GetMapping("/rachas/{id}/solicitacoes")
    public ResponseEntity<List<SolicitacaoEntradaResponseDTO>> listarSolicitacoes(@PathVariable UUID id) {
        return ResponseEntity.ok(conviteService.listarPendentes(id, usuarioLogado()));
    }

    @Operation(summary = "Aprovar solicitação", description = "Aprova solicitação pendente e vincula/cria membro no racha (apenas Admin)")
    @PostMapping("/rachas/{id}/solicitacoes/{solicitacaoId}/aprovar")
    public ResponseEntity<Void> aprovarSolicitacao(
            @PathVariable UUID id,
            @PathVariable UUID solicitacaoId) {
        conviteService.aprovarSolicitacao(id, solicitacaoId, usuarioLogado());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Recusar solicitação", description = "Recusa uma solicitação pendente (apenas Admin)")
    @PostMapping("/rachas/{id}/solicitacoes/{solicitacaoId}/recusar")
    public ResponseEntity<Void> recusarSolicitacao(
            @PathVariable UUID id,
            @PathVariable UUID solicitacaoId) {
        conviteService.recusarSolicitacao(id, solicitacaoId, usuarioLogado());
        return ResponseEntity.ok().build();
    }

    private Usuario usuarioLogado() {
        return (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
