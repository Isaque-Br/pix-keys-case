package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
@Component
public class EmailKeyValidator implements KeyValidator {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int MAX_LEN = 77;

    @Override
    public KeyType supports() {
        return KeyType.EMAIL;
    }

    @Override
    public void validate(String value) {
        if (value == null) {
            throw new IllegalArgumentException("email nao pode ser nulo");
        }

        String v = value.strip();

        if (v.isBlank()) {
            throw new IllegalArgumentException("email nao pode ser vazio");
        }
        if (v.length() > MAX_LEN) {
            throw new IllegalArgumentException("email excede 77 caracteres");
        }
        if (!EMAIL.matcher(v).matches()) {
            throw new IllegalArgumentException("formato do email invalido");
        }
    }
}
