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
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceUnitTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS) KeyValidatorFactory factory;
    @Mock KeyValidator validator;
    @Mock PixKeyRepository repo;

    @InjectMocks
    PixKeyService service;

    @BeforeEach
    void setup() {
        // deixa o factory sempre devolver o mock de validator
        lenient().when(factory.forType(any())).thenReturn(validator);
        // por padrão, validação é no-op (cada teste pode sobrescrever)
        lenient().doNothing().when(validator).validate(anyString());
    }

    @Test
    void create_random_invalid_throws422() {
        // quando a Strategy validar a chave "bad", lança 422
        doThrow(new BusinessRuleViolationException(
                "random inválido: esperado 32 caracteres alfanuméricos"
        )).when(validator).validate("bad");

        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.RANDOM, "bad",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // não deve consultar duplicidade nem salvar quando falha na validação
        verify(repo, never()).findByKeyValue(anyString());
        verify(repo, never()).save(any());
    }

    @Test
    void create_duplicate_throws422_and_doesNotCheckLimit() {
        // DADO: já existe chave com esse valor
        when(repo.findByKeyValue("dup@example.com"))
                .thenReturn(Optional.of(mock(PixKey.class)));

        // QUANDO + ENTÃO: estoura 422 por duplicidade
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.EMAIL, "dup@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // não consulta limite nem tenta salvar
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
        verify(repo, never()).save(any());
    }


    @Test
    void create_limitReached_throws422_and_doesNotSave() {
        // DADO: não é duplicada, mas a conta já atingiu o limite
        when(repo.findByKeyValue("ok@example.com")).thenReturn(Optional.empty());
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(5L);

        // QUANDO + ENTÃO: estoura 422 por limite
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.EMAIL, "ok@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // confirma que consultou o limite e não salvou
        verify(repo).countByAgencyAndAccount("1250", "00001234");
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
