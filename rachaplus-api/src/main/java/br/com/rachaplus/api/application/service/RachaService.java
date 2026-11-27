package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.AcaoPartidaDTO;
import br.com.rachaplus.api.application.dto.AvaliacaoDTO;
import br.com.rachaplus.api.application.dto.ConviteRachaResponseDTO;
import br.com.rachaplus.api.application.dto.CreateRachaMemberDTO;
import br.com.rachaplus.api.application.dto.EntrarRachaDTO;
import br.com.rachaplus.api.application.dto.EquilibrarTimesRequestDTO;
import br.com.rachaplus.api.application.dto.EstadoJogoDTO;
import br.com.rachaplus.api.application.dto.EstadoRachaResponseDTO;
import br.com.rachaplus.api.application.dto.IniciarPartidaDTO;
import br.com.rachaplus.api.application.dto.PartidaResponseDTO;
import br.com.rachaplus.api.application.dto.RachaMemberResponseDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.application.dto.UpdatePlacarDTO;
import br.com.rachaplus.api.application.dto.UpdateRachaMemberDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.rachaplus.api.domain.*;
import br.com.rachaplus.api.domain.exception.AcessoNegadoException;
import br.com.rachaplus.api.domain.exception.RecursoNaoEncontradoException;
import br.com.rachaplus.api.domain.exception.RegraNegocioException;
import br.com.rachaplus.api.domain.repository.PartidaRepository;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import br.com.rachaplus.api.domain.repository.RachaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RachaService {

    private final RachaRepository rachaRepository;
    private final RachaMemberRepository rachaMemberRepository;
    private final PartidaRepository partidaRepository;
    private final AvaliacaoService avaliacaoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RachaService(RachaRepository rachaRepository,
                        RachaMemberRepository rachaMemberRepository,
                        PartidaRepository partidaRepository,
                        AvaliacaoService avaliacaoService) {
        this.rachaRepository = rachaRepository;
        this.rachaMemberRepository = rachaMemberRepository;
        this.partidaRepository = partidaRepository;
        this.avaliacaoService = avaliacaoService;
    }

    // ===================== RACHA =====================

    @Transactional
    public Racha create(String nome, String descricao, String esporte, Usuario owner) {
        return create(nome, descricao, esporte, null, null, owner);
    }

    @Transactional
    public Racha create(String nome, String descricao, String esporte, LocalDate data, LocalTime horario, Usuario owner) {
        final float defaultRating = 0.0f;

        var newRacha = new Racha();
        newRacha.setNome(nome);
        newRacha.setDescricao(descricao);
        newRacha.setEsporte(esporte);
        newRacha.setData(data);
        newRacha.setHorario(horario);
        var savedRacha = rachaRepository.saveAndFlush(newRacha);

        var membership = new RachaMember();
        membership.setRacha(savedRacha);
        membership.setUser(owner);
        membership.setNome(owner.getNomeUsuario());
        membership.setRole(RachaRole.ADMIN);
        membership.setRachaRating(defaultRating);
        membership.setAvatarJson(owner.getAvatarJson());

        rachaMemberRepository.save(membership);

        return savedRacha;
    }

    public long contarMembros(UUID rachaId) {
        return rachaMemberRepository.countByRachaId(rachaId);
    }

    public RachaResponseDTO buscarRacha(UUID rachaId, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirMembro(rachaId, requester);
        return new RachaResponseDTO(racha, rachaMemberRepository.countByRachaId(rachaId));
    }

    public ConviteRachaResponseDTO obterConviteRacha(UUID rachaId, Usuario requesterOptional) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        List<Avaliacao> avaliacoes = avaliacaoService.listarDoRacha(rachaId);
        List<RachaMember> entidades = rachaMemberRepository.findAllByRachaId(rachaId);
        List<RachaMemberResponseDTO> membros = montarListaRespostas(racha, entidades, avaliacoes, requesterOptional);

        RachaResponseDTO rachaDto = new RachaResponseDTO(racha, membros.size());
        return new ConviteRachaResponseDTO(rachaDto, membros);
    }

    @Transactional
    public RachaMemberResponseDTO entrarNoRacha(UUID rachaId, EntrarRachaDTO dto, Usuario requesterOptional) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirNaoFinalizado(racha);

        // Usuário logado: vincula à conta (fica salvo em "Minhas rachas" / histórico)
        if (requesterOptional != null) {
            var existente = rachaMemberRepository.findByRachaIdAndUserId(rachaId, requesterOptional.getId());
            if (existente.isPresent()) {
                RachaMember member = existente.get();
                Posicao posicaoAnterior = member.getPosicao();
                if (dto != null && dto.nome() != null && !dto.nome().isBlank()) {
                    member.setNome(dto.nome().trim());
                }
                if (dto != null && dto.posicao() != null && !dto.posicao().isBlank()) {
                    member.setPosicao(validarPosicao(dto.posicao(), racha));
                }
                if (dto != null && dto.avatarJson() != null && !dto.avatarJson().isBlank()) {
                    member.setAvatarJson(dto.avatarJson());
                }
                member = rachaMemberRepository.save(member);
                if (posicaoAnterior != member.getPosicao()) {
                    avaliacaoService.recalcularPorMudancaDePosicao(member);
                }
                return montarResposta(member, requesterOptional);
            }

            Posicao posicao = (dto != null && dto.posicao() != null && !dto.posicao().isBlank())
                    ? validarPosicao(dto.posicao(), racha)
                    : posicaoPadraoDoRacha(racha);
            String nomeExibicao = (dto != null && dto.nome() != null && !dto.nome().isBlank())
                    ? dto.nome().trim()
                    : requesterOptional.getNomeUsuario();
            String avatar = (dto != null && dto.avatarJson() != null && !dto.avatarJson().isBlank())
                    ? dto.avatarJson()
                    : requesterOptional.getAvatarJson();

            var membership = new RachaMember();
            membership.setRacha(racha);
            membership.setUser(requesterOptional);
            membership.setNome(nomeExibicao);
            membership.setPosicao(posicao);
            membership.setRole(RachaRole.PLAYER);
            membership.setRachaRating(0.0f);
            membership.setAvatarJson(avatar);
            membership = rachaMemberRepository.saveAndFlush(membership);
            return new RachaMemberResponseDTO(membership, null, 0);
        }

        // Convidado sem login: exige nome e entra como avulso (sem histórico vinculado a conta)
        String nomeConvidado = dto != null && dto.nome() != null ? dto.nome().trim() : "";
        if (nomeConvidado.isBlank()) {
            throw new RegraNegocioException("nome", "Informe seu nome para entrar no racha como convidado");
        }

        Posicao posicao = (dto != null && dto.posicao() != null && !dto.posicao().isBlank())
                ? validarPosicao(dto.posicao(), racha)
                : posicaoPadraoDoRacha(racha);

        var convidado = new RachaMember();
        convidado.setRacha(racha);
        convidado.setUser(null);
        convidado.setNome(nomeConvidado);
        convidado.setPosicao(posicao);
        convidado.setRole(RachaRole.PLAYER);
        convidado.setRachaRating(0.0f);
        if (dto != null && dto.avatarJson() != null && !dto.avatarJson().isBlank()) {
            convidado.setAvatarJson(dto.avatarJson());
        }
        convidado = rachaMemberRepository.saveAndFlush(convidado);
        return new RachaMemberResponseDTO(convidado, null, 0);
    }

    private Posicao posicaoPadraoDoRacha(Racha racha) {
        Esporte esporte = Esporte.deTexto(racha.getEsporte());
        if (esporte == null) return null;
        List<Posicao> posicoes = Posicao.doEsporte(esporte);
        return posicoes.isEmpty() ? null : posicoes.getFirst();
    }

    public List<RachaResponseDTO> listarRachasDoUsuario(Usuario user) {
        return rachaMemberRepository.findAllByUser(user)
                .stream()
                .map(RachaMember::getRacha)
                .map(racha -> new RachaResponseDTO(racha, rachaMemberRepository.countByRachaId(racha.getId())))
                .toList();
    }

    public List<RachaMember> listByUser(Usuario user) {
        return rachaMemberRepository.findAllByUser(user);
    }

    public List<Racha> listarRachasCriadosPor(Usuario user) {
        return rachaMemberRepository.findAllByUserAndRole(user, RachaRole.ADMIN)
                .stream()
                .map(RachaMember::getRacha)
                .toList();
    }

    // ===================== MEMBROS =====================

    public List<RachaMemberResponseDTO> listMembers(UUID rachaId, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirMembro(rachaId, requester);

        // Busca todas as avaliações do racha de uma vez e agrupa em memória
        List<Avaliacao> avaliacoes = avaliacaoService.listarDoRacha(rachaId);
        List<RachaMember> entidades = rachaMemberRepository.findAllByRachaId(rachaId);
        return montarListaRespostas(racha, entidades, avaliacoes, requester);
    }

    @Transactional
    public RachaMemberResponseDTO adicionarJogador(UUID rachaId, CreateRachaMemberDTO dto, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirAdmin(rachaId, requester);
        exigirNaoFinalizado(racha);

        // Valida tudo antes de salvar
        Posicao posicao = validarPosicao(dto.posicao(), racha);
        if (dto.notaInicial() != null) {
            AvaliacaoService.validarNota(dto.notaInicial(), "notaInicial");
        }

        // Jogador avulso (sem Usuario vinculado nesta sprint)
        RachaMember novoJogador = new RachaMember();
        novoJogador.setRacha(racha);
        novoJogador.setNome(dto.nome());
        novoJogador.setPosicao(posicao);
        novoJogador.setRole(RachaRole.PLAYER);
        novoJogador.setRachaRating(0.0f);
        novoJogador = rachaMemberRepository.saveAndFlush(novoJogador);

        // notaInicial vira a avaliação GERAL do Admin e o OVR passa a ser essa nota
        if (dto.notaInicial() != null) {
            avaliacaoService.avaliar(novoJogador, requester, dto.notaInicial());
            return new RachaMemberResponseDTO(novoJogador, dto.notaInicial(), TipoAvaliacao.GERAL, null, Map.of(), 1);
        }

        return new RachaMemberResponseDTO(novoJogador, null, null, null, Map.of(), 0);
    }

    @Transactional
    public RachaMemberResponseDTO atualizarJogador(UUID rachaId, UUID memberId, UpdateRachaMemberDTO dto, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirAdmin(rachaId, requester);
        exigirNaoFinalizado(racha);
        RachaMember member = buscarMembroDoRacha(rachaId, memberId);

        Posicao posicaoAnterior = member.getPosicao();
        Posicao novaPosicao = validarPosicao(dto.posicao(), racha);

        member.setNome(dto.nome());
        member.setPosicao(novaPosicao);
        member = rachaMemberRepository.save(member);

        if (posicaoAnterior != novaPosicao) {
            avaliacaoService.recalcularPorMudancaDePosicao(member);
        }

        return montarResposta(member, requester);
    }

    @Transactional
    public void removerJogador(UUID rachaId, UUID memberId, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirAdmin(rachaId, requester);
        exigirNaoFinalizado(racha);
        RachaMember member = buscarMembroDoRacha(rachaId, memberId);

        if (member.getRole() == RachaRole.ADMIN) {
            throw new RegraNegocioException("Não é possível remover o admin do racha");
        }

        // Remove as avaliações recebidas antes do jogador (FK avaliado_id e avaliacao_atributos)
        avaliacaoService.removerAvaliacoesDoMembro(member);
        rachaMemberRepository.delete(member);
    }

    // ===================== AVALIAÇÃO =====================

    @Transactional
    public RachaMemberResponseDTO avaliarJogador(UUID rachaId, UUID memberId, Float nota, Usuario requester) {
        return avaliarJogador(rachaId, memberId, new AvaliacaoDTO(TipoAvaliacao.GERAL, nota, null), requester);
    }

    @Transactional
    public RachaMemberResponseDTO avaliarJogador(UUID rachaId, UUID memberId, AvaliacaoDTO dto, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirMembro(rachaId, requester);
        exigirNaoFinalizado(racha);
        RachaMember avaliado = buscarMembroDoRacha(rachaId, memberId);

        avaliado = avaliacaoService.avaliar(avaliado, requester, dto);

        return montarResposta(avaliado, requester);
    }

    // ===================== SORTEIO, PARTIDA AO VIVO E FINALIZAÇÃO =====================

    public EstadoRachaResponseDTO obterEstadoRacha(UUID rachaId) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        EstadoJogoDTO estado = lerEstadoJogo(racha);
        List<PartidaResponseDTO> partidas = partidaRepository.findAllByRachaId(rachaId)
                .stream()
                .map(PartidaResponseDTO::new)
                .toList();
        String status = racha.getStatus() != null ? racha.getStatus() : "ABERTO";
        return new EstadoRachaResponseDTO(status, System.currentTimeMillis(), estado, partidas);
    }

    @Transactional
    public EstadoRachaResponseDTO iniciarPartida(UUID rachaId, IniciarPartidaDTO dto, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirAdmin(rachaId, requester);
        exigirNaoFinalizado(racha);

        EstadoJogoDTO estado = lerEstadoJogo(racha);
        if (estado == null || estado.times() == null || estado.times().size() < 2) {
            throw new RegraNegocioException("Sorteie os times antes de iniciar a partida");
        }

        int idA = dto.timeAId() != null ? dto.timeAId() : estado.times().get(0).id();
        int idB = dto.timeBId() != null ? dto.timeBId() : estado.times().get(1).id();
        if (idA == idB) {
            throw new RegraNegocioException("Selecione dois times diferentes para a partida");
        }

        EstadoJogoDTO.TimeSorteadoDTO timeA = estado.times().stream()
                .filter(t -> t.id() == idA)
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Time A selecionado não encontrado"));
        EstadoJogoDTO.TimeSorteadoDTO timeB = estado.times().stream()
                .filter(t -> t.id() == idB)
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Time B selecionado não encontrado"));

        Esporte esporte = Esporte.deTexto(racha.getEsporte());
        boolean ehVolei = esporte == Esporte.VOLEI;

        int duracao = dto.duracaoMinutos() != null && dto.duracaoMinutos() >= 1 ? dto.duracaoMinutos() : 10;
        int intervalo = dto.intervaloMinutos() != null && dto.intervaloMinutos() >= 0 ? dto.intervaloMinutos() : (ehVolei ? 0 : 2);
        boolean usarCronometro = dto.usarCronometro() != null ? dto.usarCronometro() : !ehVolei;
        int metaPontos = dto.metaPontos() != null && dto.metaPontos() >= 0
                ? dto.metaPontos()
                : (ehVolei ? 15 : 0);
        int melhorDeSets = dto.melhorDeSets() != null && dto.melhorDeSets() >= 1
                ? dto.melhorDeSets()
                : (ehVolei ? 3 : 0);
        long agora = System.currentTimeMillis();

        EstadoJogoDTO.PartidaAtivaDTO partida = new EstadoJogoDTO.PartidaAtivaDTO(
                true,
                "EM_JOGO",
                timeA.id(),
                timeA.nome(),
                0,
                timeB.id(),
                timeB.nome(),
                0,
                duracao,
                intervalo,
                duracao * 60L,
                usarCronometro,
                agora,
                metaPontos,
                melhorDeSets,
                0,
                0,
                usarCronometro
        );

        racha.setStatus("EM_ANDAMENTO");
        salvarEstadoJogo(racha, new EstadoJogoDTO(estado.formato(), estado.times(), estado.deFora(), partida));
        return obterEstadoRacha(rachaId);
    }

    @Transactional
    public EstadoRachaResponseDTO executarAcaoPartida(UUID rachaId, AcaoPartidaDTO dto, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirAdmin(rachaId, requester);
        exigirNaoFinalizado(racha);

        EstadoJogoDTO estado = lerEstadoJogo(racha);
        if (estado == null || estado.partidaAtual() == null || !estado.partidaAtual().ativa()) {
            throw new RegraNegocioException("Não há uma partida em andamento");
        }

        EstadoJogoDTO.PartidaAtivaDTO p = estado.partidaAtual();
        long agora = System.currentTimeMillis();
        long restantesAtuais = calcularRestantes(p, agora);
        boolean usaCron = p.usarCronometro() == null || p.usarCronometro();
        int setsA = p.setsA() != null ? p.setsA() : 0;
        int setsB = p.setsB() != null ? p.setsB() : 0;
        int melhorDe = p.melhorDeSets() != null ? p.melhorDeSets() : 0;
        String acao = dto.acao() != null ? dto.acao().trim().toUpperCase(Locale.ROOT) : "";

        EstadoJogoDTO.PartidaAtivaDTO novaPartida = switch (acao) {
            case "PLACAR" -> new EstadoJogoDTO.PartidaAtivaDTO(
                    true,
                    p.fase(),
                    p.timeAId(),
                    p.timeANome(),
                    Math.max(0, dto.placarA() != null ? dto.placarA() : p.placarA()),
                    p.timeBId(),
                    p.timeBNome(),
                    Math.max(0, dto.placarB() != null ? dto.placarB() : p.placarB()),
                    p.duracaoMinutos(),
                    p.intervaloMinutos(),
                    restantesAtuais,
                    p.rodando(),
                    agora,
                    p.metaPontos(),
                    p.melhorDeSets(),
                    setsA,
                    setsB,
                    usaCron
            );
            case "FECHAR_SET" -> {
                if (p.placarA() == p.placarB()) {
                    throw new RegraNegocioException("O set está empatado e não pode ser encerrado");
                }
                int novoSetsA = p.placarA() > p.placarB() ? setsA + 1 : setsA;
                int novoSetsB = p.placarB() > p.placarA() ? setsB + 1 : setsB;
                int setsNecessarios = melhorDe > 1 ? (melhorDe / 2) + 1 : 1;

                if (novoSetsA >= setsNecessarios || novoSetsB >= setsNecessarios) {
                    EstadoJogoDTO.PartidaAtivaDTO encerrada = new EstadoJogoDTO.PartidaAtivaDTO(
                            true, p.fase(), p.timeAId(), p.timeANome(), p.placarA(),
                            p.timeBId(), p.timeBNome(), p.placarB(), p.duracaoMinutos(),
                            p.intervaloMinutos(), restantesAtuais, false, agora,
                            p.metaPontos(), melhorDe, novoSetsA, novoSetsB, usaCron
                    );
                    registrarPartidaHistorico(racha, encerrada);
                    yield null;
                }

                yield new EstadoJogoDTO.PartidaAtivaDTO(
                        true,
                        "EM_JOGO",
                        p.timeAId(),
                        p.timeANome(),
                        0,
                        p.timeBId(),
                        p.timeBNome(),
                        0,
                        p.duracaoMinutos(),
                        p.intervaloMinutos(),
                        restantesAtuais,
                        usaCron && p.rodando(),
                        agora,
                        p.metaPontos(),
                        melhorDe,
                        novoSetsA,
                        novoSetsB,
                        usaCron
                );
            }
            case "PAUSAR" -> new EstadoJogoDTO.PartidaAtivaDTO(
                    true,
                    "PAUSADO",
                    p.timeAId(),
                    p.timeANome(),
                    p.placarA(),
                    p.timeBId(),
                    p.timeBNome(),
                    p.placarB(),
                    p.duracaoMinutos(),
                    p.intervaloMinutos(),
                    restantesAtuais,
                    false,
                    agora,
                    p.metaPontos(),
                    p.melhorDeSets(),
                    setsA,
                    setsB,
                    usaCron
            );
            case "RETOMAR" -> new EstadoJogoDTO.PartidaAtivaDTO(
                    true,
                    "EM_JOGO",
                    p.timeAId(),
                    p.timeANome(),
                    p.placarA(),
                    p.timeBId(),
                    p.timeBNome(),
                    p.placarB(),
                    p.duracaoMinutos(),
                    p.intervaloMinutos(),
                    restantesAtuais > 0 ? restantesAtuais : p.duracaoMinutos() * 60L,
                    usaCron,
                    agora,
                    p.metaPontos(),
                    p.melhorDeSets(),
                    setsA,
                    setsB,
                    usaCron
            );
            case "INTERVALO" -> new EstadoJogoDTO.PartidaAtivaDTO(
                    true,
                    "INTERVALO",
                    p.timeAId(),
                    p.timeANome(),
                    p.placarA(),
                    p.timeBId(),
                    p.timeBNome(),
                    p.placarB(),
                    p.duracaoMinutos(),
                    p.intervaloMinutos(),
                    Math.max(60L, p.intervaloMinutos() * 60L),
                    true,
                    agora,
                    p.metaPontos(),
                    p.melhorDeSets(),
                    setsA,
                    setsB,
                    usaCron
            );
            case "RETOMAR_DO_INTERVALO" -> new EstadoJogoDTO.PartidaAtivaDTO(
                    true,
                    "EM_JOGO",
                    p.timeAId(),
                    p.timeANome(),
                    p.placarA(),
                    p.timeBId(),
                    p.timeBNome(),
                    p.placarB(),
                    p.duracaoMinutos(),
                    p.intervaloMinutos(),
                    p.duracaoMinutos() * 60L,
                    usaCron,
                    agora,
                    p.metaPontos(),
                    p.melhorDeSets(),
                    setsA,
                    setsB,
                    usaCron
            );
            case "ENCERRAR_PARTIDA" -> {
                registrarPartidaHistorico(racha, p);
                yield null;
            }
            default -> throw new RegraNegocioException("acao", "Ação de partida inválida: " + acao);
        };

        salvarEstadoJogo(racha, new EstadoJogoDTO(estado.formato(), estado.times(), estado.deFora(), novaPartida));
        return obterEstadoRacha(rachaId);
    }

    @Transactional
    public EstadoRachaResponseDTO finalizarRacha(UUID rachaId, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirAdmin(rachaId, requester);
        exigirNaoFinalizado(racha);

        EstadoJogoDTO estado = lerEstadoJogo(racha);
        if (estado != null && estado.partidaAtual() != null && estado.partidaAtual().ativa()) {
            registrarPartidaHistorico(racha, estado.partidaAtual());
            estado = new EstadoJogoDTO(estado.formato(), estado.times(), estado.deFora(), null);
        }

        racha.setStatus("FINALIZADO");
        if (estado != null) {
            salvarEstadoJogo(racha, estado);
        } else {
            rachaRepository.save(racha);
        }
        return obterEstadoRacha(rachaId);
    }

    private void registrarPartidaHistorico(Racha racha, EstadoJogoDTO.PartidaAtivaDTO p) {
        Partida partida = new Partida();
        partida.setRacha(racha);
        partida.setTimeANome(p.timeANome());
        partida.setTimeBNome(p.timeBNome());

        Esporte esporte = Esporte.deTexto(racha.getEsporte());
        int setsA = p.setsA() != null ? p.setsA() : 0;
        int setsB = p.setsB() != null ? p.setsB() : 0;
        int melhorDe = p.melhorDeSets() != null ? p.melhorDeSets() : 0;

        if (esporte == Esporte.VOLEI && melhorDe > 1 && (setsA > 0 || setsB > 0)) {
            partida.setPlacarTimeA(setsA);
            partida.setPlacarTimeB(setsB);
            partida.setDetalhePlacar("Sets");
        } else if (esporte == Esporte.VOLEI) {
            partida.setPlacarTimeA(p.placarA());
            partida.setPlacarTimeB(p.placarB());
            partida.setDetalhePlacar("Pts (Set)");
        } else if (esporte == Esporte.BASQUETE) {
            partida.setPlacarTimeA(p.placarA());
            partida.setPlacarTimeB(p.placarB());
            partida.setDetalhePlacar("Pts");
        } else {
            partida.setPlacarTimeA(p.placarA());
            partida.setPlacarTimeB(p.placarB());
            partida.setDetalhePlacar("Gols");
        }

        partidaRepository.save(partida);
    }

    private long calcularRestantes(EstadoJogoDTO.PartidaAtivaDTO p, long agoraMs) {
        if (!p.rodando()) {
            return Math.max(0L, p.segundosRestantes());
        }
        long decorridoSeg = Math.max(0L, (agoraMs - p.atualizadoEmEpochMs()) / 1000L);
        return Math.max(0L, p.segundosRestantes() - decorridoSeg);
    }

    private EstadoJogoDTO.JogadorSorteadoDTO paraJogadorSorteado(RachaMember m) {
        String nome = m.getUser() != null ? m.getUser().getNomeUsuario() : m.getNome();
        String uid = m.getUser() != null && m.getUser().getId() != null ? m.getUser().getId().toString() : null;
        String pos = m.getPosicao() != null ? m.getPosicao().name() : "";
        float nota = m.getRachaRating() > 0 ? m.getRachaRating() : 4.0f;
        return new EstadoJogoDTO.JogadorSorteadoDTO(m.getId().toString(), uid, nome, pos, nota);
    }

    private EstadoJogoDTO lerEstadoJogo(Racha racha) {
        if (racha.getEstadoJogoJson() == null || racha.getEstadoJogoJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(racha.getEstadoJogoJson(), EstadoJogoDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void salvarEstadoJogo(Racha racha, EstadoJogoDTO estado) {
        try {
            racha.setEstadoJogoJson(objectMapper.writeValueAsString(estado));
            rachaRepository.save(racha);
        } catch (Exception e) {
            throw new RegraNegocioException("Não foi possível salvar o estado da partida");
        }
    }

    private void exigirNaoFinalizado(Racha racha) {
        if ("FINALIZADO".equalsIgnoreCase(racha.getStatus())) {
            throw new RegraNegocioException("Este racha já foi finalizado e não permite mais alterações");
        }
    }

    // ===================== PARTIDAS =====================

    public Partida criarPartida(UUID rachaId, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirMembro(rachaId, requester);

        Partida partida = new Partida();
        partida.setRacha(racha);
        return partidaRepository.save(partida);
    }

    public List<Partida> listarPartidas(UUID rachaId, Usuario requester) {
        exigirMembro(rachaId, requester);
        return partidaRepository.findAllByRachaId(rachaId);
    }

    public Partida atualizarPlacar(UUID rachaId, UUID partidaId, UpdatePlacarDTO dto, Usuario requester) {
        exigirMembro(rachaId, requester);

        Partida partida = partidaRepository.findById(partidaId)
                .filter(p -> p.getRacha().getId().equals(rachaId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Partida não encontrada neste racha"));

        partida.setPlacarTimeA(dto.placarTimeA());
        partida.setPlacarTimeB(dto.placarTimeB());
        return partidaRepository.save(partida);
    }

    // ===================== AUXILIARES =====================

    private Racha buscarRachaOuFalhar(UUID rachaId) {
        return rachaRepository.findById(rachaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Racha não encontrado"));
    }

    private RachaMember buscarMembroDoRacha(UUID rachaId, UUID memberId) {
        return rachaMemberRepository.findById(memberId)
                .filter(m -> m.getRacha().getId().equals(rachaId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Jogador não encontrado neste racha"));
    }

    private RachaMember exigirMembro(UUID rachaId, Usuario requester) {
        return rachaMemberRepository.findByRachaIdAndUserId(rachaId, requester.getId())
                .orElseThrow(() -> new AcessoNegadoException("Acesso negado: você não é membro deste racha"));
    }

    private RachaMember exigirAdmin(UUID rachaId, Usuario requester) {
        RachaMember membro = exigirMembro(rachaId, requester);
        if (membro.getRole() != RachaRole.ADMIN) {
            throw new AcessoNegadoException("Acesso negado: apenas o admin pode gerenciar jogadores");
        }
        return membro;
    }

    // A posição precisa existir e pertencer ao esporte do racha
    private Posicao validarPosicao(String valor, Racha racha) {
        Esporte esporte = Esporte.deTexto(racha.getEsporte());
        if (esporte == null) {
            throw new RegraNegocioException("posicao", "O racha não possui um esporte válido definido");
        }

        Posicao posicao;
        try {
            posicao = Posicao.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            posicao = null;
        }

        if (posicao == null || !posicao.pertenceA(esporte)) {
            throw new RegraNegocioException("posicao",
                    "Posição inválida para " + esporte + ". Posições válidas: " + Posicao.doEsporte(esporte));
        }
        return posicao;
    }

    private List<RachaMemberResponseDTO> montarListaRespostas(
            Racha racha,
            List<RachaMember> membros,
            List<Avaliacao> avaliacoesDoRacha,
            Usuario requesterOptional
    ) {
        Esporte esporte = Esporte.deTexto(racha.getEsporte());
        Map<UUID, List<Avaliacao>> porMembro = avaliacoesDoRacha.stream()
                .collect(Collectors.groupingBy(a -> a.getAvaliado().getId()));

        Map<UUID, Avaliacao> minhaPorMembro = requesterOptional == null
                ? Map.of()
                : avaliacoesDoRacha.stream()
                        .filter(a -> a.getAvaliador().getId().equals(requesterOptional.getId()))
                        .collect(Collectors.toMap(a -> a.getAvaliado().getId(), a -> a, (a1, a2) -> a2));

        return membros.stream()
                .map(member -> {
                    List<Avaliacao> avs = porMembro.getOrDefault(member.getId(), List.of());
                    Avaliacao minhaAv = minhaPorMembro.get(member.getId());
                    return new RachaMemberResponseDTO(
                            member,
                            minhaAv != null ? minhaAv.getNota() : null,
                            avaliacaoService.tipoDe(minhaAv),
                            avaliacaoService.atributosDe(minhaAv, esporte),
                            avaliacaoService.calcularMediaAtributos(member, avs),
                            avs.size()
                    );
                })
                .toList();
    }

    private RachaMemberResponseDTO montarResposta(RachaMember member, Usuario requester) {
        List<Avaliacao> avs = avaliacaoService.listarDoAvaliado(member.getId());
        Avaliacao minhaAv = requester == null
                ? null
                : avs.stream()
                        .filter(a -> a.getAvaliador().getId().equals(requester.getId()))
                        .findFirst()
                        .orElse(null);
        Esporte esporte = Esporte.deTexto(member.getRacha().getEsporte());

        return new RachaMemberResponseDTO(
                member,
                minhaAv != null ? minhaAv.getNota() : null,
                avaliacaoService.tipoDe(minhaAv),
                avaliacaoService.atributosDe(minhaAv, esporte),
                avaliacaoService.calcularMediaAtributos(member, avs),
                avs.size()
        );
    }
}
