package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.CreateRachaDTO;
import br.com.rachaplus.api.application.dto.RachaMemberResponseDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.application.service.RachaService;
import br.com.rachaplus.api.domain.Posicao;
import br.com.rachaplus.api.domain.Racha;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.repository.UsuarioRepository;
import br.com.rachaplus.api.infrastructure.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RachaController.class)
@AutoConfigureMockMvc(addFilters = false)
class RachaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RachaService rachaService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private Usuario usuarioLogado;

    @BeforeEach
    void setUp() {
        usuarioLogado = new Usuario();
        usuarioLogado.setId(UUID.randomUUID());
        usuarioLogado.setUsername("organizador");
        usuarioLogado.setEmail("organizador@email.com");

        var auth = new UsernamePasswordAuthenticationToken(usuarioLogado, null, usuarioLogado.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve criar um racha com sucesso e retornar HTTP 201 Created")
    void deveCriarRachaComSucesso() throws Exception {
        var rachaId = UUID.randomUUID();
        var dto = new CreateRachaDTO("Racha de Sexta", "Futebol society toda sexta", "FUTEBOL");

        var rachaSalvo = new Racha();
        rachaSalvo.setId(rachaId);
        rachaSalvo.setNome("Racha de Sexta");
        rachaSalvo.setDescricao("Futebol society toda sexta");
        rachaSalvo.setEsporte("FUTEBOL");
        rachaSalvo.setDataCriacao(LocalDateTime.now());

        when(rachaService.create(eq("Racha de Sexta"), eq("Futebol society toda sexta"), eq("FUTEBOL"), any(Usuario.class)))
                .thenReturn(rachaSalvo);
        when(rachaService.contarMembros(rachaId)).thenReturn(1L);

        mockMvc.perform(post("/api/v1/rachas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/rachas/" + rachaId))
                .andExpect(jsonPath("$.id").value(rachaId.toString()))
                .andExpect(jsonPath("$.nome").value("Racha de Sexta"))
                .andExpect(jsonPath("$.descricao").value("Futebol society toda sexta"))
                .andExpect(jsonPath("$.esporte").value("FUTEBOL"))
                .andExpect(jsonPath("$.confirmados").value(1));
    }

    @Test

    @DisplayName("Deve retornar HTTP 400 Bad Request ao criar racha com nome em branco ou esporte inválido")
    void deveRetornarBadRequestQuandoNomeEmBrancoOuEsporteInvalido() throws Exception {
        var dtoInvalido = new CreateRachaDTO("", "Descricao sem nome", "ESPORTE_INVALIDO");

        mockMvc.perform(post("/api/v1/rachas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve listar os rachas do usuário logado e retornar HTTP 200")
    void deveListarMeusRachas() throws Exception {
        var rachaId = UUID.randomUUID();
        var rachaResponse = new RachaResponseDTO(rachaId, "Racha de Quinta", "Pelada semanal", "FUTEBOL", 10L, LocalDateTime.now());

        when(rachaService.listarRachasDoUsuario(any(Usuario.class))).thenReturn(List.of(rachaResponse));

        mockMvc.perform(get("/api/v1/rachas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rachaId.toString()))
                .andExpect(jsonPath("$[0].nome").value("Racha de Quinta"))
                .andExpect(jsonPath("$[0].esporte").value("FUTEBOL"))
                .andExpect(jsonPath("$[0].confirmados").value(10));
    }

    @Test
    @DisplayName("Deve listar membros de um racha específico por ID")
    void deveListarMembrosDoRacha() throws Exception {
        var rachaId = UUID.randomUUID();
        var racha = new Racha();
        racha.setId(rachaId);
        racha.setNome("Pelada Domingo");

        var member = new RachaMember();
        member.setId(UUID.randomUUID());
        member.setRacha(racha);
        member.setUser(usuarioLogado);
        member.setPosicao(Posicao.ATACANTE);
        member.setRachaRating(4.5f);
        member.setRole(RachaRole.PLAYER);

        when(rachaService.listMembers(eq(rachaId), any(Usuario.class)))
                .thenReturn(List.of(new RachaMemberResponseDTO(member, 4.5f, 1)));

        mockMvc.perform(get("/api/v1/rachas/{id}/members", rachaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rachaId").value(rachaId.toString()))
                .andExpect(jsonPath("$[0].nomeRacha").value("Pelada Domingo"))
                .andExpect(jsonPath("$[0].nomeJogador").value("organizador"))
                .andExpect(jsonPath("$[0].posicao").value("ATACANTE"))
                .andExpect(jsonPath("$[0].nota").value(4.5))
                .andExpect(jsonPath("$[0].minhaNota").value(4.5))
                .andExpect(jsonPath("$[0].totalAvaliacoes").value(1))
                .andExpect(jsonPath("$[0].papel").value("PLAYER"));
    }

    @Test
    @DisplayName("Deve detalhar um racha por ID e retornar HTTP 200")
    void deveDetalharRacha() throws Exception {
        var rachaId = UUID.randomUUID();
        var rachaResponse = new RachaResponseDTO(rachaId, "Vôlei de Sábado", null, "VOLEI", 3L, LocalDateTime.now());

        when(rachaService.buscarRacha(eq(rachaId), any(Usuario.class))).thenReturn(rachaResponse);

        mockMvc.perform(get("/api/v1/rachas/{id}", rachaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rachaId.toString()))
                .andExpect(jsonPath("$.esporte").value("VOLEI"))
                .andExpect(jsonPath("$.confirmados").value(3));
    }

    @Test
    @DisplayName("Deve avaliar jogador no modo AVANCADA por atributos e retornar HTTP 200")
    void deveAvaliarJogadorModoAvancadoNoController() throws Exception {
        var rachaId = UUID.randomUUID();
        var memberId = UUID.randomUUID();
        var racha = new Racha();
        racha.setId(rachaId);
        racha.setNome("Basquete de Domingo");
        racha.setEsporte("BASQUETE");

        var member = new RachaMember();
        member.setId(memberId);
        member.setRacha(racha);
        member.setNome("Rafael");
        member.setPosicao(Posicao.ARMADOR);
        member.setRachaRating(4.4f);
        member.setRole(RachaRole.PLAYER);

        var attrs = java.util.Map.of(
                br.com.rachaplus.api.domain.AtributoEsporte.FINALIZACAO, 4.0f,
                br.com.rachaplus.api.domain.AtributoEsporte.ARREMESSO, 4.5f,
                br.com.rachaplus.api.domain.AtributoEsporte.CONTROLE_BOLA, 5.0f,
                br.com.rachaplus.api.domain.AtributoEsporte.PASSE, 5.0f,
                br.com.rachaplus.api.domain.AtributoEsporte.DEFESA, 3.0f,
                br.com.rachaplus.api.domain.AtributoEsporte.REBOTE, 1.5f
        );

        var resposta = new RachaMemberResponseDTO(
                member,
                4.4f,
                br.com.rachaplus.api.domain.TipoAvaliacao.AVANCADA,
                attrs,
                attrs,
                1
        );

        when(rachaService.avaliarJogador(
                eq(rachaId),
                eq(memberId),
                any(br.com.rachaplus.api.application.dto.AvaliacaoDTO.class),
                any(Usuario.class)
        )).thenReturn(resposta);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/rachas/{id}/members/{memberId}/avaliacao", rachaId, memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "AVANCADA",
                                  "atributos": {
                                    "FINALIZACAO": 4.0,
                                    "ARREMESSO": 4.5,
                                    "CONTROLE_BOLA": 5.0,
                                    "PASSE": 5.0,
                                    "DEFESA": 3.0,
                                    "REBOTE": 1.5
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeJogador").value("Rafael"))
                .andExpect(jsonPath("$.nota").value(4.4))
                .andExpect(jsonPath("$.meuTipoAvaliacao").value("AVANCADA"))
                .andExpect(jsonPath("$.mediaAtributos.CONTROLE_BOLA").value(5.0));
    }
}
