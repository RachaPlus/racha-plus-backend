package br.com.rachaplus.api.domain.repository;

import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RachaMemberRepository extends JpaRepository<RachaMember, UUID> {
    List<RachaMember> findAllByUser(Usuario user);

    List<RachaMember> findAllByRachaId(UUID rachaId);

    boolean existsByRachaIdAndUserId(UUID rachaId, UUID userId);
}
