package br.com.rachaplus.api.application.service;

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

    public List<RachaMember> listMembers(UUID rachaId, Usuario requester) {
        boolean isMember = rachaMemberRepository.existsByRachaIdAndUserId(rachaId, requester.getId());

        if (!isMember) {
            throw new RuntimeException("Acesso negado: você não é membro deste racha.");
        }

        return rachaMemberRepository.findAllByRachaId(rachaId);
    }
}
