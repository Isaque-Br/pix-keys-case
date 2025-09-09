package br.com.itau.pixkeys.domain.model;

import br.com.itau.pixkeys.domain.AccountType;
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
        @CompoundIndex(name = "idx_account",  def = "{ 'agency': 1, 'account': 1 }")
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
            KeyType t, String v,
            AccountType at, String ag, String acc,
            String name, String surname
    ) {
        Objects.requireNonNull(t,  "keyType não pode ser nulo");
        Objects.requireNonNull(at, "accountType não pode ser nulo");

        return new PixKey(
                UUID.randomUUID().toString(), // gera UUID conforme case
                t,
                nzs(v),        // obrigatório: não-nulo + strip() (unicode-aware)
                at,
                nzs(ag),       // obrigatório
                nzs(acc),      // obrigatório
                nzs(name),     // obrigatório
                nz(surname),   // opcional: null -> "" + strip()
                KeyStatus.ACTIVE,
                Instant.now(),
                null
        );
    }

    public boolean isInactive() {
        return status == KeyStatus.INACTIVE;
    }

    // Transição: inativar (somente uma vez)
    public PixKey inactivate() {
        if (isInactive()) throw new IllegalArgumentException("chave já inativada");
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
            AccountType at, String ag, String acc, String name, String surname
    ) {
        if (isInactive()) throw new IllegalArgumentException("chave inativa");
        Objects.requireNonNull(at, "accountType não pode ser nulo");
        return new PixKey(
                id,
                keyType,
                keyValue,
                at,
                nzs(ag),
                nzs(acc),
                nzs(name),
                nz(surname),
                status,
                createdAt,
                inactivatedAt
        );
    }

    // Helpers: normalização consistente
    private static String nz(String s) {
        return s == null ? "" : s.strip();
    }
    private static String nzs(String s) {
        return Objects.requireNonNull(s, "valor obrigatório").strip();
    }
}
