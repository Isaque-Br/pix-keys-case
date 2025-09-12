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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceUpdateTest {

    @Mock KeyValidatorFactory factory; // injeta dependência não usada aqui
    @Mock PixKeyRepository repo;       // mock do repositório
    @InjectMocks PixKeyService service; // SUT (System Under Test)

    @Test
    void updateAccount_shouldSave_whenMovingToAnotherAccount_underLimit() {
        // dado: chave ativa na conta A (1250/00001234)
        PixKey current = new PixKey("k1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE, Instant.now(), null);
        when(repo.findById("k1")).thenReturn(Optional.of(current)); // 1) encontra e retorna a chave atual
        when(repo.countByAgencyAndAccount("2222", "00002222")).thenReturn(0L); // 2) destino abaixo do limite
        when(repo.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0)); // 4) ecoa salvo

        // quando: muda para conta B (2222/00002222)
        PixKey out = service.updateAccount("k1", AccountType.SAVINGS,
                "2222", "00002222", "Ana Paula", "Silva");

        // então: salvou com novos dados
        assertEquals("2222", out.agency());                   // agência mudou
        assertEquals("00002222", out.account());              // conta mudou
        assertEquals(AccountType.SAVINGS, out.accountType()); // tipo mudou
        assertEquals("Ana Paula", out.holderName());          // titular atualizado
        verify(repo).save(any(PixKey.class));                 // persistiu alteração
    }

    @Test
    void updateAccount_shouldIgnoreLimit_whenStayingInSameAccount_andUpdateHolderName() {
        // DADO: chave ativa na mesma conta (1250/00001234)
        PixKey current = new PixKey("k1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE, Instant.now(), null);
        when(repo.findById("k1")).thenReturn(Optional.of(current));

        // NÃO consulta o limite quando é a mesma conta
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());

        when(repo.save(any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        // QUANDO: atualizo só o nome do titular, mantendo a mesma agência/conta
        PixKey out = service.updateAccount("k1", AccountType.CHECKING,
                "1250", "00001234", "Ana Paula", "Silva");

        // ENTÃO: não bloqueia por limite e persiste a alteração de titular
        assertEquals("1250", out.agency());
        assertEquals("00001234", out.account());
        assertEquals("Ana Paula", out.holderName());
        verify(repo).save(any(PixKey.class));

        // confirma que não consultou o limite:
        verify(repo, never()).countByAgencyAndAccount(anyString(), anyString());
    }

    @Test
    void updateAccount_shouldThrow404_whenIdNotFound() {
        // dado: repo não encontra
        when(repo.findById("x")).thenReturn(Optional.empty());

        // então: lança NotFoundException
        assertThrows(NotFoundException.class, () ->
                service.updateAccount("x", AccountType.CHECKING, "1250", "00001234", "Ana", "Silva"));

        // e não tenta salvar
        verify(repo, never()).save(any());
    }

    @Test
    void updateAccount_shouldThrow422_whenLimitReachedOnTarget() {
        // dado: chave ativa na conta A
        PixKey current = new PixKey("k1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva", KeyStatus.ACTIVE, Instant.now(), null);
        when(repo.findById("k1")).thenReturn(Optional.of(current)); // 1) encontra
        when(repo.countByAgencyAndAccount("2222", "00002222")).thenReturn(5L); // 2) destino no limite

        // então: 422 por “limite de chaves por conta atingido”
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () ->
                service.updateAccount("k1", AccountType.SAVINGS,
                        "2222", "00002222", "Ana Paula", "Silva"));
        assertTrue(ex.getMessage().toLowerCase().contains("limite"));
        verify(repo, never()).save(any());
    }

    @Test
    void updateAccount_shouldThrow422_whenCurrentKeyIsInactive() {
        // dado: chave inativa
        PixKey inactive = new PixKey("k1", KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva", KeyStatus.INACTIVE, Instant.now(), Instant.now());
        when(repo.findById("k1")).thenReturn(Optional.of(inactive)); // 1) encontra, mas INACTIVE

        // então: domínio barra atualização de chave inativa
        assertThrows(BusinessRuleViolationException.class, () ->
                service.updateAccount("k1", AccountType.SAVINGS,
                        "2222", "00002222", "Ana Paula", "Silva"));
        verify(repo, never()).save(any());
    }
}
