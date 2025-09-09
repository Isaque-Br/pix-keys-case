package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.stereotype.Component;

@Component
public class RandomKeyValidator implements KeyValidator {

    @Override
    public KeyType supports() {
        return KeyType.RANDOM;
    }

    @Override
    public void validate(String key) {
        if (key == null) {
            throw new IllegalArgumentException("random inválido");
        }

        String v = key.strip();
        if (v.isEmpty() || v.length() > 36 || !v.matches("^[A-Za-z0-9]{1,36}$")) {
            throw new IllegalArgumentException("random inválido");
        }
    }
}
