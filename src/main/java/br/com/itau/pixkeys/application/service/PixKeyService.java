package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.api.NotFoundException;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.springframework.stereotype.Service;

@Service
public class PixKeyService {

    private static final int ACCOUNT_KEYS_LIMIT = 5;

    private final KeyValidatorFactory factory;
    private final PixKeyRepository repo;

    public PixKeyService(KeyValidatorFactory factory, PixKeyRepository repo) {
        this.factory = factory;
        this.repo = repo;
    }

    /**
     * Cria a chave aplicando regras de negócio e retorna o ID gerado.
     */
    public String create(
            KeyType keyType, String keyValue,
            AccountType accountType, String agency, String account,
            String holderName, String holderSurname
    ) {
        // 1) Validação (delegada à Strategy)
        factory.forType(keyType).validate(keyValue);

        // 2) Unicidade global
        if (repo.findByKeyValue(keyValue).isPresent()) {
            throw new BusinessRuleViolationException("chave já cadastrada para outro correntista");
        }

        // 3) Limite por conta
        long current = repo.countByAgencyAndAccount(agency, account);
        if (current >= ACCOUNT_KEYS_LIMIT) {
            throw new BusinessRuleViolationException("limite de chaves por conta atingido");
        }

        // 4) Persistência
        PixKey entity = PixKey.create(
                keyType, keyValue, accountType, agency, account, holderName, holderSurname
        );
        return repo.save(entity).id();
    }

    /**
     * Busca por ID ou lança 404 (NotFoundException) para o handler transformar em HTTP 404.
     */
    public PixKey findById(String id) {
        return repo.findById(id).orElseThrow(() ->
                new NotFoundException("pix key não encontrada: " + id));
    }

    /**
     * Inativa (soft delete) a chave. Lança 404 se não existir e 422 se já estiver inativa.
     */
    public PixKey inactivate(String id) {
        PixKey current = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("pix key não encontrada: " + id));
        if (current.isInactive()) {
            throw new BusinessRuleViolationException("chave já inativada");
        }
        PixKey updated = current.inactivate();
        return repo.save(updated);
    }

    /**
     * Troca a conta da chave (valida limite quando muda de conta).
     */
    public PixKey updateAccount(
            String id,
            AccountType newAccountType,
            String newAgency,
            String newAccount,
            String newHolderName,
            String newHolderSurname
    ) {
        // 1) Carrega ou 404
        PixKey current = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("pix key não encontrada: " + id));

        // 2) Regra mais barata: não permite alterar chave inativa
        if (current.isInactive()) {
            throw new BusinessRuleViolationException("chave inativa");
        }

        // 3) Só valida limite se realmente trocar agência/conta
        boolean sameAccount = current.agency().equals(newAgency) && current.account().equals(newAccount);
        if (!sameAccount) {
            long countAtTarget = repo.countByAgencyAndAccount(newAgency, newAccount);
            if (countAtTarget >= ACCOUNT_KEYS_LIMIT) {
                throw new BusinessRuleViolationException("limite de chaves por conta atingido");
            }
        }

        // 4) Atualiza no domínio (mantém invariantes e normalização)
        PixKey updated = current.updateAccount(
                newAccountType, newAgency, newAccount, newHolderName, newHolderSurname
        );

        // 5) Persiste e retorna
        return repo.save(updated);
    }
}