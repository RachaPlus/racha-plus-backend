package br.com.rachaplus.api.domain.repository;

import br.com.rachaplus.api.domain.Partida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartidaRepository extends JpaRepository<Partida, UUID> {
    List<Partida> findAllByRachaId(UUID rachaId);
}