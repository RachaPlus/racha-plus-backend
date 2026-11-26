package br.com.rachaplus.api.domain.exception;

// Racha, jogador ou partida inexistente -> 404 Not Found
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
