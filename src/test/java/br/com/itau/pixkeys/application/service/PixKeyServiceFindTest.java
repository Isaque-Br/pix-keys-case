package br.com.itau.pixkeys.application.service;

import br.com.itau.pixkeys.api.NotFoundException;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyStatus;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import br.com.itau.pixkeys.validation.KeyValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testa o comportamento do método findById() da classe PixKeyService.
 * Garante que a busca por ID funciona corretamente, cobrindo:
 *  - cenário em que a chave não é encontrada (lança NotFoundException)
 *  - cenário em que a chave existe (retorna a entidade corretamente)
 */
class PixKeyServiceFindTest {

    // Mocks para simular dependências externas
    KeyValidatorFactory factory = mock(KeyValidatorFactory.class);
    PixKeyRepository repo = mock(PixKeyRepository.class);

    // Instância do serviço com mocks injetados
    PixKeyService service = new PixKeyService(factory, repo);

    @Test
    @DisplayName("Deve lançar NotFoundException quando ID não for encontrado")
    void findById_shouldThrowNotFound_whenAbsent() {
        // Arrange
        when(repo.findById("nope")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> service.findById("nope"));

        // Verifica que o método de repositório foi realmente chamado
        verify(repo).findById("nope");
    }

    @Test
    @DisplayName("Deve retornar a entidade quando o ID existir no repositório")
    void findById_shouldReturnEntity_whenPresent() {
        // Arrange: cria uma entidade PixKey simulada
        var entity = new PixKey(
                "abc-123",
                KeyType.EMAIL,
                "a@b.com",
                AccountType.CHECKING,
                "1234",
                "00001234",
                "Ana",
                "Silva",
                KeyStatus.ACTIVE,
                Instant.parse("2025-01-01T00:00:00Z"),
                null
        );

        when(repo.findById("abc-123")).thenReturn(Optional.of(entity));

        // Act
        var result = service.findById("abc-123");

        // Assert
        assertEquals("abc-123", result.id());
        assertEquals(KeyType.EMAIL, result.keyType());
        verify(repo).findById("abc-123");
    }
}
