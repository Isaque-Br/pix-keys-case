package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class RandomKeyValidator implements KeyValidator {

    private static final Pattern P = Pattern.compile("^[A-Za-z0-9]{32}$");

    @Override
    public KeyType supports() {
        return KeyType.RANDOM;
    }

    @Override
    public void validate(String key) {
        if (key == null) {
            throw new BusinessRuleViolationException(
                    "random inválido: esperado 32 caracteres alfanuméricos"
            );
        }
        String v = key.strip();
        if (!P.matcher(v).matches()) {
            throw new BusinessRuleViolationException(
                    "random inválido: esperado 32 caracteres alfanuméricos"
            );
        }
    }
}
