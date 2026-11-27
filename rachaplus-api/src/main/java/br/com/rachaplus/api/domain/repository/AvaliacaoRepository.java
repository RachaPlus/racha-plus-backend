package br.com.rachaplus.api.domain.repository;

import br.com.rachaplus.api.domain.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {

    Optional<Avaliacao> findByAvaliadorIdAndAvaliadoId(UUID avaliadorId, UUID avaliadoId);

    List<Avaliacao> findAllByRachaId(UUID rachaId);

    List<Avaliacao> findAllByAvaliadoId(UUID avaliadoId);

    long countByAvaliadoId(UUID avaliadoId);

    @Query("select avg(a.nota) from Avaliacao a where a.avaliado.id = :avaliadoId")
    Double mediaPorAvaliado(@Param("avaliadoId") UUID avaliadoId);

    @Modifying
    @Query("delete from Avaliacao a where a.avaliado.id = :avaliadoId")
    void deleteAllByAvaliadoId(@Param("avaliadoId") UUID avaliadoId);
}
