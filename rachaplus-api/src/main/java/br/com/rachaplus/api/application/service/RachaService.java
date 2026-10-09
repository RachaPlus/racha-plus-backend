package br.com.rachaplus.api.application.service;

import br.com.rachaplus.api.application.dto.CriarRachaDTO;
import br.com.rachaplus.api.application.dto.RachaResponseDTO;
import br.com.rachaplus.api.domain.Racha;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.repository.RachaRepository;
import br.com.rachaplus.api.domain.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class RachaService {

    private final RachaRepository rachaRepository;
    private final UsuarioRepository usuarioRepository;

    public RachaService(RachaRepository rachaRepository, UsuarioRepository usuarioRepository) {
        this.rachaRepository = rachaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public RachaResponseDTO criar(CriarRachaDTO dto) {
        // Pega o email do usuário logado via JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario criador = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        Racha racha = new Racha();
        racha.setNome(dto.nome());
        racha.setEsporte(dto.esporte());
        racha.setCriador(criador);
        racha.setToken(gerarToken());

        return new RachaResponseDTO(rachaRepository.save(racha));
    }

    private String gerarToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}