package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.CadastroUsuarioDTO;
import br.com.rachaplus.api.application.dto.UsuarioResponseDTO;
import br.com.rachaplus.api.application.service.UsuarioService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Deve cadastrar usuário com sucesso e retornar HTTP 201 Created com header Location")
    void deveCadastrarUsuarioComSucesso() throws Exception {
        var id = UUID.randomUUID();
        var dto = new CadastroUsuarioDTO("jogador1", "jogador1@email.com", "SenhaForte123");
        var responseDTO = new UsuarioResponseDTO(id, "jogador1", "jogador1@email.com");

        when(usuarioService.cadastrar(any(CadastroUsuarioDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/users/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value("jogador1"))
                .andExpect(jsonPath("$.email").value("jogador1@email.com"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 Bad Request ao tentar cadastrar usuário com dados inválidos")
    void deveRetornarBadRequestQuandoDadosInvalidos() throws Exception {
        // username curto demais, email invalido, senha sem maiuscula/numero
        var dtoInvalido = new CadastroUsuarioDTO("ab", "email-invalido", "123");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());
    }
}

