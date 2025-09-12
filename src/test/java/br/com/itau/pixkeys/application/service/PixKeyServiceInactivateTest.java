package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.api.NotFoundException;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyStatus;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceInactivateTest {

    @Mock KeyValidatorFactory factory;              // não usado aqui, mas exigido pelo ctor do service
    @Mock PixKeyRepository repo;

    @InjectMocks PixKeyService service;

    @Test
    void shouldThrowNotFound_whenIdAbsent() {
        when(repo.findById("x")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.inactivate("x"));

        verify(repo, never()).save(any());
    }

    @Test
    void shouldThrow422_whenAlreadyInactive() {
        // DADO: chave já inativa no repositório
        PixKey inactive = new PixKey(
                "id1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1234", "00001234",
                "Ana", "Silva", KeyStatus.INACTIVE,
                Instant.now(), Instant.now()
        );
        when(repo.findById("id1")).thenReturn(Optional.of(inactive));

        // QUANDO + ENTÃO: service deve falhar com 422 (exceção de regra de negócio)
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.inactivate("id1"));

        // Mensagem coerente com a regra
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().toLowerCase().contains("inativ"));

        // Interações esperadas
        verify(repo).findById("id1");
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldSaveInactive_whenActive() {
        PixKey active = new PixKey(
                "id2", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1234", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE,
                Instant.now(), null
        );
        when(repo.findById("id2")).thenReturn(Optional.of(active));
        when(repo.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        PixKey result = service.inactivate("id2");

        assertEquals(KeyStatus.INACTIVE, result.status());
        assertNotNull(result.inactivatedAt());
        verify(repo).save(any(PixKey.class));
    }
}
