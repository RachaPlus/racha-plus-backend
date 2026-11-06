package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.CreateRachaDTO;
import br.com.rachaplus.api.application.dto.CreateRachaMemberDTO;
import br.com.rachaplus.api.application.dto.RachaMemberResponseDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.domain.Racha;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.repository.PartidaRepository;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import br.com.rachaplus.api.domain.repository.RachaRepository;
import br.com.rachaplus.api.infrastructure.controller.GlobalExceptionHandler;
import br.com.rachaplus.api.infrastructure.controller.RachaController;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RachaServiceTest {

    private final Map<UUID, Racha> rachasStore = new LinkedHashMap<>();
    private final List<RachaMember> membersStore = new ArrayList<>();

    private RachaService rachaService;
    private Validator validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        rachasStore.clear();
        membersStore.clear();

        RachaRepository rachaRepository = (RachaRepository) Proxy.newProxyInstance(
                RachaRepository.class.getClassLoader(),
                new Class<?>[]{RachaRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save", "saveAndFlush" -> {
                        Racha r = (Racha) args[0];
                        if (r.getId() == null) {
                            r.setId(UUID.randomUUID());
                        }
                        if (r.getDataCriacao() == null) {
                            r.setDataCriacao(LocalDateTime.of(2026, 11, 5, 19, 0, 0));
                        }
                        rachasStore.put(r.getId(), r);
                        yield r;
                    }
                    case "findById" -> Optional.ofNullable(rachasStore.get((UUID) args[0]));
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        RachaMemberRepository rachaMemberRepository = (RachaMemberRepository) Proxy.newProxyInstance(
                RachaMemberRepository.class.getClassLoader(),
                new Class<?>[]{RachaMemberRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save", "saveAndFlush" -> {
                        RachaMember m = (RachaMember) args[0];
                        if (m.getId() == null) {
                            m.setId(UUID.randomUUID());
                        }
                        membersStore.removeIf(existing -> existing.getId().equals(m.getId()));
                        membersStore.add(m);
                        yield m;
                    }
                    case "findAllByUser" -> {
                        Usuario u = (Usuario) args[0];
                        yield membersStore.stream()
                                .filter(m -> m.getUser() != null && m.getUser().getId().equals(u.getId()))
                                .toList();
                    }
                    case "countByRachaId" -> {
                        UUID rachaId = (UUID) args[0];
                        yield membersStore.stream()
                                .filter(m -> m.getRacha().getId().equals(rachaId))
                                .count();
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        PartidaRepository partidaRepository = (PartidaRepository) Proxy.newProxyInstance(
                PartidaRepository.class.getClassLoader(),
                new Class<?>[]{PartidaRepository.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }
        );

        rachaService = new RachaService(rachaRepository, rachaMemberRepository, partidaRepository);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RachaController(rachaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveValidarCreateRachaDTOAceitandoApenasEsportesPermitidos() {
        assertTrue(validator.validate(new CreateRachaDTO("Pelada de Quinta", "Society às 20h", "FUTEBOL")).isEmpty());
        assertTrue(validator.validate(new CreateRachaDTO("Racha Quadra", null, "BASQUETE")).isEmpty());
        assertTrue(validator.validate(new CreateRachaDTO("Vôlei Sábado", "", "VOLEI")).isEmpty());

        assertFalse(validator.validate(new CreateRachaDTO("", "Desc", "FUTEBOL")).isEmpty());
        assertFalse(validator.validate(new CreateRachaDTO("Pelada", "Desc", "")).isEmpty());
        assertFalse(validator.validate(new CreateRachaDTO("Pelada", "Desc", "TENIS")).isEmpty());
    }

    @Test
    void deveCriarRachaComEsporteEListarComConfirmadosEmPortugues() {
        Usuario owner = new Usuario();
        owner.setId(UUID.randomUUID());
        owner.setUsername("admin_racha");
        owner.setEmail("admin@email.com");
        owner.setSenha("Senha123");

        Racha criado = rachaService.create("Pelada de Quinta", "Society toda quinta às 20h", "FUTEBOL", owner);

        assertEquals("Pelada de Quinta", criado.getNome());
        assertEquals("Society toda quinta às 20h", criado.getDescricao());
        assertEquals("FUTEBOL", criado.getEsporte());

        List<RachaResponseDTO> listagem = rachaService.listarRachasDoUsuario(owner);
        assertEquals(1, listagem.size());

        RachaResponseDTO dto = listagem.getFirst();
        assertEquals(criado.getId(), dto.id());
        assertEquals("Pelada de Quinta", dto.nome());
        assertEquals("Society toda quinta às 20h", dto.descricao());
        assertEquals("FUTEBOL", dto.esporte());
        assertEquals(1L, dto.confirmados());
        assertNotNull(dto.dataCriacao());
    }

    @Test
    void deveAdicionarJogadorAvulsoComRolePlayerESemNullPointerNoDTO() {
        Usuario owner = new Usuario();
        owner.setId(UUID.randomUUID());
        owner.setUsername("admin_racha");

        Racha racha = rachaService.create("Pelada de Quinta", "Society", "FUTEBOL", owner);

        RachaMember salvo = rachaService.adicionarJogador(racha.getId(), new CreateRachaMemberDTO("Carlos"));

        assertEquals(RachaRole.PLAYER, salvo.getRole());
        assertNull(salvo.getUser());
        assertEquals("Carlos", salvo.getNome());

        RachaMemberResponseDTO responseDTO = assertDoesNotThrow(() -> new RachaMemberResponseDTO(salvo));
        assertEquals(salvo.getId(), responseDTO.id());
        assertEquals(racha.getId(), responseDTO.rachaId());
        assertEquals("Pelada de Quinta", responseDTO.nomeRacha());
        assertNull(responseDTO.usuarioId());
        assertEquals("Carlos", responseDTO.nomeJogador());
        assertEquals(RachaRole.PLAYER, responseDTO.papel());
    }

    @Test
    void deveAtenderFluxoHttpDeCriacaoListagemEJogadorAvulsoNoController() throws Exception {
        Usuario owner = new Usuario();
        owner.setId(UUID.randomUUID());
        owner.setUsername("admin_racha");
        owner.setEmail("admin@email.com");
        owner.setSenha("Senha123");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities())
        );

        // 1. POST /api/v1/rachas com nome, descricao e esporte
        mockMvc.perform(post("/api/v1/rachas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Pelada de Quinta",
                                  "descricao": "Society toda quinta às 20h",
                                  "esporte": "FUTEBOL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Pelada de Quinta"))
                .andExpect(jsonPath("$.descricao").value("Society toda quinta às 20h"))
                .andExpect(jsonPath("$.esporte").value("FUTEBOL"))
                .andExpect(jsonPath("$.confirmados").value(1));

        UUID rachaId = rachasStore.keySet().iterator().next();

        // 2. GET /api/v1/rachas devolvendo a lista padronizada com confirmados: 1
        mockMvc.perform(get("/api/v1/rachas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rachaId.toString()))
                .andExpect(jsonPath("$[0].nome").value("Pelada de Quinta"))
                .andExpect(jsonPath("$[0].descricao").value("Society toda quinta às 20h"))
                .andExpect(jsonPath("$[0].esporte").value("FUTEBOL"))
                .andExpect(jsonPath("$[0].confirmados").value(1))
                .andExpect(jsonPath("$[0].dataCriacao").exists());

        // 3. POST /api/v1/rachas/{id}/members retornando 201 Created sem NullPointerException
        mockMvc.perform(post("/api/v1/rachas/" + rachaId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Carlos"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.rachaId").value(rachaId.toString()))
                .andExpect(jsonPath("$.nomeRacha").value("Pelada de Quinta"))
                .andExpect(jsonPath("$.usuarioId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.nomeJogador").value("Carlos"))
                .andExpect(jsonPath("$.papel").value("PLAYER"));

        // 4. Validação 400 Bad Request ao enviar esporte inválido
        mockMvc.perform(post("/api/v1/rachas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Racha Inválido",
                                  "esporte": "TENIS"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
