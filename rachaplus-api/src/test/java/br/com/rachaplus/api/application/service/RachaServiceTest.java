package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.CreateRachaDTO;
import br.com.rachaplus.api.application.dto.CreateRachaMemberDTO;
import br.com.rachaplus.api.application.dto.RachaMemberResponseDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.domain.Avaliacao;
import br.com.rachaplus.api.domain.Posicao;
import br.com.rachaplus.api.domain.Racha;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.repository.AvaliacaoRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RachaServiceTest {

    private final Map<UUID, Racha> rachasStore = new LinkedHashMap<>();
    private final List<RachaMember> membersStore = new ArrayList<>();
    private final List<Avaliacao> avaliacoesStore = new ArrayList<>();
    private final List<br.com.rachaplus.api.domain.Partida> partidasStore = new ArrayList<>();

    private RachaService rachaService;
    private Validator validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        rachasStore.clear();
        membersStore.clear();
        avaliacoesStore.clear();

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
                    case "findAllByRachaId" -> {
                        UUID rachaId = (UUID) args[0];
                        yield membersStore.stream()
                                .filter(m -> m.getRacha().getId().equals(rachaId))
                                .toList();
                    }
                    case "findById" -> membersStore.stream()
                            .filter(m -> m.getId().equals(args[0]))
                            .findFirst();
                    case "findByRachaIdAndUserId" -> membersStore.stream()
                            .filter(m -> m.getRacha().getId().equals(args[0])
                                    && m.getUser() != null && m.getUser().getId().equals(args[1]))
                            .findFirst();
                    case "existsByRachaIdAndUserId" -> membersStore.stream()
                            .anyMatch(m -> m.getRacha().getId().equals(args[0])
                                    && m.getUser() != null && m.getUser().getId().equals(args[1]));
                    case "delete" -> {
                        RachaMember m = (RachaMember) args[0];
                        membersStore.removeIf(existing -> existing.getId().equals(m.getId()));
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        partidasStore.clear();
        PartidaRepository partidaRepository = (PartidaRepository) Proxy.newProxyInstance(
                PartidaRepository.class.getClassLoader(),
                new Class<?>[]{PartidaRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save", "saveAndFlush" -> {
                        var p = (br.com.rachaplus.api.domain.Partida) args[0];
                        if (p.getId() == null) p.setId(UUID.randomUUID());
                        if (p.getDataPartida() == null) p.setDataPartida(LocalDateTime.now());
                        partidasStore.removeIf(existing -> existing.getId().equals(p.getId()));
                        partidasStore.add(p);
                        yield p;
                    }
                    case "findAllByRachaId" -> {
                        UUID rId = (UUID) args[0];
                        yield partidasStore.stream()
                                .filter(p -> p.getRacha().getId().equals(rId))
                                .toList();
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        AvaliacaoRepository avaliacaoRepository = (AvaliacaoRepository) Proxy.newProxyInstance(
                AvaliacaoRepository.class.getClassLoader(),
                new Class<?>[]{AvaliacaoRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save", "saveAndFlush" -> {
                        Avaliacao a = (Avaliacao) args[0];
                        if (a.getId() == null) {
                            a.setId(UUID.randomUUID());
                        }
                        avaliacoesStore.removeIf(existing -> existing.getId().equals(a.getId()));
                        avaliacoesStore.add(a);
                        yield a;
                    }
                    case "findByAvaliadorIdAndAvaliadoId" -> avaliacoesStore.stream()
                            .filter(a -> a.getAvaliador().getId().equals(args[0]) && a.getAvaliado().getId().equals(args[1]))
                            .findFirst();
                    case "findAllByRachaId" -> avaliacoesStore.stream()
                            .filter(a -> a.getRacha().getId().equals(args[0]))
                            .toList();
                    case "findAllByAvaliadoId" -> avaliacoesStore.stream()
                            .filter(a -> a.getAvaliado().getId().equals(args[0]))
                            .toList();
                    case "countByAvaliadoId" -> avaliacoesStore.stream()
                            .filter(a -> a.getAvaliado().getId().equals(args[0]))
                            .count();
                    case "mediaPorAvaliado" -> {
                        OptionalDouble media = avaliacoesStore.stream()
                                .filter(a -> a.getAvaliado().getId().equals(args[0]))
                                .mapToDouble(Avaliacao::getNota)
                                .average();
                        yield media.isPresent() ? media.getAsDouble() : null;
                    }
                    case "deleteAll" -> {
                        Iterable<?> itens = (Iterable<?>) args[0];
                        Set<UUID> idsRemover = new HashSet<>();
                        for (Object item : itens) {
                            Avaliacao av = (Avaliacao) item;
                            if (av.getAtributos() != null) {
                                av.getAtributos().clear();
                            }
                            idsRemover.add(av.getId());
                        }
                        avaliacoesStore.removeIf(a -> idsRemover.contains(a.getId()));
                        yield null;
                    }
                    case "deleteAllByAvaliadoId" -> {
                        avaliacoesStore.removeIf(a -> a.getAvaliado().getId().equals(args[0]));
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        AvaliacaoService avaliacaoService = new AvaliacaoService(avaliacaoRepository, rachaMemberRepository);
        rachaService = new RachaService(rachaRepository, rachaMemberRepository, partidaRepository, avaliacaoService);
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

        RachaMemberResponseDTO responseDTO = assertDoesNotThrow(() ->
                rachaService.adicionarJogador(racha.getId(), new CreateRachaMemberDTO("Carlos", "ATACANTE", null), owner));

        RachaMember salvo = membersStore.stream()
                .filter(m -> m.getId().equals(responseDTO.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(RachaRole.PLAYER, salvo.getRole());
        assertNull(salvo.getUser());
        assertEquals("Carlos", salvo.getNome());

        assertEquals(racha.getId(), responseDTO.rachaId());
        assertEquals("Pelada de Quinta", responseDTO.nomeRacha());
        assertNull(responseDTO.usuarioId());
        assertEquals("Carlos", responseDTO.nomeJogador());
        assertEquals(Posicao.ATACANTE, responseDTO.posicao());
        assertNull(responseDTO.minhaNota());
        assertEquals(0, responseDTO.totalAvaliacoes());
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
                                  "nome": "Carlos",
                                  "posicao": "ATACANTE"
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

    // ===================== Posições + Rating =====================

    private Usuario logarAdmin() {
        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setUsername("admin_racha");
        admin.setEmail("admin@email.com");
        admin.setSenha("Senha123");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );
        return admin;
    }

    private UUID idDoJogador(String nome) {
        return membersStore.stream()
                .filter(m -> nome.equals(m.getNome()) && m.getRole() == RachaRole.PLAYER)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    @Test
    void deveRetornar400AoAdicionarGoleiroEmRachaDeVolei() throws Exception {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Vôlei de Sábado", null, "VOLEI", admin);

        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Carlos", "posicao": "GOLEIRO" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("posicao"));

        // Posição válida para vôlei passa
        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Carlos", "posicao": "LEVANTADOR" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.posicao").value("LEVANTADOR"));
    }

    @Test
    void deveRegistrarNotaInicialComoAvaliacaoDoAdmin() throws Exception {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Pelada de Quinta", null, "FUTEBOL", admin);

        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Carlos", "posicao": "ATACANTE", "notaInicial": 3.5 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.nota").value(3.5))
                .andExpect(jsonPath("$.minhaNota").value(3.5))
                .andExpect(jsonPath("$.totalAvaliacoes").value(1));

        mockMvc.perform(get("/api/v1/rachas/" + racha.getId() + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nomeJogador == 'Carlos')].nota").value(3.5))
                .andExpect(jsonPath("$[?(@.nomeJogador == 'Carlos')].minhaNota").value(3.5))
                .andExpect(jsonPath("$[?(@.nomeJogador == 'Carlos')].totalAvaliacoes").value(1));

        assertEquals(1, avaliacoesStore.size());
        assertEquals(admin.getId(), avaliacoesStore.getFirst().getAvaliador().getId());
    }

    @Test
    void deveRetornar400ParaNotaInicialForaDoPassoDeMeioPonto() throws Exception {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Pelada de Quinta", null, "FUTEBOL", admin);

        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Carlos", "posicao": "ATACANTE", "notaInicial": 4.3 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("notaInicial"));

        assertEquals(1, membersStore.size()); // só o admin, nada foi salvo
    }

    @Test
    void deveAtualizarAvaliacaoSemDuplicarRegistro() throws Exception {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Pelada de Quinta", null, "FUTEBOL", admin);
        rachaService.adicionarJogador(racha.getId(), new CreateRachaMemberDTO("Carlos", "ATACANTE", 3.5f), admin);
        UUID carlosId = idDoJogador("Carlos");

        mockMvc.perform(put("/api/v1/rachas/" + racha.getId() + "/members/" + carlosId + "/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nota": 4.5 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(4.5))
                .andExpect(jsonPath("$.minhaNota").value(4.5))
                .andExpect(jsonPath("$.totalAvaliacoes").value(1));

        assertEquals(1, avaliacoesStore.size());
        assertEquals(4.5f, avaliacoesStore.getFirst().getNota());
    }

    @Test
    void deveCalcularOvrComoMediaDeVariosAvaliadoresArredondada() {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Pelada de Quinta", null, "FUTEBOL", admin);
        rachaService.adicionarJogador(racha.getId(), new CreateRachaMemberDTO("Carlos", "ATACANTE", 4.0f), admin);
        UUID carlosId = idDoJogador("Carlos");

        // Segundo avaliador (próxima sprint: membro com usuário vinculado)
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        outro.setUsername("outro");
        RachaMember membroOutro = new RachaMember();
        membroOutro.setId(UUID.randomUUID());
        membroOutro.setRacha(racha);
        membroOutro.setUser(outro);
        membroOutro.setRole(RachaRole.PLAYER);
        membersStore.add(membroOutro);

        RachaMemberResponseDTO r = rachaService.avaliarJogador(racha.getId(), carlosId, 4.5f, outro);

        // (4.0 + 4.5) / 2 = 4.25 -> 4.3
        assertEquals(4.3f, r.nota());
        assertEquals(4.5f, r.minhaNota());
        assertEquals(2, r.totalAvaliacoes());
    }

    @Test
    void deveRetornar400AoAdminAvaliarASiMesmo() throws Exception {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Pelada de Quinta", null, "FUTEBOL", admin);
        UUID adminMemberId = membersStore.getFirst().getId();

        mockMvc.perform(put("/api/v1/rachas/" + racha.getId() + "/members/" + adminMemberId + "/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nota": 5.0 }
                                """))
                .andExpect(status().isBadRequest());

        assertTrue(avaliacoesStore.isEmpty());
    }

    @Test
    void deveEditarERemoverJogadorApagandoAvaliacoes() throws Exception {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Pelada de Quinta", null, "FUTEBOL", admin);
        rachaService.adicionarJogador(racha.getId(), new CreateRachaMemberDTO("Carlos", "ATACANTE", 3.0f), admin);
        UUID carlosId = idDoJogador("Carlos");

        mockMvc.perform(put("/api/v1/rachas/" + racha.getId() + "/members/" + carlosId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Carlão", "posicao": "zagueiro" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeJogador").value("Carlão"))
                .andExpect(jsonPath("$.posicao").value("ZAGUEIRO"))
                .andExpect(jsonPath("$.minhaNota").value(3.0));

        mockMvc.perform(delete("/api/v1/rachas/" + racha.getId() + "/members/" + carlosId))
                .andExpect(status().isNoContent());

        assertTrue(membersStore.stream().noneMatch(m -> m.getId().equals(carlosId)));
        assertTrue(avaliacoesStore.isEmpty());

        // Admin não pode ser removido
        UUID adminMemberId = membersStore.getFirst().getId();
        mockMvc.perform(delete("/api/v1/rachas/" + racha.getId() + "/members/" + adminMemberId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveDetalharRachaApenasParaMembros() throws Exception {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Pelada de Quinta", "Society", "FUTEBOL", admin);

        mockMvc.perform(get("/api/v1/rachas/" + racha.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pelada de Quinta"))
                .andExpect(jsonPath("$.esporte").value("FUTEBOL"))
                .andExpect(jsonPath("$.confirmados").value(1));

        mockMvc.perform(get("/api/v1/rachas/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());

        // Outro usuário, que não é membro
        Usuario intruso = new Usuario();
        intruso.setId(UUID.randomUUID());
        intruso.setUsername("intruso");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(intruso, null, intruso.getAuthorities())
        );

        mockMvc.perform(get("/api/v1/rachas/" + racha.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void naoDeveExporDetalhesInternosOuSqlEmErroInesperado() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.tratarErroInesperado(
                new RuntimeException("ERROR: null value in column \"nome\" of relation \"usuarios\" $2a$10$hashSecreto")
        );

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Erro interno no servidor. Tente novamente mais tarde.", response.getBody());
        assertFalse(response.getBody().contains("usuarios"));
        assertFalse(response.getBody().contains("$2a$10$"));
    }

    // ===================== Sistema de Rating Dinâmico v2 (Atributos + Arquétipos) =====================

    @Test
    void deveTerPesosDeArquetipoSomandoCemPorCentoEmTodasAsPosicoes() {
        for (Posicao posicao : Posicao.values()) {
            var atributosEsperados = br.com.rachaplus.api.domain.AtributoEsporte.doEsporte(posicao.getEsporte());
            assertEquals(6, atributosEsperados.size());
            assertEquals(new HashSet<>(atributosEsperados), posicao.getPesos().keySet(),
                    "Posição " + posicao + " deve possuir exatamente os 6 atributos de " + posicao.getEsporte());

            double somaPesos = posicao.getPesos().values().stream()
                    .mapToDouble(Float::doubleValue)
                    .sum();
            assertEquals(1.0, somaPesos, 1e-5, "Soma dos pesos de " + posicao + " deve ser 1.0 (100%)");
        }
    }

    @Test
    void deveCalcularAvaliacaoAvancadaPonderadaERecalcularAoTrocarPosicao() throws Exception {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Basquete de Domingo", null, "BASQUETE", admin);
        rachaService.adicionarJogador(racha.getId(), new CreateRachaMemberDTO("Rafael", "ARMADOR", null), admin);
        UUID rafaelId = idDoJogador("Rafael");

        // ARMADOR: FINALIZACAO(0.15)*4.0 + ARREMESSO(0.20)*4.5 + CONTROLE_BOLA(0.25)*5.0
        //        + PASSE(0.25)*5.0 + DEFESA(0.10)*3.0 + REBOTE(0.05)*1.5 = 4.375 -> 4.4
        mockMvc.perform(put("/api/v1/rachas/" + racha.getId() + "/members/" + rafaelId + "/avaliacao")
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
                .andExpect(jsonPath("$.posicao").value("ARMADOR"))
                .andExpect(jsonPath("$.nota").value(4.4))
                .andExpect(jsonPath("$.minhaNota").value(4.4))
                .andExpect(jsonPath("$.meuTipoAvaliacao").value("AVANCADA"))
                .andExpect(jsonPath("$.meusAtributos.CONTROLE_BOLA").value(5.0))
                .andExpect(jsonPath("$.meusAtributos.REBOTE").value(1.5))
                .andExpect(jsonPath("$.mediaAtributos.CONTROLE_BOLA").value(5.0))
                .andExpect(jsonPath("$.mediaAtributos.REBOTE").value(1.5))
                .andExpect(jsonPath("$.totalAvaliacoes").value(1));

        // Troca posição ARMADOR -> PIVO:
        // PIVO: FINALIZACAO(0.25)*4.0 + ARREMESSO(0.05)*4.5 + CONTROLE_BOLA(0.05)*5.0
        //     + PASSE(0.10)*5.0 + DEFESA(0.25)*3.0 + REBOTE(0.30)*1.5 = 3.175 -> 3.2
        mockMvc.perform(put("/api/v1/rachas/" + racha.getId() + "/members/" + rafaelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Rafael", "posicao": "PIVO" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicao").value("PIVO"))
                .andExpect(jsonPath("$.nota").value(3.2))
                .andExpect(jsonPath("$.minhaNota").value(3.2))
                .andExpect(jsonPath("$.meuTipoAvaliacao").value("AVANCADA"))
                .andExpect(jsonPath("$.mediaAtributos.REBOTE").value(1.5));
    }

    @Test
    void deveCombinarAvaliacaoGeralEAvancadaEPermitirUpsertEntreModosERemoverSemErro() throws Exception {
        Usuario admin = logarAdmin();
        Racha racha = rachaService.create("Basquete de Domingo", null, "BASQUETE", admin);
        rachaService.adicionarJogador(racha.getId(), new CreateRachaMemberDTO("Rafael", "ARMADOR", null), admin);
        UUID rafaelId = idDoJogador("Rafael");

        // 1) Admin envia avaliação AVANCADA -> nota 4.4
        mockMvc.perform(put("/api/v1/rachas/" + racha.getId() + "/members/" + rafaelId + "/avaliacao")
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
                .andExpect(jsonPath("$.nota").value(4.4));

        // 2) Segundo usuário envia avaliação GERAL (4.0)
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        outro.setUsername("outro_player");
        RachaMember membroOutro = new RachaMember();
        membroOutro.setId(UUID.randomUUID());
        membroOutro.setRacha(racha);
        membroOutro.setUser(outro);
        membroOutro.setRole(RachaRole.PLAYER);
        membersStore.add(membroOutro);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(outro, null, outro.getAuthorities())
        );

        // OVR = (4.4 + 4.0) / 2 = 4.2; mediaAtributos reflete apenas a avaliação AVANCADA!
        mockMvc.perform(put("/api/v1/rachas/" + racha.getId() + "/members/" + rafaelId + "/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "tipo": "GERAL", "nota": 4.0 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(4.2))
                .andExpect(jsonPath("$.minhaNota").value(4.0))
                .andExpect(jsonPath("$.meuTipoAvaliacao").value("GERAL"))
                .andExpect(jsonPath("$.meusAtributos").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.mediaAtributos.CONTROLE_BOLA").value(5.0))
                .andExpect(jsonPath("$.totalAvaliacoes").value(2));

        // 3) Admin faz upsert trocando sua avaliação de AVANCADA -> GERAL (5.0): limpa atributos anteriores
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        mockMvc.perform(put("/api/v1/rachas/" + racha.getId() + "/members/" + rafaelId + "/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nota": 5.0 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(4.5))
                .andExpect(jsonPath("$.minhaNota").value(5.0))
                .andExpect(jsonPath("$.meuTipoAvaliacao").value("GERAL"))
                .andExpect(jsonPath("$.meusAtributos").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.mediaAtributos").isEmpty())
                .andExpect(jsonPath("$.totalAvaliacoes").value(2));

        // 4) Admin volta para AVANCADA e depois remove o jogador -> 204 No Content sem órfãos
        mockMvc.perform(put("/api/v1/rachas/" + racha.getId() + "/members/" + rafaelId + "/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "AVANCADA",
                                  "atributos": {
                                    "FINALIZACAO": 4.0,
                                    "ARREMESSO": 4.0,
                                    "CONTROLE_BOLA": 4.0,
                                    "PASSE": 4.0,
                                    "DEFESA": 4.0,
                                    "REBOTE": 4.0
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/rachas/" + racha.getId() + "/members/" + rafaelId))
                .andExpect(status().isNoContent());

        assertTrue(avaliacoesStore.isEmpty());
    }

    @Test
    void deveValidarAtributoDeEsporteErradoEAceitarLiberoSemAtaqueEBloqueio() throws Exception {
        Usuario admin = logarAdmin();
        Racha rachaBasquete = rachaService.create("Basquete de Domingo", null, "BASQUETE", admin);
        rachaService.adicionarJogador(rachaBasquete.getId(), new CreateRachaMemberDTO("Rafael", "ARMADOR", null), admin);
        UUID rafaelId = idDoJogador("Rafael");

        // Atributo de Futebol (AGILIDADE) em racha de Basquete -> 400 Bad Request
        mockMvc.perform(put("/api/v1/rachas/" + rachaBasquete.getId() + "/members/" + rafaelId + "/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "AVANCADA",
                                  "atributos": {
                                    "FINALIZACAO": 4.0,
                                    "ARREMESSO": 4.0,
                                    "CONTROLE_BOLA": 4.0,
                                    "PASSE": 4.0,
                                    "DEFESA": 4.0,
                                    "AGILIDADE": 4.5
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("atributos"));

        // Em Vôlei, LIBERO possui peso 0.00 em ATAQUE e BLOQUEIO -> aceita omiti-los
        Racha rachaVolei = rachaService.create("Vôlei de Quarta", null, "VOLEI", admin);
        RachaMemberResponseDTO marcos = rachaService.adicionarJogador(
                rachaVolei.getId(), new CreateRachaMemberDTO("Marcos", "LIBERO", null), admin);

        // SAQUE(0.05)*4.0 + RECEPCAO(0.45)*5.0 + LEVANTAMENTO(0.10)*4.0 + DEFESA_FUNDO(0.40)*4.5 = 4.65 -> 4.7
        mockMvc.perform(put("/api/v1/rachas/" + rachaVolei.getId() + "/members/" + marcos.id() + "/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "AVANCADA",
                                  "atributos": {
                                    "SAQUE": 4.0,
                                    "RECEPCAO": 5.0,
                                    "LEVANTAMENTO": 4.0,
                                    "DEFESA_FUNDO": 4.5
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(4.7))
                .andExpect(jsonPath("$.meuTipoAvaliacao").value("AVANCADA"));
    }
}
