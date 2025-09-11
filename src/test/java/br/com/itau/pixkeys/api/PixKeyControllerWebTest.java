package br.com.itau.pixkeys.api;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import br.com.itau.pixkeys.application.service.PixKeyService;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyStatus;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.domain.model.PixKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PixKeyController.class)
@Import(ApiExceptionHandler.class) // garante o mapeamento 404 no slice
class PixKeyControllerWebTest {

    @Autowired MockMvc mvc;

    @MockitoBean
    PixKeyService service; // <-- necessário no slice

    @Test
    void post_shouldReturn201_andBodyWithId_whenValid() throws Exception {
        when(service.create(any(), anyString(), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn("abc-123");

        mvc.perform(post("/pix-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"keyType":"EMAIL","keyValue":"a@b.com","accountType":"corrente",
                     "agency":"1234","account":"00001234","holderName":"Ana","holderSurname":"Silva"}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/pix-keys/abc-123"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("abc-123"));
    }

    @Test
    void post_shouldReturn400_withFields_whenDtoInvalid() throws Exception {
        // sem stub do service: a validação do @Valid quebra antes (400)
        mvc.perform(post("/pix-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"keyType":null,"keyValue":"","accountType":null,
                     "agency":"12","account":"1","holderName":"","holderSurname":null}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"))
                .andExpect(jsonPath("$.fields.agency").exists())
                .andExpect(jsonPath("$.fields.account").exists());
    }

    @Test
    void post_shouldReturn422_withDetail_whenBusinessRuleFails() throws Exception {
        when(service.create(any(), anyString(), any(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("chave já cadastrada"));

        mvc.perform(post("/pix-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"keyType":"EMAIL","keyValue":"dup@b.com","accountType":"corrente",
                     "agency":"1234","account":"00001234","holderName":"Ana","holderSurname":"Silva"}
                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio inválida"))
                .andExpect(jsonPath("$.detail").value("chave já cadastrada"));
    }

    @Test
    void get_shouldReturn200_andBody_whenFound() throws Exception {
        // dado um PixKey fixo (sem usar .create() para manter determinismo)
        PixKey found = new PixKey(
                "abc",
                KeyType.PHONE,
                "+5511999991234",
                AccountType.CHECKING,
                "1250",
                "00001234",
                "Ana",
                "Silva",
                KeyStatus.ACTIVE,
                Instant.parse("2025-01-01T10:00:00Z"),
                null
        );
        when(service.findById("abc")).thenReturn(found);

        mvc.perform(get("/pix-keys/{id}", "abc").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("abc"))
                .andExpect(jsonPath("$.keyType").value("PHONE"))
                .andExpect(jsonPath("$.keyValue").value("+5511999991234"))
                .andExpect(jsonPath("$.accountType").value("corrente"))
                .andExpect(jsonPath("$.agency").value("1250"))
                .andExpect(jsonPath("$.account").value("00001234"))
                .andExpect(jsonPath("$.holderName").value("Ana"))
                .andExpect(jsonPath("$.holderSurname").value("Silva"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                // datas em ISO-8601; aqui só garantimos que existe
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.inactivatedAt").doesNotExist()); // null não aparece no DTO
    }

    @Test
    void get_shouldReturn404_whenNotFound() throws Exception {
        when(service.findById(anyString()))
                .thenThrow(new NotFoundException("pix key não encontrada: x"));

        mvc.perform(get("/pix-keys/x").accept(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("pix key não encontrada: x"));
    }

    @Test
    void get_shouldCallService_withSamePathId() throws Exception {
        when(service.findById("xyz")).thenThrow(new NotFoundException("pix key não encontrada: xyz"));

        mvc.perform(get("/pix-keys/{id}", "xyz").accept(APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(service).findById("xyz"); // garante o encaminhamento correto do id
    }

    // --------------------------------------------
    // PUT 200 - sucesso
    // --------------------------------------------
    @Test
    void put_shouldReturn200_andUpdatedBody_whenServiceSucceeds() throws Exception {
        // Por quê: comprova contrato HTTP (200) e payload atualizado quando o service devolve a entidade.

        PixKey updated = new PixKey(
                "abc",
                KeyType.EMAIL,
                "a@b.com",
                AccountType.SAVINGS,
                "9999",
                "00009999",
                "Ana Paula",
                "Silva",
                KeyStatus.ACTIVE,
                Instant.parse("2025-01-01T10:00:00Z"),
                null
        );
        when(service.updateAccount(eq("abc"), any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(updated);

        mvc.perform(put("/pix-keys/{id}/account", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"accountType":"poupanca","agency":"9999","account":"00009999","holderName":"Ana Paula","holderSurname":"Silva"}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("abc"))
                .andExpect(jsonPath("$.accountType").value("poupanca"))
                .andExpect(jsonPath("$.agency").value("9999"))
                .andExpect(jsonPath("$.account").value("00009999"))
                .andExpect(jsonPath("$.holderName").value("Ana Paula"))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.inactivatedAt").doesNotExist());
    }

    // --------------------------------------------
    // PUT 404 - id não encontrado
    // --------------------------------------------
    @Test
    void put_shouldReturn404_whenIdNotFound() throws Exception {
        // Por quê: o case exige 404 quando o ID não existe.
        when(service.updateAccount(anyString(), any(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new NotFoundException("pix key não encontrada: xyz"));

        mvc.perform(put("/pix-keys/{id}/account", "xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"accountType":"corrente","agency":"1250","account":"00001234","holderName":"Ana","holderSurname":"Silva"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("pix key não encontrada: xyz"));
    }

    // --------------------------------------------
    // PUT 422 - regra de negócio (limite de chaves)
    // --------------------------------------------
    @Test
    void put_shouldReturn422_whenBusinessRuleFails() throws Exception {
        // Por quê: converte IllegalStateException em 422 via ApiExceptionHandler.
        when(service.updateAccount(anyString(), any(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("limite de chaves por conta atingido"));

        mvc.perform(put("/pix-keys/{id}/account", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"accountType":"poupanca","agency":"9999","account":"00009999","holderName":"Ana","holderSurname":"Silva"}
                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio inválida"))
                .andExpect(jsonPath("$.detail").value("limite de chaves por conta atingido"));
    }
}
