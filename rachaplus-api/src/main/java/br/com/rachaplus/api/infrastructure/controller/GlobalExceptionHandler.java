package br.com.rachaplus.api.infrastructure.controller;

import br.com.rachaplus.api.application.dto.ValidacaoErroDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidacaoErroDTO>> tratarErroValidacao(MethodArgumentNotValidException ex) {
        var erros = ex.getFieldErrors().stream()
                .map(erro -> new ValidacaoErroDTO(erro.getField(), erro.getDefaultMessage()))
                .toList();
        
        return ResponseEntity.badRequest().body(erros);
    }

    @ExceptionHandler(br.com.rachaplus.api.domain.exception.ConflitoRegistroException.class)
    public ResponseEntity<String> tratarConflitoRegistro(br.com.rachaplus.api.domain.exception.ConflitoRegistroException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
