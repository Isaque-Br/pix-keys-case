package br.com.itau.pixkeys.api;

import br.com.itau.pixkeys.api.dto.CreatePixKeyRequest;
import br.com.itau.pixkeys.api.dto.CreatePixKeyResponse;
import br.com.itau.pixkeys.application.service.PixKeyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/pix-keys")
public class PixKeyController {

    private final PixKeyService service;

    public PixKeyController(PixKeyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreatePixKeyResponse> create(@Valid @RequestBody CreatePixKeyRequest req) {
        String id = service.create(
                req.keyType(), req.keyValue(),
                req.accountType(), req.agency(), req.account(),
                req.holderName(), req.holderSurname()
        );
        return ResponseEntity
                .created(URI.create("/pix-keys/" + id))
                .body(new CreatePixKeyResponse(id));
    }
}
