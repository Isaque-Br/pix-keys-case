# API de Chaves PIX

API para cadastro, alteração, consulta e inativação de **chaves PIX**.

## Stack
- **Spring Boot 3** — Web (REST) + Validation (Hibernate Validator) + **Spring Data MongoDB**
- **MongoDB** via Docker Compose (dev) e **Testcontainers** (testes)
- **JUnit 5 + Mockito**; (JaCoCo ≥ 90% será adicionado)
- **springdoc-openapi** (Swagger UI)

## Por que MongoDB?
Modelo de documentos combina com os JSONs da API, esquema flexível (sem migrações) e **índice único** para garantir unicidade da chave. Configuração simples com Spring Data + Docker/Testcontainers.

## Padrões de Projeto (Design Patterns)

**Referência:** https://refactoring.guru/

#### Plano Principal:
- **Strategy + Factory**  
  **Por que:** as regras de validação mudam por **tipo de chave** (telefone, e-mail, CPF, CNPJ, aleatória).  
  **Como aplica:** cada tipo tem um **validador** (uma Strategy) com suas regras; uma **Factory** entrega a Strategy certa a partir do `KeyType`.  
  **Benefícios:** elimina `if/switch`, facilita **OCP** (abertura para novos tipos), deixa a **regra testável** por unidade (um teste por Strategy).

  ### Suporte
- **Specification-like (Criteria)**  
  **Por que:** o case pede **consultas combináveis** (tipo, agência+conta, nome, data de inclusão **ou** de inativação), e no Mongo **não** há `JpaSpecification`.  
  **Como aplica:** montamos `Query` com `Criteria` (via `MongoTemplate`) conforme filtros válidos, incluindo as restrições do case (ex.: **não** combinar inclusão **e** inativação).  
  **Benefícios:** consultas expressivas sem `if` bagunçado no repositório; regras de combinação ficam centralizadas.
_________________________________________________________________________________________________________________________________________________________________________

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
