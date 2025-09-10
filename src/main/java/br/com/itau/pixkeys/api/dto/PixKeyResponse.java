package br.com.itau.pixkeys.api.dto;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyStatus;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;

import java.time.Instant;

public record PixKeyResponse(
        String id,
        KeyType keyType,
        String keyValue,
        AccountType accountType,
        String agency,
        String account,
        String holderName,
        String holderSurname,
        KeyStatus status,
        Instant createdAt,
        Instant inactivatedAt
) {
    public static PixKeyResponse from(PixKey k) {
        return new PixKeyResponse(
                k.id(), k.keyType(), k.keyValue(), k.accountType(), k.agency(), k.account(),
                k.holderName(), k.holderSurname(), k.status(), k.createdAt(), k.inactivatedAt()
        );
    }
}
