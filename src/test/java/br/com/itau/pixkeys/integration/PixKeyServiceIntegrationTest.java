package br.com.itau.pixkeys.integration;

import br.com.itau.pixkeys.application.service.PixKeyService;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
class PixKeyServiceIntegrationTest {

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.14");;

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        // entrega host/porta do container
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
        // e define explicitamente o database (resolve o erro “Database name must not be empty”)
        registry.add("spring.data.mongodb.database", () -> "pixkeys_it");
        // cria índices automaticamente nos testes
        registry.add("spring.data.mongodb.auto-index-creation", () -> true);
    }

    @Autowired PixKeyService service;
    @Autowired PixKeyRepository repo;

    @Test
    void create_then_findById_ok() {
        String id = service.create(
                KeyType.EMAIL, "it-int@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        assertNotNull(id);
        PixKey found = service.findById(id);
        assertEquals("it-int@example.com", found.keyValue());
    }

    @Test
    void shouldReject_duplicateValue() {
        // primeiro cadastro
        service.create(
                KeyType.EMAIL, "dup-int@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        // duplicidade
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.EMAIL, "dup-int@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );
        assertEquals("chave já cadastrada para outro correntista", ex.getMessage());
    }

    @Test
    void shouldReject_limitReached_onTargetAccount() {
        // simula conta no limite (5) criando 5 chaves diferentes
        for (int i = 0; i < 5; i++) {
            service.create(
                    KeyType.EMAIL, "limit"+i+"@example.com",
                    AccountType.CHECKING, "1250", "00001234",
                    "Ana", "Silva"
            );
        }
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.EMAIL, "over@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );
        assertEquals("limite de chaves por conta atingido", ex.getMessage());
    }

    @Test
    void inactivate_then_block_update() {
        String id = service.create(
                KeyType.EMAIL, "block@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        service.inactivate(id);
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () ->
                service.updateAccount(id, AccountType.SAVINGS, "2222", "00002222", "Ana", "Silva")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("inativ"));
    }
}
