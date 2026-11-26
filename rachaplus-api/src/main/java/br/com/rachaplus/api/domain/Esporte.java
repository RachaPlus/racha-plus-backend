package br.com.rachaplus.api.domain;

public enum Esporte {
    FUTEBOL,
    BASQUETE,
    VOLEI;

    // Converte o texto salvo em Racha.esporte; devolve null se estiver vazio ou inválido
    public static Esporte deTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Esporte.valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
