package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.stereotype.Component;

@Component
public class CnpjKeyValidator implements KeyValidator {

    @Override
    public KeyType supports() {
        return KeyType.CNPJ;
    }

    @Override
    public void validate(String key) {
        String d = key == null ? "" : key.replaceAll("\\D", "");
        if (d.length() != 14) throw new IllegalArgumentException("cnpj inválido");
        if (d.chars().distinct().count() == 1) throw new IllegalArgumentException("cnpj inválido");
        if (!checkDigits(d)) throw new IllegalArgumentException("cnpj inválido");
    }

    private boolean checkDigits(String d) {
        int dv1 = calc(d.substring(0, 12), new int[]{5,4,3,2,9,8,7,6,5,4,3,2});
        int dv2 = calc(d.substring(0, 12) + dv1, new int[]{6,5,4,3,2,9,8,7,6,5,4,3,2});
        return (d.charAt(12) - '0') == dv1 && (d.charAt(13) - '0') == dv2;
    }

    private int calc(String base, int[] w) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) sum += (base.charAt(i) - '0') * w[i];
        int mod = sum % 11;
        return (mod < 2) ? 0 : 11 - mod;
    }
}
