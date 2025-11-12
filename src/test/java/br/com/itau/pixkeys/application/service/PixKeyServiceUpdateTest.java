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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PixKeyService.updateAccount: regras de atualização de conta/titular")
class PixKeyServiceUpdateTest {

    @Mock KeyValidatorFactory factory; // dependência não usada aqui; mantida pelo construtor do service
    @Mock PixKeyRepository repo;
    @InjectMocks PixKeyService service; // SUT

    @Test
    @DisplayName("Deve mover para outra conta quando abaixo do limite e persistir alterações")
    void updateAccount_shouldSave_whenMovingToAnotherAccount_underLimit() {
        // DADO: chave ativa na conta A (1250/00001234)
        PixKey current = new PixKey(
                "k1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE, Instant.now(), null
        );
        when(repo.findById("k1")).thenReturn(Optional.of(current));
        when(repo.countByAgencyAndAccount("2222", "00002222")).thenReturn(0L);
        when(repo.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        // QUANDO: muda para conta B (2222/00002222)
        PixKey out = service.updateAccount("k1", AccountType.SAVINGS,
                "2222", "00002222", "Ana Paula", "Silva");

        // ENTÃO: campos atualizados e persistência efetuada
        assertEquals("2222", out.agency());
        assertEquals("00002222", out.account());
        assertEquals(AccountType.SAVINGS, out.accountType());
        assertEquals("Ana Paula", out.holderName());
        verify(repo).countByAgencyAndAccount("2222", "00002222");
        verify(repo).save(any(PixKey.class));
        verify(repo).findById("k1");
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("Não deve checar limite quando permanecer na mesma agência/conta; apenas atualiza titular")
    void updateAccount_shouldIgnoreLimit_whenStayingInSameAccount_andUpdateHolderName() {
        // DADO: chave ativa na mesma conta (1250/00001234)
        PixKey current = new PixKey(
                "k1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE, Instant.now(), null
        );
        when(repo.findById("k1")).thenReturn(Optional.of(current));
        when(repo.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        // QUANDO: atualiza apenas dados do titular
        PixKey out = service.updateAccount("k1", AccountType.CHECKING,
                "1250", "00001234", "Ana Paula", "Silva");

        // ENTÃO: mantém agência/conta, altera titular e não consulta limite
        assertEquals("1250", out.agency());
        assertEquals("00001234", out.account());
        assertEquals("Ana Paula", out.holderName());
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
        verify(repo).save(any(PixKey.class));
        verify(repo).findById("k1");
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando ID não existir")
    void updateAccount_shouldThrow404_whenIdNotFound() {
        // DADO
        when(repo.findById("x")).thenReturn(Optional.empty());

        // QUANDO + ENTÃO
        assertThrows(NotFoundException.class, () ->
                service.updateAccount("x", AccountType.CHECKING, "1250", "00001234", "Ana", "Silva"));
        verify(repo).findById("x");
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("Deve lançar 422 quando o destino já atingiu o limite por conta")
    void updateAccount_shouldThrow422_whenLimitReachedOnTarget() {
        // DADO: chave ativa e conta destino no limite
        PixKey current = new PixKey(
                "k1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE, Instant.now(), null
        );
        when(repo.findById("k1")).thenReturn(Optional.of(current));
        when(repo.countByAgencyAndAccount("2222", "00002222")).thenReturn(5L);

        // QUANDO + ENTÃO
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () ->
                service.updateAccount("k1", AccountType.SAVINGS, "2222", "00002222", "Ana Paula", "Silva"));
        assertTrue(ex.getMessage() == null || ex.getMessage().toLowerCase().contains("limite"));
        verify(repo).findById("k1");
        verify(repo).countByAgencyAndAccount("2222", "00002222");
        verify(repo, never()).save(any());
        verifyNoMoreInteractions(repo);
    }

    @Test
    @DisplayName("Deve lançar 422 quando a chave estiver INATIVA (bloqueia atualização)")
    void updateAccount_shouldThrow422_whenCurrentKeyIsInactive() {
        // DADO: chave inativa
        PixKey inactive = new PixKey(
                "k1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva", KeyStatus.INACTIVE, Instant.now(), Instant.now()
        );
        when(repo.findById("k1")).thenReturn(Optional.of(inactive));

        // então: domínio barra atualização de chave inativa
        assertThrows(BusinessRuleViolationException.class, () ->
                service.updateAccount("k1", AccountType.SAVINGS, "2222", "00002222", "Ana Paula", "Silva"));

        //  verifique as interações esperadas
        verify(repo).findById("k1");

        // ❌ essas chamadas não devem ocorrer quando a chave está INACTIVE
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
        verify(repo, never()).save(any());

        // agora sim podemos afirmar que não há outras interações
        verifyNoMoreInteractions(repo);
    }
}
