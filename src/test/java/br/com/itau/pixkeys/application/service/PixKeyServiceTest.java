package br.com.itau.pixkeys.application.service;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Escopo: testar regras de criação (create) no service.
 * Convenções:
 * - Mensagens de erro em PT-BR
 * - Exceções de regra de negócio: BusinessRuleViolationException (422).
 * - DADO / QUANDO / ENTÃO nos comentários para leitura rápida.
 */
@ExtendWith(MockitoExtension.class)
class PixKeyServiceTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS) KeyValidatorFactory factory;
    @Mock KeyValidator validator;
    @Mock PixKeyRepository repo;
    @InjectMocks PixKeyService service;

    @BeforeEach
    void setup() {
        lenient().when(factory.forType(any())).thenReturn(validator);
        lenient().doNothing().when(validator).validate(anyString());
    }

    @Test
    void shouldCreate_andReturnId_whenValid_andUnderLimit_andNotDuplicate() {
        // DADO: formato válido
        doNothing().when(validator).validate("ana@example.com");

        // DADO: não é duplicada e está abaixo do limite por conta
        when(repo.findByKeyValue("ana@example.com")).thenReturn(Optional.empty());
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(3L);

        // DADO: ecoa o objeto salvo (permite checar o que o service construiu)
        when(repo.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        // QUANDO: cria
        String id = service.create(
                KeyType.EMAIL, "ana@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );

        // ENTÃO: retorna id e persiste com os campos esperados
        assertNotNull(id, "o id gerado pela fábrica do domínio não deve ser nulo");

        // Captura o PixKey enviado ao save para validar campos (opcional, mas didático)
        ArgumentCaptor<PixKey> captor = ArgumentCaptor.forClass(PixKey.class);
        verify(repo).save(captor.capture());
        PixKey saved = captor.getValue();

        assertAll("campos persistidos",
                () -> assertEquals(KeyType.EMAIL, saved.keyType()),
                () -> assertEquals("ana@example.com", saved.keyValue()),
                () -> assertEquals(AccountType.CHECKING, saved.accountType()),
                () -> assertEquals("1250", saved.agency()),
                () -> assertEquals("00001234", saved.account()),
                () -> assertEquals("Ana", saved.holderName()),
                () -> assertEquals("Silva", saved.holderSurname())
        );

        // Interações essenciais
        verify(validator).validate("ana@example.com");
        verify(repo).findByKeyValue("ana@example.com");
        verify(repo).countByAgencyAndAccount("1250", "00001234");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldFail_whenDuplicateKeyValue() {
        // dado
        PixKey existing = PixKey.create(
                KeyType.EMAIL, "dup@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        // só isso é necessário para este cenário:
        when(repo.findByKeyValue("dup@example.com")).thenReturn(Optional.of(existing));

        // quando + então
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.EMAIL, "dup@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // não consulta limite nem salva
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
        verify(repo, never()).save(any());
    }

    @Test
    void shouldFail_whenAccountLimitReached() {
        // dado
        when(repo.findByKeyValue("+5511999990001")).thenReturn(Optional.empty());
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(5L); // atinge limite

        // quando + então
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.PHONE, "+5511999990001",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // não salva quando estoura limite
        verify(repo, never()).save(any());
    }
}
