package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.api.NotFoundException;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidator;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceUnitTest {

    @Mock KeyValidatorFactory factory;
    @Mock KeyValidator validator;
    @Mock PixKeyRepository repo;

    PixKeyService service;

    @BeforeEach
    void setup() {
        service = new PixKeyService(factory, repo);
    }

    // --- create(): cobre RANDOM (ramo true) ---
    @Test
    void create_random_invalid_throws422() {
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () ->
                service.create(KeyType.RANDOM, "abc", // não bate o regex dos 32 alfanuméricos
                        AccountType.CHECKING, "1250", "00001234", "Ana", "Silva")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("random inválido"));
        verifyNoInteractions(factory); // não deve chamar validators normais
        verify(repo, never()).save(any());
    }

    // --- create(): cobre duplicidade (isPresent() == true) ---
    @Test
    void create_duplicate_throws422_and_doesNotCheckLimit() {
        when(factory.get(any())).thenReturn(validator);
        doNothing().when(validator).validate(anyString());
        when(repo.findByKeyValue("dup@x.com")).thenReturn(Optional.of(
                PixKey.create(KeyType.EMAIL, "dup@x.com",
                        AccountType.CHECKING, "1250", "00001234", "Ana", "Silva")
        ));

        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(KeyType.EMAIL, "dup@x.com",
                        AccountType.CHECKING, "1250", "00001234", "Ana", "Silva"));

        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
        verify(repo, never()).save(any());
    }

    // --- create(): cobre limite (current >= LIMIT) ---
    @Test
    void create_limitReached_throws422_and_doesNotSave() {
        when(factory.get(any())).thenReturn(validator);
        doNothing().when(validator).validate(anyString());
        when(repo.findByKeyValue("ok@x.com")).thenReturn(Optional.empty());
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(5L); // >= 5

        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(KeyType.EMAIL, "ok@x.com",
                        AccountType.CHECKING, "1250", "00001234", "Ana", "Silva"));

        verify(repo, never()).save(any());
    }

    // --- updateAccount(): cobre sameAccount == true (não checa limite) ---
    @Test
    void updateAccount_sameAccount_ok_noLimitCheck() {
        PixKey current = PixKey.create(
                KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234", "Ana", "Silva"
        );
        when(repo.findById("IDX")).thenReturn(Optional.of(current));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PixKey updated = service.updateAccount(
                "IDX", AccountType.SAVINGS, "1250", "00001234", "Ana", "Silva"
        );

        assertEquals(AccountType.SAVINGS, updated.accountType());
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
    }

    // --- updateAccount(): cobre !sameAccount e (countAtTarget >= LIMIT) ---
    @Test
    void updateAccount_limitReached_onTarget_throws422() {
        PixKey current = PixKey.create(
                KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234", "Ana", "Silva"
        );
        when(repo.findById("ID2")).thenReturn(Optional.of(current));
        when(repo.countByAgencyAndAccount("2222", "00002222")).thenReturn(5L); // >= 5

        assertThrows(BusinessRuleViolationException.class, () ->
                service.updateAccount("ID2", AccountType.SAVINGS, "2222", "00002222", "Ana", "Silva"));

        verify(repo, never()).save(any());
    }

    // --- findById(): ramo not found ---
    @Test
    void findById_notFound_404() {
        when(repo.findById("NOPE")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.findById("NOPE"));
    }
}
