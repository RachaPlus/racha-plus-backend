package br.com.rachaplus.api.application.dto;

import br.com.rachaplus.api.domain.AtributoEsporte;
import br.com.rachaplus.api.domain.TipoAvaliacao;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public record AvaliacaoDTO(
        TipoAvaliacao tipo,
        Float nota,
        Map<AtributoEsporte, Float> atributos
) {
    @JsonCreator
    public AvaliacaoDTO {
    }

    public AvaliacaoDTO(Float nota) {
        this(TipoAvaliacao.GERAL, nota, null);
    }
}
