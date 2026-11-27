package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.EquilibrarTimesRequestDTO;
import br.com.rachaplus.api.application.dto.EquilibrarTimesResponseDTO;
import br.com.rachaplus.api.application.dto.EstadoJogoDTO;
import br.com.rachaplus.api.application.dto.TimeGeradoDTO;
import br.com.rachaplus.api.domain.Esporte;
import br.com.rachaplus.api.domain.Posicao;
import br.com.rachaplus.api.domain.Racha;
import br.com.rachaplus.api.domain.RachaMember;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.exception.AcessoNegadoException;
import br.com.rachaplus.api.domain.exception.RecursoNaoEncontradoException;
import br.com.rachaplus.api.domain.exception.RegraNegocioException;
import br.com.rachaplus.api.domain.repository.RachaMemberRepository;
import br.com.rachaplus.api.domain.repository.RachaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EquilibrioTimesService {

    private static final Map<Esporte, Set<Integer>> FORMATOS_PERMITIDOS = Map.of(
            Esporte.FUTEBOL, Set.of(5, 6, 7, 8, 11),
            Esporte.BASQUETE, Set.of(2, 3, 4, 5),
            Esporte.VOLEI, Set.of(2, 4, 5, 6)
    );

    private final RachaRepository rachaRepository;
    private final RachaMemberRepository rachaMemberRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random;

    public EquilibrioTimesService(RachaRepository rachaRepository,
                                  RachaMemberRepository rachaMemberRepository) {
        this(rachaRepository, rachaMemberRepository, new Random());
    }

    public EquilibrioTimesService(RachaRepository rachaRepository,
                                  RachaMemberRepository rachaMemberRepository,
                                  Random random) {
        this.rachaRepository = rachaRepository;
        this.rachaMemberRepository = rachaMemberRepository;
        this.random = random;
    }

    @Transactional
    public EquilibrarTimesResponseDTO equilibrarTimes(UUID rachaId,
                                                      EquilibrarTimesRequestDTO dto,
                                                      Usuario requester) {
        Racha racha = rachaRepository.findById(rachaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Racha não encontrado"));

        if ("FINALIZADO".equalsIgnoreCase(racha.getStatus())) {
            throw new RegraNegocioException("Este racha já foi finalizado e não permite novo sorteio");
        }

        if (requester == null || !rachaMemberRepository.existsByRachaIdAndUserId(rachaId, requester.getId())) {
            throw new AcessoNegadoException("Acesso negado: você não pertence a este racha");
        }

        Esporte esporte = Esporte.deTexto(racha.getEsporte());
        if (esporte == null) {
            throw new RegraNegocioException("esporte", "O racha não possui um esporte válido definido");
        }

        if (dto == null || dto.jogadoresPorTime() == null) {
            throw new RegraNegocioException("jogadoresPorTime", "Informe o número de jogadores por time");
        }

        int porTime = dto.jogadoresPorTime();
        Set<Integer> permitidos = FORMATOS_PERMITIDOS.getOrDefault(esporte, Set.of());
        if (!permitidos.contains(porTime)) {
            throw new RegraNegocioException("jogadoresPorTime",
                    "Formato " + porTime + "x" + porTime + " inválido para " + esporte
                            + ". Formatos permitidos: " + permitidos.stream().sorted().toList());
        }

        List<UUID> idsEnviados = dto.jogadoresConfirmadosIds();
        if (idsEnviados == null || idsEnviados.isEmpty()) {
            throw new RegraNegocioException("jogadoresConfirmadosIds",
                    "Informe os jogadores confirmados para o sorteio");
        }

        Set<UUID> idsUnicos = new HashSet<>(idsEnviados);
        if (idsUnicos.size() != idsEnviados.size()) {
            throw new RegraNegocioException("jogadoresConfirmadosIds",
                    "A lista de jogadores confirmados contém IDs duplicados");
        }

        Map<UUID, RachaMember> membrosDoRacha = rachaMemberRepository.findAllByRachaId(rachaId)
                .stream()
                .collect(Collectors.toMap(RachaMember::getId, m -> m));

        List<RachaMember> confirmados = new ArrayList<>(idsEnviados.size());
        for (UUID memberId : idsEnviados) {
            RachaMember m = membrosDoRacha.get(memberId);
            if (m == null) {
                throw new RegraNegocioException("jogadoresConfirmadosIds",
                        "O jogador " + memberId + " não pertence a este racha");
            }
            confirmados.add(m);
        }

        int minimoConfirmados = 2 * porTime;
        if (confirmados.size() < minimoConfirmados) {
            throw new RegraNegocioException("jogadoresConfirmadosIds",
                    "São necessários pelo menos " + minimoConfirmados
                            + " jogadores confirmados para formar no mínimo 2 times no formato "
                            + porTime + "x" + porTime + " (confirmados: " + confirmados.size() + ")");
        }

        int qtdTimes = confirmados.size() / porTime;
        int qtdDeFora = confirmados.size() % porTime;

        Posicao posicaoCritica = obterPosicaoCritica(esporte, porTime);

        // 1. Separar grupo "De fora" de forma imparcial/aleatória, protegendo posições críticas escassas
        List<RachaMember> titulares = new ArrayList<>(confirmados);
        List<RachaMember> sobra = new ArrayList<>();

        if (qtdDeFora > 0) {
            sobra = selecionarJogadoresDeFora(titulares, qtdTimes, qtdDeFora, posicaoCritica);
            Set<UUID> sobraIds = sobra.stream().map(RachaMember::getId).collect(Collectors.toSet());
            titulares = confirmados.stream()
                    .filter(m -> !sobraIds.contains(m.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        // 2. Distribuição inicial tática (agrupada por posição, ordenada por OVR decrescente com desempate aleatório)
        List<List<RachaMember>> times = distribuirPorPosicaoEOvr(titulares, qtdTimes, porTime, posicaoCritica);

        // 3. Etapa de refinamento (pairwise swap) reduzindo o spread (maior soma - menor soma de OVR)
        refinarComPairwiseSwap(times, posicaoCritica);

        // 4. Montar resposta DTO
        List<TimeGeradoDTO> timesGerados = new ArrayList<>(qtdTimes);
        for (int i = 0; i < qtdTimes; i++) {
            List<RachaMember> integrantes = times.get(i);
            List<TimeGeradoDTO.JogadorTimeDTO> jogadoresDto = integrantes.stream()
                    .map(TimeGeradoDTO.JogadorTimeDTO::new)
                    .toList();
            float mediaNota = calcularMediaArredondada(integrantes);
            timesGerados.add(new TimeGeradoDTO("Time " + (i + 1), mediaNota, jogadoresDto));
        }

        TimeGeradoDTO deForaDto = null;
        if (qtdDeFora > 0) {
            List<TimeGeradoDTO.JogadorTimeDTO> sobraDto = sobra.stream()
                    .map(TimeGeradoDTO.JogadorTimeDTO::new)
                    .toList();
            deForaDto = new TimeGeradoDTO("De fora", calcularMediaArredondada(sobra), sobraDto);
        }

        sincronizarEstadoRacha(racha, porTime, dto.formato(), timesGerados, deForaDto);

        return new EquilibrarTimesResponseDTO(timesGerados, deForaDto);
    }

    private Posicao obterPosicaoCritica(Esporte esporte, int porTime) {
        if (esporte == Esporte.FUTEBOL) {
            return Posicao.GOLEIRO;
        }
        if (esporte == Esporte.VOLEI && porTime == 6) {
            return Posicao.LEVANTADOR;
        }
        return null;
    }

    private List<RachaMember> selecionarJogadoresDeFora(List<RachaMember> confirmados,
                                                        int qtdTimes,
                                                        int qtdDeFora,
                                                        Posicao posicaoCritica) {
        List<RachaMember> embaralhados = new ArrayList<>(confirmados);
        Collections.shuffle(embaralhados, random);

        if (posicaoCritica == null) {
            return new ArrayList<>(embaralhados.subList(0, qtdDeFora));
        }

        List<RachaMember> criticos = embaralhados.stream()
                .filter(m -> m.getPosicao() == posicaoCritica)
                .collect(Collectors.toCollection(ArrayList::new));
        List<RachaMember> demais = embaralhados.stream()
                .filter(m -> m.getPosicao() != posicaoCritica)
                .collect(Collectors.toCollection(ArrayList::new));

        // Protege até `qtdTimes` atletas da posição crítica para que cada time titular tenha ao menos 1
        int protegidos = Math.min(criticos.size(), qtdTimes);
        List<RachaMember> elegiveisParaSair = new ArrayList<>(demais);
        if (criticos.size() > protegidos) {
            elegiveisParaSair.addAll(criticos.subList(protegidos, criticos.size()));
        }
        Collections.shuffle(elegiveisParaSair, random);

        List<RachaMember> escolhidos = new ArrayList<>(qtdDeFora);
        int deElegiveis = Math.min(qtdDeFora, elegiveisParaSair.size());
        escolhidos.addAll(elegiveisParaSair.subList(0, deElegiveis));

        if (escolhidos.size() < qtdDeFora) {
            escolhidos.addAll(criticos.subList(0, qtdDeFora - escolhidos.size()));
        }

        return escolhidos;
    }

    private List<List<RachaMember>> distribuirPorPosicaoEOvr(List<RachaMember> titulares,
                                                             int qtdTimes,
                                                             int porTime,
                                                             Posicao posicaoCritica) {
        List<List<RachaMember>> times = new ArrayList<>(qtdTimes);
        for (int i = 0; i < qtdTimes; i++) {
            times.add(new ArrayList<>(porTime));
        }

        // Agrupa por posição colocando posição crítica primeiro
        Map<Posicao, List<RachaMember>> porPosicao = new LinkedHashMap<>();
        if (posicaoCritica != null) {
            porPosicao.put(posicaoCritica, new ArrayList<>());
        }
        for (RachaMember m : titulares) {
            porPosicao.computeIfAbsent(m.getPosicao(), k -> new ArrayList<>()).add(m);
        }

        for (Map.Entry<Posicao, List<RachaMember>> entry : porPosicao.entrySet()) {
            Posicao pos = entry.getKey();
            List<RachaMember> grupo = entry.getValue();
            if (grupo.isEmpty()) continue;

            // Desempate aleatório para notas iguais antes da ordenação estável por OVR decrescente
            Collections.shuffle(grupo, random);
            grupo.sort(Comparator.comparingDouble((RachaMember m) -> notaEfetiva(m)).reversed());

            for (RachaMember atleta : grupo) {
                int melhorTimeIdx = escolherTimeAlvo(times, porTime, pos);
                times.get(melhorTimeIdx).add(atleta);
            }
        }

        return times;
    }

    private int escolherTimeAlvo(List<List<RachaMember>> times, int porTime, Posicao pos) {
        List<Integer> candidatos = new ArrayList<>();
        long menorQtdPosicao = Long.MAX_VALUE;
        double menorSomaOvr = Double.MAX_VALUE;
        int menorTamanho = Integer.MAX_VALUE;

        for (int i = 0; i < times.size(); i++) {
            List<RachaMember> t = times.get(i);
            if (t.size() >= porTime) continue;

            long qtdMesmaPos = t.stream().filter(m -> m.getPosicao() == pos).count();
            double soma = somaOvr(t);
            int tam = t.size();

            boolean melhor = false;
            boolean empate = false;

            if (qtdMesmaPos < menorQtdPosicao) {
                melhor = true;
            } else if (qtdMesmaPos == menorQtdPosicao) {
                if (soma < menorSomaOvr - 1e-6) {
                    melhor = true;
                } else if (Math.abs(soma - menorSomaOvr) <= 1e-6) {
                    if (tam < menorTamanho) {
                        melhor = true;
                    } else if (tam == menorTamanho) {
                        empate = true;
                    }
                }
            }

            if (melhor) {
                menorQtdPosicao = qtdMesmaPos;
                menorSomaOvr = soma;
                menorTamanho = tam;
                candidatos.clear();
                candidatos.add(i);
            } else if (empate) {
                candidatos.add(i);
            }
        }

        return candidatos.get(random.nextInt(candidatos.size()));
    }

    private void refinarComPairwiseSwap(List<List<RachaMember>> times, Posicao posicaoCritica) {
        // Passo 1: trocas entre atletas de mesma posição
        executarSwaps(times, posicaoCritica, true);
        // Passo 2: trocas entre posições equivalentes (sem quebrar posições críticas nem concentrar posições)
        executarSwaps(times, posicaoCritica, false);
    }

    private void executarSwaps(List<List<RachaMember>> times,
                               Posicao posicaoCritica,
                               boolean apenasMesmaPosicao) {
        boolean melhorou = true;
        int iteracoes = 0;

        while (melhorou && iteracoes < 200) {
            melhorou = false;
            iteracoes++;

            double spreadAtual = calcularSpread(times);
            double varianciaAtual = calcularSomaQuadradosDesvio(times);
            if (spreadAtual <= 1e-6) break;

            int bestTi = -1, bestTj = -1, bestPi = -1, bestPj = -1;
            double melhorSpread = spreadAtual;
            double melhorVariancia = varianciaAtual;

            for (int ti = 0; ti < times.size(); ti++) {
                for (int tj = ti + 1; tj < times.size(); tj++) {
                    List<RachaMember> timeA = times.get(ti);
                    List<RachaMember> timeB = times.get(tj);

                    for (int pi = 0; pi < timeA.size(); pi++) {
                        for (int pj = 0; pj < timeB.size(); pj++) {
                            RachaMember a = timeA.get(pi);
                            RachaMember b = timeB.get(pj);

                            if (apenasMesmaPosicao && a.getPosicao() != b.getPosicao()) {
                                continue;
                            }
                            if (!apenasMesmaPosicao && !trocaTaticamenteValida(timeA, timeB, a, b, posicaoCritica)) {
                                continue;
                            }

                            // Simula a troca
                            timeA.set(pi, b);
                            timeB.set(pj, a);

                            double novoSpread = calcularSpread(times);
                            double novaVariancia = calcularSomaQuadradosDesvio(times);

                            boolean reduzSpread = novoSpread < melhorSpread - 1e-6;
                            boolean empataSpreadReduzVariancia = Math.abs(novoSpread - melhorSpread) <= 1e-6
                                    && novaVariancia < melhorVariancia - 1e-6;

                            if (reduzSpread || empataSpreadReduzVariancia) {
                                melhorSpread = novoSpread;
                                melhorVariancia = novaVariancia;
                                bestTi = ti;
                                bestTj = tj;
                                bestPi = pi;
                                bestPj = pj;
                            }

                            // Reverte simulação
                            timeA.set(pi, a);
                            timeB.set(pj, b);
                        }
                    }
                }
            }

            if (bestTi >= 0) {
                RachaMember temp = times.get(bestTi).get(bestPi);
                times.get(bestTi).set(bestPi, times.get(bestTj).get(bestPj));
                times.get(bestTj).set(bestPj, temp);
                melhorou = true;
            }
        }
    }

    private boolean trocaTaticamenteValida(List<RachaMember> timeA,
                                           List<RachaMember> timeB,
                                           RachaMember a,
                                           RachaMember b,
                                           Posicao posicaoCritica) {
        if (a.getPosicao() == b.getPosicao()) {
            return true;
        }
        // Nunca deixa um time que tinha posição crítica ficar com 0 após a troca
        if (posicaoCritica != null) {
            if (a.getPosicao() == posicaoCritica) {
                long criticosA = timeA.stream().filter(m -> m.getPosicao() == posicaoCritica).count();
                if (criticosA <= 1) return false;
            }
            if (b.getPosicao() == posicaoCritica) {
                long criticosB = timeB.stream().filter(m -> m.getPosicao() == posicaoCritica).count();
                if (criticosB <= 1) return false;
            }
        }
        return true;
    }

    private double calcularSpread(List<List<RachaMember>> times) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (List<RachaMember> t : times) {
            double s = somaOvr(t);
            if (s < min) min = s;
            if (s > max) max = s;
        }
        return max - min;
    }

    private double calcularSomaQuadradosDesvio(List<List<RachaMember>> times) {
        double media = times.stream().mapToDouble(this::somaOvr).average().orElse(0.0);
        double acc = 0.0;
        for (List<RachaMember> t : times) {
            double diff = somaOvr(t) - media;
            acc += diff * diff;
        }
        return acc;
    }

    private double somaOvr(List<RachaMember> integrantes) {
        double soma = 0.0;
        for (RachaMember m : integrantes) {
            soma += notaEfetiva(m);
        }
        return soma;
    }

    private double notaEfetiva(RachaMember m) {
        return m.getRachaRating();
    }

    private float calcularMediaArredondada(List<RachaMember> integrantes) {
        if (integrantes.isEmpty()) return 0.0f;
        double media = somaOvr(integrantes) / integrantes.size();
        return Math.round(media * 10f) / 10f;
    }

    private void sincronizarEstadoRacha(Racha racha,
                                        int porTime,
                                        String formatoOpcional,
                                        List<TimeGeradoDTO> timesGerados,
                                        TimeGeradoDTO deForaDto) {
        try {
            EstadoJogoDTO atual = null;
            if (racha.getEstadoJogoJson() != null && !racha.getEstadoJogoJson().isBlank()) {
                try {
                    atual = objectMapper.readValue(racha.getEstadoJogoJson(), EstadoJogoDTO.class);
                } catch (Exception ignored) {
                }
            }

            List<EstadoJogoDTO.TimeSorteadoDTO> timesEstado = new ArrayList<>(timesGerados.size());
            for (int i = 0; i < timesGerados.size(); i++) {
                TimeGeradoDTO tg = timesGerados.get(i);
                List<EstadoJogoDTO.JogadorSorteadoDTO> jDtos = tg.jogadores().stream()
                        .map(j -> new EstadoJogoDTO.JogadorSorteadoDTO(
                                j.id() != null ? j.id().toString() : "",
                                null,
                                j.nomeJogador(),
                                j.posicao() != null ? j.posicao().name() : "",
                                j.nota()))
                        .toList();
                timesEstado.add(new EstadoJogoDTO.TimeSorteadoDTO(i + 1, tg.nome(), tg.mediaNota(), jDtos));
            }

            List<EstadoJogoDTO.JogadorSorteadoDTO> deForaEstado = deForaDto == null
                    ? List.of()
                    : deForaDto.jogadores().stream()
                            .map(j -> new EstadoJogoDTO.JogadorSorteadoDTO(
                                    j.id() != null ? j.id().toString() : "",
                                    null,
                                    j.nomeJogador(),
                                    j.posicao() != null ? j.posicao().name() : "",
                                    j.nota()))
                            .toList();

            String formato = (formatoOpcional != null && !formatoOpcional.isBlank())
                    ? formatoOpcional
                    : porTime + "x" + porTime;

            EstadoJogoDTO novoEstado = new EstadoJogoDTO(
                    formato,
                    timesEstado,
                    deForaEstado,
                    atual != null ? atual.partidaAtual() : null
            );

            racha.setEstadoJogoJson(objectMapper.writeValueAsString(novoEstado));
            rachaRepository.save(racha);
        } catch (Exception ignored) {
            // Sincronização auxiliar não bloqueia o retorno do algoritmo
        }
    }
}
