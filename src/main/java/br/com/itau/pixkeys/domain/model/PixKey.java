package br.com.itau.pixkeys.domain.model;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyStatus;
import br.com.itau.pixkeys.domain.KeyType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Document("pix_keys")
@CompoundIndexes({
        // Unicidade global do VALOR DA CHAVE (critério do case)
        @CompoundIndex(name = "uk_key_value", def = "{ 'keyValue': 1 }", unique = true),
        // Índice para consultas por conta (agência+conta)
        @CompoundIndex(name = "idx_account", def = "{ 'agency': 1, 'account': 1 }")
})
public record PixKey(
        @Id String id,                 // ID exigido pelo case em formato UUID (string)
        KeyType keyType,               // tipo da chave (PHONE/EMAIL/CPF/CNPJ/RANDOM)
        String keyValue,               // valor da chave (único no banco)
        AccountType accountType,       // corrente/poupança
        String agency,                 // 4 dígitos (validado no DTO)
        String account,                // 8 dígitos (validado no DTO)
        String holderName,             // obrigatório
        String holderSurname,          // opcional (nulo vira "")
        KeyStatus status,              // ACTIVE/INACTIVE
        Instant createdAt,             // data de inclusão
        Instant inactivatedAt          // preenchido somente quando inativa
) {
    // Fábrica: cria chave ATIVA e normaliza campos
    public static PixKey create(
            KeyType keyType,
            String keyValue,
            AccountType accountType,
            String agency,
            String account,
            String holderName,
            String holderSurname
    ) {
        Objects.requireNonNull(keyType, "keyType não pode ser nulo");
        Objects.requireNonNull(accountType, "accountType não pode ser nulo");

        return new PixKey(
                UUID.randomUUID().toString(),  // gera UUID conforme case
                keyType,                       // tipo da chave (PHONE/EMAIL/CPF/CNPJ)
                requireAndTrim(keyValue),      // obrigatório: não-nulo + strip()
                accountType,                   // corrente/poupança
                requireAndTrim(agency),        // obrigatório
                requireAndTrim(account),       // obrigatório
                requireAndTrim(holderName),    // obrigatório
                sanitizeOptional(holderSurname), // opcional: null -> "" + strip()
                KeyStatus.ACTIVE,              // status inicial
                Instant.now(),                 // data de criação
                null                           // ainda não inativada
        );
    }

    public boolean isInactive() {
        return status == KeyStatus.INACTIVE;
    }

    // Transição: inativar (somente uma vez)
    public PixKey inactivate() {
        if (isInactive()) {
            throw new BusinessRuleViolationException("chave já inativada");
        }
        return new PixKey(
                id, keyType, keyValue, accountType, agency, account,
                holderName, holderSurname,
                KeyStatus.INACTIVE,
                createdAt,
                Instant.now()
        );
    }

    // Atualiza dados da conta/titular (não altera id/tipo/valor)
    public PixKey updateAccount(
            AccountType accountType,
            String agency,
            String account,
            String holderName,
            String holderSurname
    ) {
        if (isInactive()) {
            throw new BusinessRuleViolationException("chave inativa");
        }
        Objects.requireNonNull(accountType, "accountType não pode ser nulo");

        return new PixKey(
                id,
                keyType,
                keyValue,
                accountType,
                requireAndTrim(agency),
                requireAndTrim(account),
                requireAndTrim(holderName),
                sanitizeOptional(holderSurname),
                status,
                createdAt,
                inactivatedAt
        );
    }

    // Helpers: normalização consistente
    private static String sanitizeOptional(String value) {
        return value == null ? "" : value.strip();
    }

    private static String requireAndTrim(String value) {
        return Objects.requireNonNull(value, "valor obrigatório").strip();
    }
}
