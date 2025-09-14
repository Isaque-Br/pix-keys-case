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
class PixKeyServiceInactivateTest {

    @Mock KeyValidatorFactory factory;              // não usado aqui, mas exigido pelo ctor do service
    @Mock PixKeyRepository repo;

    @InjectMocks PixKeyService service;

    @Test
    void shouldThrowNotFound_whenIdAbsent() {
        // DADO: id inexistente no repositório
        when(repo.findById("x")).thenReturn(Optional.empty());

        // QUANDO + ENTÃO: service.inactivate lança 404 e não persiste
        assertThrows(NotFoundException.class, () -> service.inactivate("x"));

        // ENTÃO: interações esperadas
        verify(repo).findById("x");
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
        // bônus: garantir que a validação (Strategy) não é acionada na inativação
        verifyNoInteractions(factory);
    }

    @Test
    void shouldThrow422_whenAlreadyInactive() {
        // DADO: chave já inativa no repositório
        PixKey inactive = new PixKey(
                "id1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1234", "00001234",
                "Ana", "Silva", KeyStatus.INACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"), // criado em data fixa
                Instant.parse("2024-01-02T00:00:00Z")  // já estava inativa
        );
        when(repo.findById("id1")).thenReturn(Optional.of(inactive));

        // QUANDO + ENTÃO: service deve falhar com 422 (regra de negócio)
        BusinessRuleViolationException ex =
                assertThrows(BusinessRuleViolationException.class, () -> service.inactivate("id1"));

        // ENTÃO: mensagem coerente com a regra
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().toLowerCase().contains("inativ"));

        // ENTÃO: interações esperadas
        verify(repo).findById("id1");
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(factory);
    }

    @Test
    void shouldSaveInactive_whenActive() {
        // DADO: chave ativa existente
        PixKey active = new PixKey(
                "id2", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1234", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE,
                Instant.parse("2024-01-01T00:00:00Z"),
                null
        );
        when(repo.findById("id2")).thenReturn(Optional.of(active));
        when(repo.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        // QUANDO: inativo a chave
        PixKey result = service.inactivate("id2");

        // ENTÃO: status e timestamp de inativação definidos
        assertEquals(KeyStatus.INACTIVE, result.status());
        assertNotNull(result.inactivatedAt());

        // ENTÃO: captura o objeto salvo e valida invariantes (nada “muda indevidamente”)
        ArgumentCaptor<PixKey> captor = ArgumentCaptor.forClass(PixKey.class);
        verify(repo).save(captor.capture());
        PixKey saved = captor.getValue();

        assertEquals("id2", saved.id());                      // id preservado
        assertEquals(KeyType.EMAIL, saved.keyType());         // tipo preservado
        assertEquals("a@b.com", saved.keyValue());            // valor preservado
        assertEquals("1234", saved.agency());                 // agência preservada
        assertEquals("00001234", saved.account());            // conta preservada
        assertEquals(AccountType.CHECKING, saved.accountType());
        assertEquals("Ana", saved.holderName());
        assertEquals("Silva", saved.holderSurname());

        // ENTÃO: interações esperadas
        verify(repo).findById("id2");
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(factory);
    }
}