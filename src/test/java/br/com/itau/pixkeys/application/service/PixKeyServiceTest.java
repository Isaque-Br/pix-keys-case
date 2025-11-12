package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidator;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Escopo: testar as regras de criação de uma nova chave Pix (create).
 *
 * Cenários cobertos:
 *  - Criação bem-sucedida (campos válidos, chave única, dentro do limite).
 *  - Falha por duplicidade de chave (unicidade global).
 *  - Falha por limite máximo de chaves por conta atingido.
 *
 * Estratégia: teste unitário isolado da camada de serviço,
 * com dependências simuladas (mocks do Repository e do Factory).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PixKeyService.create: regras de criação de chave Pix")
class PixKeyServiceCreateTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    KeyValidatorFactory factory;

    @Mock KeyValidator validator;
    @Mock PixKeyRepository repo;

    @InjectMocks PixKeyService service;

    @BeforeEach
    void setup() {
        lenient().when(factory.forType(any())).thenReturn(validator);
        lenient().doNothing().when(validator).validate(anyString());
    }

    @Test
    @DisplayName("Deve criar e retornar ID quando válida, única e abaixo do limite")
    void shouldCreate_andReturnId_whenValid_andUnderLimit_andNotDuplicate() {
        // DADO: formato válido e chave única
        doNothing().when(validator).validate("ana@example.com");
        when(repo.findByKeyValue("ana@example.com")).thenReturn(Optional.empty());
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(3L);

        // DADO: mock salva e devolve a própria entidade criada
        when(repo.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        // QUANDO: cria chave Pix
        String id = service.create(
                KeyType.EMAIL, "ana@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );

        // ENTÃO: id retornado e campos persistidos corretos
        assertNotNull(id, "o id gerado não deve ser nulo");

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

        // Verifica interações esperadas
        verify(validator).validate("ana@example.com");
        verify(repo).findByKeyValue("ana@example.com");
        verify(repo).countByAgencyAndAccount("1250", "00001234");
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("Deve lançar exceção quando chave já existir (duplicada)")
    void shouldFail_whenDuplicateKeyValue() {
        // DADO: chave duplicada já existente
        PixKey existing = PixKey.create(
                KeyType.EMAIL, "dup@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        when(repo.findByKeyValue("dup@example.com")).thenReturn(Optional.of(existing));

        // QUANDO + ENTÃO: falha por regra de unicidade global
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.EMAIL, "dup@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // ENTÃO: não verifica limite nem salva
        verify(repo).findByKeyValue("dup@example.com");
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando limite máximo de chaves por conta for atingido")
    void shouldFail_whenAccountLimitReached() {
        // DADO: chave única mas limite atingido
        when(repo.findByKeyValue("+5511999990001")).thenReturn(Optional.empty());
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(5L); // limite atingido

        // QUANDO + ENTÃO: falha por regra de negócio
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.PHONE, "+5511999990001",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // ENTÃO: não salva no repositório
        verify(repo).findByKeyValue("+5511999990001");
        verify(repo).countByAgencyAndAccount("1250", "00001234");
        verify(repo, never()).save(any());
    }
}
