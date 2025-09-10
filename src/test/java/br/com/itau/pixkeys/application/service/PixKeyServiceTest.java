package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidator;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceTest {

    @Mock KeyValidatorFactory factory;
    @Mock KeyValidator validator;
    @Mock PixKeyRepository repo;

    @InjectMocks PixKeyService service;

    @Test
    void shouldCreate_andReturnId_whenValid_andUnderLimit_andNotDuplicate() {
        // arrange
        when(factory.get(KeyType.EMAIL)).thenReturn(validator); // valida formato
        doNothing().when(validator).validate("ana@example.com");

        when(repo.findByKeyValue("ana@example.com")).thenReturn(Optional.empty()); // não duplicada
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(3L);      // abaixo do limite

        // retorna entidade “salva” com id preenchido
        PixKey persisted = PixKey.create(
                KeyType.EMAIL, "ana@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        // simulamos que o repositório devolveu o mesmo objeto (com id gerado pela factory)
        when(repo.save(any(PixKey.class))).thenReturn(persisted);

        // act
        String id = service.create(
                KeyType.EMAIL, "ana@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );

        // assert
        assertNotNull(id);
        verify(validator).validate("ana@example.com");
        verify(repo).save(any(PixKey.class));
    }

    @Test
    void shouldFail_whenDuplicateKeyValue() {
        when(factory.get(KeyType.EMAIL)).thenReturn(validator);
        doNothing().when(validator).validate("dup@example.com");

        // já existe no banco
        PixKey existing = PixKey.create(
                KeyType.EMAIL, "dup@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        when(repo.findByKeyValue("dup@example.com")).thenReturn(Optional.of(existing));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.create(
                        KeyType.EMAIL, "dup@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );
        assertTrue(ex.getMessage().toLowerCase().contains("já cadastrada"));
        verify(repo, never()).save(any());
    }

    @Test
    void shouldFail_whenAccountLimitReached() {
        when(factory.get(KeyType.PHONE)).thenReturn(validator);
        doNothing().when(validator).validate("+5511999990001");

        when(repo.findByKeyValue("+5511999990001")).thenReturn(Optional.empty());
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(5L); // já no limite

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.create(
                        KeyType.PHONE, "+5511999990001",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );
        assertTrue(ex.getMessage().toLowerCase().contains("limite"));
        verify(repo, never()).save(any());
    }
}
