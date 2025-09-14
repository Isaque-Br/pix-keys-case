package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;
import java.util.Set;

public interface KeyValidatorFactory {
    KeyValidator get(KeyType type);
    Set<KeyType> supportedTypes();

    // alias para quem preferir semântica "forType"
    default KeyValidator forType(KeyType type) {
        return get(type);
    }
}
