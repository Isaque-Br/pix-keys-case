package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.api.NotFoundException;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
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
        // 1) Validação de formato/semântica
        if (keyType == KeyType.RANDOM) {
            // 32 caracteres alfanuméricos (A–Z, a–z, 0–9)
            if (keyValue == null || !keyValue.matches("^[A-Za-z0-9]{32}$")) {
                throw new BusinessRuleViolationException("random inválido: esperado 32 caracteres alfanuméricos");
            }
        } else {
            factory.get(keyType).validate(keyValue);
        }

        // 2) Unicidade global do valor da chave
        if (repo.findByKeyValue(keyValue).isPresent()) {
            throw new BusinessRuleViolationException("chave já cadastrada para outro correntista");
        }

        // 3) Limite por conta
        final int LIMIT = 5;
        long current = repo.countByAgencyAndAccount(agency, account);
        if (current >= LIMIT) {
            throw new BusinessRuleViolationException("limite de chaves por conta atingido");
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

    /** Inativa (soft delete) a chave. Lança 404 se não existir e 422 se já estiver inativa. */
    public PixKey inactivate(String id) {
        PixKey current = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("pix key não encontrada: " + id));

        if (current.isInactive()) {
            throw new BusinessRuleViolationException("chave já inativada");
        }

        PixKey updated = current.inactivate();
        return repo.save(updated);
    }

    public PixKey updateAccount(
            String id,
            AccountType accountType,
            String agency,
            String account,
            String holderName,
            String holderSurname
    ) {
        // 1) Carrega ou 404
        PixKey current = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("pix key não encontrada: " + id));

        // 2) Regras de negócio de limite por conta
        boolean sameAccount = agency.equals(current.agency()) && account.equals(current.account());
        if (!sameAccount) {
            final int LIMIT = 5; // TODO: quando houver HolderType (PF/PJ), usar PF=5 / PJ=20
            long countAtTarget = repo.countByAgencyAndAccount(agency, account);
            if (countAtTarget >= LIMIT) {
                throw new BusinessRuleViolationException("limite de chaves por conta atingido");
            }
        }

        // 3) Atualiza no domínio (valida inatividade)
        PixKey updated = current.updateAccount(accountType, agency, account, holderName, holderSurname);

        // 4) Persiste e retorna entidade
        return repo.save(updated);
    }
}
