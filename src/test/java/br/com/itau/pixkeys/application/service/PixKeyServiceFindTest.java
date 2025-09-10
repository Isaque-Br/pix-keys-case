package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.api.NotFoundException;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyStatus;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PixKeyServiceFindTest {

    KeyValidatorFactory factory = mock(KeyValidatorFactory.class);
    PixKeyRepository repo = mock(PixKeyRepository.class);
    PixKeyService service = new PixKeyService(factory, repo);

    @Test
    void findById_shouldThrowNotFound_whenAbsent() {
        when(repo.findById("nope")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.findById("nope"));
    }

    @Test
    void findById_shouldReturnEntity_whenPresent() {
        var entity = new PixKey(
                "abc-123", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1234", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE,
                Instant.parse("2025-01-01T00:00:00Z"), null
        );
        when(repo.findById("abc-123")).thenReturn(Optional.of(entity));

        var out = service.findById("abc-123");
        assertEquals("abc-123", out.id());
        verify(repo).findById("abc-123");
    }
}
