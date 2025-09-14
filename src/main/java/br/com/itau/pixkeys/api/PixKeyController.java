package br.com.itau.pixkeys.api;

import br.com.itau.pixkeys.api.dto.CreatePixKeyRequest;
import br.com.itau.pixkeys.api.dto.CreatePixKeyResponse;
import br.com.itau.pixkeys.api.dto.PixKeyResponse;
import br.com.itau.pixkeys.api.dto.UpdatePixKeyAccountRequest;
import br.com.itau.pixkeys.application.service.PixKeyService;
import br.com.itau.pixkeys.domain.model.PixKey;
import io.swagger.v3.oas.annotations.Hidden;
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
                .created(URI.create("/pix-keys/" + id))   // seta Location + 201
                .body(new CreatePixKeyResponse(id));
    }

    @PostMapping("/{id}:inactivate")
    public ResponseEntity<PixKeyResponse> inactivate(@PathVariable String id) {
        PixKey updated = service.inactivate(id);
        return ResponseEntity.ok(PixKeyResponse.from(updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PixKeyResponse> getById(@PathVariable String id) {
        var k = service.findById(id);
        return ResponseEntity.ok(PixKeyResponse.from(k));
    }

    @PutMapping("/{id}/account")
    public ResponseEntity<PixKeyResponse> updateAccount(
            @PathVariable String id,
            @Valid @RequestBody UpdatePixKeyAccountRequest req
    ) {
        var updated = service.updateAccount(
                id,
                req.accountType(), req.agency(), req.account(),
                req.holderName(), req.holderSurname()
        );
        return ResponseEntity.ok(PixKeyResponse.from(updated));
    }

    @Hidden
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.inactivate(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
