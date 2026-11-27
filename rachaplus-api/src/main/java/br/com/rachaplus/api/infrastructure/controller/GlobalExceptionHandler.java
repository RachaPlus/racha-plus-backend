package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.ValidacaoErroDTO;
import br.com.rachaplus.api.domain.exception.AcessoNegadoException;
import br.com.rachaplus.api.domain.exception.RecursoNaoEncontradoException;
import br.com.rachaplus.api.domain.exception.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidacaoErroDTO>> tratarErroValidacao(MethodArgumentNotValidException ex) {
        var erros = ex.getFieldErrors().stream()
                .map(erro -> new ValidacaoErroDTO(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(erros);
    }

    // Regra de negócio (posição fora do esporte, nota inválida, autoavaliação...) -> 400 no mesmo formato da validação
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<List<ValidacaoErroDTO>> tratarRegraNegocio(RegraNegocioException ex) {
        return ResponseEntity.badRequest().body(List.of(new ValidacaoErroDTO(ex.getCampo(), ex.getMessage())));
    }

    // JSON mal formatado ou tipo errado (ex: "nota": "abc") -> 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<List<ValidacaoErroDTO>> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(List.of(new ValidacaoErroDTO(null, "Corpo da requisição inválido ou mal formatado")));
    }

    // ID inválido na URL (ex: /rachas/abc) -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<List<ValidacaoErroDTO>> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(List.of(new ValidacaoErroDTO(ex.getName(), "Valor inválido na URL")));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<String> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<String> tratarAcessoNegado(AcessoNegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(br.com.rachaplus.api.domain.exception.ConflitoRegistroException.class)
    public ResponseEntity<String> tratarConflitoRegistro(
            br.com.rachaplus.api.domain.exception.ConflitoRegistroException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<String> tratarBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).body("Credenciais inválidas");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> tratarErroInesperado(Exception ex) {
        log.error("Erro interno inesperado no servidor", ex);
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro interno no servidor. Tente novamente mais tarde.");
    }
}
