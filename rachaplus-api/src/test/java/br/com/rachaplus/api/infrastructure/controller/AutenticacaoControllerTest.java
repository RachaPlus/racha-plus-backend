package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.AutenticacaoDTO;
import br.com.rachaplus.api.domain.Usuario;
import br.com.rachaplus.api.domain.repository.UsuarioRepository;
import br.com.rachaplus.api.infrastructure.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutenticacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AutenticacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Deve autenticar com sucesso e retornar HTTP 200 com Token JWT")
    void deveAutenticarComSucesso() throws Exception {
        var dto = new AutenticacaoDTO("jogador@email.com", "SenhaForte123");
        var usuario = new Usuario();
        usuario.setEmail("jogador@email.com");
        var authToken = new UsernamePasswordAuthenticationToken(usuario, null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authToken);
        when(tokenService.gerarToken(any(Usuario.class))).thenReturn("token-jwt-mockado-123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt-mockado-123"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 Bad Request ao tentar login com campos vazios ou inválidos")
    void deveRetornarBadRequestQuandoDadosInvalidos() throws Exception {
        var dtoInvalido = new AutenticacaoDTO("", "123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve falhar autenticação quando credenciais forem inválidas")
    void deveRetornarErroQuandoCredenciaisInvalidas() throws Exception {
        var dto = new AutenticacaoDTO("jogador@email.com", "SenhaIncorreta123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }
}

