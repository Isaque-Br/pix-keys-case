package br.com.itau.pixkeys.domain;

import java.text.Normalizer;
import java.util.Locale;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountType {
    CHECKING("corrente"),
    SAVINGS("poupanca");

    private final String jsonValue;
    AccountType(String jsonValue) { this.jsonValue = jsonValue; }

    @JsonValue
    public String json() { return jsonValue; }

    @JsonCreator
    public static AccountType from(String value) {
        if (value == null) return null;

        String stripped = value.strip();
        if (stripped.isEmpty()) return null;

        String v = Normalizer.normalize(stripped, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);

        return switch (v) {
            case "corrente"  -> CHECKING;
            case "poupanca"  -> SAVINGS;
            default -> throw new IllegalArgumentException("tipo conta inválido: " + value);
        };
    }
}
