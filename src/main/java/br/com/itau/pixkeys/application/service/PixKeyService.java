package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.springframework.stereotype.Service;

@Service
public class PixKeyService {
    private final KeyValidatorFactory factory;

    public PixKeyService(KeyValidatorFactory factory) { this.factory = factory; }

    // por enquanto só valida; persistência virá depois
    public void create(
            KeyType keyType, String keyValue,
            AccountType accountType, String agency, String account,
            String holderName, String holderSurname) {

        factory.get(keyType).validate(keyValue);
    }
}
