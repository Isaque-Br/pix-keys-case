package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.api.NotFoundException;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.springframework.stereotype.Service;

/**
 * Regras de criação de chaves Pix.
 * Passos: validar formato → checar unicidade → checar limite por conta → persistir.
 */
@Service
public class PixKeyService {

    private final KeyValidatorFactory factory;
    private final PixKeyRepository repo;

    public PixKeyService(KeyValidatorFactory factory, PixKeyRepository repo) {
        this.factory = factory;
        this.repo = repo;
    }

    /** Cria a chave aplicando regras de negócio e retorna o ID gerado. */
    public String create(
            KeyType keyType, String keyValue,
            AccountType accountType, String agency, String account,
            String holderName, String holderSurname
    ) {
        // 1) Validação de formato/semântica (delegada ao validador do tipo)
        factory.get(keyType).validate(keyValue);

        // 2) Unicidade global do valor da chave
        if (repo.findByKeyValue(keyValue).isPresent()) {
            throw new IllegalStateException("chave já cadastrada para outro correntista");
        }

        // 3) Limite por conta
        // TODO: quando houver HolderType (PF/PJ), aplicar PF=5 e PJ=20. Por ora, default=5.
        int limit = 5;
        long current = repo.countByAgencyAndAccount(agency, account);
        if (current >= limit) {
            throw new IllegalStateException("limite de chaves por conta atingido");
        }

        // 4) Persistência
        PixKey entity = PixKey.create(
                keyType, keyValue, accountType, agency, account, holderName, holderSurname
        );
        PixKey saved = repo.save(entity);
        return saved.id();
    }

    /** Busca por ID ou lança 404 (NotFoundException) para o handler transformar em HTTP 404. */
    public PixKey findById(String id) {
        return repo.findById(id).orElseThrow(() ->
                new NotFoundException("pix key não encontrada: " + id));
    }
}
