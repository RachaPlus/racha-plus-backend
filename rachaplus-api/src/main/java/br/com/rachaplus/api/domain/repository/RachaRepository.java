package br.com.rachaplus.api.domain.repository;

import br.com.rachaplus.api.domain.Racha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RachaRepository extends JpaRepository<Racha, UUID> {
}
