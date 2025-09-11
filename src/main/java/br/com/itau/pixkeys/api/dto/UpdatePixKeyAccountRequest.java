package br.com.itau.pixkeys.api.dto;

import br.com.itau.pixkeys.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePixKeyAccountRequest(
        @NotNull AccountType accountType,
        @NotNull @Pattern(regexp="\\d{4}") String agency,
        @NotNull @Pattern(regexp="\\d{8}") String account,
        @NotBlank @Size(max=30) String holderName,
        @Size(max=45) String holderSurname
) {}
