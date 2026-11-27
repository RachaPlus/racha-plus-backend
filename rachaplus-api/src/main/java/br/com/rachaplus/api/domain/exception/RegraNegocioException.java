package br.com.rachaplus.api.domain.exception;

// Erro de regra de negócio -> 400 Bad Request (mesmo formato da validação: campo + mensagem)
public class RegraNegocioException extends RuntimeException {

    private final String campo;

    public RegraNegocioException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public RegraNegocioException(String mensagem) {
        this(null, mensagem);
    }

    public String getCampo() {
        return campo;
    }
}
