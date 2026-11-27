package br.com.rachaplus.api.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "solicitacoes_entrada")
@Data
public class SolicitacaoEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "racha_id", nullable = false)
    private Racha racha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSolicitacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

    @ManyToOne
    @JoinColumn(name = "member_alvo_id")
    private RachaMember memberAlvo;

    @Column(name = "nome_novo_perfil")
    private String nomeNovoPerfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "posicao_novo_perfil")
    private Posicao posicaoNovoPerfil;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
