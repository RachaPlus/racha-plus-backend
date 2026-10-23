package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.CreateRachaMemberDTO;
import br.com.rachaplus.api.application.dto.UpdatePlacarDTO;
import br.com.rachaplus.api.application.dto.UpdateRachaMemberDTO;
import br.com.rachaplus.api.domain.*;
import br.com.rachaplus.api.domain.repository.PartidaRepository;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import br.com.rachaplus.api.domain.repository.RachaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RachaService {

    private final RachaRepository rachaRepository;
    private final RachaMemberRepository rachaMemberRepository;

    // adiciona no construtor também:
    public RachaService(RachaRepository rachaRepository, RachaMemberRepository rachaMemberRepository, PartidaRepository partidaRepository) {
        this.rachaRepository = rachaRepository;
        this.rachaMemberRepository = rachaMemberRepository;
        this.partidaRepository = partidaRepository;
    }

    public Racha create(String name, String description, Usuario owner) {
        final float defaultRating = 0.0f;

        var newRacha = new Racha();
        newRacha.setName(name);
        newRacha.setDescription(description);
        var savedRacha = rachaRepository.save(newRacha);

        var membership = new RachaMember();
        membership.setRacha(savedRacha);
        membership.setUser(owner);
        membership.setRole(RachaRole.ADMIN);
        membership.setRachaRating(defaultRating);

        rachaMemberRepository.save(membership);

        return savedRacha;
    }

    public List<RachaMember> listByUser(Usuario user) {
        return rachaMemberRepository.findAllByUser(user);
    }

    public List<RachaMember> listMembers(UUID rachaId, Usuario requester) {
        boolean isMember = rachaMemberRepository.existsByRachaIdAndUserId(rachaId, requester.getId());

        if (!isMember) {
            throw new RuntimeException("Acesso negado: você não é membro deste racha.");
        }

        return rachaMemberRepository.findAllByRachaId(rachaId);
    }

    public RachaMember adicionarJogador(UUID rachaId, CreateRachaMemberDTO dto) {
        // 1. Busca o racha pelo UUID
        Racha racha = rachaRepository.findById(rachaId)
                .orElseThrow(() -> new RuntimeException("Racha não encontrado"));

        // 2. Instancia o novo jogador (sem vincular a um Usuario do sistema)
        RachaMember novoJogador = new RachaMember();
        novoJogador.setNome(dto.nome()); // Use dto.getNome() se não estiver usando 'record'
        novoJogador.setRacha(racha);

        // Mantendo o padrão de nota inicial do seu método create
        novoJogador.setRachaRating(0.0f);

        // Defina o role padrão para um jogador comum (ajuste se o nome do seu enum for diferente)
        // novoJogador.setRole(RachaRole.PLAYER);

        // 3. Salva e retorna a entidade
        return rachaMemberRepository.save(novoJogador);
    }

    private final PartidaRepository partidaRepository;

    public RachaMember atualizarJogador(UUID rachaId, UUID memberId, UpdateRachaMemberDTO dto) {
        RachaMember member = rachaMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        if (!member.getRacha().getId().equals(rachaId)) {
            throw new RuntimeException("Jogador não pertence a este racha");
        }

        member.setPosicao(dto.posicao());
        return rachaMemberRepository.save(member);
    }

    public void removerJogador(UUID rachaId, UUID memberId) {
        RachaMember member = rachaMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Jogador não encontrado"));

        if (!member.getRacha().getId().equals(rachaId)) {
            throw new RuntimeException("Jogador não pertence a este racha");
        }

        if (member.getRole() == RachaRole.ADMIN) {
            throw new RuntimeException("Não é possível remover o admin do racha");
        }

        rachaMemberRepository.delete(member);
    }

    public Partida criarPartida(UUID rachaId, Usuario requester) {
        boolean isMember = rachaMemberRepository.existsByRachaIdAndUserId(rachaId, requester.getId());
        if (!isMember) {
            throw new RuntimeException("Acesso negado: você não é membro deste racha");
        }

        Racha racha = rachaRepository.findById(rachaId)
                .orElseThrow(() -> new RuntimeException("Racha não encontrado"));

        Partida partida = new Partida();
        partida.setRacha(racha);
        return partidaRepository.save(partida);
    }

    public List<Partida> listarPartidas(UUID rachaId, Usuario requester) {
        boolean isMember = rachaMemberRepository.existsByRachaIdAndUserId(rachaId, requester.getId());
        if (!isMember) {
            throw new RuntimeException("Acesso negado: você não é membro deste racha");
        }
        return partidaRepository.findAllByRachaId(rachaId);
    }

    public Partida atualizarPlacar(UUID rachaId, UUID partidaId, UpdatePlacarDTO dto, Usuario requester) {
        boolean isMember = rachaMemberRepository.existsByRachaIdAndUserId(rachaId, requester.getId());
        if (!isMember) {
            throw new RuntimeException("Acesso negado: você não é membro deste racha");
        }

        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new RuntimeException("Partida não encontrada"));

        if (!partida.getRacha().getId().equals(rachaId)) {
            throw new RuntimeException("Partida não pertence a este racha");
        }

        partida.setPlacarTimeA(dto.placarTimeA());
        partida.setPlacarTimeB(dto.placarTimeB());
        return partidaRepository.save(partida);
    }
}
