package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.stereotype.Component;

@Component
public class PhoneKeyValidator implements KeyValidator {

    @Override
    public KeyType supports() {
        return KeyType.PHONE;
    }

    @Override
    public void validate(String value) {
        if (value == null) {
            throw new IllegalArgumentException("telefone não pode ser nulo");
        }
        String v = value.strip();
        if (v.isBlank()) {
            throw new IllegalArgumentException("telefone não pode ser vazio");
        }
        if (!v.startsWith("+55")) {
            throw new IllegalArgumentException("telefone deve iniciar com +55");
        }

        String digits = v.substring(3);

        if (!digits.matches("\\d{10,11}")) {
            throw new IllegalArgumentException("telefone inválido (esperado +55 e 10-11 dígitos)");
        }
    }
}
