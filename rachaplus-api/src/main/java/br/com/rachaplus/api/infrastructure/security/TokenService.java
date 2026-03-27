package br.com.rachaplus.api.infrastructure.security;

import br.com.rachaplus.api.domain.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String SECRET_KEY;

    public String gerarToken(Usuario usuario) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

        return Jwts.builder()
                .subject(usuario.getEmail())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(7200)))
                .signWith(key)
                .compact();
    }
}
