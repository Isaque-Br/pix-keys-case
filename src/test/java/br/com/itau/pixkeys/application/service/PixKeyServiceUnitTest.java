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
import org.junit.jupiter.api.DisplayName;
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

/**
 * Escopo: testes unitários do PixKeyService cobrindo
 * as regras de negócio de criação, atualização e busca por ID.
 *
 * Estratégia:
 * - Usa mocks para isolar o comportamento da camada de serviço (sem dependências externas).
 * - Cobre ramos de sucesso e erro, simulando os cenários de:
 *   (a) falha na validação da chave,
 *   (b) duplicidade,
 *   (c) limite de chaves por conta,
 *   (d) atualização de conta (mesma e diferente),
 *   (e) busca por ID inexistente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PixKeyService: testes unitários de criação, atualização e busca")
class PixKeyServiceUnitTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    KeyValidatorFactory factory;

    @Mock KeyValidator validator;
    @Mock PixKeyRepository repo;

    @InjectMocks PixKeyService service;

    @BeforeEach
    void setup() {
        // Factory sempre devolve o mock de validator
        lenient().when(factory.forType(any())).thenReturn(validator);
        // Validação padrão: não faz nada (cada teste define o comportamento específico)
        lenient().doNothing().when(validator).validate(anyString());
    }

    @Test
    @DisplayName("create(): deve lançar 422 quando a validação Strategy falhar")
    void create_random_invalid_throws422() {
        // DADO: Strategy lança erro de validação
        doThrow(new BusinessRuleViolationException("random inválido: esperado 32 caracteres alfanuméricos"))
                .when(validator).validate("bad");

        // QUANDO + ENTÃO
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.RANDOM, "bad",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // ENTÃO: não deve consultar duplicidade nem salvar
        verify(repo, never()).findByKeyValue(anyString());
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("create(): deve lançar 422 quando chave duplicada (não verifica limite)")
    void create_duplicate_throws422_and_doesNotCheckLimit() {
        // DADO: chave já cadastrada
        when(repo.findByKeyValue("dup@example.com")).thenReturn(Optional.of(mock(PixKey.class)));

        // QUANDO + ENTÃO
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.EMAIL, "dup@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // ENTÃO: não consulta limite nem salva
        verify(repo).findByKeyValue("dup@example.com");
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("create(): deve lançar 422 quando limite por conta atingido")
    void create_limitReached_throws422_and_doesNotSave() {
        // DADO: chave é única, mas limite da conta atingido
        when(repo.findByKeyValue("ok@example.com")).thenReturn(Optional.empty());
        when(repo.countByAgencyAndAccount("1250", "00001234")).thenReturn(5L);

        // QUANDO + ENTÃO
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(
                        KeyType.EMAIL, "ok@example.com",
                        AccountType.CHECKING, "1250", "00001234",
                        "Ana", "Silva"
                )
        );

        // ENTÃO: verificou o limite e não salvou
        verify(repo).countByAgencyAndAccount("1250", "00001234");
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("updateAccount(): deve atualizar conta no mesmo número sem checar limite")
    void updateAccount_sameAccount_ok_noLimitCheck() {
        // DADO: chave existente na mesma conta (não deve consultar limite)
        PixKey current = PixKey.create(
                KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        when(repo.findById("IDX")).thenReturn(Optional.of(current));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // QUANDO: atualiza apenas o tipo da conta
        PixKey updated = service.updateAccount(
                "IDX", AccountType.SAVINGS, "1250", "00001234", "Ana", "Silva"
        );

        // ENTÃO
        assertEquals(AccountType.SAVINGS, updated.accountType());
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
    }

    @Test
    @DisplayName("updateAccount(): deve lançar 422 se destino ultrapassa limite por conta")
    void updateAccount_limitReached_onTarget_throws422() {
        // DADO: chave atual e conta destino já no limite
        PixKey current = PixKey.create(
                KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        when(repo.findById("ID2")).thenReturn(Optional.of(current));
        when(repo.countByAgencyAndAccount("2222", "00002222")).thenReturn(5L);

        // QUANDO + ENTÃO
        assertThrows(BusinessRuleViolationException.class, () ->
                service.updateAccount("ID2", AccountType.SAVINGS, "2222", "00002222", "Ana", "Silva")
        );

        // ENTÃO
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("findById(): deve lançar NotFoundException quando ID inexistente")
    void findById_notFound_404() {
        // DADO
        when(repo.findById("NOPE")).thenReturn(Optional.empty());

        // QUANDO + ENTÃO
        assertThrows(NotFoundException.class, () -> service.findById("NOPE"));
    }
}
