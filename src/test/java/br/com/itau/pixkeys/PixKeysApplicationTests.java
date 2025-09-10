package br.com.itau.pixkeys;

import br.com.itau.pixkeys.infrastructure.repository.PixKeyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"spring.autoconfigure.exclude=" +
						"org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
						"org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
		}
)
class PixKeysApplicationTests {

	// Mocka o repo para o PixKeyService poder ser criado sem Mongo
	@MockitoBean
	PixKeyRepository repo;

	@Test
	void contextLoads() {
	}
}
