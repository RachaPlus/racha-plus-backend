package br.com.rachaplus.api.domain.exception;

// Usuário logado sem permissão no racha -> 403 Forbidden
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
