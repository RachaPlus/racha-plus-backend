package br.com.rachaplus.api.domain.exception;

public class ConflitoRegistroException extends RuntimeException {
    public ConflitoRegistroException(String mensagem) {
        super(mensagem);
    }
}
