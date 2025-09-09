package br.com.itau.pixkeys.domain.model;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyType;

public record PixKey(
        KeyType keyType,
        String keyValue,
        AccountType accountType,
        String agency,
        String account,
        String holderName,
        String holderSurname
) {}
