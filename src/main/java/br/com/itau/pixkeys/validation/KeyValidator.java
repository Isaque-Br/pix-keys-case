package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;

public interface KeyValidator {

    KeyType supports();

    void validate(String value);
}
