package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.ConviteDetalhesResponseDTO;
import br.com.rachaplus.api.application.dto.GerarConviteResponseDTO;
import br.com.rachaplus.api.application.dto.SolicitacaoEntradaResponseDTO;
import br.com.rachaplus.api.application.dto.SolicitarEntradaDTO;
import br.com.rachaplus.api.domain.*;
import br.com.rachaplus.api.domain.exception.AcessoNegadoException;
import br.com.rachaplus.api.domain.exception.RecursoNaoEncontradoException;
import br.com.rachaplus.api.domain.exception.RegraNegocioException;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import br.com.rachaplus.api.domain.repository.RachaRepository;
import br.com.rachaplus.api.domain.repository.SolicitacaoEntradaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ConviteService {

    private static final String ALFABETO = "abcdefghijkmnpqrstuvwxyz23456789";
    private final SecureRandom random = new SecureRandom();

    private final RachaRepository rachaRepository;
    private final RachaMemberRepository rachaMemberRepository;
    private final SolicitacaoEntradaRepository solicitacaoRepository;

    public ConviteService(RachaRepository rachaRepository,
                          RachaMemberRepository rachaMemberRepository,
                          SolicitacaoEntradaRepository solicitacaoRepository) {
        this.rachaRepository = rachaRepository;
        this.rachaMemberRepository = rachaMemberRepository;
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Transactional
    public GerarConviteResponseDTO gerarConvite(UUID rachaId, Usuario requester) {
        Racha racha = buscarRacha(rachaId);
        exigirAdmin(rachaId, requester);

        if (racha.getCodigoConvite() == null || racha.getCodigoConvite().isBlank()) {
            racha.setCodigoConvite(gerarCodigoUnico());
            racha = rachaRepository.save(racha);
        }

        return new GerarConviteResponseDTO(
                racha.getCodigoConvite(),
                racha.getId(),
                racha.getNome(),
                racha.getEsporte()
        );
    }

    public ConviteDetalhesResponseDTO obterConvite(String codigo, Usuario requester) {
        Racha racha = buscarRachaPorCodigo(codigo);
        boolean jaEMembro = requester != null &&
                rachaMemberRepository.existsByRachaIdAndUserId(racha.getId(), requester.getId());
        boolean pendente = requester != null &&
                solicitacaoRepository.existsByRachaIdAndSolicitanteIdAndStatus(
                        racha.getId(), requester.getId(), StatusSolicitacao.PENDENTE);

        List<ConviteDetalhesResponseDTO.PerfilDisponivelDTO> perfisDisponiveis = rachaMemberRepository
                .findAllByRachaId(racha.getId())
                .stream()
                .filter(m -> m.getUser() == null)
                .map(m -> new ConviteDetalhesResponseDTO.PerfilDisponivelDTO(
                        m.getId(),
                        m.getNome(),
                        m.getPosicao(),
                        m.getRachaRating()
                ))
                .toList();

        return new ConviteDetalhesResponseDTO(
                racha.getId(),
                racha.getNome(),
                racha.getEsporte(),
                jaEMembro,
                pendente,
                perfisDisponiveis
        );
    }

    @Transactional
    public SolicitacaoEntradaResponseDTO solicitarEntrada(String codigo, SolicitarEntradaDTO dto, Usuario requester) {
        Racha racha = buscarRachaPorCodigo(codigo);
        if ("FINALIZADO".equalsIgnoreCase(racha.getStatus())) {
            throw new RegraNegocioException("Este racha já foi finalizado e não aceita novas entradas");
        }
        if (rachaMemberRepository.existsByRachaIdAndUserId(racha.getId(), requester.getId())) {
            throw new RegraNegocioException("Você já é membro deste racha");
        }
        if (solicitacaoRepository.existsByRachaIdAndSolicitanteIdAndStatus(
                racha.getId(), requester.getId(), StatusSolicitacao.PENDENTE)) {
            throw new RegraNegocioException("Você já possui uma solicitação pendente para este racha");
        }
        if (dto == null || dto.tipo() == null) {
            throw new RegraNegocioException("tipo", "Informe o tipo de solicitação (REIVINDICAR ou NOVO_PERFIL)");
        }

        SolicitacaoEntrada sol = new SolicitacaoEntrada();
        sol.setRacha(racha);
        sol.setSolicitante(requester);
        sol.setTipo(dto.tipo());
        sol.setStatus(StatusSolicitacao.PENDENTE);

        if (dto.tipo() == TipoSolicitacao.REIVINDICAR) {
            if (dto.memberId() == null) {
                throw new RegraNegocioException("memberId", "Selecione o perfil virtual que deseja reivindicar");
            }
            RachaMember alvo = rachaMemberRepository.findById(dto.memberId())
                    .filter(m -> m.getRacha().getId().equals(racha.getId()))
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil virtual não encontrado neste racha"));
            if (alvo.getUser() != null) {
                throw new RegraNegocioException("memberId", "Este perfil já está vinculado a outro usuário");
            }
            sol.setMemberAlvo(alvo);
        } else {
            String nome = dto.nome() != null ? dto.nome().trim() : "";
            if (nome.isBlank()) {
                throw new RegraNegocioException("nome", "Informe seu nome para entrar no racha");
            }
            Posicao pos = validarPosicao(dto.posicao(), racha);
            sol.setNomeNovoPerfil(nome);
            sol.setPosicaoNovoPerfil(pos);
        }

        sol = solicitacaoRepository.saveAndFlush(sol);
        return new SolicitacaoEntradaResponseDTO(sol);
    }

    public List<SolicitacaoEntradaResponseDTO> listarPendentes(UUID rachaId, Usuario requester) {
        exigirAdmin(rachaId, requester);
        return solicitacaoRepository
                .findAllByRachaIdAndStatusOrderByCriadoEmDesc(rachaId, StatusSolicitacao.PENDENTE)
                .stream()
                .map(SolicitacaoEntradaResponseDTO::new)
                .toList();
    }

    @Transactional
    public void aprovarSolicitacao(UUID rachaId, UUID solicitacaoId, Usuario requester) {
        exigirAdmin(rachaId, requester);
        SolicitacaoEntrada sol = solicitacaoRepository.findById(solicitacaoId)
                .filter(s -> s.getRacha().getId().equals(rachaId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));

        if (sol.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new RegraNegocioException("Esta solicitação já foi processada");
        }

        Usuario solicitante = sol.getSolicitante();
        if (rachaMemberRepository.existsByRachaIdAndUserId(rachaId, solicitante.getId())) {
            throw new RegraNegocioException("O usuário solicitante já é membro deste racha");
        }

        if (sol.getTipo() == TipoSolicitacao.REIVINDICAR) {
            RachaMember alvo = sol.getMemberAlvo();
            if (alvo == null || alvo.getUser() != null) {
                throw new RegraNegocioException("O perfil alvo não está mais disponível para reivindicação");
            }
            alvo.setUser(solicitante);
            if (alvo.getAvatarJson() == null && solicitante.getAvatarJson() != null) {
                alvo.setAvatarJson(solicitante.getAvatarJson());
            }
            rachaMemberRepository.save(alvo);
        } else {
            RachaMember novo = new RachaMember();
            novo.setRacha(sol.getRacha());
            novo.setUser(solicitante);
            novo.setNome(sol.getNomeNovoPerfil() != null ? sol.getNomeNovoPerfil() : solicitante.getNomeUsuario());
            novo.setPosicao(sol.getPosicaoNovoPerfil());
            novo.setRole(RachaRole.PLAYER);
            novo.setRachaRating(0.0f);
            novo.setAvatarJson(solicitante.getAvatarJson());
            rachaMemberRepository.save(novo);
        }

        sol.setStatus(StatusSolicitacao.APROVADA);
        solicitacaoRepository.save(sol);
    }

    @Transactional
    public void recusarSolicitacao(UUID rachaId, UUID solicitacaoId, Usuario requester) {
        exigirAdmin(rachaId, requester);
        SolicitacaoEntrada sol = solicitacaoRepository.findById(solicitacaoId)
                .filter(s -> s.getRacha().getId().equals(rachaId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));

        if (sol.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new RegraNegocioException("Esta solicitação já foi processada");
        }

        sol.setStatus(StatusSolicitacao.RECUSADA);
        solicitacaoRepository.save(sol);
    }

    private String gerarCodigoUnico() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    private Racha buscarRacha(UUID rachaId) {
        return rachaRepository.findById(rachaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Racha não encontrado"));
    }

    private Racha buscarRachaPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new RecursoNaoEncontradoException("Convite inválido ou não encontrado");
        }
        return rachaRepository.findByCodigoConvite(codigo.trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite inválido ou não encontrado"));
    }

    private void exigirAdmin(UUID rachaId, Usuario requester) {
        RachaMember membro = rachaMemberRepository.findByRachaIdAndUserId(rachaId, requester.getId())
                .orElseThrow(() -> new AcessoNegadoException("Acesso negado: você não é membro deste racha"));
        if (membro.getRole() != RachaRole.ADMIN) {
            throw new AcessoNegadoException("Acesso negado: apenas o admin pode gerenciar convites e solicitações");
        }
    }

    private Posicao validarPosicao(String valor, Racha racha) {
        Esporte esporte = Esporte.deTexto(racha.getEsporte());
        if (esporte == null) {
            throw new RegraNegocioException("posicao", "O racha não possui um esporte válido definido");
        }
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException("posicao", "Informe sua posição no racha");
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
}
