package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.CreateRachaMemberDTO;
import br.com.rachaplus.api.domain.Racha;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.RachaRole;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import br.com.rachaplus.api.domain.repository.RachaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RachaService {

    private final RachaRepository rachaRepository;
    private final RachaMemberRepository rachaMemberRepository;

    public RachaService(RachaRepository rachaRepository, RachaMemberRepository rachaMemberRepository) {
        this.rachaRepository = rachaRepository;
        this.rachaMemberRepository = rachaMemberRepository;
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


    public List<Racha> listarRachasCriadosPor(Usuario user) {
        return rachaMemberRepository.findAllByUserAndRole(user, RachaRole.ADMIN)
                .stream()
                .map(RachaMember::getRacha)
                .toList();
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
}
