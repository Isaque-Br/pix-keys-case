package br.com.itau.pixkeys.infrastructure.repository;

import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PixKeyRepository extends MongoRepository<PixKey, String> {
    Optional<PixKey> findByKeyValue(String keyValue);
    long countByAgencyAndAccount(String agency, String account);
    boolean existsByAgencyAndAccountAndKeyType(String agency, String account, KeyType keyType);
}
