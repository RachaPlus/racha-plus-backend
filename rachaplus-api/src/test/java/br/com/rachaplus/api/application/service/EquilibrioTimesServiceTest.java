package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.EquilibrarTimesRequestDTO;
import br.com.rachaplus.api.application.dto.EquilibrarTimesResponseDTO;
import br.com.rachaplus.api.application.dto.TimeGeradoDTO;
import br.com.rachaplus.api.domain.Posicao;
import br.com.rachaplus.api.domain.Racha;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import br.com.rachaplus.api.domain.repository.RachaRepository;
import br.com.rachaplus.api.infrastructure.controller.EquilibrioTimesController;
import br.com.rachaplus.api.infrastructure.controller.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquilibrioTimesServiceTest {

    private final Map<UUID, Racha> rachasStore = new LinkedHashMap<>();
    private final List<RachaMember> membersStore = new ArrayList<>();

    private EquilibrioTimesService equilibrioTimesService;
    private MockMvc mockMvc;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        rachasStore.clear();
        membersStore.clear();

        RachaRepository rachaRepository = (RachaRepository) Proxy.newProxyInstance(
                RachaRepository.class.getClassLoader(),
                new Class<?>[]{RachaRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.ofNullable(rachasStore.get((UUID) args[0]));
                    case "save", "saveAndFlush" -> {
                        Racha r = (Racha) args[0];
                        rachasStore.put(r.getId(), r);
                        yield r;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        RachaMemberRepository rachaMemberRepository = (RachaMemberRepository) Proxy.newProxyInstance(
                RachaMemberRepository.class.getClassLoader(),
                new Class<?>[]{RachaMemberRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByRachaId" -> {
                        UUID rachaId = (UUID) args[0];
                        yield membersStore.stream()
                                .filter(m -> m.getRacha().getId().equals(rachaId))
                                .toList();
                    }
                    case "existsByRachaIdAndUserId" -> membersStore.stream()
                            .anyMatch(m -> m.getRacha().getId().equals(args[0])
                                    && m.getUser() != null && m.getUser().getId().equals(args[1]));
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        equilibrioTimesService = new EquilibrioTimesService(rachaRepository, rachaMemberRepository, new Random(42));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EquilibrioTimesController(equilibrioTimesService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setUsername("admin_racha");
        admin.setEmail("admin@rachaplus.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Racha criarRacha(String nome, String esporte) {
        Racha r = new Racha();
        r.setId(UUID.randomUUID());
        r.setNome(nome);
        r.setEsporte(esporte);
        rachasStore.put(r.getId(), r);

        RachaMember mAdmin = new RachaMember();
        mAdmin.setId(UUID.randomUUID());
        mAdmin.setRacha(r);
        mAdmin.setUser(admin);
        mAdmin.setNome(admin.getNomeUsuario());
        mAdmin.setRole(RachaRole.ADMIN);
        mAdmin.setRachaRating(4.0f);
        if ("BASQUETE".equals(esporte)) mAdmin.setPosicao(Posicao.ARMADOR);
        else if ("VOLEI".equals(esporte)) mAdmin.setPosicao(Posicao.LEVANTADOR);
        else mAdmin.setPosicao(Posicao.GOLEIRO);
        membersStore.add(mAdmin);
        return r;
    }

    private RachaMember addJogador(Racha racha, String nome, Posicao pos, float nota) {
        RachaMember m = new RachaMember();
        m.setId(UUID.randomUUID());
        m.setRacha(racha);
        m.setNome(nome);
        m.setPosicao(pos);
        m.setRole(RachaRole.PLAYER);
        m.setRachaRating(nota);
        membersStore.add(m);
        return m;
    }

    @Test
    void deveRetornar400AoEnviarFormatoInvalidoParaBasquete() throws Exception {
        Racha racha = criarRacha("Basquete Domingo", "BASQUETE");
        UUID adminMemberId = membersStore.getFirst().getId();

        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/equilibrar-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jogadoresConfirmadosIds": ["%s"],
                                  "jogadoresPorTime": 7
                                }
                                """.formatted(adminMemberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("jogadoresPorTime"));
    }

    @Test
    void deveEquilibrar12JogadoresNoBasquete3x3Com4TimesEDeForaNull() throws Exception {
        Racha racha = criarRacha("Basquete 3x3", "BASQUETE");
        // Admin já é ARMADOR 4.0; adiciona mais 11 jogadores = 12 no total (4 ARMADOR, 4 ALA, 4 PIVO)
        addJogador(racha, "Armador2", Posicao.ARMADOR, 5.0f);
        addJogador(racha, "Armador3", Posicao.ARMADOR, 3.5f);
        addJogador(racha, "Armador4", Posicao.ARMADOR, 3.0f);

        addJogador(racha, "Ala1", Posicao.ALA, 4.5f);
        addJogador(racha, "Ala2", Posicao.ALA, 4.0f);
        addJogador(racha, "Ala3", Posicao.ALA, 3.5f);
        addJogador(racha, "Ala4", Posicao.ALA, 3.0f);

        addJogador(racha, "Pivo1", Posicao.PIVO, 5.0f);
        addJogador(racha, "Pivo2", Posicao.PIVO, 4.5f);
        addJogador(racha, "Pivo3", Posicao.PIVO, 4.0f);
        addJogador(racha, "Pivo4", Posicao.PIVO, 3.5f);

        List<UUID> todosIds = membersStore.stream().map(RachaMember::getId).toList();
        String idsJson = todosIds.stream().map(id -> "\"" + id + "\"").reduce((a, b) -> a + "," + b).orElse("");

        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/equilibrar-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jogadoresConfirmadosIds": [%s],
                                  "jogadoresPorTime": 3
                                }
                                """.formatted(idsJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.times", hasSize(4)))
                .andExpect(jsonPath("$.times[0].nome").value("Time 1"))
                .andExpect(jsonPath("$.times[0].jogadores", hasSize(3)))
                .andExpect(jsonPath("$.times[3].nome").value("Time 4"))
                .andExpect(jsonPath("$.times[3].jogadores", hasSize(3)))
                .andExpect(jsonPath("$.deFora").value(nullValue()));

        EquilibrarTimesResponseDTO resp = equilibrioTimesService.equilibrarTimes(
                racha.getId(),
                new EquilibrarTimesRequestDTO(todosIds, 3),
                admin
        );

        assertNull(resp.deFora());
        assertEquals(4, resp.times().size());

        double minMedia = resp.times().stream().mapToDouble(TimeGeradoDTO::mediaNota).min().orElse(0);
        double maxMedia = resp.times().stream().mapToDouble(TimeGeradoDTO::mediaNota).max().orElse(0);
        assertTrue(maxMedia - minMedia <= 0.4, "Médias dos times devem ser muito próximas");

        // Cada time deve ter 1 ARMADOR, 1 ALA e 1 PIVO
        for (TimeGeradoDTO time : resp.times()) {
            assertEquals(3, time.jogadores().size());
            assertEquals(3, time.jogadores().stream().map(TimeGeradoDTO.JogadorTimeDTO::posicao).distinct().count());
        }
    }

    @Test
    void deveEquilibrar22JogadoresNoVolei6x6Com3TimesE4DeForaPreservandoLevantadores() {
        Racha racha = criarRacha("Vôlei 6x6", "VOLEI");
        // Admin já é LEVANTADOR; adiciona mais 2 LEVANTADORES (total 3 escassos para 3 times) + 19 outras posições = 22 jogadores
        addJogador(racha, "Lev2", Posicao.LEVANTADOR, 4.5f);
        addJogador(racha, "Lev3", Posicao.LEVANTADOR, 3.5f);

        Posicao[] outras = {Posicao.PONTEIRO, Posicao.CENTRAL, Posicao.OPOSTO, Posicao.LIBERO};
        for (int i = 1; i <= 19; i++) {
            Posicao pos = outras[i % outras.length];
            float nota = 2.5f + ((i % 5) * 0.5f);
            addJogador(racha, "JogadorVolei" + i, pos, nota);
        }

        List<UUID> todosIds = membersStore.stream().map(RachaMember::getId).toList();
        assertEquals(22, todosIds.size());

        EquilibrarTimesResponseDTO resp = equilibrioTimesService.equilibrarTimes(
                racha.getId(),
                new EquilibrarTimesRequestDTO(todosIds, 6),
                admin
        );

        assertEquals(3, resp.times().size());
        for (TimeGeradoDTO time : resp.times()) {
            assertEquals(6, time.jogadores().size());
            long levantadoresNoTime = time.jogadores().stream()
                    .filter(j -> j.posicao() == Posicao.LEVANTADOR)
                    .count();
            assertEquals(1, levantadoresNoTime, "Cada time titular de 6x6 deve receber 1 levantador escasso");
        }

        assertNotNull(resp.deFora());
        assertEquals("De fora", resp.deFora().nome());
        assertEquals(4, resp.deFora().jogadores().size());
        assertTrue(resp.deFora().jogadores().stream().noneMatch(j -> j.posicao() == Posicao.LEVANTADOR),
                "Nenhum levantador escasso deve ir para o grupo De fora");
    }

    @Test
    void deveValidarDuplicadosMinimoConfirmadosEAcesso() throws Exception {
        Racha racha = criarRacha("Pelada 5x5", "FUTEBOL");
        for (int i = 1; i <= 5; i++) {
            addJogador(racha, "Atleta" + i, Posicao.MEIO_CAMPISTA, 4.0f);
        }

        List<UUID> seisIds = membersStore.stream().map(RachaMember::getId).toList();
        String seisIdsJson = seisIds.stream().map(id -> "\"" + id + "\"").reduce((a, b) -> a + "," + b).orElse("");

        // Menos que 2 * 5 = 10 confirmados -> 400
        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/equilibrar-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jogadoresConfirmadosIds": [%s],
                                  "jogadoresPorTime": 5
                                }
                                """.formatted(seisIdsJson)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("jogadoresConfirmadosIds"));

        // IDs duplicados -> 400
        UUID primeiro = seisIds.getFirst();
        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/equilibrar-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jogadoresConfirmadosIds": ["%s", "%s"],
                                  "jogadoresPorTime": 5
                                }
                                """.formatted(primeiro, primeiro)))
                .andExpect(status().isBadRequest());

        // Usuário não pertencente ao racha -> 403
        Usuario estranho = new Usuario();
        estranho.setId(UUID.randomUUID());
        estranho.setUsername("estranho");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(estranho, null, estranho.getAuthorities())
        );

        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/equilibrar-times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jogadoresConfirmadosIds": [%s],
                                  "jogadoresPorTime": 5
                                }
                                """.formatted(seisIdsJson)))
                .andExpect(status().isForbidden());
    }
}
