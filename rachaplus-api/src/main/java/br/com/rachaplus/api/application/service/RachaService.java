package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.CreateRachaMemberDTO;
import br.com.rachaplus.api.application.dto.RachaMemberResponseDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.application.dto.UpdatePlacarDTO;
import br.com.rachaplus.api.application.dto.UpdateRachaMemberDTO;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RachaService {

    private final RachaRepository rachaRepository;
    private final RachaMemberRepository rachaMemberRepository;
    private final PartidaRepository partidaRepository;
    private final AvaliacaoService avaliacaoService;

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
        exigirMembro(rachaId, requester);

        // Busca todas as avaliações do racha de uma vez e agrupa em memória
        List<Avaliacao> avaliacoes = avaliacaoService.listarDoRacha(rachaId);

        Map<UUID, Long> totalPorMembro = avaliacoes.stream()
                .collect(Collectors.groupingBy(a -> a.getAvaliado().getId(), Collectors.counting()));

        Map<UUID, Float> minhasNotas = avaliacoes.stream()
                .filter(a -> a.getAvaliador().getId().equals(requester.getId()))
                .collect(Collectors.toMap(a -> a.getAvaliado().getId(), Avaliacao::getNota));

        return rachaMemberRepository.findAllByRachaId(rachaId)
                .stream()
                .map(member -> new RachaMemberResponseDTO(
                        member,
                        minhasNotas.get(member.getId()),
                        totalPorMembro.getOrDefault(member.getId(), 0L)))
                .toList();
    }

    @Transactional
    public RachaMemberResponseDTO adicionarJogador(UUID rachaId, CreateRachaMemberDTO dto, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirAdmin(rachaId, requester);

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

        // notaInicial vira a avaliação do Admin e o OVR passa a ser essa nota
        if (dto.notaInicial() != null) {
            avaliacaoService.avaliar(novoJogador, requester, dto.notaInicial());
            return new RachaMemberResponseDTO(novoJogador, dto.notaInicial(), 1);
        }

        return new RachaMemberResponseDTO(novoJogador, null, 0);
    }

    @Transactional
    public RachaMemberResponseDTO atualizarJogador(UUID rachaId, UUID memberId, UpdateRachaMemberDTO dto, Usuario requester) {
        Racha racha = buscarRachaOuFalhar(rachaId);
        exigirAdmin(rachaId, requester);
        RachaMember member = buscarMembroDoRacha(rachaId, memberId);

        member.setNome(dto.nome());
        member.setPosicao(validarPosicao(dto.posicao(), racha));
        member = rachaMemberRepository.save(member);

        return montarResposta(member, requester);
    }

    @Transactional
    public void removerJogador(UUID rachaId, UUID memberId, Usuario requester) {
        buscarRachaOuFalhar(rachaId);
        exigirAdmin(rachaId, requester);
        RachaMember member = buscarMembroDoRacha(rachaId, memberId);

        if (member.getRole() == RachaRole.ADMIN) {
            throw new RegraNegocioException("Não é possível remover o admin do racha");
        }

        // Remove as avaliações recebidas antes do jogador (FK avaliado_id)
        avaliacaoService.removerAvaliacoesDoMembro(member);
        rachaMemberRepository.delete(member);
    }

    // ===================== AVALIAÇÃO =====================

    @Transactional
    public RachaMemberResponseDTO avaliarJogador(UUID rachaId, UUID memberId, Float nota, Usuario requester) {
        buscarRachaOuFalhar(rachaId);
        exigirMembro(rachaId, requester);
        RachaMember avaliado = buscarMembroDoRacha(rachaId, memberId);

        avaliado = avaliacaoService.avaliar(avaliado, requester, nota);

        return montarResposta(avaliado, requester);
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

    private RachaMemberResponseDTO montarResposta(RachaMember member, Usuario requester) {
        return new RachaMemberResponseDTO(
                member,
                avaliacaoService.notaDe(requester, member),
                avaliacaoService.totalDe(member));
    }
}
