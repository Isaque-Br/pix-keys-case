package br.com.itau.pixkeys.api;

import br.com.itau.pixkeys.api.dto.CreatePixKeyRequest;
import br.com.itau.pixkeys.validation.KeyValidator;
import br.com.itau.pixkeys.validation.SimpleKeyValidatorFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pix-keys")
public class PixKeyController {

    private final SimpleKeyValidatorFactory factory;

    public PixKeyController(SimpleKeyValidatorFactory factory) {
        this.factory = factory;
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CreatePixKeyRequest req) {
        KeyValidator v = factory.get(req.keyType());
        v.validate(req.keyValue());
        return ResponseEntity.noContent().build();
    }
}
