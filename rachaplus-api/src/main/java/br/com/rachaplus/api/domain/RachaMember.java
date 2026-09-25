package br.com.rachaplus.api.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "racha_members", uniqueConstraints = @UniqueConstraint(columnNames = {"racha_id", "user_id"}))
@Data
public class RachaMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    @ManyToOne
    @JoinColumn(name = "racha_id")
    private Racha racha;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Usuario user;

    @Column(name = "racha_rating")
    private float rachaRating;

    @Enumerated(EnumType.STRING)
    private RachaRole role;
}
