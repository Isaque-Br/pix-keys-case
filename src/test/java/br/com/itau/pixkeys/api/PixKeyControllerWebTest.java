package br.com.itau.pixkeys.api;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import br.com.itau.pixkeys.application.service.PixKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PixKeyController.class)
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
}
