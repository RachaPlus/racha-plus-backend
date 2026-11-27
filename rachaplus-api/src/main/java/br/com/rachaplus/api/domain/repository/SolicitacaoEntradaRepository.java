package br.com.rachaplus.api.domain.repository;

import br.com.rachaplus.api.domain.SolicitacaoEntrada;
import br.com.rachaplus.api.domain.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SolicitacaoEntradaRepository extends JpaRepository<SolicitacaoEntrada, UUID> {

    List<SolicitacaoEntrada> findAllByRachaIdAndStatusOrderByCriadoEmDesc(UUID rachaId, StatusSolicitacao status);

    boolean existsByRachaIdAndSolicitanteIdAndStatus(UUID rachaId, UUID solicitanteId, StatusSolicitacao status);
}
