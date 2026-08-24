# Similar Products API

Solución de la prueba técnica backend de Inditex. La aplicación expone los detalles de los productos similares a uno dado, utilizando las dos APIs externas descritas en [`existingApis.yaml`](existingApis.yaml).

## Stack

- Java 21
- Spring Boot 4.1
- Maven
- Spring MVC `RestClient`
- JUnit 5, AssertJ y RestAssured
- Swagger UI

No se utiliza persistencia. Tampoco se añaden Lombok, MapStruct, WebFlux, Resilience4j ni librerías de mock HTTP: no son necesarias para este caso de uso.

## Ejecutar

Requisitos: Java 21, Maven 3.6.3+ y, para usar los mocks proporcionados, Docker.

Arrancar la API externa simulada:

```bash
docker compose up -d simulado
mvn spring-boot:run
```

La aplicación escucha en `http://localhost:5000`:

```bash
curl http://localhost:5000/product/1/similar
```

Swagger UI está disponible en `http://localhost:5000/swagger-ui.html`. Utiliza directamente [`similarProducts.yaml`](similarProducts.yaml), que es la fuente de verdad API First.

### Docker

```bash
mvn clean package
docker compose up --build app
```

La conexión con la API externa se configura mediante `PRODUCT_API_BASE_URL`,
`PRODUCT_API_CONNECT_TIMEOUT` y `PRODUCT_API_READ_TIMEOUT`. Sus valores por defecto son
`http://localhost:3001`, `1s` y `10s`, respectivamente.

## Tests

Ejecutar todos los tests unitarios y funcionales:

```bash
mvn test
```

Los tests E2E con RestAssured levantan la aplicación en un puerto real y un servidor HTTP externo aislado, sin requerir Docker. Cubren:

1. Respuesta correcta y ordenada, aceptando los IDs numéricos de los mocks.
2. Lista vacía de productos similares.
3. Producto no encontrado.
4. Producto externo sin precio.
5. Error `5xx` de la API externa.
6. Timeout de la API externa.
7. Parámetro vacío.
8. Descarga concurrente conservando el orden de similitud.

La prueba de carga proporcionada se ejecuta con:

```bash
docker compose up -d simulado influxdb grafana
docker compose run --rm k6 run scripts/test.js
```

## Arquitectura

Se utiliza arquitectura hexagonal en un único módulo Maven para mantener el código mínimo:

```text
HTTP request
    -> input REST adapter
    -> GetSimilarProductsUseCase
    -> application service
    -> output ports
    -> existing-products HTTP adapter
    -> external API
```

- `domain`: modelos inmutables e invariantes (`Product`, `ProductId`).
- `application/port/in`: caso de uso ofrecido por la aplicación.
- `application/port/out`: capacidades externas requeridas por el caso de uso.
- `application`: orquestación independiente de Spring y HTTP.
- `infrastructure/input`: endpoint REST, DTO de respuesta y traducción de errores.
- `infrastructure/output`: consumo y validación de las respuestas de la API externa.
- `infrastructure/config`: composición de beans y configuración del cliente.

El dominio y la aplicación no dependen de Spring ni de DTOs HTTP.

## Decisiones

### Concurrencia y orden

Los detalles se solicitan concurrentemente mediante virtual threads de Java 21. Los resultados se recogen en el orden de los IDs de similitud, por lo que finalizar en distinto orden no altera el contrato. Los IDs duplicados se eliminan conservando su primera posición.

### Validación

- `ProductId` rechaza valores nulos o vacíos.
- El adaptador externo valida todos los campos requeridos antes de crear el modelo de dominio.
- `price` utiliza `BigDecimal` y su ausencia genera una respuesta controlada.
- Los mocks devuelven IDs numéricos aunque el OpenAPI declara strings; el adaptador acepta ambos y los normaliza a string.

### Errores

Se aplica una política estricta para no inventar resultados parciales no especificados:

| Situación | HTTP |
|---|---:|
| Parámetro inválido | 400 |
| Producto no encontrado | 404 |
| Producto externo incompleto o API externa en error | 502 |
| Timeout externo | 504 |

La lista vacía es una respuesta válida `200 []`. Los timeouts son configurables; por defecto la conexión espera 1 segundo y la respuesta 10 segundos.
