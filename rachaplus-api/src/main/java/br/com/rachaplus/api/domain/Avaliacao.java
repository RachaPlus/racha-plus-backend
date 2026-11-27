package br.com.rachaplus.api.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "avaliacoes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"avaliador_id", "avaliado_id"})
)
@Data
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "racha_id", nullable = false)
    private Racha racha;

    // Quem deu a nota (usuário logado)
    @ManyToOne(optional = false)
    @JoinColumn(name = "avaliador_id", nullable = false)
    private Usuario avaliador;

    // Quem recebeu a nota (jogador do racha, pode não ter usuário vinculado)
    @ManyToOne(optional = false)
    @JoinColumn(name = "avaliado_id", nullable = false)
    private RachaMember avaliado;

    @Column(nullable = false)
    private float nota;

    // Anulável no DDL para compatibilidade com registros legados em ddl-auto=update
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoAvaliacao tipo = TipoAvaliacao.GERAL;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "avaliacao_atributos",
            joinColumns = @JoinColumn(name = "avaliacao_id")
    )
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "atributo")
    @Column(name = "nota", nullable = false)
    private Map<AtributoEsporte, Float> atributos = new EnumMap<>(AtributoEsporte.class);

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
