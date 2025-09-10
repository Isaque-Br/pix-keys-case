package br.com.itau.pixkeys.api.dto;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyType;
import jakarta.validation.constraints.*;

public record CreatePixKeyRequest(

        @NotNull
        KeyType keyType,

        @NotBlank
        @Size(max = 77)
        String keyValue,

        @NotNull
        AccountType accountType,

        @NotNull
        @Pattern(regexp = "\\d{4}", message = "agencia deve ter 4 digitos")
        String agency,

        @NotNull
        @Pattern(regexp = "\\d{8}", message = "conta deve ter 8 digitos")
        String account,

        @NotBlank
        @Size(max = 30)
        String holderName,

        @Size(max = 45)
        String holderSurname
) {}
