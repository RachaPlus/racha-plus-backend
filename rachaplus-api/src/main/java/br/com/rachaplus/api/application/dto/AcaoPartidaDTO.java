package br.com.rachaplus.api.application.dto;

public record AcaoPartidaDTO(
        String acao, // PLACAR, FECHAR_SET, PAUSAR, RETOMAR, INTERVALO, RETOMAR_DO_INTERVALO, ENCERRAR_PARTIDA
        Integer placarA,
        Integer placarB
) {
}
