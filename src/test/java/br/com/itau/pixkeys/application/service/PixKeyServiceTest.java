package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidator;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock KeyValidatorFactory factory; // dublê para obter o validador do tipo
    @Mock KeyValidator validator;      // dublê do validador do tipo específico
    @Mock PixKeyRepository repo;       // dublê do repositório (I/O)
    @InjectMocks PixKeyService service; // SUT

    @Test
    void shouldCreate_andReturnId_whenValid_andUnderLimit_andNotDuplicate() {
        // DADO: formato válido
        when(factory.get(KeyType.EMAIL)).thenReturn(validator);
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
        // DADO: formato válido
        when(factory.get(KeyType.EMAIL)).thenReturn(validator);
        doNothing().when(validator).validate("dup@example.com");

        // DADO: já existe chave com o mesmo valor
        PixKey existing = PixKey.create(
                KeyType.EMAIL, "dup@example.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        when(repo.findByKeyValue("dup@example.com")).thenReturn(Optional.of(existing));

        // QUANDO + ENTÃO: falha por duplicidade (422)
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.EMAIL, "dup@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );
        assertTrue(ex.getMessage().toLowerCase().contains("cadastrada"),
                "mensagem deve indicar duplicidade");

        // Recados:
        // - Não deve tentar salvar nada quando há duplicidade
        // - Não deve consultar limite, pois a verificação de unicidade ocorre antes
        verify(repo).findByKeyValue("dup@example.com");
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldFail_whenAccountLimitReached() {
        // DADO: formato válido
        when(factory.get(KeyType.PHONE)).thenReturn(validator);
        doNothing().when(validator).validate("+5511999990001");

        // DADO: não é duplicada
        when(repo.findByKeyValue("+5511999990001")).thenReturn(Optional.empty());

        // DADO: conta já no limite (ex.: 5)
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(5L);

        // QUANDO + ENTÃO: falha por limite atingido (422)
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.PHONE, "+5511999990001",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );
        assertTrue(ex.getMessage().toLowerCase().contains("limite"),
                "mensagem deve indicar limite por conta atingido");

        // Recados:
        // - Em caso de limite atingido, não deve salvar
        verify(repo).findByKeyValue("+5511999990001");
        verify(repo).countByAgencyAndAccount("1250", "00001234");
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
    }
}
