package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RandomKeyValidator implements KeyValidator {

    @Override
    public KeyType supports() {
        return KeyType.RANDOM;
    }

    @Override
    public void validate(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("random inválido");
        }
        try {
            UUID.fromString(key.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("random inválido");
        }
    }
}
