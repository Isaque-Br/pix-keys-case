package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CnpjKeyValidator implements KeyValidator {

    private static final String MSG = "cnpj inválido: esperado 14 dígitos válidos (com ou sem máscara)";

    // Aceita apenas dígitos, espaços e caracteres de máscara comuns (. - /)
    private static final Pattern ALLOWED = Pattern.compile("^[0-9 .\\-\\/]+$");
    private static final Pattern ALL_EQUAL = Pattern.compile("^(\\d)\\1{13}$");

    private static final int[] W1 = {5,4,3,2,9,8,7,6,5,4,3,2};
    private static final int[] W2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};

    @Override
    public KeyType supports() {
        return KeyType.CNPJ;
    }

    @Override
    public void validate(String key) {
        if (key == null) {
            throw new BusinessRuleViolationException(MSG);
        }
        String s = key.strip();
        if (s.isEmpty()) {
            throw new BusinessRuleViolationException(MSG);
        }
        // Rejeita caracteres não permitidos antes de normalizar
        if (!ALLOWED.matcher(s).matches()) {
            throw new BusinessRuleViolationException(MSG);
        }

        // Normaliza para apenas dígitos
        String d = s.replaceAll("\\D", "");
        if (d.length() != 14) {
            throw new BusinessRuleViolationException(MSG);
        }
        if (ALL_EQUAL.matcher(d).matches()) {
            throw new BusinessRuleViolationException(MSG);
        }
        if (!checkDigits(d)) {
            throw new BusinessRuleViolationException(MSG);
        }
    }

    private static boolean checkDigits(String d) {
        int dv1 = calc(d, W1, 12);
        int dv2 = calc(d.substring(0, 12) + dv1, W2, 13);
        return (d.charAt(12) - '0') == dv1 && (d.charAt(13) - '0') == dv2;
    }

    private static int calc(String s, int[] weights, int len) {
        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += (s.charAt(i) - '0') * weights[i];
        }
        int mod = sum % 11;
        return (mod < 2) ? 0 : 11 - mod;
    }
}
