package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.domain.*;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import br.com.rachaplus.api.domain.repository.RachaRepository;
import br.com.rachaplus.api.domain.repository.SolicitacaoEntradaRepository;
import br.com.rachaplus.api.infrastructure.controller.ConviteController;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConviteServiceTest {

    private final Map<UUID, Racha> rachasStore = new LinkedHashMap<>();
    private final List<RachaMember> membersStore = new ArrayList<>();
    private final List<SolicitacaoEntrada> solicitacoesStore = new ArrayList<>();

    private MockMvc mockMvc;
    private Usuario admin;
    private Racha racha;
    private RachaMember perfilVirtual;

    @BeforeEach
    void setUp() {
        rachasStore.clear();
        membersStore.clear();
        solicitacoesStore.clear();

        RachaRepository rachaRepository = (RachaRepository) Proxy.newProxyInstance(
                RachaRepository.class.getClassLoader(),
                new Class<?>[]{RachaRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save", "saveAndFlush" -> {
                        Racha r = (Racha) args[0];
                        if (r.getId() == null) r.setId(UUID.randomUUID());
                        rachasStore.put(r.getId(), r);
                        yield r;
                    }
                    case "findById" -> Optional.ofNullable(rachasStore.get((UUID) args[0]));
                    case "findByCodigoConvite" -> rachasStore.values().stream()
                            .filter(r -> args[0].equals(r.getCodigoConvite()))
                            .findFirst();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        RachaMemberRepository memberRepository = (RachaMemberRepository) Proxy.newProxyInstance(
                RachaMemberRepository.class.getClassLoader(),
                new Class<?>[]{RachaMemberRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save", "saveAndFlush" -> {
                        RachaMember m = (RachaMember) args[0];
                        if (m.getId() == null) m.setId(UUID.randomUUID());
                        membersStore.removeIf(e -> e.getId().equals(m.getId()));
                        membersStore.add(m);
                        yield m;
                    }
                    case "findById" -> membersStore.stream()
                            .filter(m -> m.getId().equals(args[0]))
                            .findFirst();
                    case "findAllByRachaId" -> membersStore.stream()
                            .filter(m -> m.getRacha().getId().equals(args[0]))
                            .toList();
                    case "findByRachaIdAndUserId" -> membersStore.stream()
                            .filter(m -> m.getRacha().getId().equals(args[0])
                                    && m.getUser() != null && m.getUser().getId().equals(args[1]))
                            .findFirst();
                    case "existsByRachaIdAndUserId" -> membersStore.stream()
                            .anyMatch(m -> m.getRacha().getId().equals(args[0])
                                    && m.getUser() != null && m.getUser().getId().equals(args[1]));
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        SolicitacaoEntradaRepository solicitacaoRepository = (SolicitacaoEntradaRepository) Proxy.newProxyInstance(
                SolicitacaoEntradaRepository.class.getClassLoader(),
                new Class<?>[]{SolicitacaoEntradaRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save", "saveAndFlush" -> {
                        SolicitacaoEntrada s = (SolicitacaoEntrada) args[0];
                        if (s.getId() == null) s.setId(UUID.randomUUID());
                        if (s.getCriadoEm() == null) s.setCriadoEm(LocalDateTime.now());
                        solicitacoesStore.removeIf(e -> e.getId().equals(s.getId()));
                        solicitacoesStore.add(s);
                        yield s;
                    }
                    case "findById" -> solicitacoesStore.stream()
                            .filter(s -> s.getId().equals(args[0]))
                            .findFirst();
                    case "findAllByRachaIdAndStatusOrderByCriadoEmDesc" -> solicitacoesStore.stream()
                            .filter(s -> s.getRacha().getId().equals(args[0]) && s.getStatus() == args[1])
                            .toList();
                    case "existsByRachaIdAndSolicitanteIdAndStatus" -> solicitacoesStore.stream()
                            .anyMatch(s -> s.getRacha().getId().equals(args[0])
                                    && s.getSolicitante().getId().equals(args[1])
                                    && s.getStatus() == args[2]);
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        ConviteService service = new ConviteService(rachaRepository, memberRepository, solicitacaoRepository);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ConviteController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setUsername("admin_racha");

        racha = new Racha();
        racha.setId(UUID.randomUUID());
        racha.setNome("Basquete de Domingo");
        racha.setEsporte("BASQUETE");
        rachasStore.put(racha.getId(), racha);

        RachaMember adminMember = new RachaMember();
        adminMember.setId(UUID.randomUUID());
        adminMember.setRacha(racha);
        adminMember.setUser(admin);
        adminMember.setNome("admin_racha");
        adminMember.setRole(RachaRole.ADMIN);
        membersStore.add(adminMember);

        perfilVirtual = new RachaMember();
        perfilVirtual.setId(UUID.randomUUID());
        perfilVirtual.setRacha(racha);
        perfilVirtual.setNome("Rafael");
        perfilVirtual.setPosicao(Posicao.ARMADOR);
        perfilVirtual.setRachaRating(4.3f);
        perfilVirtual.setRole(RachaRole.PLAYER);
        membersStore.add(perfilVirtual);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(Usuario u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities())
        );
    }

    @Test
    void deveGerarConviteReivindicarPerfilEAprovarPeloAdmin() throws Exception {
        autenticar(admin);
        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/convite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").isNotEmpty())
                .andExpect(jsonPath("$.nomeRacha").value("Basquete de Domingo"));

        String codigo = racha.getCodigoConvite();
        assertNotNull(codigo);

        Usuario convidado = new Usuario();
        convidado.setId(UUID.randomUUID());
        convidado.setUsername("rafa_23");
        autenticar(convidado);

        mockMvc.perform(get("/api/v1/convites/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jaEMembro").value(false))
                .andExpect(jsonPath("$.solicitacaoPendente").value(false))
                .andExpect(jsonPath("$.perfisDisponiveis[0].nomeJogador").value("Rafael"));

        mockMvc.perform(post("/api/v1/convites/" + codigo + "/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "tipo": "REIVINDICAR", "memberId": "%s" }
                                """.formatted(perfilVirtual.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("REIVINDICAR"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        UUID solId = solicitacoesStore.getFirst().getId();

        autenticar(admin);
        mockMvc.perform(get("/api/v1/rachas/" + racha.getId() + "/solicitacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usernameSolicitante").value("rafa_23"))
                .andExpect(jsonPath("$[0].nomePerfilAlvo").value("Rafael"));

        mockMvc.perform(post("/api/v1/rachas/" + racha.getId() + "/solicitacoes/" + solId + "/aprovar"))
                .andExpect(status().isOk());

        assertEquals(convidado.getId(), perfilVirtual.getUser().getId());
        assertEquals(StatusSolicitacao.APROVADA, solicitacoesStore.getFirst().getStatus());
    }
}
