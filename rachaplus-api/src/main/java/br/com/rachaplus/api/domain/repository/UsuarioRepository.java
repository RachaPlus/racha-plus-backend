package br.com.rachaplus.api.domain.repository;

import br.com.rachaplus.api.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, java.util.UUID> {
    Optional<Usuario> findByEmail(String email);
}
