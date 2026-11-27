package br.com.rachaplus.api.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partidas")
@Data
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "racha_id", nullable = false)
    private Racha racha;

    @Column(name = "placar_time_a")
    private Integer placarTimeA = 0;

    @Column(name = "placar_time_b")
    private Integer placarTimeB = 0;

    @CreationTimestamp
    @Column(name = "data_partida", nullable = false, updatable = false)
    private LocalDateTime dataPartida;
}