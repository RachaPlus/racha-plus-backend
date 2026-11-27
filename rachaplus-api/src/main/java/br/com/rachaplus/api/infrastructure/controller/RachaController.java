package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.AcaoPartidaDTO;
import br.com.rachaplus.api.application.dto.AvaliacaoDTO;
import br.com.rachaplus.api.application.dto.ConviteRachaResponseDTO;
import br.com.rachaplus.api.application.dto.CreateRachaDTO;
import br.com.rachaplus.api.application.dto.CreateRachaMemberDTO;
import br.com.rachaplus.api.application.dto.EntrarRachaDTO;
import br.com.rachaplus.api.application.dto.EquilibrarTimesRequestDTO;
import br.com.rachaplus.api.application.dto.EstadoRachaResponseDTO;
import br.com.rachaplus.api.application.dto.IniciarPartidaDTO;
import br.com.rachaplus.api.application.dto.RachaMemberResponseDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.application.dto.UpdateRachaMemberDTO;
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

        var racha = (newRachaData.data() == null && newRachaData.horario() == null)
                ? rachaService.create(
                        newRachaData.nome(),
                        newRachaData.descricao(),
                        newRachaData.esporte(),
                        user
                )
                : rachaService.create(
                        newRachaData.nome(),
                        newRachaData.descricao(),
                        newRachaData.esporte(),
                        newRachaData.data(),
                        newRachaData.horario(),
                        user
                );
        var confirmados = rachaService.contarMembros(racha.getId());

        var uri = uriBuilder.path("/api/v1/rachas/{id}").buildAndExpand(racha.getId()).toUri();
        return ResponseEntity.created(uri).body(new RachaResponseDTO(racha, confirmados));
    }

    @Operation(summary = "Listar meus Rachas", description = "Lista todos os rachas em que o usuário autenticado é membro ou organizador")
    @GetMapping
    public ResponseEntity<List<RachaResponseDTO>> listMyRachas() {
        var user = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var response = rachaService.listarRachasDoUsuario(user);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Detalhar Racha", description = "Retorna os dados de um racha. O usuário autenticado precisa ser membro")
    @GetMapping("/{id}")
    public ResponseEntity<RachaResponseDTO> detail(@PathVariable UUID id) {
        var user = usuarioLogado();
        return ResponseEntity.ok(rachaService.buscarRacha(id, user));
    }

    @Operation(summary = "Visualizar Racha por Convite", description = "Retorna dados do racha e lista de membros para acesso via link ou QR Code (logado ou convidado)")
    @GetMapping("/{id}/convite")
    public ResponseEntity<ConviteRachaResponseDTO> detailConvite(@PathVariable UUID id) {
        var user = usuarioLogadoOpcional();
        return ResponseEntity.ok(rachaService.obterConviteRacha(id, user));
    }

    @Operation(summary = "Entrar no Racha", description = "Entra no racha via convite (vincula à conta se logado; entra como avulso sem histórico se não logado)")
    @PostMapping("/{id}/entrar")
    public ResponseEntity<RachaMemberResponseDTO> join(
            @PathVariable UUID id,
            @RequestBody(required = false) EntrarRachaDTO dto) {
        var user = usuarioLogadoOpcional();
        return ResponseEntity.ok(rachaService.entrarNoRacha(id, dto, user));
    }

    @Operation(summary = "Listar membros do Racha", description = "Lista os membros do racha com OVR (nota), minhaNota e totalAvaliacoes")
    @GetMapping("/{id}/members")
    public ResponseEntity<List<RachaMemberResponseDTO>> listMembers(@PathVariable UUID id) {
        var user = usuarioLogado();
        return ResponseEntity.ok(rachaService.listMembers(id, user));
    }

    @Operation(summary = "Adicionar jogador", description = "Adiciona um jogador avulso com posição do esporte do racha e nota inicial opcional (apenas admin)")
    @PostMapping("/{id}/members")
    public ResponseEntity<RachaMemberResponseDTO> addMember(
            @PathVariable UUID id,
            @RequestBody @Valid CreateRachaMemberDTO newMemberData,
            UriComponentsBuilder uriBuilder) {
        var user = usuarioLogado();
        var savedMember = rachaService.adicionarJogador(id, newMemberData, user);

        var uri = uriBuilder.path("/api/v1/rachas/{id}/members/{memberId}")
                .buildAndExpand(id, savedMember.id()).toUri();

        return ResponseEntity.created(uri).body(savedMember);
    }

    @Operation(summary = "Editar jogador", description = "Atualiza nome e posição de um jogador do racha (apenas admin)")
    @PutMapping("/{id}/members/{memberId}")
    public ResponseEntity<RachaMemberResponseDTO> updateMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @RequestBody @Valid UpdateRachaMemberDTO dto) {
        var user = usuarioLogado();
        return ResponseEntity.ok(rachaService.atualizarJogador(id, memberId, dto, user));
    }

    @Operation(summary = "Remover jogador", description = "Remove um jogador do racha e as avaliações que ele recebeu (apenas admin)")
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID memberId) {
        var user = usuarioLogado();
        rachaService.removerJogador(id, memberId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Avaliar jogador", description = "Cria ou atualiza a avaliação (GERAL ou AVANCADA por atributos) do usuário logado para o jogador e recalcula o OVR")
    @PutMapping("/{id}/members/{memberId}/avaliacao")
    public ResponseEntity<RachaMemberResponseDTO> rateMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @RequestBody @Valid AvaliacaoDTO dto) {
        var user = usuarioLogado();
        return ResponseEntity.ok(rachaService.avaliarJogador(id, memberId, dto, user));
    }

    @Operation(summary = "Estado ao vivo do Racha", description = "Retorna times sorteados, partida em andamento, cronômetro, placar e histórico de partidas (público por convite)")
    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoRachaResponseDTO> getLiveState(@PathVariable UUID id) {
        return ResponseEntity.ok(rachaService.obterEstadoRacha(id));
    }

    @Operation(summary = "Iniciar Partida", description = "Inicia uma partida entre dois times sorteados definindo duração e tempo de intervalo (apenas admin)")
    @PostMapping("/{id}/partida/iniciar")
    public ResponseEntity<EstadoRachaResponseDTO> startMatch(
            @PathVariable UUID id,
            @RequestBody IniciarPartidaDTO dto) {
        var user = usuarioLogado();
        return ResponseEntity.ok(rachaService.iniciarPartida(id, dto, user));
    }

    @Operation(summary = "Ação na Partida", description = "Atualiza placar, pausa/retoma cronômetro, aciona intervalo ou encerra a partida atual (apenas admin)")
    @PutMapping("/{id}/partida/acao")
    public ResponseEntity<EstadoRachaResponseDTO> matchAction(
            @PathVariable UUID id,
            @RequestBody AcaoPartidaDTO dto) {
        var user = usuarioLogado();
        return ResponseEntity.ok(rachaService.executarAcaoPartida(id, dto, user));
    }

    @Operation(summary = "Finalizar Racha", description = "Finaliza o racha definitivamente, tornando-o somente leitura no histórico (apenas admin)")
    @PostMapping("/{id}/finalizar")
    public ResponseEntity<EstadoRachaResponseDTO> finishRacha(@PathVariable UUID id) {
        var user = usuarioLogado();
        return ResponseEntity.ok(rachaService.finalizarRacha(id, user));
    }

    private Usuario usuarioLogado() {
        return (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Usuario usuarioLogadoOpcional() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        return null;
    }
}
