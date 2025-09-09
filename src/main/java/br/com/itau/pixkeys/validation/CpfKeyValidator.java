package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.stereotype.Component;

@Component
public class CpfKeyValidator implements KeyValidator {

    @Override
    public KeyType supports() {
        return KeyType.CPF;
    }

    @Override
    public void validate(String key) {
        String digits = key == null ? "" : key.replaceAll("\\D", "");
        if (digits.length() != 11) throw new IllegalArgumentException("cpf inválido");

        if (digits.chars().distinct().count() == 1)
            throw new IllegalArgumentException("cpf inválido");

        if (!checkDigits(digits)) throw new IllegalArgumentException("cpf inválido");
    }

    private boolean checkDigits(String d) {
        int dv1 = calcDigit(d.substring(0, 9), new int[]{10,9,8,7,6,5,4,3,2});
        int dv2 = calcDigit(d.substring(0,10), new int[]{11,10,9,8,7,6,5,4,3,2});
        return (d.charAt(9) - '0') == dv1 && (d.charAt(10) - '0') == dv2;
    }

    private int calcDigit(String base, int[] weights) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += (base.charAt(i) - '0') * weights[i];
        }
        int mod = sum % 11;
        return (mod < 2) ? 0 : 11 - mod;
    }
}
