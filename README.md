# API de Chaves PIX

API para cadastro, alteração, consulta e inativação de **chaves PIX**.

## Stack
- **Spring Boot 3** — Web (REST) + Validation (Hibernate Validator) + **Spring Data MongoDB**
- **MongoDB** via Docker Compose (dev) e **Testcontainers** (testes)
- **JUnit 5 + Mockito**; (JaCoCo ≥ 90% será adicionado)
- **springdoc-openapi** (Swagger UI)

## Por que MongoDB?
Modelo de documentos combina com os JSONs da API, esquema flexível (sem migrações) e **índice único** para garantir unicidade da chave. Configuração simples com Spring Data + Docker/Testcontainers.

## ## Padrões de Projeto (Design Patterns)

**Referência:** https://refactoring.guru/

#### Plano:
- **Strategy + Factory (planejado)** — encapsular validações por **tipo de chave** (evita `if/switch`, favorece OCP).
- **Specification-like (planejado)** — consultas **combináveis** no Mongo usando `MongoTemplate + Criteria` (regras: sem inclusão+inativação juntas).
- **Value Object (planejado)** — `Account` (tipo/agência/conta) **imutável** (igualdade por valor).
________________________________________________________________________________________________________________________

## Validação (Smoke)

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI (JSON)**: `http://localhost:8080/v3/api-docs`

**Testes automatizados (classe `OpenApiDocsAvailabilityTest`):**
- `swaggerUi_shouldRespond2xxOr3xx_andHaveLocationWhenRedirects`
    - Verifica que a UI do Swagger responde **2xx** ou **3xx**;
    - Se for **3xx**, confere presença do header **Location** apontando para a UI.
- `apiDocs_shouldContainOpenApiField`
    - Garante **HTTP 200** e que o corpo JSON contém o campo **"openapi"**.

> Estes testes **não dependem do MongoDB**. Nesta classe a auto-configuração do Mongo é **excluída** via:
> `@SpringBootTest(properties="spring.autoconfigure.exclude=...MongoAutoConfiguration,...MongoDataAutoConfiguration")`.

**Como executar:**
```bash
# todos os testes
./mvnw test

# apenas a classe
./mvnw -Dtest=OpenApiDocsAvailabilityTest test

# apenas um método
./mvnw -Dtest=OpenApiDocsAvailabilityTest#swaggerUi_shouldRespond2xxOr3xx_andHaveLocationWhenRedirects test
./mvnw -Dtest=OpenApiDocsAvailabilityTest#apiDocs_shouldContainOpenApiField test
```

### 1) Banco
cd local
docker compose up -d

### 2) App
cd ..
./mvnw spring-boot:run
#### Swagger: http://localhost:8080/swagger-ui/index.html
