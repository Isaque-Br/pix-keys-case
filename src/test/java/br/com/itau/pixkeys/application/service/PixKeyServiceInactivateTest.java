package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.api.NotFoundException;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyStatus;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PixKeyService.inactivate: regras de inativação")
class PixKeyServiceInactivateTest {

    @Mock KeyValidatorFactory factory; // não usado na inativação; mantido por dependência do service
    @Mock PixKeyRepository repo;

    @InjectMocks PixKeyService service;

    @Test
    @DisplayName("Deve lançar NotFoundException quando ID não existir")
    void shouldThrowNotFound_whenIdAbsent() {
        // arrange
        when(repo.findById("x")).thenReturn(Optional.empty());

        // act + assert
        assertThrows(NotFoundException.class, () -> service.inactivate("x"));

        // verify
        verify(repo).findById("x");
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(factory);
    }

    @Test
    @DisplayName("Deve lançar 422 quando a chave já estiver INATIVA")
    void shouldThrow422_whenAlreadyInactive() {
        // arrange: chave já inativa no repositório
        PixKey inactive = new PixKey(
                "id1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1234", "00001234",
                "Ana", "Silva", KeyStatus.INACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z")
        );
        when(repo.findById("id1")).thenReturn(Optional.of(inactive));

        // act + assert
        BusinessRuleViolationException ex =
                assertThrows(BusinessRuleViolationException.class, () -> service.inactivate("id1"));
        assertTrue(ex.getMessage() == null || ex.getMessage().toLowerCase().contains("inativ"),
                "mensagem deve indicar que já está inativada");

        // verify
        verify(repo).findById("id1");
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(factory);
    }

    @Test
    @DisplayName("Deve salvar e retornar a chave inativada quando estiver ATIVA")
    void shouldSaveInactive_whenActive() {
        // arrange: chave ativa existente
        PixKey active = new PixKey(
                "id2", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1234", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );
        when(repo.findById("id2")).thenReturn(Optional.of(active));
        when(repo.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        // act
        PixKey result = service.inactivate("id2");

        // assert: status/timestamp
        assertEquals(KeyStatus.INACTIVE, result.status());
        assertNotNull(result.inactivatedAt(), "timestamp de inativação deve ser definido");

        // assert: invariantes preservadas
        ArgumentCaptor<PixKey> captor = ArgumentCaptor.forClass(PixKey.class);
        verify(repo).save(captor.capture());
        PixKey saved = captor.getValue();

        assertEquals("id2", saved.id());
        assertEquals(KeyType.EMAIL, saved.keyType());
        assertEquals("a@b.com", saved.keyValue());
        assertEquals("1234", saved.agency());
        assertEquals("00001234", saved.account());
        assertEquals(AccountType.CHECKING, saved.accountType());
        assertEquals("Ana", saved.holderName());
        assertEquals("Silva", saved.holderSurname());

        // verify
        verify(repo).findById("id2");
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(factory);
    }
}
