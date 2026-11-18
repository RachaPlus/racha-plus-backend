package br.com.rachaplus.api.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatePlacarDTO(
        @NotNull @Min(0) Integer placarTimeA,
        @NotNull @Min(0) Integer placarTimeB
) {}