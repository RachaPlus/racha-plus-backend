package br.com.rachaplus.api.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "rachas")
@Data
public class Racha {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String nome;

    @Column(name = "description")
    private String descricao;

    @Column(name = "esporte")
    private String esporte;

    @Column(name = "data")
    private LocalDate data;

    @Column(name = "horario")
    private LocalTime horario;

    @Column(name = "status")
    private String status = "ABERTO";

    @Column(name = "estado_jogo_json", columnDefinition = "TEXT")
    private String estadoJogoJson;

    @Column(name = "codigo_convite", unique = true)
    private String codigoConvite;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
}
