package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.AvaliacaoDTO;
import br.com.rachaplus.api.domain.AtributoEsporte;
import br.com.rachaplus.api.domain.Avaliacao;
import br.com.rachaplus.api.domain.Esporte;
import br.com.rachaplus.api.domain.Posicao;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.TipoAvaliacao;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.exception.RegraNegocioException;
import br.com.rachaplus.api.domain.repository.AvaliacaoRepository;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    // Sobrecarga retrocompatível para avaliação GERAL direta
    @Transactional
    public RachaMember avaliar(RachaMember avaliado, Usuario avaliador, Float nota) {
        return avaliar(avaliado, avaliador, new AvaliacaoDTO(TipoAvaliacao.GERAL, nota, null));
    }

    // Upsert v2: suporta avaliação GERAL e AVANCADA por atributos do esporte e arquétipo de posição
    @Transactional
    public RachaMember avaliar(RachaMember avaliado, Usuario avaliador, AvaliacaoDTO dto) {
        if (dto == null) {
            throw new RegraNegocioException("nota", "Os dados da avaliação são obrigatórios");
        }

        TipoAvaliacao tipoEfetivo = resolverTipo(dto);

        if (tipoEfetivo == TipoAvaliacao.GERAL) {
            validarNota(dto.nota(), "nota");
        } else {
            validarAvaliacaoAvancada(avaliado, dto.atributos());
        }

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

        if (avaliacao.getAtributos() == null) {
            avaliacao.setAtributos(new EnumMap<>(AtributoEsporte.class));
        }

        if (tipoEfetivo == TipoAvaliacao.GERAL) {
            avaliacao.setTipo(TipoAvaliacao.GERAL);
            avaliacao.getAtributos().clear();
            avaliacao.setNota(dto.nota());
        } else {
            float notaPonderada = avaliado.getPosicao().calcularNotaPonderada(dto.atributos());
            avaliacao.setTipo(TipoAvaliacao.AVANCADA);
            avaliacao.getAtributos().clear();
            avaliacao.getAtributos().putAll(dto.atributos());
            avaliacao.setNota(notaPonderada);
        }

        avaliacaoRepository.saveAndFlush(avaliacao);
        recalcularRating(avaliado);
        return avaliado;
    }

    private TipoAvaliacao resolverTipo(AvaliacaoDTO dto) {
        if (dto.tipo() != null) {
            return dto.tipo();
        }
        if (dto.atributos() != null && !dto.atributos().isEmpty() && dto.nota() == null) {
            return TipoAvaliacao.AVANCADA;
        }
        return TipoAvaliacao.GERAL;
    }

    private void validarAvaliacaoAvancada(RachaMember avaliado, Map<AtributoEsporte, Float> atributos) {
        Posicao posicao = avaliado.getPosicao();
        if (posicao == null) {
            throw new RegraNegocioException("posicao",
                    "O jogador avaliado precisa possuir uma posição definida para receber avaliação avançada");
        }

        Esporte esporte = Esporte.deTexto(avaliado.getRacha().getEsporte());
        if (esporte == null) {
            throw new RegraNegocioException("esporte", "O racha não possui um esporte válido definido");
        }

        if (atributos == null || atributos.isEmpty()) {
            throw new RegraNegocioException("atributos", "Informe as notas dos atributos para a avaliação avançada");
        }

        for (var entry : atributos.entrySet()) {
            AtributoEsporte attr = entry.getKey();
            if (attr == null || !attr.pertenceA(esporte)) {
                throw new RegraNegocioException("atributos",
                        "Atributo inválido para o esporte " + esporte + ": " + attr);
            }
            validarNota(entry.getValue(), attr.name());
        }

        List<AtributoEsporte> esperados = AtributoEsporte.doEsporte(esporte);
        for (AtributoEsporte esperado : esperados) {
            boolean opcionalPesoZero = posicao == Posicao.LIBERO && posicao.pesoDe(esperado) == 0f;
            if (!atributos.containsKey(esperado) && !opcionalPesoZero) {
                throw new RegraNegocioException("atributos",
                        "Atributo obrigatório ausente para " + esporte + ": " + esperado);
            }
        }
    }

    // Recalcula automaticamente todas as avaliações AVANCADA do membro quando sua posição muda
    @Transactional
    public void recalcularPorMudancaDePosicao(RachaMember membro) {
        if (membro.getPosicao() == null) {
            return;
        }

        List<Avaliacao> avaliacoes = avaliacaoRepository.findAllByAvaliadoId(membro.getId());
        if (avaliacoes.isEmpty()) {
            return;
        }

        boolean alterouAlguma = false;
        for (Avaliacao avaliacao : avaliacoes) {
            if (avaliacao.getTipo() == TipoAvaliacao.AVANCADA
                    && avaliacao.getAtributos() != null
                    && !avaliacao.getAtributos().isEmpty()) {
                float novaNota = membro.getPosicao().calcularNotaPonderada(avaliacao.getAtributos());
                avaliacao.setNota(novaNota);
                avaliacaoRepository.saveAndFlush(avaliacao);
                alterouAlguma = true;
            }
        }

        if (alterouAlguma) {
            recalcularRating(membro);
        }
    }

    public Optional<Avaliacao> avaliacaoDe(Usuario avaliador, RachaMember avaliado) {
        if (avaliador == null) {
            return Optional.empty();
        }
        return avaliacaoRepository.findByAvaliadorIdAndAvaliadoId(avaliador.getId(), avaliado.getId());
    }

    public Float notaDe(Usuario avaliador, RachaMember avaliado) {
        return avaliacaoDe(avaliador, avaliado)
                .map(Avaliacao::getNota)
                .orElse(null);
    }

    public TipoAvaliacao tipoDe(Avaliacao avaliacao) {
        if (avaliacao == null) {
            return null;
        }
        return avaliacao.getTipo() != null ? avaliacao.getTipo() : TipoAvaliacao.GERAL;
    }

    public Map<AtributoEsporte, Float> atributosDe(Avaliacao avaliacao, Esporte esporte) {
        if (avaliacao == null || tipoDe(avaliacao) != TipoAvaliacao.AVANCADA) {
            return null;
        }
        if (avaliacao.getAtributos() == null || avaliacao.getAtributos().isEmpty()) {
            return null;
        }
        Map<AtributoEsporte, Float> ordenado = new LinkedHashMap<>();
        for (AtributoEsporte attr : AtributoEsporte.doEsporte(esporte)) {
            if (avaliacao.getAtributos().containsKey(attr)) {
                ordenado.put(attr, avaliacao.getAtributos().get(attr));
            }
        }
        for (var entry : avaliacao.getAtributos().entrySet()) {
            ordenado.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(ordenado);
    }

    public Map<AtributoEsporte, Float> calcularMediaAtributos(RachaMember avaliado, List<Avaliacao> avaliacoesDoMembro) {
        if (avaliacoesDoMembro == null || avaliacoesDoMembro.isEmpty()) {
            return Map.of();
        }

        List<Avaliacao> avancadas = avaliacoesDoMembro.stream()
                .filter(a -> a.getTipo() == TipoAvaliacao.AVANCADA
                        && a.getAtributos() != null
                        && !a.getAtributos().isEmpty())
                .toList();

        if (avancadas.isEmpty()) {
            return Map.of();
        }

        Esporte esporte = Esporte.deTexto(avaliado.getRacha().getEsporte());
        List<AtributoEsporte> ordem = esporte != null
                ? AtributoEsporte.doEsporte(esporte)
                : List.of(AtributoEsporte.values());

        Map<AtributoEsporte, Float> medias = new LinkedHashMap<>();
        for (AtributoEsporte attr : ordem) {
            double soma = 0.0;
            int qtd = 0;
            for (Avaliacao av : avancadas) {
                Float valor = av.getAtributos().get(attr);
                if (valor != null) {
                    soma += valor.doubleValue();
                    qtd++;
                }
            }
            if (qtd > 0) {
                float mediaArredondada = Math.round((soma / qtd) * 10.0) / 10.0f;
                medias.put(attr, mediaArredondada);
            }
        }
        return Collections.unmodifiableMap(medias);
    }

    public List<Avaliacao> listarDoAvaliado(UUID avaliadoId) {
        return avaliacaoRepository.findAllByAvaliadoId(avaliadoId);
    }

    public long totalDe(RachaMember avaliado) {
        return avaliacaoRepository.countByAvaliadoId(avaliado.getId());
    }

    public List<Avaliacao> listarDoRacha(UUID rachaId) {
        return avaliacaoRepository.findAllByRachaId(rachaId);
    }

    @Transactional
    public void removerAvaliacoesDoMembro(RachaMember membro) {
        // Carrega as entidades antes de deletar para disparar remoção dos filhos em avaliacao_atributos (@ElementCollection)
        List<Avaliacao> avaliacoes = avaliacaoRepository.findAllByAvaliadoId(membro.getId());
        if (!avaliacoes.isEmpty()) {
            avaliacaoRepository.deleteAll(avaliacoes);
        }
    }

    // OVR = média aritmética de todas as notas recebidas (GERAL + AVANCADA), arredondada para 1 casa decimal
    private void recalcularRating(RachaMember membro) {
        Double media = avaliacaoRepository.mediaPorAvaliado(membro.getId());
        float ovr = media == null ? 0f : Math.round(media * 10) / 10f;
        membro.setRachaRating(ovr);
        rachaMemberRepository.save(membro);
    }
}
