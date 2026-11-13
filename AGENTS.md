# AGENTS.md

Quarkus 3.40.1.2 / Java 21 demo: **MCP server** (Model Context Protocol) construido con **Clean Architecture** en modo **imperativo**, expuesto por **Streamable HTTP** en `/mcp` y desplegado en **Google Cloud Run** como **imagen nativa** (Mandrel 25). Es un servidor MCP — no hay entry point CLI. Maven wrapper incluido (no requiere Maven del sistema). **No hay tests automatizados por decisión de alcance.**

## Coordenadas

- `groupId` `pe.com.incoda`, `artifactId` `mcp-serverless-demo`, `BASE_PACKAGE` `pe.com.incoda.mcpserverlessdemo`.
- Quarkus `3.40.1.2` (LTS) + `io.quarkiverse.mcp:quarkus-mcp-server-http:2.1.0.CR1`.
- GCP: proyecto `training-lab-504513`, región `us-central1`, servicio Cloud Run `mcp-serverless-demo`.
- Imagen: `us-central1-docker.pkg.dev/training-lab-504513/mcp-serverless-demo/mcp-serverless-demo`.

## Commands

Windows: `mvnw.cmd`; Unix: `./mvnw`.

- Dev mode (live reload, Dev UI en http://localhost:8080/q/dev/): `mvnw.cmd quarkus:dev`
- Build: `mvnw.cmd package` → `target/quarkus-app/quarkus-run.jar`
- Build imagen JVM: `docker build -f docker/Dockerfile.jvm -t mcp-serverless-demo:jvm .`
- Build imagen **nativa**: `docker build -f docker/Dockerfile.native -t mcp-serverless-demo:native .`
- Deploy manual: ver `README.md` §5.1 (Artifact Registry + `gcloud run deploy`).

`package` no ejecuta tests porque se removieron `quarkus-junit`/`rest-assured` y `src/test` no existe. No agregar tests salvo pedido explícito.

**Requisito de nativa:** Quarkus 3.40 exige **GraalVM/Mandrel 25**. El `Dockerfile.native` usa `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25`. Si tu `JAVA_HOME` es GraalVM 21, `mvnw.cmd package -Pnative` falla: compilá la nativa con Docker.

## Architecture

Root package: `pe.com.incoda.mcpserverlessdemo`.

- `domain/model` — records puros, sin anotaciones de framework (`LoanRequest`, `LoanQuote`, `ExchangeQuote`).
- `domain/service` — `LoanCalculator` (servicio de dominio **puro**, sin I/O) y `CurrencyRateProvider` (puerto de salida: `ExchangeQuote fetchRate(base, target)`).
- `application` — casos de uso (`LoanQuoteUseCase`, `CurrencyQuoteUseCase`); dependen solo de `domain`.
- `infrastructure/input/mcp` — `FinanceMcpTools` (`@Singleton`) con los `@Tool`.
- `infrastructure/output/client` — `CurrencyRateApi` (REST Client), `CurrencyRateClient` (implementa el puerto), `CurrencyRateResponse` (shape de la API).

Reglas: `domain`/`application` **nunca** importan `infrastructure`; solo `infrastructure` conoce detalles HTTP/JSON; el DTO externo (`CurrencyRateResponse`) no se filtra al dominio (se mapea a `ExchangeQuote` en el adapter). Comentarios/Javadoc en español.

## MCP

- Extension: `io.quarkiverse.mcp:quarkus-mcp-server-http`. Trae `quarkus-vertx-http` + `quarkus-mcp-server-core`.
- Endpoint Streamable HTTP: `POST /mcp`. SSE legacy (`/mcp/sse`) no se usa (deprecado en MCP 2025-03-26).
- Anotaciones en `io.quarkiverse.mcp.server`: `@Tool(name, description, annotations)`, `@ToolArg(name, description)`, `TextContent`, `ToolResponse.success(...)/error(...)`. Los beans no necesitan `@Singleton` (se agrega si falta), pero acá es explícito.
- Tools: `calcular_cuota_prestamo(principal, tasaAnualPct, meses)` (dominio puro) y `cotizacion_divisa(monedaBase, monedaDestino)` (REST Client).
- **Modelo de ejecución:** un `@Tool` que devuelve un tipo **no asíncrono** (`ToolResponse`, `String`, etc.) es **bloqueante por convención** y el runtime lo ejecuta fuera del event loop (worker thread). Además se usa `@io.smallrye.common.annotation.RunOnVirtualThread` (Java 21). Para async real devolver `Uni<T>`/`CompletionStage<T>` (no usado acá).
- **Modo stateless:** `quarkus.mcp.server.http.streamable.auto-init=true` → cada request crea su propia sesión MCP; no hace falta reutilizar `Mcp-Session-Id`. Ideal para Cloud Run/Postman. **Tradeoff:** sampling, roots, progress y elicitation pueden no funcionar; para esas features, quitar `auto-init` y volver al flujo `initialize` + sesión.
- **DNS rebinding:** `quarkus.mcp.server.http.dns-rebinding-check.enabled` está activo solo con `quarkus.http.host` local/ausente. En prod Quarkus bindea `0.0.0.0` (se desactiva solo); en `%dev` se desactiva explícitamente para MCP Inspector. Requests sin header `Origin` (curl/Postman) nunca se rechazan.
- **CORS:** habilitado solo en `%dev` para `http://localhost:6274,http://localhost:6277` (MCP Inspector).
- **Traffic logging:** `%dev.quarkus.mcp.server.traffic-logging.enabled=true` (muestra los JSON-RPC en vivo).

### Probar el server

```bash
curl -s -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

- Postman: `postman/mcp-serverless-demo.postman_collection.json` (variable `{{baseUrl}}`; sin manejo de sesión gracias a `auto-init`).
- MCP Inspector: `npx @modelcontextprotocol/inspector` → transporte Streamable HTTP → `http://localhost:8080/mcp`.
- Cliente real (Claude/Cursor): `{"type":"http","url":"https://<servicio>-<hash>.a.run.app/mcp"}`.

## External services

- **open.er-api.com** (cotizaciones, sin API key; free tier ~1500 req/mes; tasas actualizadas ~1 vez al día): endpoint canónico `https://open.er-api.com`, path `GET /v6/latest/{base}`. Respuesta `{result, base_code, time_last_update_utc, rates:{EUR, PEN, ...}}`; la target se toma del map `rates`. Date de referencia = `time_last_update_utc`.
- Cuidado: open.er-api devuelve **HTTP 200** con `result:"error"` + `error-type:"unsupported-code"` para base no soportada (y la target ausente en `rates` si no existe). El adapter verifica `result == "success"` y `rates.get(target) != null`.
- `CurrencyRateResponse` lleva `@RegisterForReflection` para deserializar en nativa. `quarkus.jackson.fail-on-unknown-properties=false`.

## Deployment

- CI/CD: `.github/workflows/deploy.yml` (push a `main` / `workflow_dispatch`) → auth GCP por **Workload Identity Federation** → `docker build -f docker/Dockerfile.native` → push a Artifact Registry → `deploy-cloudrun`.
- Secrets requeridos: `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT`. Los imprime `scripts/gcp-setup.sh` (requiere `export GITHUB_REPO=owner/repo`).
- Cloud Run flags: `--allow-unauthenticated --port=8080 --cpu=1 --memory=512Mi --min-instances=0 --max-instances=3 --timeout=300`.
- La demo es **pública**; para producción proteger `/mcp` con bearer/OIDC (`quarkus-oidc`) y CORS restringido. Nunca commitear credenciales.

## Conventions / gotchas

- Java **records** para `domain/model`; Lombok (`@RequiredArgsConstructor`, `@Inject` manual donde haga falta) solo en `application`/`infrastructure`. `lombok` está en `provided` y como `annotationProcessorPath`.
- `-parameters` está activo en `maven-compiler-plugin`: los nombres de argumentos de los `@Tool` salen de los parámetros Java y deben coincidir con el JSON Schema que consume Postman.
- Un `@Tool` con `@RestClient` bloqueante debe seguir siendo no-asíncrono (para que corra en worker/virtual thread). No cambiar a `Uni` sin revisar el modo imperativo.
- DTOs de REST Client que se deserialicen en nativa requieren `@RegisterForReflection`.
- Sin tests: no reintroducir `quarkus-junit`/`rest-assured` ni `src/test` sin pedido explícito.
- Docker assets viven en `docker/` (no en `src/main/docker`); el `.dockerignore` por Dockerfile es el que permite incluir `src`/`pom.xml`/`mvnw` en el build.
