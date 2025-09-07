package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SimpleKeyValidatorFactory implements KeyValidatorFactory {
    private final Map<KeyType, KeyValidator> byType = new EnumMap<>(KeyType.class);

    public SimpleKeyValidatorFactory(List<KeyValidator> validators) {

        Objects.requireNonNull(validators, "lista de validadores não pode ser nula");

        for (KeyValidator v : validators) {
            Objects.requireNonNull(v, "Validador não pode ser nulo");
            KeyType type = Objects.requireNonNull(v.supports(),
                    "supports() não pode retornar nulo");

            KeyValidator prev = byType.putIfAbsent(type, v);
            if (prev != null) {
                throw new IllegalStateException(
                        "Validador duplicado para " + type + ": "
                                + prev.getClass().getSimpleName() + " e "
                                + v.getClass().getSimpleName());
            }
        }
    }

    @Override
    public KeyValidator get(KeyType type) {
        Objects.requireNonNull(type, "KeyType não pode ser nulo");
        KeyValidator v = byType.get(type);
        if (v == null)
            throw new IllegalStateException(
                        "Nenhum validador registrado para o tipo: " + type);
        return v;
    }

    @Override
    public Set<KeyType> supportedTypes() {
        return Set.copyOf(byType.keySet());
    }
}