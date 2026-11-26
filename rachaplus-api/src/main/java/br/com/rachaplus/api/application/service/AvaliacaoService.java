package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.domain.Avaliacao;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.exception.RegraNegocioException;
import br.com.rachaplus.api.domain.repository.AvaliacaoRepository;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final RachaMemberRepository rachaMemberRepository;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, RachaMemberRepository rachaMemberRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.rachaMemberRepository = rachaMemberRepository;
    }

    // Nota válida: de 0.5 a 5.0, em passos de 0.5 (0.5, 1.0, 1.5 ... 5.0)
    public static void validarNota(Float nota, String campo) {
        if (nota == null || nota < 0.5f || nota > 5.0f || (nota * 2) % 1 != 0) {
            throw new RegraNegocioException(campo, "A nota deve estar entre 0.5 e 5.0, em passos de 0.5");
        }
    }

    // Upsert: cria o voto ou atualiza o voto anterior do mesmo avaliador, e recalcula o OVR
    @Transactional
    public RachaMember avaliar(RachaMember avaliado, Usuario avaliador, Float nota) {
        validarNota(nota, "nota");

        if (avaliado.getUser() != null && avaliado.getUser().getId().equals(avaliador.getId())) {
            throw new RegraNegocioException("nota", "Você não pode avaliar a si mesmo");
        }

        Avaliacao avaliacao = avaliacaoRepository
                .findByAvaliadorIdAndAvaliadoId(avaliador.getId(), avaliado.getId())
                .orElseGet(() -> {
                    Avaliacao nova = new Avaliacao();
                    nova.setRacha(avaliado.getRacha());
                    nova.setAvaliador(avaliador);
                    nova.setAvaliado(avaliado);
                    return nova;
                });

        avaliacao.setNota(nota);
        avaliacaoRepository.saveAndFlush(avaliacao);

        recalcularRating(avaliado);
        return avaliado;
    }

    public Float notaDe(Usuario avaliador, RachaMember avaliado) {
        return avaliacaoRepository.findByAvaliadorIdAndAvaliadoId(avaliador.getId(), avaliado.getId())
                .map(Avaliacao::getNota)
                .orElse(null);
    }

    public long totalDe(RachaMember avaliado) {
        return avaliacaoRepository.countByAvaliadoId(avaliado.getId());
    }

    public List<Avaliacao> listarDoRacha(UUID rachaId) {
        return avaliacaoRepository.findAllByRachaId(rachaId);
    }

    @Transactional
    public void removerAvaliacoesDoMembro(RachaMember membro) {
        avaliacaoRepository.deleteAllByAvaliadoId(membro.getId());
    }

    // OVR = média aritmética de todas as notas recebidas, arredondada para 1 casa decimal
    private void recalcularRating(RachaMember membro) {
        Double media = avaliacaoRepository.mediaPorAvaliado(membro.getId());
        float ovr = media == null ? 0f : Math.round(media * 10) / 10f;
        membro.setRachaRating(ovr);
        rachaMemberRepository.save(membro);
    }
}
